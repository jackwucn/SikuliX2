/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.api.Do;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.EventObject;

class ScriptTable extends JTable {

  private Script script;

  public ScriptTable(Script script, AbstractTableModel tableModel) {
    super(tableModel);
    this.script = script;
  }

  @Override
  public boolean editCellAt(int row, int col, EventObject e) {
    script.currentCell = null;
    if (e instanceof KeyEvent) {
      int keyCode = ((KeyEvent) e).getExtendedKeyCode();
      if (keyCode == 0 || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_META) {
        return false;
      }
      if (col == 0) {
        if (keyCode == KeyEvent.VK_PLUS) {
          script.data.add(row + 1, new ArrayList<>());
          tableHasChanged();
          setSelection(row + 1, col + 1);
          new Thread(new Runnable() {
            @Override
            public void run() {
              Do.write("#ESC.");
            }
          }).start();
          return false;
        }
        if (keyCode == KeyEvent.VK_BACK_SPACE) {
          PopUpMenus.savedLine = script.cellAt(row, col + 1).setLine();
          tableHasChanged();
          setSelection(row, col);
          return false;
        }
        if (keyCode == KeyEvent.VK_GREATER) {
          script.indent(row, col);
          return false;
        }
        if (keyCode == KeyEvent.VK_LESS) {
          script.dedent(row, col);
          return false;
        }
      }
      if (col == 1 && keyCode == KeyEvent.VK_SPACE && script.cellAt(row, col).isEmpty()) {
        Rectangle cellRect = getCellRect(row, col, false);
        PopUpMenus.Command.pop(this, cellRect.x, cellRect.y);
        return false;
      } else if (keyCode == KeyEvent.VK_SPACE) {
        script.editBox(row, col);
        return false;
      } else if (keyCode == KeyEvent.VK_BACK_SPACE && script.cellAt(row, col).isEmpty()) {
        getModel().setValueAt(script.savedCell, row, col);
        return false;
      } else if (keyCode == KeyEvent.VK_F1) {
        Script.log.trace("(%d,%d): F1: help: %s", row, col, getValueAt(row, col));
        return false;
      } else if (keyCode == KeyEvent.VK_F2) {
        Script.log.trace("Colx: F2: save script");
        script.saveScript();
        return false;
      } else if (keyCode == KeyEvent.VK_F3) {
        Script.log.trace("Colx: F3: open script");
        script.loadScript();
        getModel().setValueAt("", -1, -1);
        return false;
      } else if (keyCode == KeyEvent.VK_F4) {
        Script.log.trace("Colx: F4: run script");
        if (col == 0) {
          script.runScript(0, script.data.size() - 1);
        } else {
          script.runScript(row, row);
        }
        return false;
      } else if (keyCode == KeyEvent.VK_F5) {
        Script.log.trace("(%d,%d): F5: capture: %s", row, col, script.cellAt(row, col).get());
        script.cellAt(row, col).capture();
        return false;
      } else if (keyCode == KeyEvent.VK_F6) {
        Script.log.trace("(%d,%d): F6: show: %s", row, col, script.cellAt(row, col).get());
        script.cellAt(row, col).show();
        return false;
      } else if (keyCode == KeyEvent.VK_F7) {
        Script.log.trace("(%d,%d): F7: find: %s", row, col, script.cellAt(row, col).get());
        script.cellAt(row, col).find();
        return false;
      } else if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
        Script.log.trace("(%d,%d): DELETE: make cell empty", row, col);
        script.savedCell = (String) getModel().getValueAt(row, col);
        getModel().setValueAt("", row, col);
        return false;
      }
      Script.log.trace("keycode: %d %s", keyCode, KeyEvent.getKeyText(keyCode));
    }
    if (col > 0) {
      return super.editCellAt(row, col, e);
    }
    return false;
  }

  protected void setSelection(int row, int col) {
    setRowSelectionInterval(row, row);
    setColumnSelectionInterval(col, col);
  }

  //TODO correct possible focus problems
  protected void tableHasChanged() {
    getModel().setValueAt(null, -1, -1);
  }

  protected void tableHasChanged(int row, int col) {
    getModel().setValueAt(null, row, col);
  }
}
