package pond.web;

import pond.common.S;
import pond.common.STRING;
import pond.common.f.Callback;
import pond.web.http.HttpMethod;

import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Controller extends Router {

  public Controller() {
    convertToRouter();
  }


  final static
  Map<Class<? extends Annotation>,
      Callback.C3<Controller, Annotation, Method>> annotationResolvePolices =
      S._tap(new HashMap<>(),
             map -> map.put(Mapping.class, (ctrl, a, m) -> {

               String val = ((Mapping) a).value();

               //if blank, use methodName as mapping name
               if (STRING.isBlank(val)) {
                 val = "/" + m.getName();
               }

               ctrl.use(HttpMethod.mask(((Mapping) a).methods()),
                        val,
                        (req, resp) -> {
                          Object[] args = new Object[]{req, resp};
                          S._try(() -> {
                            m.setAccessible(true);
                            m.invoke(ctrl, args);
                          });
                        });
             })
      );

  /**
   * @param cls    annotation class
   * @param policy policy procedure -- callback with controller, annotation, method
   */
  public static void newAnnotationResolvePolicy(Class<? extends Annotation> cls,
                                                Callback.C3<Controller, Annotation, Method> policy) {
    S._assertNotNull(cls, policy);
    annotationResolvePolices.put(cls, policy);
  }


  private void convertToRouter() {
    Method[] methods = this.getClass().getDeclaredMethods();

    for (Method m : methods) {

      Annotation[] annotations = m.getDeclaredAnnotations();

      for (Annotation a : annotations) {

        Class aClass = a.annotationType();

        for (Map.Entry<Class<? extends Annotation>,
            Callback.C3<Controller, Annotation, Method>> e : annotationResolvePolices.entrySet()) {
          Class defClass = e.getKey();

          //for annotations, strict equal works
          if (defClass.equals(aClass)) {
            e.getValue().apply(this, a, m);
            break;
          }
        }
      }
    }
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

}

