package nextstep.subway.common.config.datasource;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.connection.lookup.AbstractRoutingConnectionFactory;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
public class DynamicRoutingConnectionFactory extends AbstractRoutingConnectionFactory {
    private static final String MASTER = "master";
    private static final String SLAVE = "slave";

    public DynamicRoutingConnectionFactory(ConnectionFactory masterConnectionFactory,
                                           ConnectionFactory slaveConnectionFactory) {
        super();
        setTargetConnectionFactories(Map.of(MASTER, masterConnectionFactory, SLAVE, slaveConnectionFactory));
        setDefaultTargetConnectionFactory(masterConnectionFactory);
        afterPropertiesSet();
    }

    @Override
    protected Mono<Object> determineCurrentLookupKey() {
        return TransactionSynchronizationManager.forCurrentTransaction()
                .map(synchronizationManager -> {
                    log.debug("synchronizationManager.isActualTransactionActive(): {}", synchronizationManager.isActualTransactionActive());
                    log.debug("synchronizationManager.isSynchronizationActive(): {}", synchronizationManager.isSynchronizationActive());
                    log.debug("synchronizationManager.isCurrentTransactionReadOnly(): {}", synchronizationManager.isCurrentTransactionReadOnly());
                    if (synchronizationManager.isActualTransactionActive() && synchronizationManager.isCurrentTransactionReadOnly()) {
                        log.debug("RoutingConnectionFactory: {}", SLAVE);
                        return SLAVE;
                    }
                    log.debug("RoutingConnectionFactory: {}", MASTER);
                    return MASTER;
                });
    }
}
