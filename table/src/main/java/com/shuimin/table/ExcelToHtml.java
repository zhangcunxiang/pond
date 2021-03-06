package com.shuimin.table;

import org.apache.poi.hssf.converter.ExcelToHtmlConverter;
import org.w3c.dom.Document;
import pond.common.FILE;
import pond.common.PATH;
import pond.common.S;
import pond.common.STREAM;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Created by ed on 8/14/14.
 */
public class ExcelToHtml {


  public static void convert(InputStream xls, OutputStream out_) {
    String rootPath = PATH.classpathRoot();
    File tmp = new File(rootPath, "tmp");
    if (!tmp.exists()) {
      S._assert(tmp.mkdirs());
    }
    File f = new File(tmp, "in_" + S.now() + ".xls");
    File o = new File(tmp, "out_" + S.now() + ".xls");
    try {
      if (!(f.createNewFile() && o.createNewFile()))
        throw new RuntimeException("can not create tmp files");
      FILE.inputStreamToFile(xls, f);
      Document doc = ExcelToHtmlConverter.process(f);
      PrintWriter out = new PrintWriter(new OutputStreamWriter(out_, Charset.defaultCharset()));
      DOMSource domSource = new DOMSource(doc);
      StreamResult streamResult = new StreamResult(out);

      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer serializer = tf.newTransformer();
      // TODO set encoding from a command argument
      serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      serializer.setOutputProperty(OutputKeys.INDENT, "no");
      serializer.setOutputProperty(OutputKeys.METHOD, "html");
      serializer.transform(domSource, streamResult);

      STREAM.write(new FileInputStream(o), out_);

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      S._assert(f.delete());
      S._assert(o.delete());
    }

  }
}
