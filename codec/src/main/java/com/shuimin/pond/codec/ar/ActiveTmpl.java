package com.shuimin.pond.codec.ar;

import com.shuimin.pond.codec.ar.sql.dialect.Dialect;
import com.shuimin.pond.codec.db.JdbcOperator;
import com.shuimin.pond.codec.db.JdbcTmpl;
import com.shuimin.pond.core.Server;

import java.util.Arrays;

/**
 * Created by ed on 2014/4/30.
 */
public class ActiveTmpl extends JdbcTmpl {

    ActiveRecordPlugin config = Server.register(ActiveRecordPlugin.class);

    Dialect dialect;

    public ActiveTmpl dialect(Dialect d) {
        dialect = d;
        return this;
    }

    public ActiveTmpl(JdbcOperator oper) {
        super(oper);
    }

    public Table createTable(String name, String... columns) {
        String idMark = Server.register(ActiveRecordPlugin.class).keyId;
        String create = String.format(
            "CREATE TABLE %s (%s %s, %s, create_time timestamp, update_time timestamp)",
            name, idMark, dialect.primaryKeyMarkOnCreate(),
            String.join(",", Arrays.asList(columns))
        );
        config.beforeTableCreation.apply(name);
        exec(create);
        Table active = active(name);
        config.afterTableCreation.apply(active);
        return active;
    }

    public void dropTable(String name) {
        drop(name);
    }

    public Table active(String tableName) {
        return null;
    }


}