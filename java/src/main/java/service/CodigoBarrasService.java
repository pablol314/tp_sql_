package service;

import config.DatabaseConnection;
import dao.CodigoBarrasDao;
import entities.CodigoBarras;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio para códigos de barras.
 */
public class CodigoBarrasService implements GenericService<CodigoBarras> {

    private final DatabaseConnection databaseConnection;
    private final CodigoBarrasDao codigoBarrasDao;

    public CodigoBarrasService(DatabaseConnection databaseConnection, CodigoBarrasDao codigoBarrasDao) {
        this.databaseConnection = databaseConnection;
        this.codigoBarrasDao = codigoBarrasDao;
    }

    @Override
    public CodigoBarras create(CodigoBarras entity) {
        validate(entity);
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            CodigoBarras saved = codigoBarrasDao.save(connection, entity);
            connection.commit();
            return saved;
        } catch (SQLException e) {
            throw new ServiceException("No se pudo crear el código de barras", e);
        }
    }

    @Override
    public CodigoBarras update(CodigoBarras entity) {
        validate(entity);
        if (entity.getId() == null) {
            throw new IllegalArgumentException("El id del código de barras es obligatorio para actualizar");
        }
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            CodigoBarras updated = codigoBarrasDao.update(connection, entity);
            connection.commit();
            return updated;
        } catch (SQLException e) {
            throw new ServiceException("No se pudo actualizar el código de barras", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            codigoBarrasDao.deleteById(connection, id);
            connection.commit();
        } catch (SQLException e) {
            throw new ServiceException("No se pudo eliminar el código de barras", e);
        }
    }

    @Override
    public Optional<CodigoBarras> findById(Long id) {
        try (Connection connection = databaseConnection.getConnection()) {
            return codigoBarrasDao.findById(connection, id);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el código de barras", e);
        }
    }

    public Optional<CodigoBarras> findByProductoId(Long productoId) {
        try (Connection connection = databaseConnection.getConnection()) {
            return codigoBarrasDao.findByProductoId(connection, productoId);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el código de barras por producto", e);
        }
    }

    public Optional<CodigoBarras> findByCodigo(String codigo) {
        try (Connection connection = databaseConnection.getConnection()) {
            return codigoBarrasDao.findByCodigo(connection, codigo);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el código de barras por código", e);
        }
    }

    @Override
    public List<CodigoBarras> findAll() {
        try (Connection connection = databaseConnection.getConnection()) {
            return codigoBarrasDao.findAll(connection);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo listar los códigos de barras", e);
        }
    }

    private void validate(CodigoBarras codigoBarras) {
        if (codigoBarras.getProductoId() == null) {
            throw new IllegalArgumentException("El código de barras debe estar asociado a un producto");
        }
        if (codigoBarras.getCodigo() == null || codigoBarras.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El valor del código de barras es obligatorio");
        }
    }
}
