package com.oregonMarkets.domain.blockchain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.oregonMarkets.domain.blockchain.model.BlockchainChain;
import com.oregonMarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonMarkets.domain.enclave.model.EnclaveChainAddress;
import com.oregonMarkets.domain.enclave.repository.EnclaveChainAddressRepository;
import com.oregonMarkets.domain.payment.model.Deposit;
import com.oregonMarkets.domain.payment.repository.DepositRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.*;
import reactor.core.publisher.Mono;

class DepositScannerServiceTest {

  @Mock private BlockchainChainRepository chainRepository;
  @Mock private EnclaveChainAddressRepository addressRepository;
  @Mock private DepositRepository depositRepository;
  @Mock private Web3j web3j;

  private DepositScannerService service;
  private BlockchainChain chain;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    try (var ignored = MockitoAnnotations.openMocks(this)) {
      service = new DepositScannerService(chainRepository, addressRepository, depositRepository);

      chain =
          BlockchainChain.builder()
              .id(UUID.randomUUID())
              .chainName("polygon")
              .rpcUrl("http://localhost:8545")
              .usdcTokenAddress("0xToken")
              .usdcDecimals(6)
              .requiredConfirmations(12)
              .lastScannedBlock(0L)
              .isActive(true)
              .build();

      ReflectionTestUtils.setField(
          service, "web3jClients", new java.util.concurrent.ConcurrentHashMap<>());
      @SuppressWarnings("unchecked")
      Map<UUID, Web3j> web3Map =
          (Map<UUID, Web3j>) ReflectionTestUtils.getField(service, "web3jClients");
      web3Map.put(chain.getId(), web3j);

      when(chainRepository.findById(chain.getId())).thenReturn(Mono.just(chain));
      when(chainRepository.save(chain)).thenReturn(Mono.just(chain));

      mockBlockNumberResponse();
      mockLogResponse();
      mockTransactionResponse();

      when(addressRepository.findByDepositAddress("0xabc0000000000000000000000000000000000000"))
          .thenReturn(
              Mono.just(
                  EnclaveChainAddress.builder()
                      .id(UUID.randomUUID())
                      .userId(UUID.randomUUID())
                      .chainType(EnclaveChainAddress.ChainType.POLYGON)
                      .depositAddress("0xabc0000000000000000000000000000000000000")
                      .build()));

      when(depositRepository.findByTxHash("0xdeadbeef")).thenReturn(Mono.empty());
      when(depositRepository.save(any(Deposit.class)))
          .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private void mockBlockNumberResponse() throws java.io.IOException {
    EthBlockNumber blockNumber = new EthBlockNumber();
    blockNumber.setResult("0x64");

    org.web3j.protocol.core.Request<?, EthBlockNumber> mockRequest = mock(org.web3j.protocol.core.Request.class);
    doReturn(blockNumber).when(mockRequest).send();
    doReturn(mockRequest).when(web3j).ethBlockNumber();
  }

  @SuppressWarnings("unchecked")
  private void mockLogResponse() throws java.io.IOException {
    EthLog.LogObject logObject = new EthLog.LogObject();
    logObject.setData("0x00000000000000000000000000000000000000000000000000000000000f4240");
    logObject.setBlockNumber("0x10");
    logObject.setTransactionHash("0xdeadbeef");
    logObject.setTopics(
        List.of(
            "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
            "0x0000000000000000000000001234567890123456789012345678901234567890",
            "0x000000000000000000000000abc0000000000000000000000000000000000000"));

    EthLog logResponse = new EthLog();
    logResponse.setResult(List.of(logObject));

    org.web3j.protocol.core.Request<?, EthLog> mockRequest = mock(org.web3j.protocol.core.Request.class);
    doReturn(logResponse).when(mockRequest).send();
    doReturn(mockRequest).when(web3j).ethGetLogs(any(EthFilter.class));
  }

  @SuppressWarnings("unchecked")
  private void mockTransactionResponse() throws java.io.IOException {
    EthTransaction transactionResponse = new EthTransaction();
    transactionResponse.setResult(new Transaction());

    org.web3j.protocol.core.Request<?, EthTransaction> mockRequest = mock(org.web3j.protocol.core.Request.class);
    doReturn(transactionResponse).when(mockRequest).send();
    doReturn(mockRequest).when(web3j).ethGetTransactionByHash("0xdeadbeef");
  }

  @Test
  void scanChainForDeposits_SavesDetectedDeposit() throws InterruptedException {
    service.scanChainForDeposits(chain.getId()).block();

    // Wait for async subscribe() operations to complete
    Thread.sleep(1000);

    ArgumentCaptor<Deposit> depositCaptor = ArgumentCaptor.forClass(Deposit.class);
    verify(depositRepository).save(depositCaptor.capture());
    Deposit saved = depositCaptor.getValue();
    assertEquals("USDC", saved.getCurrency());
    verify(chainRepository).save(chain);
  }
}

