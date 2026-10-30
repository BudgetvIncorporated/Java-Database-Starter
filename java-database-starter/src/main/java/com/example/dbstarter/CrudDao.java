package com.example.dbstarter;

import java.sql.SQLException;
import java.util.Optional;

public interface CrudDao<T> {
    int create(T item) throws SQLException;

    Optional<T> findById(int id) throws SQLException;

    boolean update(T item) throws SQLException;

    boolean delete(int id) throws SQLException;
}
