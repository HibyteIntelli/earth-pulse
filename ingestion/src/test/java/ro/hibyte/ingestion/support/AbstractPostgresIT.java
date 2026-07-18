package ro.hibyte.ingestion.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

@ActiveProfiles("test")
public abstract class AbstractPostgresIT {
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.10");

    static {
        postgres.start();
    }
}
