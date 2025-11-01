package dao;

import entities.CodigoBarras;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para manejar la persistencia de códigos de barras.
 */
public class CodigoBarrasDao implements GenericDao<CodigoBarras> {

    private static final String INSERT_SQL = "INSERT INTO codigo_barras (producto_id, gtin13, tipo, activo) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE codigo_barras SET gtin13 = ?, tipo = ?, activo = ? WHERE producto_id = ?";
    private static final String DELETE_SQL = "UPDATE codigo_barras SET activo = false WHERE producto_id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT producto_id, gtin13, tipo, activo FROM codigo_barras WHERE producto_id = ?";
    private static final String SELECT_BY_PRODUCTO_SQL = SELECT_BY_ID_SQL;
    private static final String SELECT_BY_CODIGO_SQL = "SELECT producto_id, gtin13, tipo, activo FROM codigo_barras WHERE gtin13 = ?";
    private static final String SELECT_ALL_SQL = "SELECT producto_id, gtin13, tipo, activo FROM codigo_barras";

    @Override
    public CodigoBarras save(Connection connection, CodigoBarras entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setLong(1, entity.getProductoId());
            statement.setString(2, entity.getGtin13());
            statement.setString(3, entity.getTipo());
            statement.setBoolean(4, entity.isActivo());
            statement.executeUpdate();
            return entity;
        }
    }

    @Override
    public CodigoBarras update(Connection connection, CodigoBarras entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, entity.getGtin13());
            statement.setString(2, entity.getTipo());
            statement.setBoolean(3, entity.isActivo());
            statement.setLong(4, entity.getProductoId());
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
        codigoBarras.setProductoId(resultSet.getLong("producto_id"));
        codigoBarras.setGtin13(resultSet.getString("gtin13"));
        codigoBarras.setTipo(resultSet.getString("tipo"));
        codigoBarras.setActivo(resultSet.getBoolean("activo"));
        return codigoBarras;
    }
}
