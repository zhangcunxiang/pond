package com.shuimin.pond.codec.sql;

/**
 * Created by ed on 2014/4/30.
 */
public interface SqlInsert extends Sql{
    public SqlInsert into(String table);
    public SqlInsert values (String... columns);
}
