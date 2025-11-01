package service;

import config.DatabaseConnection;
import dao.CodigoBarrasDao;
import dao.ProductoDao;
import dto.ProductoConCodigoDto;
import entities.CodigoBarras;
import entities.Producto;
import util.Validations;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio para {@link Producto} y su relación 1:1 con {@link CodigoBarras}.
 */
public class ProductoService implements GenericService<Producto> {

    private final DatabaseConnection databaseConnection;
    private final ProductoDao productoDao;
    private final CodigoBarrasDao codigoBarrasDao;

    public ProductoService(DatabaseConnection databaseConnection, ProductoDao productoDao, CodigoBarrasDao codigoBarrasDao) {
        this.databaseConnection = databaseConnection;
        this.productoDao = productoDao;
        this.codigoBarrasDao = codigoBarrasDao;
    }

    @Override
    public Producto create(Producto entity) {
        validateProducto(entity);
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            Producto saved = productoDao.save(connection, entity);
            connection.commit();
            return saved;
        } catch (SQLException e) {
            throw new ServiceException("No se pudo crear el producto", e);
        }
    }

    public Producto createWithCodigo(ProductoConCodigoDto dto) {
        validateDto(dto);
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            ensureBarcodeIsUnique(connection, dto.getCodigoBarras(), null);
            Producto producto = new Producto(dto.getNombre(), dto.getDescripcion(), dto.getPrecio());
            productoDao.save(connection, producto);
            CodigoBarras codigoBarras = new CodigoBarras(producto.getId(), dto.getCodigoBarras());
            codigoBarrasDao.save(connection, codigoBarras);
            connection.commit();
            return producto;
        } catch (SQLException e) {
            throw new ServiceException("No se pudo crear el producto con su código de barras", e);
        }
    }

    @Override
    public Producto update(Producto entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio para actualizar");
        }
        validateProducto(entity);
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            Producto updated = productoDao.update(connection, entity);
            connection.commit();
            return updated;
        } catch (SQLException e) {
            throw new ServiceException("No se pudo actualizar el producto", e);
        }
    }

    public Producto updateWithCodigo(ProductoConCodigoDto dto) {
        if (dto.getIdProducto() == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio para actualizar");
        }
        validateDto(dto);
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            Producto producto = productoDao.findById(connection, dto.getIdProducto())
                    .orElseThrow(() -> new ServiceException("Producto inexistente"));
            producto.setNombre(dto.getNombre());
            producto.setDescripcion(dto.getDescripcion());
            producto.setPrecio(dto.getPrecio());
            productoDao.update(connection, producto);

            ensureBarcodeIsUnique(connection, dto.getCodigoBarras(), producto.getId());
            CodigoBarras codigoBarras = codigoBarrasDao.findByProductoId(connection, producto.getId())
                    .orElse(new CodigoBarras(producto.getId(), dto.getCodigoBarras()));
            codigoBarras.setProductoId(producto.getId());
            codigoBarras.setCodigo(dto.getCodigoBarras());
            if (codigoBarras.getId() == null) {
                codigoBarrasDao.save(connection, codigoBarras);
            } else {
                codigoBarrasDao.update(connection, codigoBarras);
            }
            connection.commit();
            return producto;
        } catch (SQLException e) {
            throw new ServiceException("No se pudo actualizar el producto con su código de barras", e);
        }
    }

    @Override
    public void delete(Long id) {
        try (Connection connection = databaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            productoDao.deleteById(connection, id);
            codigoBarrasDao.findByProductoId(connection, id)
                    .ifPresent(codigo -> {
                        try {
                            codigoBarrasDao.deleteById(connection, codigo.getId());
                        } catch (SQLException e) {
                            throw new ServiceException("No se pudo eliminar el código de barras", e);
                        }
                    });
            connection.commit();
        } catch (SQLException e) {
            throw new ServiceException("No se pudo eliminar el producto", e);
        }
    }

    @Override
    public Optional<Producto> findById(Long id) {
        try (Connection connection = databaseConnection.getConnection()) {
            return productoDao.findById(connection, id);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el producto", e);
        }
    }

    public Optional<Producto> findByCodigo(String codigo) {
        try (Connection connection = databaseConnection.getConnection()) {
            return codigoBarrasDao.findByCodigo(connection, codigo)
                    .flatMap(codigoBarras -> productoDao.findById(connection, codigoBarras.getProductoId()));
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el producto por código de barras", e);
        }
    }

    public List<Producto> findByNombre(String nombre) {
        try (Connection connection = databaseConnection.getConnection()) {
            return productoDao.findByNombre(connection, nombre);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo buscar el producto por nombre", e);
        }
    }

    @Override
    public List<Producto> findAll() {
        try (Connection connection = databaseConnection.getConnection()) {
            return productoDao.findAll(connection);
        } catch (SQLException e) {
            throw new ServiceException("No se pudo listar los productos", e);
        }
    }

    private void validateProducto(Producto producto) {
        Validations.requireNotBlank(producto.getNombre(), "El nombre del producto es obligatorio");
        Validations.requirePositive(producto.getPrecio(), "El precio del producto debe ser positivo");
    }

    private void validateDto(ProductoConCodigoDto dto) {
        Validations.requireNotBlank(dto.getNombre(), "El nombre es obligatorio");
        Validations.requireNotBlank(dto.getCodigoBarras(), "El código de barras es obligatorio");
        Validations.requirePositive(dto.getPrecio(), "El precio debe ser positivo");
    }

    private void ensureBarcodeIsUnique(Connection connection, String codigo, Long productoActualId) throws SQLException {
        codigoBarrasDao.findByCodigo(connection, codigo).ifPresent(existing -> {
            if (productoActualId == null || !productoActualId.equals(existing.getProductoId())) {
                throw new ServiceException("El código de barras ya está asociado a otro producto");
            }
        });
    }
}
