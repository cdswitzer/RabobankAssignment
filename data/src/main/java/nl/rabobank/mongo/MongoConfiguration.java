package nl.rabobank.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "nl.rabobank.repository")
@EnableConfigurationProperties(MongoProperties.class)
@RequiredArgsConstructor
public class MongoConfiguration extends AbstractMongoClientConfiguration {
    private final MongoProperties mongoProperties;

    @Override
    @NonNull
    protected String getDatabaseName() {
        return mongoProperties.getMongoClientDatabase();
    }

    @Override
    @NonNull
    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        return MongoClients.create(mongoProperties.determineUri());
    }
}
