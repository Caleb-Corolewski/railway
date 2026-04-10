package com.heroku.java;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;

@RestController
@SpringBootApplication
public class GettingStartedApplication {

    @Autowired
    private DataSource dataSource;

    public static void main(String[] args) {
        SpringApplication.run(GettingStartedApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Hello World!";
    }

    @GetMapping("/database")
    public String database() {
        StringBuilder html = new StringBuilder();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS table_timestamp_and_random_string (" +
                            "tick timestamp, " +
                            "random_string varchar(50))"
            );

            statement.executeUpdate(
                    "INSERT INTO table_timestamp_and_random_string VALUES " +
                            "(now(), '" + getRandomString() + "')"
            );

            ResultSet resultSet = statement.executeQuery(
                    "SELECT tick, random_string " +
                            "FROM table_timestamp_and_random_string " +
                            "ORDER BY tick DESC"
            );

            html.append("<h1>Database Entries</h1>");
            html.append("<ul>");

            while (resultSet.next()) {
                html.append("<li>")
                        .append(resultSet.getTimestamp("tick"))
                        .append(" | ")
                        .append(resultSet.getString("random_string"))
                        .append("</li>");
            }

            html.append("</ul>");
            resultSet.close();

            System.out.println("Caleb accessed /database");

            return html.toString();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String getRandomString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            randomString.append(chars.charAt(random.nextInt(chars.length())));
        }

        return randomString.toString();
    }
}