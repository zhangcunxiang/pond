package pond.db;

import org.junit.Test;
import pond.common.S;

import java.util.Collections;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void testA(){
        String a = String.format("jdbc:mysql://%s/%s?%s", "ssss","weee","wwww");
        assertEquals("jdbc:mysql://ssss/weee?wwww",a);
    }


    @Test
    public void test(){
        Object a = Collections.emptyMap().get("ss");
        System.out.print(a);
    }
}
