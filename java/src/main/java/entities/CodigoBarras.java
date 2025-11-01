package entities;

import java.util.Objects;

/**
 * Representa un código de barras asociado a un producto.
 */
public class CodigoBarras {

    private Long id;
    private Long productoId;
    private String codigo;
    private boolean eliminado;

    public CodigoBarras() {
    }

    public CodigoBarras(Long id, Long productoId, String codigo, boolean eliminado) {
        this.id = id;
        this.productoId = productoId;
        this.codigo = codigo;
        this.eliminado = eliminado;
    }

    public CodigoBarras(Long productoId, String codigo) {
        this(null, productoId, codigo, false);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public void setEliminado(boolean eliminado) {
        this.eliminado = eliminado;
    }

    @Override
    public String toString() {
        return "CodigoBarras{" +
                "id=" + id +
                ", productoId=" + productoId +
                ", codigo='" + codigo + '\'' +
                ", eliminado=" + eliminado +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodigoBarras that = (CodigoBarras) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
