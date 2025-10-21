import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SimpleConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/producto_barras";
        String user = "root";
        String pass = "1996";
        
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Conexión establecida correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al conectar a la base: " + e.getMessage());
        }
    }
}
