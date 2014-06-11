package com.shuimin.common.s;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

import static com.shuimin.common.S.echo;

public class CgLibTest {
    public static void main(String[] args) {

        Foo created = (Foo) Enhancer.create(Foo.class, new Interceptor());

        echo(created.doSomething());

    }

    public static class Foo {
        public String doSomething() {
            return "I do something";
        }
    }

    private static class Interceptor implements MethodInterceptor {
        public Interceptor() {
        }

        @Override
        public Object intercept(Object o, Method method, Object[] args,
                                MethodProxy mp) throws Throwable {
            return "I do nothing";
        }
    }

}