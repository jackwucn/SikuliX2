/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.core.SX;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

class ScriptTableModel extends AbstractTableModel {

  public ScriptTableModel(Script script, int maxCol, List<List<ScriptCell>> data) {
    this.data = data;
    this.maxCol = maxCol;
    this.script = script;
  }

  Script script = null;
  List<List<ScriptCell>> data = null;
  int maxCol = 1;

  public int getColumnCount() {
    return maxCol + 1;
  }

  public int getRowCount() {
    if (data.size() == 0) {
      data.add(new ArrayList<>());
    }
    return data.size();
  }

  public String getColumnName(int col) {
    if (col == 0) return "    Line";
    if (col == 1) return "1 - Command";
    return String.format("%d - Item", col);
  }

  public Object getValueAt(int row, int tableCol) {
    int dataCol = Math.max(0, tableCol - 1);
    ScriptCell commandCell = script.commandTableCell(row);
    if (tableCol == Script.numberCol) {
      String format = "%6d%s %s";
      String sHidden = "";
      if (commandCell.isFirstHidden()) {
        sHidden = commandCell.getHidden() > 0 ? "+" : "-";
      }
      return String.format(format, script.data.get(row).get(0).getRow() + 1, sHidden, commandCell.getMarker());
    }
    String indentSpace = "";
    if (tableCol == Script.commandCol) {
      indentSpace = commandCell.getIndentSpace();
    }
    List<ScriptCell> line = data.get(row);
    if (dataCol > line.size() - 1) {
      return "";
    }
    return indentSpace + line.get(dataCol).get();
  }

  public Class getColumnClass(int c) {
    return String.class;
  }

  public boolean isCellEditable(int row, int col) {
    return true;
  }

  public void setValueAt(Object value, int row, int tableCol) {
    if (row < 0) {
      if (tableCol < 0) {
        fireTableDataChanged();
        return;
      }
    }
    if (tableCol == 0) {
      return;
    }
    int dataCol = tableCol - 1;
    String given = ((String) value).trim();
    List<ScriptCell> line = script.data.get(row);
    ScriptCell cell = script.data.get(row).get(dataCol);
    if (SX.isNull(cell)) {
      script.cellSet(row, dataCol, "");
    }
    if (tableCol == 1) {
      if (script.isLineEmpty(line)) {
        script.addCommandTemplate(given);
      }
    } else {
      cell.set(given);
      fireTableCellUpdated(row, tableCol);
    }
  }

  public void cellUpdated(int row, int col) {
    fireTableCellUpdated(row, col);
  }
}
