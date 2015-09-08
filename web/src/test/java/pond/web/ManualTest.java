package pond.web;

import pond.common.S;

/**
 * Created by ed on 9/2/15.
 */
public class ManualTest {

  static void a(){
    Pond app = Pond.init();

    app.use((req, resp) -> {
      req.ctx().put("val", 1);
    });

    app.get("/testCtx", (req, resp) -> {
      req.ctx().put("a", "a");
      resp.send(200, req.ctx().get("a").toString() + req.ctx().get("val"));
    });
    app.listen();
  }

  static class A {
    static String log = "STATIC A";

    public String toString(){
      return log;
    }
  }

  static class B extends A{
    static String log = "STATIC B";
    public String a(){
      return log;
    }
  }


  static void test_router(){
    Pond.init(app -> {
      app.use((req, resp) -> {
        S.echo("INSTALLLLLLLLLLLLLLLLLLLLL");
        req.ctx().put("val", 1);
      });

      app.get("/testCtx", (req, resp) -> {
        req.ctx().put("a", "a");
        resp.send(200, req.ctx().get("a").toString() + req.ctx().get("val"));
      });
    }).debug().listen(9090);
  }

  static void b(){
    S.echo(new B().toString());
    S.echo(B.log);
  }

  public static void main(String[] args){
//    b();
    test_router();
  }

}
