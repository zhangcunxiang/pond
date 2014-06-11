package com.shuimin.common.f;

import java.util.HashSet;

/**
 * Created by ed on 2014/4/30.
 */

/**
 * Use it by caution
 */
@Deprecated
public class NamedParams extends HashSet<Object[]> {

    public NamedParams(Object[]... namedArgs) {
        for (Object[] a : namedArgs) {
            if (a.length != 2)
                throw new RuntimeException(
                        "invalid named-parameter length, must be 2.");
            if (!(a[0] instanceof String))
                throw new RuntimeException(
                        "invalid named-parameter type, first must be String."
                );
            this.add(a);
        }
    }

    public <E> E get(String name) {
        for (Object[] a : this) {
            if (a[0].equals(name)) return (E) a[1];
        }
        return null;
    }

}
