package dao;

import entities.CodigoBarras;
import entities.Producto;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO responsable de persistir los productos.
 */
public class ProductoDao implements GenericDao<Producto> {

    private static final String INSERT_SQL = "INSERT INTO producto (nombre, descripcion, categoria_id, marca_id, precio, costo, stock, fecha_alta, eliminado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE producto SET nombre = ?, descripcion = ?, categoria_id = ?, marca_id = ?, precio = ?, costo = ?, stock = ?, fecha_alta = ?, eliminado = ? WHERE id = ?";
    private static final String DELETE_SQL = "UPDATE producto SET eliminado = true WHERE id = ?";
    private static final String SELECT_BASE_SQL = "SELECT p.id, p.nombre, p.descripcion, p.categoria_id, p.marca_id, p.precio, p.costo, p.stock, p.fecha_alta, p.eliminado, cb.producto_id AS cb_producto_id, cb.gtin13, cb.tipo, cb.activo FROM producto p LEFT JOIN codigo_barras cb ON cb.producto_id = p.id";
    private static final String SELECT_BY_ID_SQL = SELECT_BASE_SQL + " WHERE p.id = ?";
    private static final String SELECT_ALL_SQL = SELECT_BASE_SQL;
    private static final String SELECT_BY_NAME_SQL = SELECT_BASE_SQL + " WHERE LOWER(p.nombre) LIKE ?";

    @Override
    public Producto save(Connection connection, Producto entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entity.getNombre());
            statement.setString(2, entity.getDescripcion());
            if (entity.getCategoriaId() != null) {
                statement.setLong(3, entity.getCategoriaId());
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            if (entity.getMarcaId() != null) {
                statement.setLong(4, entity.getMarcaId());
            } else {
                statement.setNull(4, java.sql.Types.BIGINT);
            }
            statement.setBigDecimal(5, entity.getPrecio());
            if (entity.getCosto() != null) {
                statement.setBigDecimal(6, entity.getCosto());
            } else {
                statement.setNull(6, java.sql.Types.DECIMAL);
            }
            if (entity.getStock() != null) {
                statement.setInt(7, entity.getStock());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            if (entity.getFechaAlta() != null) {
                statement.setDate(8, Date.valueOf(entity.getFechaAlta()));
            } else {
                statement.setNull(8, java.sql.Types.DATE);
            }
            statement.setBoolean(9, entity.isEliminado());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    entity.setId(generatedKeys.getLong(1));
                }
            }
            return entity;
        }
    }

    @Override
    public Producto update(Connection connection, Producto entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, entity.getNombre());
            statement.setString(2, entity.getDescripcion());
            if (entity.getCategoriaId() != null) {
                statement.setLong(3, entity.getCategoriaId());
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            if (entity.getMarcaId() != null) {
                statement.setLong(4, entity.getMarcaId());
            } else {
                statement.setNull(4, java.sql.Types.BIGINT);
            }
            statement.setBigDecimal(5, entity.getPrecio());
            if (entity.getCosto() != null) {
                statement.setBigDecimal(6, entity.getCosto());
            } else {
                statement.setNull(6, java.sql.Types.DECIMAL);
            }
            if (entity.getStock() != null) {
                statement.setInt(7, entity.getStock());
            } else {
                statement.setNull(7, java.sql.Types.INTEGER);
            }
            if (entity.getFechaAlta() != null) {
                statement.setDate(8, Date.valueOf(entity.getFechaAlta()));
            } else {
                statement.setNull(8, java.sql.Types.DATE);
            }
            statement.setBoolean(9, entity.isEliminado());
            statement.setLong(10, entity.getId());
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
    public Optional<Producto> findById(Connection connection, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProducto(resultSet));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Producto> findAll(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<Producto> productos = new ArrayList<>();
            while (resultSet.next()) {
                productos.add(mapProducto(resultSet));
            }
            return productos;
        }
    }

    public List<Producto> findByNombre(Connection connection, String nombre) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_BY_NAME_SQL)) {
            statement.setString(1, "%" + nombre.toLowerCase() + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Producto> productos = new ArrayList<>();
                while (resultSet.next()) {
                    productos.add(mapProducto(resultSet));
                }
                return productos;
            }
        }
    }

    private Producto mapProducto(ResultSet resultSet) throws SQLException {
        Producto producto = new Producto();
        producto.setId(resultSet.getLong("id"));
        producto.setNombre(resultSet.getString("nombre"));
        producto.setDescripcion(resultSet.getString("descripcion"));
        long categoria = resultSet.getLong("categoria_id");
        producto.setCategoriaId(resultSet.wasNull() ? null : categoria);
        long marca = resultSet.getLong("marca_id");
        producto.setMarcaId(resultSet.wasNull() ? null : marca);
        producto.setPrecio(resultSet.getBigDecimal("precio"));
        producto.setCosto(resultSet.getBigDecimal("costo"));
        int stock = resultSet.getInt("stock");
        producto.setStock(resultSet.wasNull() ? null : stock);
        Date fechaAlta = resultSet.getDate("fecha_alta");
        producto.setFechaAlta(fechaAlta != null ? fechaAlta.toLocalDate() : null);
        producto.setEliminado(resultSet.getBoolean("eliminado"));

        Long cbProductoId = resultSet.getObject("cb_producto_id", Long.class);
        if (cbProductoId != null) {
            CodigoBarras codigoBarras = new CodigoBarras();
            codigoBarras.setProductoId(cbProductoId);
            codigoBarras.setGtin13(resultSet.getString("gtin13"));
            codigoBarras.setTipo(resultSet.getString("tipo"));
            codigoBarras.setActivo(resultSet.getBoolean("activo"));
            producto.setCodigoBarras(codigoBarras);
        }
        return producto;
    }
}
