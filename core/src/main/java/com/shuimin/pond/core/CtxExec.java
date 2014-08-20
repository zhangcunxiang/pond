package com.shuimin.pond.core;

import com.shuimin.common.f.Callback;
import com.shuimin.pond.core.exception.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;


public class CtxExec {

    static Logger logger = LoggerFactory.getLogger(CtxExec.class);
    private static ThreadLocal<Ctx> ctxThreadLocal = new ThreadLocal<Ctx>();

    public static Ctx get() {
        return ctxThreadLocal.get();
    }

    static Throwable unwrapRuntimeException(RuntimeException e){
        Throwable t = e.getCause();
        if(t == null)return e;
        if(t instanceof RuntimeException){
            return unwrapRuntimeException((RuntimeException) t);
        }
        return t;
    }
    /**
     * run a ctx
     *
     * @param ctx
     */
    public static void exec(Ctx ctx, List<Mid> mids) {
        Callback.C3<Request, Response, Callback.C0> mid = ctx.nextMid();
        ctx.addMid(mids);
        try {
            ctxThreadLocal.set(ctx);
            if (mid != null) {
                logger.info("uri="+ctx.req.path()+",mid="+mid.toString());
                mid.apply(ctx.req, ctx.resp,
                        () -> exec(ctx, Collections.<Mid>emptyList()));
            }
        } catch (HttpException e) {
            e.printStackTrace();
            ctx.resp.send(e.code(), e.getMessage());
        } catch(RuntimeException e){
            Throwable t = unwrapRuntimeException(e);
            t.printStackTrace();
            ctx.resp.send(500, t.getMessage());
        } catch (Throwable e) {
            e.printStackTrace();
            ctx.resp.send(500, e.getMessage());
//            throw new RuntimeException(e);
        } finally {
            ctxThreadLocal.remove();
        }
    }
}
