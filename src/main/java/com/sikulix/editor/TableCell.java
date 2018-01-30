/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import java.util.ArrayList;
import java.util.List;

public class TableCell {
  int row = -1;
  int col = -1;
  private boolean header = false;

  Script script = null;

  private TableCell() {
  }

  public TableCell(Script script, int row, int col) {
    this.row = row;
    this.col = col;
    this.script = script;
  }

  public TableCell(Script script, int row) {
    this.row = row;
    this.col = 0;
    this.script = script;
  }

  public void setHeader() {
    header = true;
  }

  public boolean isHeader() {
    return header;
  }

  public boolean isLineNumber() {
    return col == 0;
  }

  public TableCell nextRow() {
    return new TableCell(script, row + 1);
  }

  public TableCell previousRow() {
    return new TableCell(script, row - 1);
  }

  public TableCell nextCol() {
    return new TableCell(script, row, Math.min(script.maxCol, col + 1));
  }

  public TableCell previousCol() {
    return new TableCell(script, row, Math.max(0, col - 1));
  }

  protected boolean isLineEmpty() {
//    for (ScriptCell cell : script.data.get(script.lines.get(row))) {
    for (ScriptCell cell : script.data.get(row)) {
      if (!cell.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  boolean isFirstHidden() {
    return getDataCell().isFirstHidden();
  }

  ScriptCell getDataCell() {
    return script.data.get(row).get(Math.max(0, col - 1));
  }
}
