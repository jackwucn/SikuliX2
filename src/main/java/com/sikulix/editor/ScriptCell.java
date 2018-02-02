/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.api.Do;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ScriptCell {

  enum CellType {COMMAND, IMAGE, SCRIPT, VARIABLE, LIST, MAP, IMAGELIST, IMAGEMAP, TEXT}

  private Script script;
  private String value = "";
  private String initialValue = "";
  private CellType cellType = CellType.TEXT;

  private int col = -1;

  public int getCol() {
    return col;
  }

  public boolean isItemCol() {
    return col > 1;
  }

  private int row = -1;

  public int getRow() {
    return row;
  }

  public void setRow(int row) {
    this.row = row;
  }

  private int indentLevel = 0;
  private int indentIfLevel = 0;
  private int indentLoopLevel = 0;
  private int indentFunctionLevel = 0;

  private boolean inError = false;
  private String status = "";

  protected String getStatus() {
    return status;
  }

  private ScriptCell() {

  }

  protected ScriptCell(Script script, String value, int col) {
    init(script, value, col, -1);
  }

  protected ScriptCell(Script script, String value, int col, int row) {
    init(script, value, col, row);
  }

  private void init(Script script, String value, int col, int row) {
    this.script = script;
    this.value = value.trim();
    initialValue = this.value;
    this.col = col;
    this.row = row;
  }

  protected ScriptCell copy() {
    ScriptCell cell = new ScriptCell(script, value, col);
    cell.initialValue = this.initialValue;
    return cell;
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

  protected String getIndentSpace() {
    String space = "";
    for (int n = 0; n < indentLevel; n++) {
      space += "  ";
    }
    return space;
  }

  protected boolean hasError() {
    return inError;
  }

  protected void reset() {
    indentLevel = indentIfLevel = indentLoopLevel = 0;
    inError = false;
    value = value.replace(" ?!", "");
  }

  protected void resetAll() {
    if (col == Script.commandCol) {
      indentLevel = indentIfLevel = indentLoopLevel = 0;
      inError = false;
      hiddenCount = 0;
    }
    value = "";
  }

  protected void setIndent(int level, int ifLevel, int loopLevel, int functionLevel) {
    if (level > -1) {
      indentLevel = level;
    }
    if (ifLevel > -1) {
      indentIfLevel = ifLevel;
    }
    if (loopLevel > -1) {
      indentLoopLevel = loopLevel;
    }
    if (functionLevel > -1) {
      indentFunctionLevel = loopLevel;
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

  protected int getFunctionIndent() {
    return indentFunctionLevel;
  }

  protected ScriptCell asImage() {
    if (isImage()) {
      return this;
    }
    if (isEmpty() || "@".equals(value)) {
      value = "@?";
      imagename = "";
    } else if (!value.startsWith("@") && !value.startsWith("$")) {
      imagename = value;
      value = "@" + value + "?";
    } else {
      imagename = value.replace("@", "").replace("?", "").trim();
    }
    cellType = CellType.IMAGE;
    return this;
  }

  protected boolean isImage() {
    return SX.isSet(value) && value.startsWith("@") && !value.startsWith("@@");
  }

  String imagename = "";
  Picture picture = null;

  protected void capture(int row, int col) {
    if (isEmpty() || isImage()) {
      asImage();
      imagename = value.replace("@", "").replace("?", "");
      if ("what".equals(imagename)) imagename = "";
      imagename = Do.input("Image Capture", "... enter a name", imagename);
      if (SX.isNotNull(imagename)) {
        if (SX.isNotSet(imagename)) {
          imagename = "img" + (script.images.size() + 1);
        }
        if (!script.images.contains(imagename)) {
          script.images.add(imagename);
        }
        new Thread(new Runnable() {
          @Override
          public void run() {
            script.getWindow().setVisible(false);
            SX.pause(1);
            ScriptCell.this.picture = Do.userCapture();
            if (SX.isNotNull(ScriptCell.this.picture)) {
              ScriptCell.this.picture.save(imagename, script.getScriptPath().getParent());
              ScriptCell.this.picture = null;
            } else {
              if (!existsPicture()) {
                imagename = imagename + "?";
              }
            }
            value = "@" + imagename;
            script.getWindow().setVisible(true);
          }
        }).start();
      } else {
        script.getWindow().setVisible(true);
      }
    }
  }

  protected void show(int row, int col) {
    if (!isEmpty()) {
      if (isImage() && isValid()) {
        loadPicture();
        if (SX.isNotNull(picture)) {
          new Thread(new Runnable() {
            @Override
            public void run() {
              //new Element(tCell.getRect()).show(2);
              picture.show(1);
              script.getWindow().setVisible(true);
              picture = null;
            }
          }).start();
        } else {
          value = "@" + imagename + "?";
        }
      }
    }
  }

  protected void find() {
    if (!isEmpty()) {
      if (asImage().isValid()) {
        loadPicture();
        if (SX.isNotNull(picture)) {
          new Thread(new Runnable() {
            @Override
            public void run() {
              script.getWindow().setVisible(false);
              Do.find(picture);
              Do.on().showMatch();
              script.getWindow().setVisible(true);
              picture = null;
            }
          }).start();
        } else {
          value += "?";
        }
      }
    }
  }

  private String getImageName() {
    if (value.startsWith("@")) {
      return value.substring(1).replace("?", "");
    }
    return "";
  }

  private void loadPicture() {
    File fPicture = new File(script.getScriptPath().getParentFile(), getImageName() + ".png");
    if (fPicture.exists()) {
      picture = new Picture(fPicture.getAbsolutePath());
    }
  }

  private boolean existsPicture() {
    File fPicture = new File(script.getScriptPath().getParentFile(), getImageName() + ".png");
    return fPicture.exists();
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

  protected boolean isValid() {
    return SX.isSet(value) && !value.contains("?");
  }

  private boolean header = false;

  public void setHeader() {
    header = true;
  }

  public boolean isHeader() {
    return header;
  }

  protected String get() {
    return value;
  }

  protected ScriptCell set(int col) {
    this.col = col;
    return this;
  }

  protected ScriptCell set(String value) {
    this.value = value;
    return this;
  }

  protected ScriptCell setInitial(String value) {
    initialValue = value;
    return this;
  }

  protected String getInitial() {
    return initialValue;
  }

  protected ScriptCell setValue(String value, int tableRow, int tableCol) {
    if (SX.isNotNull(value)) {
      this.value = value;
    }
    script.table.setValueAt(value, tableRow, tableCol);
    return this;
  }

  protected void setError() {
    if (!value.endsWith("?!")) {
      value += " ?!";
    }
    inError = true;
  }

  private int hiddenCount = 0;

  protected void setHidden(int count) {
    hiddenCount = count;
  }

  protected int getHidden() {
    return hiddenCount;
  }

  protected boolean isFirstHidden() {
    return hiddenCount != 0;
  }

  protected boolean isFirstCollapsed() {
    return hiddenCount > 0;
  }

  protected boolean isLineEmpty() {
    for (ScriptCell cell : script.data.get(row)) {
      if (!cell.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  public String toString() {
    return String.format("Cell: (%d,%d) %s i(%d %d %d) h%d", row, col, value, indentLevel, indentIfLevel, indentLoopLevel, hiddenCount);
  }
}
