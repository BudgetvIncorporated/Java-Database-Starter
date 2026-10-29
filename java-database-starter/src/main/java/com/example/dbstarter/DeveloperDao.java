package com.example.dbstarter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DeveloperDao {
    private final Connection connection;

    public DeveloperDao(Connection connection) {
        this.connection = connection;
    }

    public List<Developer> findAllDevelopers() throws SQLException {
        String sql = """
                SELECT developer_id, name, website_url
                FROM developers
                ORDER BY name COLLATE NOCASE
                """;
        List<Developer> developers = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                developers.add(readDeveloper(resultSet));
            }
        }
        return developers;
    }

    public Optional<Developer> findDeveloperById(int developerId) throws SQLException {
        String sql = """
                SELECT developer_id, name, website_url
                FROM developers
                WHERE developer_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, developerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next()
                        ? Optional.of(readDeveloper(resultSet))
                        : Optional.empty();
            }
        }
    }

    public int createDeveloper(Developer developer) throws SQLException {
        String sql = "INSERT INTO developers (name, website_url) VALUES (?, ?)";
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, developer.name());
            statement.setString(2, developer.websiteUrl());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        try (PreparedStatement statement =
                     connection.prepareStatement("SELECT last_insert_rowid()");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        throw new SQLException("The database did not return the new developer ID.");
    }

    public boolean updateDeveloper(Developer developer) throws SQLException {
        String sql = """
                UPDATE developers
                SET name = ?, website_url = ?
                WHERE developer_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, developer.name());
            statement.setString(2, developer.websiteUrl());
            statement.setInt(3, developer.id());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deleteDeveloper(int developerId) throws SQLException {
        String sql = "DELETE FROM developers WHERE developer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, developerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().contains("FOREIGN KEY constraint failed")) {
                throw new SQLException(
                        "Cannot delete this developer because one or more games still use it.",
                        exception);
            }
            throw exception;
        }
    }

    private Developer readDeveloper(ResultSet resultSet) throws SQLException {
        return new Developer(
                resultSet.getInt("developer_id"),
                resultSet.getString("name"),
                resultSet.getString("website_url"));
    }
}
