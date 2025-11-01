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
        if (entity.getProductoId() == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio para actualizar el código de barras");
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
    public void delete(Long productoId) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            codigoBarrasDao.deleteById(connection, productoId);
            connection.commit();
        } catch (SQLException e) {
            throw new ServiceException("No se pudo eliminar el código de barras", e);
        }
    }

    @Override
    public Optional<CodigoBarras> findById(Long productoId) {
        try (Connection connection = databaseConnection.getConnection()) {
            return codigoBarrasDao.findById(connection, productoId);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el código de barras", e);
        }
    }

    public Optional<CodigoBarras> findByProductoId(Long productoId) {
        return findById(productoId);
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
        if (codigoBarras.getGtin13() == null || codigoBarras.getGtin13().isBlank()) {
            throw new IllegalArgumentException("El valor GTIN13 es obligatorio");
        }
        if (codigoBarras.getTipo() == null || codigoBarras.getTipo().isBlank()) {
            throw new IllegalArgumentException("El tipo de código de barras es obligatorio");
        }
    }
}
