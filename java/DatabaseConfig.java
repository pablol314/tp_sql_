import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DatabaseConfig carga las credenciales de database.properties
 * y expone un método para obtener conexiones JDBC a MySQL.
 */
public class DatabaseConfig {
    private static final String DEFAULT_PROPERTIES = "database.properties";
    private final Properties properties = new Properties();

    public DatabaseConfig() {
        this(DEFAULT_PROPERTIES);
    }

    public DatabaseConfig(String propertiesFile) {
        Path path = Path.of(propertiesFile);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                "No se encontró " + propertiesFile + ". Copie database.properties.example y complete sus credenciales.");
        }
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer " + propertiesFile, e);
        }
    }

    public Connection getConnection() throws SQLException {
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        if (url == null || user == null) {
            throw new IllegalStateException("Las propiedades db.url y db.user son obligatorias");
        }
        return DriverManager.getConnection(url, user, password);
    }
}
