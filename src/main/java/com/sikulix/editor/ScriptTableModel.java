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
    if (col == 1) return "Command";
    return String.format("Item%d", col - 1);
  }

  public Object getValueAt(int tableRow, int tableCol) {
    ScriptCell cell = script.tableCell(tableRow, tableCol);
    if (SX.isNull(cell)) {
      return "";
    }
    int dataRow = script.lines.get(tableRow);
    if (tableCol == Script.numberCol) {
      ScriptCell commandCell = script.dataCell(dataRow, Script.commandCol);
      if (commandCell.isFirstHidden()) {
        return String.format("  V-%s-V ", commandCell.getHidden());
      }
      return String.format("%6d %s", dataRow + 1, commandCell.getMarker());
    }
    int lineCol = tableCol - 1;
    List<ScriptCell> line = data.get(dataRow);
    if (lineCol > line.size() - 1) {
      return "";
    }
    return script.dataCell(dataRow, tableCol).get();
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
      row = -row - 1;
      if ("DELETE".equals(value.toString())) {
        fireTableRowsDeleted(row, col);
        return;
      }
      if ("INSERT".equals(value.toString())) {
        fireTableRowsInserted(row, col);
        return;
      }
      if ("UPDATE".equals(value.toString())) {
        fireTableRowsUpdated(row, col);
        return;
      }
    }

    String given = ((String) value).trim();
    if (col == 0) {
      return;
    }
    ScriptCell cell = script.tableCell(row, col);
    if (SX.isNull(cell)) {
      int dataRow = script.lines.get(row);
      if (dataRow == -1) {
        if (col != Script.commandCol) {
          return;
        }
        int tableRow = 0;
        Integer[] linesArray = script.lines.toArray(new Integer[0]);
        for (int ref : linesArray) {
          if (ref == -1) {
            List<ScriptCell> newLine = new ArrayList<>();
            newLine.add(new ScriptCell(script, "", data.size(), Script.commandCol));
            data.add(newLine);
            script.lines.set(tableRow, data.size() - 1);
          }
          if (tableRow == row) {
            break;
          }
          tableRow++;
        }
      } else {
        script.dataCellSet(dataRow, col, "");
      }
      cell = script.tableCell(row, col);
    }
    if (col == 1) {
      if (given.isEmpty()) {
        cell.set(given);
        script.checkContent();
        script.setSelection(cell);
      } else {
        if (!cell.isLineEmpty()) {
          cell.set(given);
          script.checkContent();
        } else {
          script.addCommandTemplate(given, cell);
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
