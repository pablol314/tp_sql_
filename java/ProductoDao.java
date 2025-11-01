import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductoDao {
    private final DatabaseConfig config;

    public ProductoDao(DatabaseConfig config) {
        this.config = config;
    }

    public Producto crear(Producto producto) throws SQLException {
        String insertProducto = "INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, eliminado) " +
                "VALUES (?, ?, ?, ?, ?, ?, FALSE)";
        String insertCodigo = "INSERT INTO codigo_barras (producto_id, gtin13) VALUES (?, ?)";
        try (Connection conn = config.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(insertProducto, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, producto.getNombre());
                stmt.setLong(2, producto.getCategoriaId());
                stmt.setLong(3, producto.getMarcaId());
                stmt.setDouble(4, producto.getPrecio());
                stmt.setDouble(5, producto.getCosto());
                stmt.setInt(6, producto.getStock());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        producto.setId(id);
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(insertCodigo)) {
                stmt.setLong(1, producto.getId());
                stmt.setString(2, producto.getCodigoBarras());
                stmt.executeUpdate();
            }
            conn.commit();
            return producto;
        }
    }

    public List<Producto> listarActivos() throws SQLException {
        String sql = "SELECT p.id, p.nombre, p.precio, p.costo, p.stock, c.nombre AS categoria, m.nombre AS marca, cb.gtin13, p.eliminado " +
                "FROM producto p " +
                "JOIN categoria c ON p.categoria_id = c.id " +
                "JOIN marca m ON p.marca_id = m.id " +
                "JOIN codigo_barras cb ON cb.producto_id = p.id " +
                "WHERE p.eliminado = FALSE ORDER BY p.nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                productos.add(mapRow(rs));
            }
        }
        return productos;
    }

    public Producto buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT p.id, p.nombre, p.precio, p.costo, p.stock, c.nombre AS categoria, m.nombre AS marca, cb.gtin13, p.eliminado " +
                "FROM producto p " +
                "JOIN categoria c ON p.categoria_id = c.id " +
                "JOIN marca m ON p.marca_id = m.id " +
                "JOIN codigo_barras cb ON cb.producto_id = p.id " +
                "WHERE cb.gtin13 = ?";
        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Producto> buscarPorNombre(String termino) throws SQLException {
        String sql = "SELECT p.id, p.nombre, p.precio, p.costo, p.stock, c.nombre AS categoria, m.nombre AS marca, cb.gtin13, p.eliminado " +
                "FROM producto p " +
                "JOIN categoria c ON p.categoria_id = c.id " +
                "JOIN marca m ON p.marca_id = m.id " +
                "JOIN codigo_barras cb ON cb.producto_id = p.id " +
                "WHERE p.eliminado = FALSE AND p.nombre LIKE ? ORDER BY p.nombre";
        List<Producto> productos = new ArrayList<>();
        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + termino + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapRow(rs));
                }
            }
        }
        return productos;
    }

    public boolean actualizarPrecioStock(long id, double precio, int stock) throws SQLException {
        String sql = "UPDATE producto SET precio = ?, stock = ? WHERE id = ? AND eliminado = FALSE";
        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, precio);
            stmt.setInt(2, stock);
            stmt.setLong(3, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean bajaLogica(long id) throws SQLException {
        String sql = "UPDATE producto SET eliminado = TRUE WHERE id = ? AND eliminado = FALSE";
        try (Connection conn = config.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Producto mapRow(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setId(rs.getLong("id"));
        producto.setNombre(rs.getString("nombre"));
        producto.setPrecio(rs.getDouble("precio"));
        producto.setCosto(rs.getDouble("costo"));
        producto.setStock(rs.getInt("stock"));
        producto.setCategoriaNombre(rs.getString("categoria"));
        producto.setMarcaNombre(rs.getString("marca"));
        producto.setCodigoBarras(rs.getString("gtin13"));
        producto.setEliminado(rs.getBoolean("eliminado"));
        return producto;
    }
}
