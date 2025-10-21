package tp_sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;

public class TransaccionesDemo {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/producto_barras";
     private static final String USER = "root"; 
    private static final String PASS = "1996"; 
  

    /**
     * Simula una operación de transferencia de stock entre dos productos
     * asegurando la atomicidad (o todo sale bien, o nada cambia).
     * @return true si la transacción fue exitosa, false si falló y se hizo rollback.
     */
    public boolean transferirStock(int idOrigen, int idDestino, int cantidad) {
        Connection conn = null;
        try {
            // 1. Obtener la conexión
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            
            // 2. Desactivar el auto-commit (INICIO de la Transacción)
            conn.setAutoCommit(false);
            
            // 3. Verificar stock antes de modificar
            // (Esta es una simplificación, en un caso real se haría SELECT FOR UPDATE)
            
            // -- Paso A: Restar stock del Origen --
            String sqlRestar = "UPDATE producto SET stock = stock - ? WHERE id = ?";
            try (PreparedStatement stmtRestar = conn.prepareStatement(sqlRestar)) {
                stmtRestar.setInt(1, cantidad);
                stmtRestar.setInt(2, idOrigen);
                stmtRestar.executeUpdate();
            }

            // -- Paso B: Sumar stock al Destino --
            String sqlSumar = "UPDATE producto SET stock = stock + ? WHERE id = ?";
            try (PreparedStatement stmtSumar = conn.prepareStatement(sqlSumar)) {
                stmtSumar.setInt(1, cantidad);
                stmtSumar.setInt(2, idDestino);
                
                // OPCIONAL: Si queremos forzar un error, podríamos hacer esto:
                // if (idDestino == 9999) throw new SQLException("Error forzado para Rollback");
                
                stmtSumar.executeUpdate();
            }

            // 4. Si todo OK, COMMIT
            conn.commit(); 
            System.out.println("Transacción Exitosa: Stock transferido.");
            return true;

        } catch (SQLException e) {
            // 5. Si hay error, ROLLBACK
            System.err.println("Error SQL en transacción. Causa: " + e.getMessage());
            if (conn != null) {
                try {
                    System.out.println("Haciendo ROLLBACK...");
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Error al intentar hacer rollback: " + rollbackEx.getMessage());
                }
            }
            return false;
        } finally {
            // 6. Cerrar y resetear
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Devolver a la normalidad
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        // Ejemplo de uso: transferir 10 unidades del producto 1 al producto 2
        TransaccionService service = new TransaccionService();
        service.transferirStock(1, 2, 10);
        
        // Ejemplo de uso que falla (asumiendo stock insuficiente, o forzando un error en el código)
        // service.transferirStock(1, 2, 999999);
    }
}