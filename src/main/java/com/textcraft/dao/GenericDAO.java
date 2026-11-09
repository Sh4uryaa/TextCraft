package com.textcraft.dao;

import com.textcraft.exception.DatabaseException;
import java.util.List;
import java.util.Optional;

/** OOP: Generics + Interface - the CRUD contract every DAO implements. */
public interface GenericDAO<T, ID> {
    T save(T t) throws DatabaseException;

    Optional<T> findById(ID id) throws DatabaseException;

    List<T> findAll() throws DatabaseException;

    void delete(ID id) throws DatabaseException;
}
