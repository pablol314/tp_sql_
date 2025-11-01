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
        String errorMessage = "No se pudo crear el producto";
        try (Connection connection = databaseConnection.getConnection()) {
            Throwable txException = null;
            try {
                connection.setAutoCommit(false);
                Producto saved = productoDao.save(connection, entity);
                persistCodigoBarras(connection, saved, entity.getCodigoBarras());
                connection.commit();
                return saved;
            } catch (Exception e) {
                RuntimeException toThrow = propagateTransactionalException(connection, e, errorMessage);
                txException = toThrow;
                throw toThrow;
            } finally {
                resetAutoCommit(connection, txException);
            }
        } catch (SQLException e) {
            throw new ServiceException(errorMessage, e);
        }
    }

    public Producto createWithCodigo(ProductoConCodigoDto dto) {
        validateDto(dto);
        String errorMessage = "No se pudo crear el producto con su código de barras";
        try (Connection connection = databaseConnection.getConnection()) {
            Throwable txException = null;
            try {
                connection.setAutoCommit(false);
                ensureBarcodeIsUnique(connection, dto.getCodigoBarras(), null);

                Producto producto = buildProductoFromDto(dto);
                productoDao.save(connection, producto);

                CodigoBarras codigoBarras = new CodigoBarras(producto.getId(), dto.getCodigoBarras());
                codigoBarrasDao.save(connection, codigoBarras);
                producto.setCodigoBarras(codigoBarras);

                connection.commit();
                return producto;
            } catch (Exception e) {
                RuntimeException toThrow = propagateTransactionalException(connection, e, errorMessage);
                txException = toThrow;
                throw toThrow;
            } finally {
                resetAutoCommit(connection, txException);
            }
        } catch (SQLException e) {
            throw new ServiceException(errorMessage, e);
        }
    }

    @Override
    public Producto update(Producto entity) {
        if (entity.getId() == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio para actualizar");
        }
        validateProducto(entity);
        String errorMessage = "No se pudo actualizar el producto";
        try (Connection connection = databaseConnection.getConnection()) {
            Throwable txException = null;
            try {
                connection.setAutoCommit(false);
                Producto updated = productoDao.update(connection, entity);
                persistCodigoBarras(connection, updated, entity.getCodigoBarras());
                connection.commit();
                return updated;
            } catch (Exception e) {
                RuntimeException toThrow = propagateTransactionalException(connection, e, errorMessage);
                txException = toThrow;
                throw toThrow;
            } finally {
                resetAutoCommit(connection, txException);
            }
        } catch (SQLException e) {
            throw new ServiceException(errorMessage, e);
        }
    }

    public Producto updateWithCodigo(ProductoConCodigoDto dto) {
        if (dto.getIdProducto() == null) {
            throw new IllegalArgumentException("El id del producto es obligatorio para actualizar");
        }
        validateDto(dto);
        String errorMessage = "No se pudo actualizar el producto con su código de barras";
        try (Connection connection = databaseConnection.getConnection()) {
            Throwable txException = null;
            try {
                connection.setAutoCommit(false);
                Producto producto = productoDao.findById(connection, dto.getIdProducto())
                        .orElseThrow(() -> new ServiceException("Producto inexistente"));

                updateProductoFromDto(producto, dto);
                productoDao.update(connection, producto);

                ensureBarcodeIsUnique(connection, dto.getCodigoBarras(), producto.getId());
                Optional<CodigoBarras> existente = codigoBarrasDao.findByProductoId(connection, producto.getId());
                CodigoBarras codigoBarras = existente.orElse(new CodigoBarras(producto.getId(), dto.getCodigoBarras()));
                codigoBarras.setProductoId(producto.getId());
                codigoBarras.setGtin13(dto.getCodigoBarras());
                if (codigoBarras.getTipo() == null) {
                    codigoBarras.setTipo("EAN13");
                }
                codigoBarras.setActivo(true);

                if (existente.isPresent()) {
                    codigoBarrasDao.update(connection, codigoBarras);
                } else {
                    codigoBarrasDao.save(connection, codigoBarras);
                }
                producto.setCodigoBarras(codigoBarras);

                connection.commit();
                return producto;
            } catch (Exception e) {
                RuntimeException toThrow = propagateTransactionalException(connection, e, errorMessage);
                txException = toThrow;
                throw toThrow;
            } finally {
                resetAutoCommit(connection, txException);
            }
        } catch (SQLException e) {
            throw new ServiceException(errorMessage, e);
        }
    }

    @Override
    public void delete(Long id) {
        String errorMessage = "No se pudo eliminar el producto";
        try (Connection connection = databaseConnection.getConnection()) {
            Throwable txException = null;
            try {
                connection.setAutoCommit(false);
                productoDao.deleteById(connection, id);
                codigoBarrasDao.findByProductoId(connection, id)
                        .ifPresent(codigo -> {
                            try {
                                codigoBarrasDao.deleteById(connection, codigo.getProductoId());
                            } catch (SQLException e) {
                                throw new ServiceException("No se pudo eliminar el código de barras", e);
                            }
                        });
                connection.commit();
            } catch (Exception e) {
                RuntimeException toThrow = propagateTransactionalException(connection, e, errorMessage);
                txException = toThrow;
                throw toThrow;
            } finally {
                resetAutoCommit(connection, txException);
            }
        } catch (SQLException e) {
            throw new ServiceException(errorMessage, e);
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
            Optional<CodigoBarras> codigoBarras = codigoBarrasDao.findByCodigo(connection, codigo);
            if (codigoBarras.isEmpty()) {
                return Optional.empty();
            }
            return productoDao.findById(connection, codigoBarras.get().getProductoId());
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
        Validations.requirePositiveOrZero(producto.getCosto(), "El costo del producto debe ser mayor o igual a cero");
        Validations.requireNotNull(producto.getCategoriaId(), "La categoría del producto es obligatoria");
        Validations.requireNotNull(producto.getMarcaId(), "La marca del producto es obligatoria");
        Validations.requireNonNegative(producto.getStock(), "El stock no puede ser negativo");
        Validations.requireNotNull(producto.getFechaAlta(), "La fecha de alta es obligatoria");
    }

    private void validateDto(ProductoConCodigoDto dto) {
        Validations.requireNotBlank(dto.getNombre(), "El nombre es obligatorio");
        Validations.requireNotBlank(dto.getCodigoBarras(), "El código de barras es obligatorio");
        Validations.requirePositive(dto.getPrecio(), "El precio debe ser positivo");
        Validations.requirePositiveOrZero(dto.getCosto(), "El costo debe ser mayor o igual a cero");
        Validations.requireNotNull(dto.getCategoriaId(), "La categoría es obligatoria");
        Validations.requireNotNull(dto.getMarcaId(), "La marca es obligatoria");
        Validations.requireNonNegative(dto.getStock(), "El stock debe ser cero o positivo");
        Validations.requireNotNull(dto.getFechaAlta(), "La fecha de alta es obligatoria");
    }

    private void ensureBarcodeIsUnique(Connection connection, String codigo, Long productoActualId) throws SQLException {
        codigoBarrasDao.findByCodigo(connection, codigo).ifPresent(existing -> {
            if (productoActualId == null || !productoActualId.equals(existing.getProductoId())) {
                throw new ServiceException("El código de barras ya está asociado a otro producto");
            }
        });
    }

    private void persistCodigoBarras(Connection connection, Producto producto, CodigoBarras codigoBarras) throws SQLException {
        if (codigoBarras == null) {
            return;
        }
        if (codigoBarras.getGtin13() == null || codigoBarras.getGtin13().isBlank()) {
            throw new IllegalArgumentException("El GTIN13 del código de barras es obligatorio");
        }
        ensureBarcodeIsUnique(connection, codigoBarras.getGtin13(), producto.getId());
        codigoBarras.setProductoId(producto.getId());
        if (codigoBarras.getTipo() == null) {
            codigoBarras.setTipo("EAN13");
        }
        if (codigoBarrasDao.findByProductoId(connection, producto.getId()).isPresent()) {
            codigoBarrasDao.update(connection, codigoBarras);
        } else {
            codigoBarrasDao.save(connection, codigoBarras);
        }
        producto.setCodigoBarras(codigoBarras);
    }

    private Producto buildProductoFromDto(ProductoConCodigoDto dto) {
        Producto producto = new Producto();
        updateProductoFromDto(producto, dto);
        return producto;
    }

    private void updateProductoFromDto(Producto producto, ProductoConCodigoDto dto) {
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setCategoriaId(dto.getCategoriaId());
        producto.setMarcaId(dto.getMarcaId());
        producto.setPrecio(dto.getPrecio());
        producto.setCosto(dto.getCosto());
        producto.setStock(dto.getStock());
        producto.setFechaAlta(dto.getFechaAlta());
    }

    private RuntimeException propagateTransactionalException(Connection connection, Exception exception, String message) {
        RuntimeException toThrow;
        if (exception instanceof ServiceException) {
            toThrow = (ServiceException) exception;
        } else if (exception instanceof RuntimeException) {
            toThrow = (RuntimeException) exception;
        } else {
            toThrow = new ServiceException(message, exception);
        }
        rollbackQuietly(connection, toThrow);
        return toThrow;
    }

    private void rollbackQuietly(Connection connection, Throwable exceptionToAugment) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            if (exceptionToAugment != null) {
                exceptionToAugment.addSuppressed(rollbackException);
            }
        }
    }

    private void resetAutoCommit(Connection connection, Throwable originalException) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException autoCommitException) {
            if (originalException != null) {
                originalException.addSuppressed(autoCommitException);
            } else {
                throw new ServiceException("No se pudo restablecer el auto-commit de la conexión", autoCommitException);
            }
        }
    }
}
