package dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato básico para todas las operaciones CRUD de un repositorio JDBC.
 */
public interface GenericDao<T> {

    T save(Connection connection, T entity) throws SQLException;

    T update(Connection connection, T entity) throws SQLException;

    void deleteById(Connection connection, Long id) throws SQLException;

    Optional<T> findById(Connection connection, Long id) throws SQLException;

    List<T> findAll(Connection connection) throws SQLException;
}
