import java.sql.*;

public class ConcurrenciaDemo {
    public static void main(String[] args) throws Exception {
        Connection conn1 = DriverManager.getConnection("jdbc:mysql://localhost:3306/producto_barras", "root", "1996");
        Connection conn2 = DriverManager.getConnection("jdbc:mysql://localhost:3306/producto_barras", "root", "1996");

        conn1.setAutoCommit(false);
        conn2.setAutoCommit(false);

        System.out.println("Conexión 1 bloqueando producto 1...");
        try (PreparedStatement st1 = conn1.prepareStatement("UPDATE producto SET precio = precio + 1 WHERE id = 1")) {
            st1.executeUpdate();

            System.out.println("Conexión 2 intenta modificar el mismo registro...");
            try (PreparedStatement st2 = conn2.prepareStatement("UPDATE producto SET precio = precio + 2 WHERE id = 1")) {
                st2.executeUpdate(); // se bloquea hasta liberar conn1
            }
        }

        System.out.println("Liberando bloqueo con commit...");
        conn1.commit();
        conn2.commit();

        conn1.close();
        conn2.close();
    }
}
