package com.looky.api.support.spring;

import com.looky.api.LookyApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = LookyApiApplication.class)
class FlywayMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void appliesBaselineAndQuestionSeedMigrationsOnTheDefaultProfile() throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("select count(*) from questions");
             var resultSet = statement.executeQuery()) {
            resultSet.next();

            assertEquals(200, resultSet.getInt(1));
        }
    }
}
