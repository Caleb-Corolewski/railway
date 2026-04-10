package com.heroku.java;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
        return """
                <h1>Hello World!</h1>
                <p><a href="/database">View database entries</a></p>
                <p><a href="/dbinput">Add a string to the database</a></p>
                """;
    }

    @GetMapping("/database")
    public String database() {
        System.out.println("Caleb Corolewski");

        StringBuilder html = new StringBuilder();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS table_timestamp_and_random_string (" +
                            "tick timestamp, " +
                            "random_string varchar(50))"
            );

            ResultSet resultSet = statement.executeQuery(
                    "SELECT tick, random_string " +
                            "FROM table_timestamp_and_random_string " +
                            "ORDER BY tick DESC"
            );

            html.append("<h1>Database Entries</h1>");
            html.append("<p><a href=\"/dbinput\">Add another string</a></p>");
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

    @GetMapping("/dbinput")
    public String dbinputForm() {
        return """
                <h1>Enter a String</h1>
                <form method="post" action="/dbinput">
                    <label for="userInput">String:</label>
                    <input type="text" id="userInput" name="userInput" maxlength="50" required>
                    <button type="submit">Submit</button>
                </form>
                <p><a href="/database">View database entries</a></p>
                """;
    }

    @PostMapping("/dbinput")
    public String dbinputSubmit(@RequestParam("userInput") String userInput) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS table_timestamp_and_random_string (" +
                            "tick timestamp, " +
                            "random_string varchar(50))"
            );

            String sql = "INSERT INTO table_timestamp_and_random_string (tick, random_string) VALUES (now(), ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, userInput);
                preparedStatement.executeUpdate();
            }

            System.out.println("Caleb submitted to /dbinput: " + userInput);

            return """
                    <h1>Input Saved</h1>
                    <p>Your string was added to the database.</p>
                    <p><a href="/dbinput">Add another string</a></p>
                    <p><a href="/database">View database entries</a></p>
                    """;

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