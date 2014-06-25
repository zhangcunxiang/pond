package com.shuimin.pond.core.kernel;

import com.shuimin.common.S;
import com.shuimin.common.abs.Config;
import com.shuimin.pond.core.spi.Logger;
import com.shuimin.pond.core.spi.logger.SimpleLogger;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by ed on 2014/5/7.
 */
public final class PKernel {

    private static final Logger defaultLogger = new SimpleLogger();
    private static ConcurrentMap<String, Object> holder =
            new ConcurrentHashMap<>();
    private static ConcurrentMap<Class, Object> serviceHolder = new ConcurrentHashMap<>();

    public static <E> void register(String name, E o, Config<E> config) {
        if (config != null)
            config.config(o);
        getLogger().debug(String.format("Set %s = %s", name, o));
        holder.put(name, o);
    }

    public static void userService(Class cl, Object serv) {
        register(cl.getCanonicalName(),serv, null);
    }

    public static <E> E userService(Class cl) {
        return get(cl.getCanonicalName());
    }

    public static <E> void register(Class<? super E> clazz, E o) {
        register(clazz.getCanonicalName(), o, null);
    }
    @SuppressWarnings("unchecked")
    public static <E> E get(String name) {
        return (E) holder.get(name);
    }

    public static <E> void set(E e) {
        holder.put(e.getClass().getCanonicalName(), e);
    }

    public static Logger getLogger() {
        return S._notNullElse(findService(Logger.class), defaultLogger);
    }

    public static <E> E getService(Class<E> serviceClass) {
        @SuppressWarnings("unchecked")
        E service = (E) serviceHolder.get(serviceClass);
        if (service == null) {
            service = findService(serviceClass);
            if (service == null) throw
                    new RuntimeException("service[" + serviceClass + "] not found suitable provider");
            serviceHolder.putIfAbsent(serviceClass, service);
        }
        return service;
    }

    private static <S> S findService(Class<S> clazz) {
        ServiceLoader<S> serviceLoader = ServiceLoader.load(clazz);
        Iterator<S> sit;
        if ((sit = serviceLoader.iterator()).hasNext()) {
            return sit.next();
        }
        return null;
    }
}
