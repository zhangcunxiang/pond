package com.shuimin.pond.core.spi;

import java.io.OutputStream;
import java.util.Map;

/**
 * Created by ed on 2014/5/8.
 */
public interface ViewEngine{

    void render(OutputStream out,
                String relativePath,
                Object data);
}
