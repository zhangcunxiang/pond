package com.shuimin.pond.codec.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by ed on 2014/4/18.
 */
public interface RowMapper<E> {
    public E map(ResultSet rs);
}