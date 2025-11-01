import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class AppMenu {
    private final ProductoDao productoDao;
    private final Scanner scanner = new Scanner(System.in).useLocale(Locale.US);

    public AppMenu(ProductoDao productoDao) {
        this.productoDao = productoDao;
    }

    public static void main(String[] args) {
        try {
            DatabaseConfig config = new DatabaseConfig();
            ProductoDao dao = new ProductoDao(config);
            AppMenu menu = new AppMenu(dao);
            menu.loop();
        } catch (IllegalStateException ex) {
            System.err.println("Configuración inválida: " + ex.getMessage());
        }
    }

    private void loop() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEntero("Seleccione una opción: ");
            try {
                switch (opcion) {
                    case 1 -> altaProducto();
                    case 2 -> listarProductos();
                    case 3 -> actualizarProducto();
                    case 4 -> eliminarProducto();
                    case 5 -> buscarPorCodigo();
                    case 6 -> buscarPorNombre();
                    case 0 -> System.out.println("Saliendo...\n");
                    default -> System.out.println("Opción no válida. Intente nuevamente.\n");
                }
            } catch (SQLException e) {
                System.err.println("Error al ejecutar la operación: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("==============================");
        System.out.println(" Catálogo Producto → Código de Barras");
        System.out.println("==============================");
        System.out.println("1. Alta de producto");
        System.out.println("2. Listar productos activos");
        System.out.println("3. Actualizar precio y stock");
        System.out.println("4. Baja lógica de producto");
        System.out.println("5. Buscar por código de barras");
        System.out.println("6. Buscar por nombre");
        System.out.println("0. Salir");
    }

    private void altaProducto() throws SQLException {
        System.out.println("--- Alta de producto ---");
        Producto producto = new Producto();
        producto.setNombre(leerTexto("Nombre: "));
        producto.setCategoriaId(leerEntero("ID de categoría existente: "));
        producto.setMarcaId(leerEntero("ID de marca existente: "));
        producto.setPrecio(leerDouble("Precio de venta: "));
        producto.setCosto(leerDouble("Costo: "));
        producto.setStock(leerEntero("Stock inicial: "));
        producto.setCodigoBarras(leerTexto("Código de barras GTIN-13: "));
        Producto creado = productoDao.crear(producto);
        System.out.printf("Producto creado con ID %d y código %s.%n%n", creado.getId(), creado.getCodigoBarras());
    }

    private void listarProductos() throws SQLException {
        System.out.println("--- Productos activos ---");
        List<Producto> productos = productoDao.listarActivos();
        if (productos.isEmpty()) {
            System.out.println("No hay productos activos registrados.\n");
            return;
        }
        for (Producto p : productos) {
            System.out.printf(Locale.US,
                    "[%d] %s | %s | %s | GTIN %s | Precio $%.2f | Stock %d%n",
                    p.getId(), p.getNombre(), p.getCategoriaNombre(), p.getMarcaNombre(),
                    p.getCodigoBarras(), p.getPrecio(), p.getStock());
        }
        System.out.println();
    }

    private void actualizarProducto() throws SQLException {
        System.out.println("--- Actualizar precio y stock ---");
        long id = leerEntero("ID del producto a actualizar: ");
        double precio = leerDouble("Nuevo precio: ");
        int stock = leerEntero("Nuevo stock: ");
        boolean actualizado = productoDao.actualizarPrecioStock(id, precio, stock);
        if (actualizado) {
            System.out.println("Datos actualizados correctamente.\n");
        } else {
            System.out.println("No se encontró el producto o ya está dado de baja.\n");
        }
    }

    private void eliminarProducto() throws SQLException {
        System.out.println("--- Baja lógica de producto ---");
        long id = leerEntero("ID del producto a dar de baja: ");
        boolean eliminado = productoDao.bajaLogica(id);
        if (eliminado) {
            System.out.println("Producto marcado como eliminado.\n");
        } else {
            System.out.println("No se encontró el producto o ya estaba dado de baja.\n");
        }
    }

    private void buscarPorCodigo() throws SQLException {
        System.out.println("--- Buscar por código de barras ---");
        String codigo = leerTexto("GTIN-13: ");
        Producto producto = productoDao.buscarPorCodigo(codigo);
        if (producto == null) {
            System.out.println("No se encontró ningún producto con ese código.\n");
            return;
        }
        System.out.printf(Locale.US,
                "[%d] %s | %s | %s | Precio $%.2f | Stock %d | Estado: %s%n%n",
                producto.getId(), producto.getNombre(), producto.getCategoriaNombre(),
                producto.getMarcaNombre(), producto.getPrecio(), producto.getStock(),
                producto.isEliminado() ? "Inactivo" : "Activo");
    }

    private void buscarPorNombre() throws SQLException {
        System.out.println("--- Buscar por nombre ---");
        String termino = leerTexto("Texto a buscar: ");
        List<Producto> resultados = productoDao.buscarPorNombre(termino);
        if (resultados.isEmpty()) {
            System.out.println("No se encontraron coincidencias.\n");
            return;
        }
        for (Producto p : resultados) {
            System.out.printf(Locale.US,
                    "[%d] %s | %s | %s | GTIN %s | Precio $%.2f | Stock %d%n",
                    p.getId(), p.getNombre(), p.getCategoriaNombre(), p.getMarcaNombre(),
                    p.getCodigoBarras(), p.getPrecio(), p.getStock());
        }
        System.out.println();
    }

    private int leerEntero(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine();
            try {
                return Integer.parseInt(linea.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Ingrese un número entero válido.");
            }
        }
    }

    private double leerDouble(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String linea = scanner.nextLine();
            try {
                return Double.parseDouble(linea.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Ingrese un valor numérico válido (use punto decimal).");
            }
        }
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }
}
