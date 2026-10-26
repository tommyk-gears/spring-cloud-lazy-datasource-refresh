package com.example.repro;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.health.RefreshScopeHealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A config refresh should leave the DataSource usable, with and without lazy connection fetching.
 * <p>
 * The assertions are soft, so a failing run reports every effect of the refresh at once.
 */
class LazyDataSourceRefreshTest {

    /**
     * Fails: the refresh throws, every later query fails with {@code No 'targetDataSource' set}, and
     * the {@code refreshScope} health indicator is DOWN.
     */
    @Nested
    @SpringBootTest(properties = "spring.datasource.connection-fetch=lazy")
    class Lazy extends RefreshScenario {
    }

    /**
     * Passes: the same refresh with eager connection fetching.
     */
    @Nested
    @SpringBootTest(properties = "spring.datasource.connection-fetch=eager")
    class Eager extends RefreshScenario {
    }

    abstract static class RefreshScenario {

        @Autowired
        ContextRefresher contextRefresher;

        @Autowired
        RefreshScopeHealthIndicator refreshScopeHealth;

        @Autowired
        JdbcTemplate jdbcTemplate;

        @Test
        void dataSourceIsUsableAfterRefresh() {
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(query()).as("query before the refresh").isEqualTo(1);

            softly.assertThatCode(this.contextRefresher::refresh).as("refresh").doesNotThrowAnyException();

            softly.assertThatCode(this::query).as("query after the refresh").doesNotThrowAnyException();
            softly.assertThat(this.refreshScopeHealth.health().getStatus())
                    .as("refreshScope health after the refresh")
                    .isEqualTo(Status.UP);
            softly.assertAll();
        }

        private Integer query() {
            return this.jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        }
    }
}
