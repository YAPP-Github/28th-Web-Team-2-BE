package com.looky.api.support.spring;

import com.looky.api.LookyApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = LookyApiApplication.class)
@ActiveProfiles("local")
class FlywayMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void appliesBaselineAndQuestionSeedMigrationsOnTheLocalProfile() throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     select
                         count(*) as total_questions,
                         sum(case when active = true then 1 else 0 end) as active_questions,
                         sum(case when active = true and content_self_template is not null then 1 else 0 end) as templated_active_questions
                     from questions
                     """);
             var resultSet = statement.executeQuery()) {
            resultSet.next();

            assertEquals(80, resultSet.getInt("total_questions"));
            assertEquals(80, resultSet.getInt("active_questions"));
            assertEquals(80, resultSet.getInt("templated_active_questions"));
        }

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     select trait_code, count(*) as active_count
                     from questions
                     where active = true
                     group by trait_code
                     """);
             var resultSet = statement.executeQuery()) {
            int traitCount = 0;
            while (resultSet.next()) {
                assertEquals(20, resultSet.getInt("active_count"));
                traitCount++;
            }
            assertEquals(4, traitCount);
        }

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("select count(*) from answer_options where active = true");
             var resultSet = statement.executeQuery()) {
            resultSet.next();

            assertEquals(400, resultSet.getInt(1));
        }
    }
}
