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

  public Object getValueAt(int tableRow, int tableCol) {
    ScriptCell cell = script.evalDataCell(tableRow, tableCol);
    if (SX.isNull(cell)) {
      return "";
    }
    int dataRow = tableRow;
    ScriptCell commandCell = script.dataCell(dataRow, Script.commandCol - 1);
    if (tableCol == Script.numberCol) {
      String format = "%6d%s %s";
      String sHidden = "";
      if (commandCell.isFirstHidden()) {
        sHidden = commandCell.getHidden() > 0 ? "+" : "-";
      }
      return String.format(format, script.data.get(tableRow).get(0).getRow() + 1, sHidden, commandCell.getMarker());
    }
    String indentSpace = "";
    if (tableCol == Script.commandCol) {
      indentSpace = commandCell.getIndentSpace();
    }
    int lineCol = tableCol - 1;
    List<ScriptCell> line = data.get(dataRow);
    if (lineCol > line.size() - 1) {
      return "";
    }
    return indentSpace + script.dataCell(dataRow, tableCol - 1).get();
  }

  public Class getColumnClass(int c) {
    return String.class;
  }

  public boolean isCellEditable(int row, int col) {
    return true;
  }

  public void setValueAt(Object value, int row, int col) {
    if (row < 0) {
      if (col < 0) {
        fireTableDataChanged();
        return;
      }
    }
    if (col == 0) {
      return;
    }
    String given = ((String) value).trim();
    ScriptCell cell = script.evalDataCell(row, col);
    if (SX.isNull(cell)) {
      int dataRow = script.data.get(row).get(0).getRow();
      if (dataRow == -1) {
        script.log.error("TableModel.setValueAt: row %d no data", row);
      } else {
        script.dataCellSet(dataRow, col, "");
      }
    }
    if (col == 1) {
      if (given.isEmpty()) {
        cell.set(given);
        script.checkContent();
        script.table.setSelection(row, col);
      } else {
        if (cell.isLineEmpty()) {
          script.addCommandTemplate(given, new TableCell(script, row, col), null);
        } else {
          cell.set(given);
          script.checkContent();
        }
      }
    } else {
      cell.set(given);
      fireTableCellUpdated(row, col);
    }
  }

  public void cellUpdated(int row, int col) {
    fireTableCellUpdated(row, col);
  }
}
