package com.pharmacy.dao.interfaces;

import java.util.List;
import java.util.Optional;

public interface GenericDAO<T, ID> {

    Optional<T> create(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    List<T> findAll(int limit, int offset);

    boolean update(T entity);

    boolean delete(ID id);

    boolean exists(ID id);

    long count();

    boolean saveAll(List<T> entities);
}