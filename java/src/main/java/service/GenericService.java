package service;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz base para los servicios de negocio.
 */
public interface GenericService<T> {

    T create(T entity);

    T update(T entity);

    void delete(Long id);

    Optional<T> findById(Long id);

    List<T> findAll();
}
