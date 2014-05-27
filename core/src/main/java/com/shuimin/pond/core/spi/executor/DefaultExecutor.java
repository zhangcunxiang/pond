package com.shuimin.pond.core.spi.executor;

import com.shuimin.common.f.Function;
import com.shuimin.pond.core.*;
import com.shuimin.pond.core.exception.HttpException;
import com.shuimin.pond.core.exception.YException;
import com.shuimin.pond.core.kernel.PKernel;
import com.shuimin.pond.core.spi.Logger;
import com.shuimin.pond.core.spi.MiddlewareExecutor;

import java.util.Iterator;

import static com.shuimin.common.S._notNull;
import static com.shuimin.pond.core.Pond.debug;

/**
 * Created by ed on 2014/5/8.
 */
public class DefaultExecutor implements MiddlewareExecutor {


    Logger logger = PKernel.getLogger();

    @Override
    public ExecutionContext execute(

        Function.F0<ExecutionContext> executionProvider,
        Function.F0<Iterable<Middleware>> midProvider) {


        ExecutionContext ctx = executionProvider.apply();
        Iterator<Middleware> midIter = _notNull(midProvider.apply())
            .iterator();

        Request req = ctx.req();
        Response resp = ctx.resp();

        logger.debug("request path : " + req.path());
        try {
            while (midIter.hasNext()) {
                Middleware m = midIter.next();
                debug("executing mid :" + m);
                try {
                    m.handle(ctx);
                } catch (Interrupt.JumpInterruption jump) {
                    //continue;
                } catch (Interrupt.KillInterruption kill) {
                    break;
                } catch (Interrupt.RedirectInterruption redirection) {
                    ctx.resp().redirect(redirection.uri());
                    return ctx;
                } catch (Interrupt.RenderInterruption render) {
                    render.value().render(ctx.resp());
                    return ctx;
                } catch (HttpException e) {
                    //not report until it be the last 404
                    if (e.code() == 404 && midIter.hasNext()) {
                        //continue;
                    } else {
                        throw e;
                    }
                }
            }
            //default
            ctx.resp().send(200);

        } catch (HttpException e) {
            resp.sendError(e.code(), e.getMessage());
        } catch (YException e) {
            resp.sendError(500, e.toString());
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            ExecutionContext.executionContexts.remove();
            return ctx;
        }
    }

}
