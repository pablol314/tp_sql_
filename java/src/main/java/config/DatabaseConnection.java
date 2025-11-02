package config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Encapsula la lógica de obtención de conexiones JDBC a partir de un archivo de propiedades externo.
 */
public class DatabaseConnection {

    private final Properties properties = new Properties();
    private final String propertiesFile;

    /**
     * Crea una instancia utilizando el archivo {@code database.properties} ubicado en el classpath.
     */
    public DatabaseConnection() {
        this("database.properties");
    }

    /**
     * Crea una instancia que leerá la configuración de la ruta suministrada.
     *
     * @param propertiesFile nombre del archivo de propiedades dentro del classpath.
     */
    public DatabaseConnection(String propertiesFile) {
        this.propertiesFile = Objects.requireNonNull(propertiesFile, "El archivo de propiedades es obligatorio");
        loadProperties();
        loadDriver();
    }

    private void loadProperties() {
        String resource = resolvePropertiesFile();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el archivo de propiedades: " + resource);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron leer las propiedades de base de datos", e);
        }
        applySystemOverrides();
    }

    private void loadDriver() {
        String driverClassName = properties.getProperty("jdbc.driverClassName");
        if (driverClassName == null || driverClassName.isBlank()) {
            return;
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se pudo cargar el driver JDBC: " + driverClassName, e);
        }
    }

    private String resolvePropertiesFile() {
        String override = System.getProperty("db.properties");
        if (hasText(override)) {
            return override;
        }
        String envOverride = System.getenv("DB_PROPERTIES");
        if (hasText(envOverride)) {
            return envOverride;
        }
        return propertiesFile;
    }

    private void applySystemOverrides() {
        Properties systemProps = System.getProperties();
        for (String key : systemProps.stringPropertyNames()) {
            if (key.startsWith("db.")) {
                String propertyKey = key.substring(3);
                String value = systemProps.getProperty(key);
                if (value == null || value.isBlank()) {
                    properties.remove(propertyKey);
                } else {
                    properties.setProperty(propertyKey, value);
                }
            }
        }

        for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("DB_")) {
                String value = entry.getValue();
                String propertyKey = normalizeEnvKey(key.substring(3));
                if (propertyKey != null) {
                    if (value == null || value.isBlank()) {
                        properties.remove(propertyKey);
                    } else {
                        properties.setProperty(propertyKey, value);
                    }
                }
            }
        }
    }

    private String normalizeEnvKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }
        return rawKey.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Devuelve una conexión JDBC lista para usarse.
     *
     * @return conexión activa hacia la base de datos.
     * @throws SQLException si ocurre un error al abrir la conexión.
     */
    public Connection getConnection() throws SQLException {
        String url = getRequiredProperty("jdbc.url");
        String user = getRequiredProperty("jdbc.user");
        String password = properties.getProperty("jdbc.password", "");
        return DriverManager.getConnection(url, user, password);
    }

    private String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad obligatoria: " + key);
        }
        return value;
    }
}
