package com.oregonMarkets.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.oregonMarkets.domain.blockchain.repository.BlockchainChainRepository;
import com.oregonMarkets.domain.blockchain.service.DepositScannerService;
import com.oregonMarkets.domain.enclave.repository.EnclaveChainAddressRepository;
import com.oregonMarkets.domain.market.repository.*;
import com.oregonMarkets.domain.payment.repository.DepositRepository;
import com.oregonMarkets.domain.user.repository.UserRepository;
import com.oregonMarkets.scheduler.DepositMonitoringScheduler;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;

import static org.mockito.Mockito.when;

/**
 * Test database configuration
 * Provides mocked beans for database-dependent components to avoid database connections in tests
 */
@Profile("test")
@Configuration
public class TestDatabaseConfig {

    @Bean
    public CqlSession cassandraSession() {
        return Mockito.mock(CqlSession.class);
    }

    @Bean
    public ReactiveCassandraTemplate reactiveCassandraTemplate() {
        ReactiveCassandraTemplate template = Mockito.mock(ReactiveCassandraTemplate.class);
        org.springframework.data.cassandra.core.convert.CassandraConverter converter =
            Mockito.mock(org.springframework.data.cassandra.core.convert.CassandraConverter.class);
        org.springframework.data.cassandra.core.mapping.CassandraMappingContext mappingContext =
            Mockito.mock(org.springframework.data.cassandra.core.mapping.CassandraMappingContext.class);

        when(template.getConverter()).thenReturn(converter);
        when(converter.getMappingContext()).thenReturn(mappingContext);

        return template;
    }

    @Bean("questdbConnectionFactory")
    public ConnectionFactory questdbConnectionFactory() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        ConnectionFactoryMetadata metadata = Mockito.mock(ConnectionFactoryMetadata.class);
        when(metadata.getName()).thenReturn("MockQuestDB");
        when(connectionFactory.getMetadata()).thenReturn(metadata);
        return connectionFactory;
    }

    /**
     * Provide a mocked R2dbcEntityTemplate for tests
     * This prevents DefaultR2dbcConfig from trying to create one
     * which would fail with dialect resolution error
     */
    @Primary
    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate() {
        return Mockito.mock(R2dbcEntityTemplate.class);
    }

    /**
     * Provide a mocked DatabaseClient for tests
     */
    @Bean
    public DatabaseClient databaseClient() {
        return Mockito.mock(DatabaseClient.class);
    }

    /**
     * Provide a mocked R2dbcMappingContext to prevent R2DBC auditing handler errors
     * This is needed because R2DBC auditing configuration may still try to initialize
     * even with auto-configuration excluded
     */
    @Bean
    public org.springframework.data.r2dbc.mapping.R2dbcMappingContext r2dbcMappingContext() {
        return Mockito.mock(org.springframework.data.r2dbc.mapping.R2dbcMappingContext.class);
    }

    /**
     * Mock blockchain-related repositories since R2DBC is disabled in tests
     */
    @Bean
    public BlockchainChainRepository blockchainChainRepository() {
        return Mockito.mock(BlockchainChainRepository.class);
    }

    /**
     * Mock Cassandra repositories for blockchain/payment functionality
     */
    @Bean
    public EnclaveChainAddressRepository enclaveChainAddressRepository() {
        return Mockito.mock(EnclaveChainAddressRepository.class);
    }

    @Bean
    public DepositRepository depositRepository() {
        return Mockito.mock(DepositRepository.class);
    }

    /**
     * Mock blockchain-related services to prevent initialization errors
     */
    @Bean
    public DepositScannerService depositScannerService() {
        return Mockito.mock(DepositScannerService.class);
    }

    @Bean
    public DepositMonitoringScheduler depositMonitoringScheduler() {
        return Mockito.mock(DepositMonitoringScheduler.class);
    }

    /**
     * Mock Cassandra repositories for market domain
     */
    @Bean
    public CategoryRepository categoryRepository() {
        return Mockito.mock(CategoryRepository.class);
    }

    @Bean
    public SubcategoryRepository subcategoryRepository() {
        return Mockito.mock(SubcategoryRepository.class);
    }

    @Bean
    public CountryRepository countryRepository() {
        return Mockito.mock(CountryRepository.class);
    }

    @Bean
    public LanguageRepository languageRepository() {
        return Mockito.mock(LanguageRepository.class);
    }

    @Bean
    public MarketRepository marketRepository() {
        return Mockito.mock(MarketRepository.class);
    }

    @Bean
    public OutcomeRepository outcomeRepository() {
        return Mockito.mock(OutcomeRepository.class);
    }

    @Bean
    public OrderRepository orderRepository() {
        return Mockito.mock(OrderRepository.class);
    }

    @Bean
    public TradeRepository tradeRepository() {
        return Mockito.mock(TradeRepository.class);
    }

    @Bean
    public PositionRepository positionRepository() {
        return Mockito.mock(PositionRepository.class);
    }

    /**
     * Mock user repository
     */
    @Bean
    public UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }
}
