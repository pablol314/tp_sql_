import java.sql.*;
import java.time.LocalTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrenciaDemo {
    private static final String DB_URL = System.getProperty("db.url", "jdbc:mysql://localhost:3306/producto_barras");
    private static final String DB_USER = System.getProperty("db.user", "root");
    private static final String DB_PASSWORD = System.getProperty("db.password", "1996");
    private static final String TABLE_NAME = System.getProperty("appUser.table", "app_user");
    private static final long USER_A_ID = readLongProperty("appUser.idA", 1L);
    private static final long USER_B_ID = readLongProperty("appUser.idB", 2L);
    private static final long LOCK_HOLD_MS = readLongProperty("appUser.lockHoldMs", 4000L);
    private static final long AWAIT_SECONDS = readLongProperty("appUser.awaitSeconds", 60L);

    public static void main(String[] args) throws InterruptedException {
        log("MAIN", "Iniciando demo de concurrencia sobre la tabla '" + TABLE_NAME + "'.");
        log("MAIN", "IDs involucrados: " + USER_A_ID + " y " + USER_B_ID + ".");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Queue<String> summary = new ConcurrentLinkedQueue<>();

        executor.submit(() -> runSession("Sesión-A", USER_A_ID, USER_B_ID, ready, start, summary));
        executor.submit(() -> runSession("Sesión-B", USER_B_ID, USER_A_ID, ready, start, summary));

        ready.await();
        log("MAIN", "Ambas sesiones listas. Lanzando operaciones conflictivas...");
        start.countDown();

        executor.shutdown();
        if (!executor.awaitTermination(AWAIT_SECONDS, TimeUnit.SECONDS)) {
            log("MAIN", "Las sesiones no finalizaron en " + AWAIT_SECONDS + " segundos, solicitando interrupción.");
            executor.shutdownNow();
        }

        log("MAIN", "Resumen de resultados:");
        summary.forEach(result -> log("RESUMEN", result));
        log("MAIN", "Demo finalizada.");
    }

    private static void runSession(String name, long firstId, long secondId,
                                   CountDownLatch ready, CountDownLatch start,
                                   Queue<String> summary) {
        Connection connection = null;
        boolean countedDown = false;

        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

            ready.countDown();
            countedDown = true;
            start.await();

            log(name, "Bloqueando fila id=" + firstId + " en " + TABLE_NAME + ".");
            lockAppUser(connection, firstId);

            log(name, "Fila " + firstId + " bloqueada. Manteniendo el lock por " + LOCK_HOLD_MS + " ms.");
            Thread.sleep(LOCK_HOLD_MS);

            log(name, "Intentando bloquear fila id=" + secondId + " en " + TABLE_NAME + ".");
            lockAppUser(connection, secondId);

            log(name, "Se obtuvieron ambas filas. Confirmando transacción.");
            connection.commit();
            summary.add(name + ": transacción confirmada (sin deadlock).");
        } catch (SQLTransactionRollbackException e) {
            if (!countedDown) {
                ready.countDown();
                countedDown = true;
            }
            String category = categorize(e);
            log(name, "Transacción abortada por " + category + ": " + e.getMessage());
            summary.add(name + ": " + describeOutcome(category));
            rollbackQuietly(connection, name);
        } catch (SQLTimeoutException e) {
            if (!countedDown) {
                ready.countDown();
                countedDown = true;
            }
            log(name, "Timeout esperando el lock: " + e.getMessage());
            summary.add(name + ": " + describeOutcome("timeout"));
            rollbackQuietly(connection, name);
        } catch (SQLException e) {
            if (!countedDown) {
                ready.countDown();
                countedDown = true;
            }
            String category = categorize(e);
            log(name, "Error SQL (" + category + "): " + e.getMessage());
            summary.add(name + ": " + describeOutcome(category));
            rollbackQuietly(connection, name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!countedDown) {
                ready.countDown();
                countedDown = true;
            }
            log(name, "Hilo interrumpido.");
            summary.add(name + ": hilo interrumpido.");
            rollbackQuietly(connection, name);
        } finally {
            if (!countedDown) {
                ready.countDown();
            }
            closeQuietly(connection, name);
        }
    }

    private static void lockAppUser(Connection connection, long id) throws SQLException {
        String sql = "SELECT id FROM " + TABLE_NAME + " WHERE id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No existe registro en " + TABLE_NAME + " con id=" + id);
                }
            }
        }
    }

    private static void rollbackQuietly(Connection connection, String name) {
        if (connection != null) {
            try {
                connection.rollback();
                log(name, "Rollback ejecutado.");
            } catch (SQLException rollbackError) {
                log(name, "Error al ejecutar rollback: " + rollbackError.getMessage());
            }
        }
    }

    private static void closeQuietly(Connection connection, String name) {
        if (connection != null) {
            try {
                connection.close();
                log(name, "Conexión cerrada.");
            } catch (SQLException closeError) {
                log(name, "Error al cerrar la conexión: " + closeError.getMessage());
            }
        }
    }

    private static String categorize(SQLException e) {
        String message = e.getMessage();
        if (message != null) {
            String normalized = message.toLowerCase();
            if (normalized.contains("deadlock")) {
                return "deadlock";
            }
            if (normalized.contains("lock wait timeout")) {
                return "timeout";
            }
        }

        String sqlState = e.getSQLState();
        if ("40001".equals(sqlState)) {
            return "deadlock";
        }
        if ("41000".equals(sqlState) || "HY000".equals(sqlState)) {
            return "timeout";
        }
        return "error";
    }

    private static String describeOutcome(String category) {
        return switch (category) {
            case "deadlock" -> "deadlock detectado; la transacción se abortó.";
            case "timeout" -> "timeout esperando el lock; la transacción se abortó.";
            default -> "error SQL (" + category + ").";
        };
    }

    private static long readLongProperty(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            System.err.printf("Propiedad %s='%s' inválida. Usando valor por defecto %d.%n", key, value, defaultValue);
            return defaultValue;
        }
    }

    private static void log(String session, String message) {
        System.out.printf("[%s] %-9s %s%n", LocalTime.now(), session, message);
    }
}
