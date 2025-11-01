package dao;

import entities.Producto;

import java.sql.Connection;
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

    private static final String INSERT_SQL = "INSERT INTO productos (nombre, descripcion, precio, eliminado) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE productos SET nombre = ?, descripcion = ?, precio = ?, eliminado = ? WHERE id = ?";
    private static final String DELETE_SQL = "UPDATE productos SET eliminado = true WHERE id = ?";
    private static final String SELECT_BY_ID_SQL = "SELECT id, nombre, descripcion, precio, eliminado FROM productos WHERE id = ?";
    private static final String SELECT_ALL_SQL = "SELECT id, nombre, descripcion, precio, eliminado FROM productos";
    private static final String SELECT_BY_NAME_SQL = "SELECT id, nombre, descripcion, precio, eliminado FROM productos WHERE LOWER(nombre) LIKE ?";

    @Override
    public Producto save(Connection connection, Producto entity) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, entity.getNombre());
            statement.setString(2, entity.getDescripcion());
            statement.setBigDecimal(3, entity.getPrecio());
            statement.setBoolean(4, entity.isEliminado());
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
            statement.setBigDecimal(3, entity.getPrecio());
            statement.setBoolean(4, entity.isEliminado());
            statement.setLong(5, entity.getId());
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
        producto.setPrecio(resultSet.getBigDecimal("precio"));
        producto.setEliminado(resultSet.getBoolean("eliminado"));
        return producto;
    }
}
