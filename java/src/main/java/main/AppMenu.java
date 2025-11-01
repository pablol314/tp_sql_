package main;

import config.DatabaseConnection;
import dao.CodigoBarrasDao;
import dao.ProductoDao;
import dto.ProductoConCodigoDto;
import entities.CodigoBarras;
import entities.Producto;
import service.CodigoBarrasService;
import service.ProductoService;
import service.ServiceException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Punto de entrada del programa. Presenta un menú interactivo para ejecutar operaciones CRUD.
 */
public class AppMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final ProductoService productoService;
    private final CodigoBarrasService codigoBarrasService;

    public AppMenu() {
        DatabaseConnection databaseConnection = new DatabaseConnection();
        ProductoDao productoDao = new ProductoDao();
        CodigoBarrasDao codigoBarrasDao = new CodigoBarrasDao();
        this.productoService = new ProductoService(databaseConnection, productoDao, codigoBarrasDao);
        this.codigoBarrasService = new CodigoBarrasService(databaseConnection, codigoBarrasDao);
    }

    public static void main(String[] args) {
        new AppMenu().run();
    }

    private void run() {
        boolean exit = false;
        while (!exit) {
            printMenu();
            int option = readInt("Seleccione una opción: ");
            try {
                switch (option) {
                    case 1 -> crearProducto();
                    case 2 -> actualizarProducto();
                    case 3 -> eliminarProducto();
                    case 4 -> listarProductos();
                    case 5 -> buscarPorNombre();
                    case 6 -> buscarPorCodigo();
                    case 0 -> exit = true;
                    default -> System.out.println("Opción inválida, intente nuevamente.");
                }
            } catch (ServiceException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Hasta pronto!");
    }

    private void printMenu() {
        System.out.println("\n--- Gestión de Productos ---");
        System.out.println("1. Crear producto con código de barras");
        System.out.println("2. Actualizar producto y código de barras");
        System.out.println("3. Eliminar producto");
        System.out.println("4. Listar productos");
        System.out.println("5. Buscar producto por nombre");
        System.out.println("6. Buscar producto por código de barras");
        System.out.println("0. Salir");
    }

    private void crearProducto() {
        ProductoConCodigoDto dto = capturarDatosProducto(null);
        Producto producto = productoService.createWithCodigo(dto);
        System.out.println("Producto creado: " + producto);
    }

    private void actualizarProducto() {
        Long id = readLong("Ingrese el ID del producto a actualizar: ");
        Optional<Producto> producto = productoService.findById(id);
        if (producto.isEmpty()) {
            System.out.println("No existe un producto con ese ID.");
            return;
        }
        ProductoConCodigoDto dto = capturarDatosProducto(producto.get());
        dto.setIdProducto(id);
        Producto actualizado = productoService.updateWithCodigo(dto);
        System.out.println("Producto actualizado: " + actualizado);
    }

    private void eliminarProducto() {
        Long id = readLong("Ingrese el ID del producto a eliminar: ");
        productoService.delete(id);
        System.out.println("Producto eliminado correctamente.");
    }

    private void listarProductos() {
        List<Producto> productos = productoService.findAll();
        if (productos.isEmpty()) {
            System.out.println("No hay productos registrados.");
            return;
        }
        productos.forEach(this::imprimirProductoConCodigo);
    }

    private void buscarPorNombre() {
        System.out.print("Ingrese el texto a buscar en el nombre: ");
        String nombre = scanner.nextLine();
        List<Producto> productos = productoService.findByNombre(nombre);
        if (productos.isEmpty()) {
            System.out.println("No se encontraron productos.");
            return;
        }
        productos.forEach(this::imprimirProductoConCodigo);
    }

    private void buscarPorCodigo() {
        System.out.print("Ingrese el código de barras: ");
        String codigo = scanner.nextLine();
        productoService.findByCodigo(codigo)
                .ifPresentOrElse(this::imprimirProductoConCodigo,
                        () -> System.out.println("No se encontró el producto."));
    }

    private ProductoConCodigoDto capturarDatosProducto(Producto productoExistente) {
        ProductoConCodigoDto dto = new ProductoConCodigoDto();
        System.out.print("Nombre" + textoActual(productoExistente != null ? productoExistente.getNombre() : null) + ": ");
        String nombre = scanner.nextLine();
        dto.setNombre(!nombre.isBlank() ? nombre : productoExistente != null ? productoExistente.getNombre() : null);

        System.out.print("Descripción" + textoActual(productoExistente != null ? productoExistente.getDescripcion() : null) + ": ");
        String descripcion = scanner.nextLine();
        dto.setDescripcion(!descripcion.isBlank() ? descripcion : productoExistente != null ? productoExistente.getDescripcion() : null);

        BigDecimal precio = readBigDecimal("Precio" + textoActual(productoExistente != null ? productoExistente.getPrecio().toString() : null) + ": ",
                productoExistente != null ? productoExistente.getPrecio() : null);
        dto.setPrecio(precio);

        BigDecimal costo = readBigDecimal("Costo" + textoActual(productoExistente != null && productoExistente.getCosto() != null ? productoExistente.getCosto().toString() : null) + ": ",
                productoExistente != null ? productoExistente.getCosto() : null);
        dto.setCosto(costo);

        Long categoriaId = readOptionalLong("Categoría ID" + textoActual(productoExistente != null && productoExistente.getCategoriaId() != null ? productoExistente.getCategoriaId().toString() : null) + ": ",
                productoExistente != null ? productoExistente.getCategoriaId() : null);
        dto.setCategoriaId(categoriaId);

        Long marcaId = readOptionalLong("Marca ID" + textoActual(productoExistente != null && productoExistente.getMarcaId() != null ? productoExistente.getMarcaId().toString() : null) + ": ",
                productoExistente != null ? productoExistente.getMarcaId() : null);
        dto.setMarcaId(marcaId);

        Integer stock = readOptionalInt("Stock" + textoActual(productoExistente != null && productoExistente.getStock() != null ? productoExistente.getStock().toString() : null) + ": ",
                productoExistente != null ? productoExistente.getStock() : null);
        dto.setStock(stock);

        LocalDate fechaAlta = readOptionalDate("Fecha de alta (AAAA-MM-DD)" + textoActual(productoExistente != null && productoExistente.getFechaAlta() != null ? productoExistente.getFechaAlta().toString() : null) + ": ",
                productoExistente != null ? productoExistente.getFechaAlta() : null);
        dto.setFechaAlta(fechaAlta);

        CodigoBarras codigoExistente = productoExistente != null ? productoExistente.getCodigoBarras() : null;
        if (codigoExistente == null && productoExistente != null) {
            codigoExistente = codigoBarrasService.findByProductoId(productoExistente.getId()).orElse(null);
        }
        String codigoActual = codigoExistente != null ? codigoExistente.getGtin13() : null;
        System.out.print("Código de barras" + textoActual(codigoActual) + ": ");
        String codigoNuevo = scanner.nextLine();
        dto.setCodigoBarras(!codigoNuevo.isBlank() ? codigoNuevo : codigoActual);
        return dto;
    }

    private void imprimirProductoConCodigo(Producto producto) {
        CodigoBarras codigoBarras = producto.getCodigoBarras();
        if (codigoBarras == null) {
            codigoBarras = codigoBarrasService.findByProductoId(producto.getId()).orElse(null);
        }
        String codigo = codigoBarras != null ? codigoBarras.getGtin13() : "(sin código)";
        Boolean activo = codigoBarras != null ? codigoBarras.isActivo() : null;
        System.out.printf("[%d] %s - %s - Precio: %s - Costo: %s - Stock: %s - Fecha alta: %s - Código: %s - Activo: %s - Eliminado: %s%n",
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getCosto(),
                producto.getStock(),
                producto.getFechaAlta(),
                codigo,
                activo == null ? "(desconocido)" : activo,
                producto.isEliminado());
    }

    private String textoActual(String valorActual) {
        return valorActual == null ? "" : " (actual: " + valorActual + ")";
    }

    private int readInt(String message) {
        while (true) {
            System.out.print(message);
            try {
                int value = Integer.parseInt(scanner.nextLine());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número válido.");
            }
        }
    }

    private Long readLong(String message) {
        while (true) {
            System.out.print(message);
            try {
                return Long.parseLong(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número entero válido.");
            }
        }
    }

    private BigDecimal readBigDecimal(String message, BigDecimal defaultValue) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();
            if (input.isBlank() && defaultValue != null) {
                return defaultValue;
            }
            try {
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un valor numérico válido.");
            }
        }
    }

    private Long readOptionalLong(String message, Long defaultValue) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();
            if (input.isBlank()) {
                if (defaultValue != null) {
                    return defaultValue;
                }
                System.out.println("Este valor es obligatorio.");
                continue;
            }
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número entero válido.");
            }
        }
    }

    private Integer readOptionalInt(String message, Integer defaultValue) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();
            if (input.isBlank()) {
                if (defaultValue != null) {
                    return defaultValue;
                }
                System.out.println("Este valor es obligatorio.");
                continue;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número entero válido.");
            }
        }
    }

    private LocalDate readOptionalDate(String message, LocalDate defaultValue) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine();
            if (input.isBlank()) {
                if (defaultValue != null) {
                    return defaultValue;
                }
                System.out.println("Este valor es obligatorio.");
                continue;
            }
            try {
                return LocalDate.parse(input, formatter);
            } catch (DateTimeParseException e) {
                System.out.println("Ingrese una fecha válida con formato AAAA-MM-DD.");
            }
        }
    }
}
