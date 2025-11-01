package dao;

import entities.CodigoBarras;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para manejar la persistencia de códigos de barras.
 */
public class CodigoBarrasDao implements GenericDao<CodigoBarras> {

    private static final String INSERT_SQL = "INSERT INTO codigos_barras (producto_id, codigo, eliminado) VALUES (?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE codigos_barras SET producto_id = ?, codigo = ?, eliminado = ? WHERE id = ?";
    private static final String DELETE_SQL = "UPDATE codigos_barras SET eliminado = true WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT id, producto_id, codigo, eliminado FROM codigos_barras WHERE id = ?";
    private static final String SELECT_BY_PRODUCTO_SQL = "SELECT id, producto_id, codigo, eliminado FROM codigos_barras WHERE producto_id = ?";
    private static final String SELECT_BY_CODIGO_SQL = "SELECT id, producto_id, codigo, eliminado FROM codigos_barras WHERE codigo = ?";
    private static final String SELECT_ALL_SQL = "SELECT id, producto_id, codigo, eliminado FROM codigos_barras";

    @Override
    public CodigoBarras save(Connection connection, CodigoBarras entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, entity.getProductoId());
            statement.setString(2, entity.getCodigo());
            statement.setBoolean(3, entity.isEliminado());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        }
    }

    @Override
    public CodigoBarras update(Connection connection, CodigoBarras entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, entity.getProductoId());
            statement.setString(2, entity.getCodigo());
            statement.setBoolean(3, entity.isEliminado());
            statement.setLong(4, entity.getId());
            statement.executeUpdate();
            return entity;
        }
    }

    @Override
    public void deleteById(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<CodigoBarras> findById(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCodigoBarras(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<CodigoBarras> findAll(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<CodigoBarras> codigos = new ArrayList<>();
            while (resultSet.next()) {
                codigos.add(mapCodigoBarras(resultSet));
            }
            return codigos;
        }
    }

    public Optional<CodigoBarras> findByProductoId(Connection connection, Long productoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_PRODUCTO_SQL)) {
            statement.setLong(1, productoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCodigoBarras(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<CodigoBarras> findByCodigo(Connection connection, String codigo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_CODIGO_SQL)) {
            statement.setString(1, codigo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCodigoBarras(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    private CodigoBarras mapCodigoBarras(ResultSet resultSet) throws SQLException {
        CodigoBarras codigoBarras = new CodigoBarras();
        codigoBarras.setId(resultSet.getLong("id"));
        codigoBarras.setProductoId(resultSet.getLong("producto_id"));
        codigoBarras.setCodigo(resultSet.getString("codigo"));
        codigoBarras.setEliminado(resultSet.getBoolean("eliminado"));
        return codigoBarras;
    }
}
