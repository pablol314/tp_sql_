package entities;

import java.util.Objects;

/**
 * Representa un código de barras asociado a un producto.
 */
public class CodigoBarras {

    private Long productoId;
    private String gtin13;
    private String tipo;
    private boolean activo = true;

    public CodigoBarras() {
    }

    public CodigoBarras(Long productoId, String gtin13, String tipo, boolean activo) {
        this.productoId = productoId;
        this.gtin13 = gtin13;
        this.tipo = tipo;
        this.activo = activo;
    }

    public CodigoBarras(Long productoId, String gtin13) {
        this(productoId, gtin13, "EAN13", true);
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getGtin13() {
        return gtin13;
    }

    public void setGtin13(String gtin13) {
        this.gtin13 = gtin13;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return "CodigoBarras{" +
                "productoId=" + productoId +
                ", gtin13='" + gtin13 + '\'' +
                ", tipo='" + tipo + '\'' +
                ", activo=" + activo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodigoBarras that = (CodigoBarras) o;
        return Objects.equals(productoId, that.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productoId);
    }
}
