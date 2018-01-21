/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.core.SX;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.KeyEvent;
import java.util.EventObject;

class ScriptTable extends JTable {

  private Script script;

  public ScriptTable(Script script, AbstractTableModel tableModel) {
    super(tableModel);
    this.script = script;
  }

  @Override
  public boolean editCellAt(int tableRow, int tableCol, EventObject e) {
    TableCell tCell = new TableCell(script, tableRow, tableCol);
    ScriptCell currentCell = script.evalDataCell(tableRow, tableCol);
    boolean isLineNumber = tableCol == Script.numberCol;
    boolean isCommand = tableCol == Script.commandCol;
    int keyCode = 0;
    if (SX.isNotNull(currentCell)) {
      if (e instanceof KeyEvent) {
        keyCode = ((KeyEvent) e).getExtendedKeyCode();
        if (keyCode == 0) {
          keyCode = ((KeyEvent) e).getExtendedKeyCode();
        }
        if (keyCode == 0 || keyCode == KeyEvent.VK_META) {
          return false;
        }
        boolean isCtrl = false;
        boolean isAlt = false;
        boolean isShift = false;
        int modifier = ((KeyEvent) e).getModifiers();
        if (modifier == KeyEvent.CTRL_MASK) {
          isCtrl = true;
        }
        if (modifier == KeyEvent.ALT_MASK) {
          isAlt = true;
        }
        if (modifier == KeyEvent.SHIFT_MASK) {
          isShift = true;
        }
        if (isLineNumber) {
          if (keyCode == KeyEvent.VK_ESCAPE) {
            int firstLine = script.table.getSelectedRows()[0];
            if (script.table.getSelectedRows().length > 1) {
              script.table.setLineSelection(firstLine, firstLine);
            }
            return false;
          }
          if (keyCode == KeyEvent.VK_SPACE) {
            script.popUpMenus.action(tCell);
            return false;
          }
          if (keyCode == KeyEvent.VK_PLUS) {
            currentCell.lineNew(getSelectedRows());
            return false;
          }
          if (keyCode == KeyEvent.VK_MINUS) {
            currentCell.lineDelete(getSelectedRows());
            return false;
          }
          if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (isCtrl) {
              script.log.trace("editCellAt: CTRL Backspace");
            } else {
              currentCell.lineEmpty(getSelectedRows());
            }
            setSelection(tableRow, Script.commandCol);
            return false;
          }
          if (keyCode == KeyEvent.VK_SLASH || keyCode == KeyEvent.VK_NUMBER_SIGN ||
                  keyCode == KeyEvent.VK_BRACELEFT) {
            String token = keyCode == KeyEvent.VK_SLASH ? "/" :
                    (keyCode == KeyEvent.VK_NUMBER_SIGN ? "#" : "{");
            currentCell.lineNew(new int[]{tableRow}, token);
            return false;
          }
          if (keyCode == KeyEvent.VK_E) {
            currentCell.lineEmpty(getSelectedRows());
            return false;
          }
          if (keyCode == KeyEvent.VK_C) {
            currentCell.lineCopy(getSelectedRows());
            return false;
          }
          if (keyCode == KeyEvent.VK_I) {
            currentCell.lineInsert(getSelectedRows());
            return false;
          }
          if (keyCode == KeyEvent.VK_H) {
            currentCell.lineHide(getSelectedRows());
            return false;
          }
          if (keyCode == KeyEvent.VK_R) {
            currentCell.lineRun(getSelectedRows());
            return false;
          }
        }
        if (isCommand && keyCode == KeyEvent.VK_SPACE && currentCell.isEmpty()) {
          script.popUpMenus.command(tCell);
          return false;
        } else if (keyCode == KeyEvent.VK_SPACE) {
          if (!isCommand) {
            if (isShift) {
              setSelection(tCell.row, 0);
              script.popUpMenus.action(new TableCell(script, tCell.row, 0));
            } else {
              script.editBox(tCell);
            }
          }
          return false;
        } else if (keyCode == KeyEvent.VK_BACK_SPACE && currentCell.isEmpty()) {
          if (isCommand) {
            script.setValueAt(script.savedCellText, tCell);
          } else {
            currentCell.setValue(script.savedCellText, tableRow, tableCol);
          }
          return false;
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
          setSelection(tCell.row, 0);
          return false;
        } else if (keyCode == KeyEvent.VK_F1) {
          script.assist(tCell);
          return false;
        } else if (keyCode == KeyEvent.VK_F2) {
          Script.log.trace("F2: save script");
          script.saveScript();
          return false;
        } else if (keyCode == KeyEvent.VK_F3) {
          Script.log.trace("F3: open script");
          script.loadScript();
          return false;
        } else if (keyCode == KeyEvent.VK_F4) {
          Script.log.trace("F4: run script");
          if (isLineNumber) {
            script.runScript(-1);
          } else {
            script.runScript(tableRow);
          }
          return false;
        } else if (keyCode == KeyEvent.VK_F5) {
          Script.log.trace("F5: find");
          currentCell.find();
          return false;
        } else if (keyCode == KeyEvent.VK_F6) {
          return false;
        } else if (keyCode == KeyEvent.VK_F7) {
          return false;
        } else if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
          script.savedCellText = currentCell.get();
          currentCell.setValue("", tableRow, tableCol);
          return false;
        }
        Script.log.trace("keycode: %d %s", keyCode, KeyEvent.getKeyText(keyCode));
      }
    }
    if (!isLineNumber) {
      if (e instanceof KeyEvent && keyCode == KeyEvent.VK_ESCAPE) {
        return false;
      }
      return super.editCellAt(tableRow, tableCol, e);
    }
    return false;
  }

  protected void setSelection(int row, int col) {
    setRowSelectionInterval(row, row);
    setColumnSelectionInterval(col, col);
  }

  protected void setLineSelection(int firstRow, int lastRow) {
    setRowSelectionInterval(firstRow, lastRow);
    setColumnSelectionInterval(0, 0);
  }

  //TODO correct possible focus problems
  protected void tableHasChanged() {
//    for (int ix = 0; ix < script.lines.size(); ix++) {
//      if (script.lines.get(ix) < 0) {
//        setValueAt("DELETE", -ix, ix);
//      }
//    }
    setValueAt(null, -1, -1);
  }

  protected void tableCheckContent() {
    script.checkContent();
  }

  protected void resetLineCol() {
    for (int n = 0; n < getModel().getRowCount(); n++) {
      ((ScriptTableModel) getModel()).cellUpdated(n, 0);
    }
  }
}
