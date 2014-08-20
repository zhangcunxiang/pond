package com.shuimin.pond.core.mw;

import com.shuimin.common.S;
import com.shuimin.common.f.Callback;
import com.shuimin.pond.core.Request;
import com.shuimin.pond.core.Response;
import com.shuimin.pond.core.http.HttpMethod;
import com.shuimin.pond.core.router.Router;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Controller extends Router {

    public Controller(){
        convertToRouter();
    }
    private void convertToRouter() {
        Method[] methods =
                this.getClass().getMethods();
        for (Method m : methods) {
            Annotation[] annos = m.getAnnotations();
            for (Annotation a : annos) {
                if (a instanceof Mapping) {
                    //find a handler
                    String val = ((Mapping) a).value();
                    if(S.str.isBlank(val)){
                        val = "/"+m.getName();
                    }
                    this.use(HttpMethod.mask(((Mapping) a).methods()),
                            val,
                            (req, resp, next) -> {
                                Object[] args =
                                 _setReqAndResToMethod(m,req,resp);
                                try {
                                    m.setAccessible(true);
                                    m.invoke(this,args);
                                    next.apply();
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException(e);
                                } catch (InvocationTargetException e) {
                                    throw new RuntimeException(e.getTargetException());
                                }
                            });
                }
            }
        }
    }

    private Object[] _setReqAndResToMethod(Method m,Request req,
                                           Response response) {
        Class<?>[] types =
                m.getParameterTypes();
        Object[] ret = new Object[types.length];
        Class<?> c;
        for (int i = 0; i < types.length; i++) {
            c = types[i];
            if(c.isAssignableFrom(Request.class))
                ret[i] = req;
            if(c.isAssignableFrom(Response.class))
                ret[i] = response;
        }
        return ret;
    }

   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.METHOD)
    public @interface Mapping {
        String value() default "";

        HttpMethod[] methods() default {
                HttpMethod.GET,
                HttpMethod.POST
        };
    }

    @Override
    public void apply(Request req, Response resp, Callback.C0 next) {
        super.apply(req, resp, ()->{});
    }
}

