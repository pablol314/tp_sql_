package entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa un producto que puede vincularse a un código de barras en una relación 1 a 1.
 */
public class Producto {

    private Long id;
    private String nombre;
    private String descripcion;
    private Long categoriaId;
    private Long marcaId;
    private BigDecimal precio;
    private BigDecimal costo;
    private Integer stock;
    private LocalDate fechaAlta;
    private boolean eliminado;
    private CodigoBarras codigoBarras;

    public Producto() {
    }

    public Producto(Long id,
                    String nombre,
                    String descripcion,
                    Long categoriaId,
                    Long marcaId,
                    BigDecimal precio,
                    BigDecimal costo,
                    Integer stock,
                    LocalDate fechaAlta,
                    boolean eliminado,
                    CodigoBarras codigoBarras) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoriaId = categoriaId;
        this.marcaId = marcaId;
        this.precio = precio;
        this.costo = costo;
        this.stock = stock;
        this.fechaAlta = fechaAlta;
        this.eliminado = eliminado;
        this.codigoBarras = codigoBarras;
    }

    public Producto(String nombre, String descripcion, BigDecimal precio) {
        this(null, nombre, descripcion, null, null, precio, null, null, null, false, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getMarcaId() {
        return marcaId;
    }

    public void setMarcaId(Long marcaId) {
        this.marcaId = marcaId;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDate fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    public CodigoBarras getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(CodigoBarras codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", categoriaId=" + categoriaId +
                ", marcaId=" + marcaId +
                ", precio=" + precio +
                ", costo=" + costo +
                ", stock=" + stock +
                ", fechaAlta=" + fechaAlta +
                ", eliminado=" + eliminado +
                ", codigoBarras=" + codigoBarras +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(id, producto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
