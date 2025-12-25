package com.oregonmarkets.domain.blockchain.service;

import com.oregonmarkets.common.exception.BlockchainException;
import com.oregonmarkets.domain.blockchain.model.BlockchainChain;
import com.oregonmarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonmarkets.domain.enclave.model.EnclaveChainAddress;
import com.oregonmarkets.domain.enclave.repository.EnclaveChainAddressRepository;
import com.oregonmarkets.domain.payment.model.Deposit;
import com.oregonmarkets.domain.payment.repository.DepositRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositScannerService {

  private final BlockchainChainRepository chainRepository;
  private final EnclaveChainAddressRepository addressRepository;
  private final DepositRepository depositRepository;

  private final Map<UUID, Web3j> web3jClients = new HashMap<>();

  public Mono<Void> scanChainForDeposits(UUID chainId) {
    return chainRepository
        .findById(chainId)
        .filter(BlockchainChain::getIsActive)
        .flatMap(this::scanChain)
        .then();
  }

  private Mono<Void> scanChain(BlockchainChain chain) {
    return Mono.fromCallable(
            () -> {
              try {
                Web3j web3j = getWeb3jClient(chain);

                // Get current block
                BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
                BigInteger fromBlock = BigInteger.valueOf(chain.getLastScannedBlock() + 1);
                BigInteger toBlock = fromBlock.add(BigInteger.valueOf(100));

                if (toBlock.compareTo(currentBlock) > 0) {
                  toBlock = currentBlock;
                }

                if (fromBlock.compareTo(toBlock) > 0) {
                  return null;
                }

                log.info(
                    "Scanning chain {} from block {} to {}",
                    chain.getChainName(),
                    fromBlock,
                    toBlock);

                // Scan for USDC transfers
                scanUsdcTransfers(web3j, chain, fromBlock, toBlock);

                // Update last scanned block
                chain.setLastScannedBlock(toBlock.longValue());
                chainRepository.save(chain).subscribe();

                return null;
              } catch (Exception e) {
                log.error("Error scanning chain {}: {}", chain.getChainName(), e.getMessage());
                throw new BlockchainException("Failed to scan chain " + chain.getChainName(), e);
              }
            })
        .then();
  }

  private void scanUsdcTransfers(
      Web3j web3j, BlockchainChain chain, BigInteger fromBlock, BigInteger toBlock)
      throws Exception {

    // Create filter for USDC Transfer events
    EthFilter filter =
        new EthFilter(
            DefaultBlockParameter.valueOf(fromBlock),
            DefaultBlockParameter.valueOf(toBlock),
            chain.getUsdcTokenAddress());

    // Transfer event signature
    filter.addSingleTopic("0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef");

    EthLog ethLog = web3j.ethGetLogs(filter).send();
    List<EthLog.LogResult> logs = ethLog.getLogs();

    log.info("Found {} Transfer logs for chain {}", logs.size(), chain.getChainName());

    for (EthLog.LogResult logResult : logs) {
      try {
        EthLog.LogObject logObject = (EthLog.LogObject) logResult.get();
        processTransferLog(web3j, chain, logObject);
      } catch (Exception e) {
        log.error("Error processing log: {}", e.getMessage());
      }
    }
  }

  private void processTransferLog(Web3j web3j, BlockchainChain chain, EthLog.LogObject log)
      throws Exception {

    List<String> topics = log.getTopics();
    if (topics.size() < 3) return;

    // Extract addresses
    String toAddress = "0x" + topics.get(2).substring(26);

    // Check if it's one of our monitored addresses
    addressRepository
        .findByDepositAddress(toAddress)
        .filter(addr -> addr.getChainType().name().toLowerCase().equals(chain.getChainName()))
        .flatMap(
            monitoredAddr -> {
              try {
                return createDepositEvent(web3j, chain, log, monitoredAddr);
              } catch (Exception e) {
                DepositScannerService.log.error("Error creating deposit event: {}", e.getMessage());
                return Mono.empty();
              }
            })
        .subscribe();
  }

  private Mono<Deposit> createDepositEvent(
      Web3j web3j, BlockchainChain chain, EthLog.LogObject log, EnclaveChainAddress monitoredAddr)
      throws Exception {

    String txHash = log.getTransactionHash();

    // Check if already processed
    return depositRepository
        .findByTxHash(txHash)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  try {
                    // Parse amount
                    String data = log.getData();
                    BigInteger rawAmount = new BigInteger(data.substring(2), 16);
                    BigDecimal amount =
                        new BigDecimal(rawAmount)
                            .divide(BigDecimal.TEN.pow(chain.getUsdcDecimals()));

                    // Get transaction details
                    EthTransaction ethTx = web3j.ethGetTransactionByHash(txHash).send();
                    if (ethTx.getTransaction().isEmpty()) {
                      return Mono.empty();
                    }

                    // Create deposit
                    Deposit deposit =
                        Deposit.builder()
                            .userId(monitoredAddr.getUserId())
                            .amount(amount)
                            .currency("USDC")
                            .method(Deposit.DepositMethod.CRYPTO)
                            .status(Deposit.DepositStatus.PENDING)
                            .processingStatus(Deposit.ProcessingStatus.DETECTED)
                            .txHash(txHash)
                            .chainId(chain.getChainId())
                            .toAddress(monitoredAddr.getDepositAddress())
                            .tokenAddress(chain.getUsdcTokenAddress())
                            .rawAmount(rawAmount.toString())
                            .blockNumber(log.getBlockNumber().longValue())
                            .confirmations(0)
                            .requiredConfirmations(chain.getRequiredConfirmations())
                            .build();

                    return depositRepository
                        .save(deposit)
                        .doOnSuccess(
                            saved ->
                                DepositScannerService.log.info(
                                    "Detected deposit: {} USDC to user {} (tx: {})",
                                    amount,
                                    monitoredAddr.getUserId(),
                                    txHash));

                  } catch (Exception e) {
                    DepositScannerService.log.error("Error creating deposit: {}", e.getMessage());
                    return Mono.empty();
                  }
                }));
  }

  private Web3j getWeb3jClient(BlockchainChain chain) {
    return web3jClients.computeIfAbsent(
        chain.getId(), id -> Web3j.build(new HttpService(chain.getRpcUrl())));
  }
}
