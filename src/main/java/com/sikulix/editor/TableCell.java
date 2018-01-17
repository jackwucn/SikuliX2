/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import java.awt.*;
import java.util.ArrayList;

public class TableCell {
  int row = -1;
  int col = -1;

  Script script = null;

  private TableCell() {}

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

  public TableCell nextRow() {
    return new TableCell(script,row + 1);
  }

  public TableCell previousRow() {
    return new TableCell(script,row - 1);
  }

  protected Rectangle getRect() {
    return script.table.getCellRect(row, col, false);
  }

  protected boolean isLineEmpty() {
    for (ScriptCell cell : script.data.get(script.lines.get(row))) {
      if (!cell.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  protected void lineSet(String... items) {
    int dataRow = script.lines.get(row);
    if (items.length == 0) {
      script.data.set(dataRow, new ArrayList<>());
      script.data.get(dataRow).add(new ScriptCell(script, "", Script.commandCol));
    } else {
      int col = 1;
      for (String item : items) {
        script.dataCellSet(dataRow, col++, item);
      }
    }
  }

  protected void lineAdd(String... items) {
    int dataRow;
    if (row < 0) {
      dataRow = 0;
      script.data.add(0, new ArrayList<>());
    } else if (row > script.data.size() - 1) {
      script.data.add(new ArrayList<>());
      dataRow = script.data.size() -1;
    } else {
      dataRow = script.lines.get(row);
      if (dataRow < 0) {
        script.data.add(new ArrayList<>());
        dataRow = script.data.size() - 1;
      } else {
        script.data.add(dataRow + 1, new ArrayList<>());
        dataRow++;
      }
    }
    new TableCell(script, dataRow).lineSet(items);
  }
}
