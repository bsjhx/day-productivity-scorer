package com.bsjhx.dayproductivityscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJdbcRepositories
@EnableTransactionManagement
public class DayProductivityScoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayProductivityScoreApplication.class, args);
    }

}
