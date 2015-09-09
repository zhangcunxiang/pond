package pond.web.spi.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import pond.common.S;
import pond.common.STRING;
import pond.common.f.Callback;
import pond.common.f.Tuple;
import pond.web.Request;
import pond.web.Response;
import pond.web.http.Cookie;
import pond.web.http.HttpUtils;
import pond.web.spi.BaseServer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created by ed on 9/3/15.
 */
class NettyHttpHandler extends SimpleChannelInboundHandler<Object> {

  private HttpPostRequestDecoder decoder;

  HttpRequest httpRequest = null;

  NettyReqWrapper reqWrapper = null;

  //  CompositeByteBuf content = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, true, 2);
//  CompositeByteBuf content = Unpooled.compositeBuffer();
  CompositeByteBuf content;

  String contentType;

  final Callback.C2<Request, Response> handler;

  final ExecutorService executorService;

  NettyHttpHandler(Callback.C2<Request, Response> handler, ExecutorService executorService) {
    this.handler = handler;
    this.executorService = executorService;
  }

  static {

    DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
    // on exit (in normal // exit)
    DiskFileUpload.baseDirectory = null; // system temp directory
    DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
    // exit (in normal exit)
    DiskAttribute.baseDirectory = null; // system temp directory
  }


  private static final HttpDataFactory factory =
      new DefaultHttpDataFactory(DefaultHttpDataFactory.MAXSIZE); // Disk if size exceed

  private void sendBadRequest(ChannelHandlerContext ctx) {
    ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                              HttpResponseStatus.BAD_REQUEST));
  }

  @Override
  protected void messageReceived(ChannelHandlerContext ctx, Object msg) {

    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;

      S._debug(BaseServer.logger, log -> {
        log.debug("GOT HTTP REQUEST:");
        log.debug(request.toString());
      });

      if (HttpHeaderUtil.is100ContinueExpected(request)) {
        ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                              HttpResponseStatus.CONTINUE));
      }

      if (!request.decoderResult().isSuccess()) {
        sendBadRequest(ctx);
        return;
      }

      //do the initialize

      httpRequest = request;
      //build the req
      reqWrapper = new NettyReqWrapper(ctx, httpRequest);

      //parse headers
      reqWrapper.updateHeaders(
          headers ->
              S._for(httpRequest.headers())
                  .each(
                      entry ->
                          HttpUtils.appendToMap(
                              headers,
                              entry.getKey().toString(),
                              httpRequest.headers().getAllAndConvert(entry.getKey()))
                  )
      );

      //uri-queries
      Map<String, List<String>> parsedParams = new QueryStringDecoder(httpRequest.uri()).parameters();

      S._debug(BaseServer.logger, log -> {
        log.debug("QUERY STRING: " + httpRequest.uri());
        log.debug("PARSED PARAMS: " + S.dump(parsedParams));
      });

      reqWrapper.updateParams(params -> params.putAll(parsedParams));

      //parse cookies
      reqWrapper.updateCookies(

          cookies -> {
            Set<io.netty.handler.codec.http.Cookie> decoded
                = ServerCookieDecoder.decode(
                S.avoidNull(request.headers().getAndConvert(HttpHeaderNames.COOKIE), "")
            );

            S._for(decoded).each(
                cookie ->
                    cookies.put(cookie.name(), S._tap(
                        new Cookie(cookie.name(), cookie.value()),
                        c -> {
                          c.setPath(cookie.path());
                          c.setMaxAge((int) cookie.maxAge());
                          if (STRING.notBlank(cookie.domain()))
                            c.setDomain(cookie.domain());
                          c.setComment(cookie.comment());
                        }))
            );
          });


      contentType = httpRequest.headers().getAndConvert(HttpHeaderNames.CONTENT_TYPE);
      //build the multipart decoder
      if (HttpPostRequestDecoder.isMultipart(httpRequest)) {
        HttpMethod method = httpRequest.method();

        if (method.equals(HttpMethod.POST)
            || method.equals(HttpMethod.PUT)
            || method.equals(HttpMethod.PATCH)) {
          try {
            decoder = new HttpPostRequestDecoder(factory, request);
          } catch (HttpPostRequestDecoder.ErrorDataDecoderException err) {
            BaseServer.logger.error(err.getMessage(), err);
            sendBadRequest(ctx);
            return;
          }
        } else {
          BaseServer.logger.error("unexpected multipart request caught : invalid http method: " + method);
          sendBadRequest(ctx);
          return;
        }
      } else {
        //do nothing
        content = Unpooled.compositeBuffer();
      }
    }

    if (msg instanceof HttpContent) {
      HttpContent httpContent = (HttpContent) msg;

      if (!httpContent.decoderResult().isSuccess()) {
        sendBadRequest(ctx);
        return;
      }

      if (httpRequest == null) {
        ctx.fireExceptionCaught(new NullPointerException("httpRequest"));
        return;
      }

      //multipart
      if (decoder != null && HttpPostRequestDecoder.isMultipart(httpRequest)) {
        try {
          Tuple<List<Attribute>, List<FileUpload>>
              tuple = decodeHttpContent(decoder, httpContent);

          reqWrapper.updateUploadFiles(
              files -> S._for(tuple._b).each(
                  fileUpload -> HttpUtils.appendToMap(
                      files, fileUpload.getName(), new NettyUploadFile(fileUpload))
              )
          );

          reqWrapper.updateParams(
              params ->
                  S._for(tuple._a).each(attr -> {
                    String k = attr.getName();
                    String v = S._try_ret(attr::getValue);
                    S._debug(BaseServer.logger, log -> log.debug(k + " " + S.dump(v)));
                    HttpUtils.appendToMap(params, k, v);
                  })
          );

        } catch (Exception e1) {
          BaseServer.logger.error(e1.getMessage(), e1);
          S._debug(BaseServer.logger, log -> log.debug(e1.getMessage(), e1));
          sendBadRequest(ctx);
          return;
        }

      } else {
        //merge chunks
        ByteBuf chunk = httpContent.content();
        if (chunk.isReadable()) {
          chunk.retain();
          content.addComponent(httpContent.content());
          content.writerIndex(content.writerIndex() + chunk.readableBytes());
        }
      }

      //bind inputStream
      if (reqWrapper != null && content != null) {
        reqWrapper.content(content);
      }
      //end of message
      if (msg instanceof LastHttpContent) {

        //merge trailing headers
        LastHttpContent trailer = (LastHttpContent) msg;
        if (!trailer.decoderResult().isSuccess()) {
          ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                                                    HttpResponseStatus.BAD_REQUEST));
          return;
        }

        //trailing headers
        if (!trailer.trailingHeaders().isEmpty()) {
          for (CharSequence name : trailer.trailingHeaders().names()) {
            for (CharSequence value : trailer.trailingHeaders().getAll(name)) {
              S._debug(BaseServer.logger,
                       log -> log.debug("TRAILING HEADER: " + name + " : " + value));
              httpRequest.headers().set(name, value);
              reqWrapper.updateHeaders(
                  headers -> HttpUtils.appendToMap(headers, name.toString(), value.toString()));
            }
          }
        }

        //handle the http content TODO add hooks
        if (contentType == null || contentType.toLowerCase().contains(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toLowerCase())) {

          String postData = content.toString(CharsetUtil.UTF_8);

          S._debug(BaseServer.logger, log -> log.debug("postData: " + postData));

          //default x-www-form-urlencoded parse
          Map<String, List<String>> postParams = new QueryStringDecoder(postData, CharsetUtil.UTF_8, false).parameters();

          S._debug(BaseServer.logger, log -> log.debug(S.dump(postParams)));
          S._for(postParams).each(entry -> {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            S._debug(BaseServer.logger, log -> log.debug(key + S.dump(value)));
            reqWrapper.updateParams(params -> HttpUtils.appendToMap(params, key, value));
          });
        } else {

          //TODO REFACTOR customized content parser
        }

        //execution context
        final HandlerExecutionContext exe_ctx = new HandlerExecutionContext();

        S._debug(BaseServer.logger, log -> log.debug(reqWrapper.toString()));

        //build the response
        NettyRespWrapper respWrapper = new NettyRespWrapper(exe_ctx);

        long _start_time = S.now();

        S._debug(BaseServer.logger, logger -> {
          logger.debug("resp build at " + _start_time);
        });

        S._debug(BaseServer.logger,
                 log -> log.debug("TRACE: run the exe-ctx"));

        boolean isKeepAlive = HttpHeaderUtil.isKeepAlive(httpRequest);

        executorService.submit(() -> {
          //this would affect the execution ctx
          try {
            handler.apply(reqWrapper, respWrapper);
            S._debug(BaseServer.logger,
                     log -> log.debug("exe-ctx costs " + (S.now() - _start_time) + " ms"));
            if (exe_ctx.isSuccess()) {
              switch (exe_ctx.type()) {
                case HandlerExecutionContext.UNHANDLED: {
                  BaseServer.logger.warn("unhandled request reach.");
                  sendBadRequest(ctx);
                  return;
                }
                case HandlerExecutionContext.NORMAL: {
                  sendNormal(ctx, exe_ctx.response(), isKeepAlive);
                  return;
                }
                case HandlerExecutionContext.STATIC_FILE: {
                  sendFile(ctx,
                           exe_ctx.response(),
                           exe_ctx.sendfile(),
                           exe_ctx.sendFileOffset(),
                           exe_ctx.sendFileLength(),
                           isKeepAlive
                  );
                  return;
                }
              }
            } else {
              //maybe reset, timeout ....
              //ctx.fireExceptionCaught(exe_ctx.getCause());
            }
          } catch (Exception e) {
            ctx.fireExceptionCaught(e);
          } finally {
            S._debug(BaseServer.logger,
                     log -> log.debug("TRACE: IO-SEND finished, now make clean"));
            clean();
            S._debug(BaseServer.logger,
                     log -> log.debug("TRACE: Clean finished"));
          }
        });

        S._debug(BaseServer.logger,
                 log -> log.debug("TRACE: IO-READ finished"));
      }

    }


  }


  Tuple<List<Attribute>, List<FileUpload>> decodeHttpContent(HttpPostRequestDecoder decoder, HttpContent httpContent)
      throws HttpPostRequestDecoder.ErrorDataDecoderException,
      HttpPostRequestDecoder.EndOfDataDecoderException,
      HttpPostRequestDecoder.NotEnoughDataDecoderException {

    decoder.offer(httpContent);

    List<Attribute> attrs = new ArrayList<>();
    List<FileUpload> fileUploads = new ArrayList<>();
    Tuple<List<Attribute>, List<FileUpload>> ret = Tuple.pair(attrs, fileUploads);

    while (decoder.hasNext()) {
      InterfaceHttpData data = decoder.next();
      if (data != null) {
//          // check if current HttpData is a FileUpload and previously set as partial
//          if (partialContent == data) {
//            S._debug(logger, log -> log.debug(" 100% (FinalSize: " + partialContent.length() + ")" + " 100% (FinalSize: " + partialContent.length() + ")"));
////            partialContent = null;
//          }

        InterfaceHttpData.HttpDataType type = data.getHttpDataType();


        switch (type) {
          case Attribute: {
            Attribute attr = (Attribute) data;

            S._debug(BaseServer.logger, log -> {
              try {
                log.debug("PARSE ATTR: " + attr.getName() + " : " + attr.getValue());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

            attrs.add(attr);
            break;
          }
          case FileUpload: {
            FileUpload fileUpload = (FileUpload) data;
            S._debug(BaseServer.logger, log -> {
              try {
                log.debug("PARSE FILE: " + fileUpload.getName()
                              + " : " + fileUpload.getFilename()
                              + " : " + fileUpload.getFile().getAbsolutePath());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
            fileUploads.add(fileUpload);
            break;
          }
          //TODO internal attribute
        }
      }
    }
    return ret;
  }

  void sendFile(ChannelHandlerContext ctx,
                Response response,
                RandomAccessFile raf,
                Long sendoffset,
                Long sendlength,
                boolean isKeepAlive) {
    NettyRespWrapper wrapper = ((NettyRespWrapper) response);

    long offset = sendoffset == null ? 0l : sendoffset;
    long length = 0;
    try {
      length = sendlength == null ? raf.length() : sendlength;
    } catch (IOException e) {
      ctx.fireExceptionCaught(e);
    }

    HttpResponse resp = wrapper.resp;

    ctx.write(resp);
    // Write the content.
    ChannelFuture sendFileFuture;
    ChannelFuture lastContentFuture;
//    if (ctx.pipeline().get(SslHandler.class) == null) {
//      sendFileFuture =
//          ctx.write(new DefaultFileRegion(raf.getChannel(), offset, length), ctx.newProgressivePromise());
//      // Write the end marker.
//      lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
//    } else {
    try {
      sendFileFuture =
          ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, offset, length, 65536)),
                            ctx.newProgressivePromise());
      lastContentFuture = sendFileFuture;

    } catch (IOException e) {
      ctx.fireExceptionCaught(e);
      return;
    }
    // HttpChunkedInput will pipe the end marker (LastHttpContent) for us.
//    }

    sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
      @Override
      public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
        if (total < 0) { // total unknown
          S._debug(BaseServer.logger, logger ->
              logger.debug(future.channel() + " Transfer progress: " + progress));
        } else {
          S._debug(BaseServer.logger, logger ->
              logger.debug(future.channel() + " Transfer progress: " + progress + " / " + total));
        }
      }

      @Override
      public void operationComplete(ChannelProgressiveFuture future) throws Exception {
        S._debug(BaseServer.logger, logger ->
            logger.debug(future.channel() + " Transfer complete."));
      }
    });

    // Decide whether to close the connection or not.
    if (!isKeepAlive)
      // Close the connection when the whole content is written out.
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);

  }

  void sendNormal(ChannelHandlerContext ctx, Response response, boolean isKeepAlive) {

    NettyRespWrapper wrapper = ((NettyRespWrapper) response);


    wrapper.writer.flush();

    //sendNormal
    if (isKeepAlive) {

      wrapper.resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

      if (wrapper.resp.headers().get(HttpHeaderNames.CONTENT_LENGTH) == null) {
        int contentLen = wrapper.buffer.readableBytes();
        wrapper.resp.headers().setLong(HttpHeaderNames.CONTENT_LENGTH, contentLen);
      }
    } else {
      wrapper.resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    }


    HttpResponse resp = wrapper.resp;
    ByteBuf content = wrapper.buffer;

    S._debug(BaseServer.logger, log -> {
      log.debug("----SEND STATUS---");
      log.debug(S.dump(resp.status()));

      log.debug("----SEND HEADERS---");
      log.debug(S.dump(resp.headers()));

      log.debug("----SEND BUFFER DUMP---");

      if (content.readableBytes() > 1000)
        log.debug("Content too large to display!");
      else
        log.debug(content.toString(CharsetUtil.UTF_8));
    });
    ChannelFuture lastContentFuture;

    //write head
    ctx.write(resp);
    //write content
    ctx.write(content);

    lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
//    lastContentFuture.addListener(future -> S._debug(BaseServer.logger, logger ->
//        logger.debug("all costs: " + (S.now() - wrapper._start_time) + "ms")));

    if (!isKeepAlive)
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
  }

  void clean() {
    httpRequest = null;
    reqWrapper = null;
//    content.clear();
    if (content != null && content.refCnt() > 0) {
      content.release();
    }
    resetDecoder();
  }


  void resetDecoder() {
    if (decoder != null) {
      decoder.cleanFiles();
      decoder.destroy();
      decoder = null;
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    clean();

    S._debug(BaseServer.logger, logger -> logger.debug(cause.getMessage(), cause));

    if (ctx.channel().isActive()) {
      //send error and close
      ctx.writeAndFlush(new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          Unpooled.copiedBuffer(cause.getMessage(), CharsetUtil.UTF_8)
      )).addListener(ChannelFutureListener.CLOSE);
    }

  }

}
