package com.shuimin.pond.codec.db;

import com.shuimin.common.f.Function;
import com.shuimin.common.util.logger.Logger;
import com.shuimin.pond.codec.connpool.ConnectionPool;
import com.shuimin.pond.core.exception.UnexpectedException;
import com.shuimin.pond.core.misc.Makeable;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.shuimin.common.S._throw;

/**
 * Created by ed on 2014/4/15.
 * <pre>
 *     面向Connection的数据库连接抽象
 *     try(DB b = new DB().....){
 *         //do your job
 *     }
 * </pre>
 */
public class DB implements Makeable<DB>, Closeable {
    private static final Logger logger = Logger.get();
    private JdbcTmpl tmpl;

    public DB() {
    }

    /**
     * <pre>
     *     快速的执行一个db操作
     * </pre>
     * @param connection 数据库连接
     * @param process 数据库链接打开之后执行的操作，参数为 JdbcTmpl
     * @see com.shuimin.pond.codec.db.JdbcTmpl
     * @return 执行后的结果
     */
    public static <R> R fire(Connection connection,
                           Function<R,JdbcTmpl> process) {
        try(DB b = new DB().open(()->connection)) {
            return  b.exec(process);
        }
    }

    /**
     * @see com.shuimin.pond.codec.db.DB
     *  #fire(java.sql.Connection, com.shuimin.common.f.Function, com.shuimin.common.f.Function)
     * @param connection 数据库链接
     * @param process  处理JdbcTmpl
     * @param finisher 二次处理
     *
     * @param <R> 最终结果类型
     * @param <M> 中间结果类型
     *
     * @return 最终计算结果
     */
    public static <R, M> R fire(Connection connection,
                                Function<M, JdbcTmpl> process,
                                Function<R, M> finisher) {

        try (DB b = new DB().open(() -> connection)) {
            return b.exec(process, finisher);
        }

    }

    /**
     * <p>打开一个jdbc数据库链接</p>
     * @param driverClass
     * @param connUrl
     * @param username
     * @param password
     * @return this
     */
    public DB open(String driverClass,
                   String connUrl,
                   String username,
                   String password) {

        tmpl = new JdbcTmpl(new JdbcOperator(
            newConnection(driverClass, connUrl, username, password)));
        return this;
    }

    /**
     * <p>得到当前DB对象的jdbcTmpl值</p>
     * @return JdbcTmpl
     */
    public JdbcTmpl tmpl() {
        return tmpl;
    }


    /**
     * <p>做一次查询,并映射到结果集，然后返回结果集</p>
     * @param queryFunc 查询方法
     * @param mapper 映射方法
     * @param <T> 返回类型
     * @return 类型为 T 的返回结果
     */
    public <T> T query(Function<ResultSet, JdbcTmpl> queryFunc,
                       Function<T, ResultSet> mapper) {
        return mapper.apply(queryFunc.apply(tmpl));
    }

    /**
     * <p>执行</p>
     * @param process
     * @param <R>
     * @return
     */
    public <R> R exec(Function<R,JdbcTmpl> process) {
       return process.apply(tmpl);
    }

    /**
     * @see com.shuimin.pond.codec.db.DB#exec(com.shuimin.common.f.Function)
     */
    public <R, T> R exec(Function<T, JdbcTmpl> process,
                         Function<R, T> finisher) {
        return finisher.apply(process.apply(tmpl));
    }

    public DB open(Function._0<Connection> connectionSupplier) {
        tmpl = new JdbcTmpl(new JdbcOperator(connectionSupplier.apply()));
        return this;
    }

    public DB open(ConnectionPool p) {
        tmpl = new JdbcTmpl(new JdbcOperator(p.getConnection()));
        return this;
    }

    protected static Connection newConnection(String driverClass,
                                              String connUrl,
                                              String username,
                                              String password) {
        try {
            DriverManager.registerDriver((java.sql.Driver)
                Class.forName(driverClass).newInstance());
        } catch (ClassNotFoundException e) {
            logger.debug("cont find class [" + driverClass + "]");
            e.printStackTrace();
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            return DriverManager.getConnection(connUrl, username, password);
        } catch (SQLException e) {
            logger.debug("cont open connection, sql error: " + e.toString());
            throw new UnexpectedException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (tmpl != null) {
                tmpl.close();
            }
        } catch (IOException e) {
            _throw(e);
        }
    }

}
