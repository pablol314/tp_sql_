import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {    
    private Connection connection;
    
    /**
     * Constructor que recibe una conexión a la DB de SQL
     * 
     */
    public ProductoDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Método para buscar productos por nombre usando PreparedStatement.
     */
    public List<Producto> buscarPorNombre(String nombre) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        String sql = "SELECT id, nombre, precio, stock " +
                     "FROM vw_producto_publico " +
                     "WHERE nombre LIKE ? " +
                     "LIMIT 100";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {            
            stmt.setString(1, "%" + nombre + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getLong("id"));
                    p.setNombre(rs.getString("nombre"));
                    p.setPrecio(rs.getDouble("precio"));
                    p.setStock(rs.getInt("stock"));
                    productos.add(p);
                }
            }
        }
        
        return productos;
    }
    
    /**
     * Método para buscar productos por ID usando PreparedStatement
     */
    public Producto buscarPorId(long id) throws SQLException {
        String sql = "SELECT id, nombre, categoria, marca, precio, stock, fecha_alta, gtin13 " +
                     "FROM vw_producto_publico " +
                     "WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getLong("id"));
                    p.setNombre(rs.getString("nombre"));
                    p.setCategoria(rs.getString("categoria"));
                    p.setMarca(rs.getString("marca"));
                    p.setPrecio(rs.getDouble("precio"));
                    p.setStock(rs.getInt("stock"));
                    p.setFechaAlta(rs.getDate("fecha_alta"));
                    p.setGtin13(rs.getString("gtin13"));
                    return p;
                }
            }
        }
        
        return null;
    }
    
    /**
     * inserta un nuevo producto usando PreparedStatement
     * SOLO FUNCIONA SI EL USUARIO TIENE PERMISOS DE ESCRITURA     
     */
    public long insertar(ProductoInsert p) throws SQLException {       
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (p.getPrecio() < 0 || p.getCosto() < 0) {
            throw new IllegalArgumentException("Precio y costo deben ser positivos");
        }
        if (p.getPrecio() < p.getCosto()) {
            throw new IllegalArgumentException("El precio debe ser mayor o igual al costo");
        }
        
        String sql = "INSERT INTO producto (nombre, categoria_id, marca_id, precio, costo, stock, fecha_alta) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.getNombre().trim());
            stmt.setLong(2, p.getCategoriaId());
            stmt.setLong(3, p.getMarcaId());
            stmt.setDouble(4, p.getPrecio());
            stmt.setDouble(5, p.getCosto());
            stmt.setInt(6, p.getStock());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        }
        
        throw new SQLException("No se pudo insertar el producto, no se obtuvo ID");
    }
    
    /**
     * método de demostración, es inseguro y no debería utilizarse en prod
     */
    @Deprecated
    public List<Producto> buscarPorNombreInseguro(String nombre) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        
        // permite inyección SQL
        String sql = "SELECT id, nombre, precio, stock " +
                     "FROM vw_producto_publico " +
                     "WHERE nombre LIKE '%" + nombre + "%' " +
                     "LIMIT 100";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Producto p = new Producto();
                p.setId(rs.getLong("id"));
                p.setNombre(rs.getString("nombre"));
                p.setPrecio(rs.getDouble("precio"));
                p.setStock(rs.getInt("stock"));
                productos.add(p);
            }
        }
        
        return productos;
    }
}

/**
 * clases de demostracion básicas para representar productos
 */
class Producto {
    private Long id;
    private String nombre;
    private String categoria;
    private String marca;
    private Double precio;
    private Integer stock;
    private Date fechaAlta;
    private String gtin13;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public Date getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(Date fechaAlta) { this.fechaAlta = fechaAlta; }
    
    public String getGtin13() { return gtin13; }
    public void setGtin13(String gtin13) { this.gtin13 = gtin13; }
    
    @Override
    public String toString() {
        return String.format("Producto[id=%d, nombre='%s', precio=%.2f, stock=%d]", 
                             id, nombre, precio, stock);
    }
}

/**
 * clase para inserción de productos sin ID
 */
class ProductoInsert {
    private String nombre;
    private Long categoriaId;
    private Long marcaId;
    private Double precio;
    private Double costo;
    private Integer stock;
    
    public ProductoInsert(String nombre, Long categoriaId, Long marcaId, 
                         Double precio, Double costo, Integer stock) {
        this.nombre = nombre;
        this.categoriaId = categoriaId;
        this.marcaId = marcaId;
        this.precio = precio;
        this.costo = costo;
        this.stock = stock;
    }
    
    public String getNombre() { return nombre; }
    public Long getCategoriaId() { return categoriaId; }
    public Long getMarcaId() { return marcaId; }
    public Double getPrecio() { return precio; }
    public Double getCosto() { return costo; }
    public Integer getStock() { return stock; }
}
