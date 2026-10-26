package com.example.repro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ReproApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReproApplication.class, args);
    }

    @RestController
    static class QueryController {

        private final JdbcTemplate jdbcTemplate;

        QueryController(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @GetMapping("/query")
        Integer query() {
            return this.jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        }
    }
}
