/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ScriptCell {

  enum CellType {COMMAND, IMAGE, SCRIPT, VARIABLE, LIST, MAP, IMAGELIST, IMAGEMAP, TEXT}

  private Script script;
  private String value = "";
  private CellType cellType = CellType.TEXT;
  private int row = -1;
  private int col = -1;
  private int indentLevel = 0;
  private int indentIfLevel = 0;
  private int indentLoopLevel = 0;
  private boolean inError = false;

  protected ScriptCell() {

  }

  protected ScriptCell(Script script, String value, int row, int col) {
    this.script = script;
    this.value = value.trim();
    this.row = row;
    this.col = col;
  }

  protected int getRow() {
    return row;
  }

  protected String getMarker() {
    if (inError) {
      return "?!";
    }
    if (indentLevel > 0) {
      return String.format(">%d", indentLevel);
    }
    return "";
  }

  protected boolean hasError() {
    return inError;
  }

  protected void reset() {
    indentLevel = indentIfLevel = indentLoopLevel = 0;
    inError = false;
    value = value.replace(" ?!", "");
  }

  protected void setIndent(int level, int ifLevel, int loopLevel) {
    if (level > -1) {
      indentLevel = level;
    }
    if (ifLevel > -1) {
      indentIfLevel = ifLevel;
    }
    if (loopLevel > -1) {
      indentLoopLevel = loopLevel;
    }
  }

  protected int getIndent() {
    return indentLevel;
  }

  protected int getIfIndent() {
    return indentIfLevel;
  }

  protected int getLoopIndent() {
    return indentLoopLevel;
  }

  private boolean block = false;

  protected ScriptCell asImage() {
    if (isEmpty() || "@".equals(value)) {
      value = "@?";
      imagename = "";
    } else if (!value.startsWith("@")) {
      imagename = value;
      value = "@?" + value;
    } else {
      imagename = value.replace("@", "").replace("?", "").trim();
    }
    cellType = CellType.IMAGE;
    return this;
  }

  protected boolean isImage() {
    return CellType.IMAGE.equals(cellType);
  }

  String imagename = "";
  Picture picture = null;

  private void getImage() {
    imagename = value.replace("@", "").replace("?", "");
    imagename = Do.input("Image Capture", "... enter a name", imagename);
    if (SX.isSet(imagename)) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          script.getWindow().setVisible(false);
          SX.pause(1);
          picture = Do.userCapture();
          if (SX.isNotNull(picture)) {
            picture.save(imagename, script.getScriptPath().getParent());
          } else {
            imagename = "?" + imagename;
          }
          value = "@" + imagename;
          script.table.setValueAt(value, row, col);
          script.getWindow().setVisible(true);
        }
      }).start();
    }
  }

  protected void capture() {
    if (isEmpty() || value.startsWith("@")) {
      asImage().getImage();
    }
  }

  protected Element getCellClick() {
    Point windowLocation = script.getWindow().getLocation();
    Rectangle cell = script.table.getCellRect(row, col, false);
    windowLocation.x += cell.x + 10;
    windowLocation.y += cell.y + 70;
    return new Element(windowLocation);
  }

  protected Rectangle getRect() {
    return script.table.getCellRect(row, col, false);
  }

  protected void select() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Do.on().clickFast(getCellClick());
      }
    }).start();
  }

  protected void show() {
    if (!isEmpty() && CellType.IMAGE.equals(cellType)) {
      if (isValid()) {
        loadPicture();
        if (SX.isNotNull(picture)) {
          new Thread(new Runnable() {
            @Override
            public void run() {
              picture.show(1);
              Do.on().clickFast(getCellClick());
            }
          }).start();
        } else {
          value = "@?" + imagename;
          script.table.setValueAt(value, row, col);
        }
      }
    }
  }

  protected void find() {
    if (!isEmpty() && CellType.IMAGE.equals(cellType)) {
      if (isValid()) {
        loadPicture();
        if (SX.isNotNull(picture)) {
          new Thread(new Runnable() {
            @Override
            public void run() {
              script.getWindow().setVisible(false);
              Do.find(picture);
              Do.on().showMatch();
              script.getWindow().setVisible(true);
            }
          }).start();
        } else {
          value = "@?" + imagename;
          script.table.setValueAt(value, row, col);
        }
      }
    }
  }

  private void loadPicture() {
    if (SX.isNull(picture)) {
      File fPicture = new File(script.getScriptPath().getParentFile(), imagename + ".png");
      if (fPicture.exists()) {
        picture = new Picture(fPicture.getAbsolutePath());
      }
    }
  }

  protected ScriptCell asScript() {
    if (!value.startsWith("{")) {
      value = "{" + value;
    }
    cellType = CellType.SCRIPT;
    return this;
  }

  protected boolean isScript() {
    return CellType.SCRIPT.equals(cellType);
  }

  protected ScriptCell asVariable() {
    if (!value.startsWith("$")) {
      value = "$" + value;
    }
    cellType = CellType.VARIABLE;
    return this;
  }

  protected boolean isVariable() {
    return CellType.VARIABLE.equals(cellType);
  }

  protected String eval(int row, int col) {
    return "";
  }

  protected boolean isEmpty() {
    return SX.isNotSet(value);
  }

  protected boolean isLineEmpty() {
    for (ScriptCell cell : script.data.get(row)) {
      if (!cell.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  protected boolean isValid() {
    boolean valid = SX.isSet(value);
    if (valid && isImage()) {
      valid &= !value.contains("?");
    }
    return valid;
  }

  protected String get() {
    return value;
  }

  protected ScriptCell set(int row, int col) {
    this.row = row;
    this.col = col;
    return this;
  }

  protected ScriptCell set(String value) {
    this.value = value;
    return this;
  }

  protected ScriptCell setValue(String value) {
    if (SX.isNotNull(value)) {
      this.value = value;
    }
    script.table.setValueAt(value, row, col);
    return this;
  }

  protected void setError() {
    if (!value.endsWith("?!")) {
      value += " ?!";
    }
    inError = true;
  }

  protected List<ScriptCell> setLine(String... items) {
    List<ScriptCell> oldLine = new ArrayList<>();
    for (ScriptCell cell : script.data.get(row)) {
      oldLine.add(cell);
    }
    if (items.length == 0) {
      script.data.set(row, new ArrayList<>());
    } else {
      int col = 1;
      for (String item : items) {
        script.cellAt(row, col++).set(item);
      }
    }
    return oldLine;
  }

  protected void addLine(String command) {
    script.data.add(row + 1, new ArrayList<>());
    script.cellAt(row + 1, 1).setLine(command);
    script.table.tableHasChanged();
    script.table.setSelection(row, 1);
  }

  protected void newLine(int[] selectedRows) {
    int numLines = selectedRows.length;
    int currentRow = selectedRows[numLines - 1];
    for (int n = 0; n < numLines; n++) {
      script.data.add(currentRow + n + 1, new ArrayList<>());
    }
    script.table.tableCheckContent();
    script.table.setSelection(currentRow + 1, 1);
  }

  protected void deleteLine(int[] selectedRows) {
    script.savedLine.clear();
    int currentRow = selectedRows[0];
    for (int delRow : selectedRows) {
      script.savedLine.add(script.data.remove(currentRow));
    }
    script.table.tableCheckContent();
    script.table.setSelection(Math.max(0, currentRow - 1), 1);
  }

  protected void emptyLine(int[] selectedRows) {
    script.savedLine.clear();
    int currentRow = selectedRows[0];
    for (int emptyRow : selectedRows) {
      script.savedLine.add(script.cellAt(emptyRow, 1).setLine());
    }
    script.table.tableCheckContent();
    script.table.setSelection(currentRow, 1);
  }

  protected void copyLine(int[] selectedRows) {
    for (int copyRow : selectedRows) {
      script.savedLine.add(script.data.get(copyRow));
    }
  }

  protected void runLine(int[] selectedRows) {
    script.runScript(selectedRows[0], selectedRows[selectedRows.length - 1]);
  }

  protected void insertLine(int[] selectedRows) {
    int numLines = script.savedLine.size();
    int currentRow = selectedRows[selectedRows.length - 1];
    for (int n = 0; n < numLines; n++) {
      script.data.add(currentRow + n + 1, script.savedLine.get(n));
    }
    script.table.tableCheckContent();
    script.table.setSelection(row + 1, 1);
  }
}
