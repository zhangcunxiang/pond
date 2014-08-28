package com.shuimin.pond.db;

import com.shuimin.common.S;
import com.shuimin.common.f.Callback;
import com.shuimin.common.f.Function;
import com.shuimin.common.f.Tuple;
import com.shuimin.common.sql.Criterion;
import com.shuimin.common.sql.Sql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.shuimin.common.S._for;
import static com.shuimin.common.S._notNullElse;

/**
 * Created by ed on 2014/4/18.
 * <pre>
 *      JDBCTmpl
 *      jdbc- template
 *      A Common template of data access with database
 *      using JDBC.
 *  </pre>
 */
public class JDBCTmpl implements Closeable {


    /**
     * default map
     * TODO: ugly implement
     */
    final Function<AbstractRecord, ResultSet> _default_rm =

            (ResultSet rs) -> {
                AbstractRecord ret = Record.newValue(AbstractRecord.class);
                try {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int cnt = metaData.getColumnCount();
                    String mainTableName = metaData.getTableName(1);
                    ret.table(mainTableName);
                    for (int i = 0; i < cnt; i++) {
                        String className = metaData.getColumnClassName(i + 1);
                        String retTable = metaData.getTableName(i + 1);
                        Class<?> type = Object.class;
                        try {
                            type = Class.forName(className);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        String colName = metaData.getColumnName(i + 1);
                        Object val;

                        //TODO ugly!
                        if (type.equals(byte[].class)
                                || type.equals(Byte[].class)) {
                            val = rs.getBinaryStream(i + 1);
                        } else {
                            val = JDBCOper.normalizeValue(rs.getObject(i + 1, type));
                        }

//                    S.echo(String.format("NAME : %s ,TYPE : %s", colName,
//                            val == null ? null : val.getClass()));

                        if (mainTableName.equals(retTable))
                            ret.set(colName, val);
                        else ret.setInner(retTable, colName, val);
                    }
                } catch (SQLException e) {
                    S._lazyThrow(e);
                }
                return ret;
            };


    static Logger logger = LoggerFactory.getLogger(JDBCTmpl.class);
    final
    Function<Integer, ResultSet> counter = rs -> {
        try {
            return (Integer) rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    };
    protected JDBCOper oper;

    public JDBCTmpl(
            JDBCOper oper) {
        this.oper = oper;
    }

    public List<Record> find(String sql, String... x) {
        return this.map(_default_rm, sql, x);
    }

    public List<Record> find(String sql) {
        return this.map(_default_rm, sql);
    }

    @SuppressWarnings("unchecked")
    public <R extends Record> List<R> map(
            R proto,
            String sql, Object... x
    ) {
        return this.map(proto.mapper(), sql, x);
    }

    @SuppressWarnings("unchecked")
    public <R extends Record> List<R> map(
            Class<R> clazz,
            String sql, Object... x
    ) {
        R proto = Record.newValue(clazz);
        return map(proto, sql, x);
    }

    public int count(String sql, Object[] params) {
        return _notNullElse(S.<Integer>_for(map(counter, sql,
                params)).first(), 0);
    }

    public int count(Tuple<String, Object[]> mix) {
        return count(mix._a, mix._b);
    }

// removed page function
//    public <R> Page<R> page(Function<?, ResultSet> mapper,
//                            Page<R> page ) {
//        List<R> r = map(rs_mapper, page.querySql());
//        int count = (Integer) _for(map(counter,page.querySql())).first();
//        return page.fulfill(r, count);
//    }

    public <R> List<R> map(Function<?, ResultSet> mapper,
                           Tuple<String, Object[]> mix) {
        return map(mapper, mix._a, mix._b);
    }

    @SuppressWarnings("unchecked")
    public <R> List<R> map(Function<?, ResultSet> mapper,
                           String sql, Object... x) {

        ResultSet rs;
        List<R> list = new ArrayList<>();
        try {
            rs = oper.query(sql, x);
            while (rs.next()) {
                //check
                list.add((R) mapper.apply(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
        return list;
    }

    /**
     * run callback(s) in a transaction
     *
     * @param callback callback(s)
     */
    public void tx(Callback<JDBCTmpl>... callback) {
        try {
            oper.transactionStart();
            _for(callback).each(c -> c.apply(this));
            oper.transactionCommit();
        } catch (Exception e) {
            try {
                oper.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
                throw new RuntimeSQLException(e);
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * run batch(s) in a transaction
     *
     * @param batch sql(s)
     */
    public void tx(String... batch) {
        try {
            oper.transactionStart();
            for (String sql : batch) {
                oper.execute(sql);
            }
            oper.transactionCommit();
        } catch (SQLException e) {
            try {
                oper.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
                throw new RuntimeSQLException(e);
            }
            throw new RuntimeException(e);
        }
    }


    /**
     * execute sql without prepared-statement
     */
    public int exec(String sql) {
        try {
            return oper.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

   public int exec(String sql, Object[] params, int[] types){
       try {
           return oper.execute(sql,params,types);
       } catch (SQLException e) {
           throw new RuntimeException(e);
       }
   }
    /**
     * execute sql & params
     *
     * @param sql    sql
     * @param params params
     * @return affected row number
     */
    public int exec(String sql, Object... params) {
        try {
            int[] types = new int[params.length];
            for (int i = 0; i < params.length; i++) {
                Object o = params[i];
                if (o == null) throw new IllegalArgumentException("Do not use null values without specify its types.");
                Class<?> clazz = params[i].getClass();
                types[i] = default_sql_type(clazz);
            }
            return oper.execute(sql, params, types);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    static int default_sql_type(Class<?> cls) {
        if (cls.equals(Integer.class) || cls.equals(int.class)){
            return Types.INTEGER;
        } else if (cls.equals(Double.class) || cls.equals(double.class)) {
            return Types.DOUBLE;
        } else if (cls.equals(Float.class) || cls.equals(float.class)) {
            return Types.FLOAT;
        } else if (cls.equals(Short.class) || cls.equals(short.class)) {
            return Types.SMALLINT;
        }else if (cls.equals(Boolean.class) || cls.equals(boolean.class)) {
            return Types.BOOLEAN;
        }else if (cls.equals(String.class) ){
            return Types.VARCHAR;
        }else if (cls.equals(Character.class) || cls.equals(char.class)) {
            return Types.VARCHAR;
        }else if( cls.equals(InputStream.class)) {
            return Types.BLOB;
        }else if( cls.equals(BigDecimal.class)) {
            return Types.DECIMAL;
        }
        throw new IllegalArgumentException("Class "+cls.toString()+" not supported,please specify input types.");
    }

    /**
     * //TODO: untested
     *
     * @param tbName table name
     */
    public void drop(String tbName) {
        exec("DROP TABLE " + tbName);
    }

    /**
     * //TODO: untested
     *
     * @param tbName table name
     */
    public void truncate(String tbName) {
        exec("TRUNCATE TABLE " + tbName);
    }

    public ResultSet query(String sql) {
        try {
            return oper.query(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
    }

    public ResultSet query(String sql, String... x) {
        try {
            return oper.query(sql, x);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
    }

    private void cleanComma(StringBuilder sb) {
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.delete(sb.length() - 1, sb.length());
        }
    }


    /*----CURD
    Record#db
    -------*/

    public boolean add(Record record) {
        List<Tuple<String, Object>> values = new ArrayList<>();
//        for (String f : r.declaredFields()) {
//            Object val = r.get(f);
//            if (val != null) {
//                values.add(Tuple.t2(f, val));
//            }
//        }
        Map<String, Object> db = record.db();

        _for(db).each(e -> {
            if (e.getValue() != null)
                values.add(Tuple.t2(e.getKey(), e.getValue()));
        });

        String[] keys = _for(values).map(t -> t._a).join();

        Sql sql = Sql.insert().into(record.table()).values(S.array.of(values));
        logger.debug(sql.debug());
        try {
            return oper.execute(sql.preparedSql(), sql.params(),
                    DB.getTypes(record.table(), keys)) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
    }

    public boolean del(Record record) {
        Sql sql = Sql.delete().from(record.table())
                .where(record.idName(), Criterion.EQ, (String) record.id());
        logger.debug(sql.debug());
        try {
            return oper.execute(sql.preparedSql(), sql.params(),
                    new int[]{DB.getType(record.table(), record.idName())}) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
    }


    public boolean upd(Record record) {
        List<Tuple<String, Object>> sets = new ArrayList<>();

        Map<String, Object> db = record.db();

        _for(db).each(e -> sets.add(Tuple.t2(e.getKey(), e.getValue())));
        String[] keys = _for(sets).map(t -> t._a).join();
        Sql sql = Sql.update(record.table()).set(S.array.of(sets))
                .where(record.idName(), Criterion.EQ, (String) record.id());
        logger.debug(sql.debug());
        // 多出来的最后一个类型是id
        // types = [types_of_set, $ types_of id]
        int[] types_of_sets = DB.getTypes(record.table(), keys);
        int[] types = new int[types_of_sets.length + 1];

        System.arraycopy(types_of_sets, 0, types, 0, types_of_sets.length);
        //types of id
        types[types.length-1] = DB.getType(record.table(),record.idName());

        try {
            return oper.execute(sql.preparedSql(), sql.params(),types) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
    }

    @Override
    public void close() throws IOException {
        this.oper.close();
    }

    public Set<String> tables() {
        try {
            return oper.getTableNames();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeSQLException(e);
        }
    }

    /**
     * in case ...
     *
     * @return oper
     */
    @Deprecated
    public JDBCOper oper() {
        return this.oper;
    }

}
