package lab2;

import java.sql.SQLException;
import java.util.List;

public interface DAO<T> {

    void create(T t)         throws SQLException;
    T findById(String id)    throws SQLException;
    List<T> findAll()        throws SQLException;
    void update(T t)         throws SQLException;
    void delete(String id)   throws SQLException;
}