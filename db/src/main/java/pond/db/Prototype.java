package pond.db;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ed on 10/31/14.
 */
public class Prototype {
  static Map<Class<? extends Record>, Record> protos = new HashMap<>();


  public static <E extends Record> E proto(Class<E> cls) {
    Record t = protos.get(cls);

    if (t == null) {
      t = Record.newValue(cls);
      protos.put(cls, t);
    }

    return (E) t;
  }


}
