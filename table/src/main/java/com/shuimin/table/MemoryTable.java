package com.shuimin.table;

import pond.common.f.Function;
import pond.common.struc.MatrixO;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ed on 2014/5/4.
 */
public class MemoryTable extends MatrixO implements Table {


  public MemoryTable(int rows, int cols,
                     Function.F2<Object, Integer, Integer> provider) {
    super(rows, cols, provider);
  }

  @Override
  public void set(int row, int col, Object value) {
    super.set(row, col, value);
  }

  @Override
  public Object[][] toArray() {
    Object[][] ret = new Object[rows][cols];
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        ret[r][c] = super.get(r, c);
      }
    }
    return ret;
  }

  @Override
  public MemoryTable init(int i, int j, Object initVal) throws IOException {
    return new MemoryTable(i, j, (a, b) -> initVal);
  }

  @Override
  public List<Object> row(int i) {
    return Arrays.asList(super.getRow(i));
  }

  @Override
  public List<Object> col(int i) {
    return Arrays.asList(super.getCol(i));
  }

}
