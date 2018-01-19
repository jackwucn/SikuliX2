/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ScriptCell {

  enum CellType {COMMAND, IMAGE, SCRIPT, VARIABLE, LIST, MAP, IMAGELIST, IMAGEMAP, TEXT}

  private Script script;
  private String value = "";
  private CellType cellType = CellType.TEXT;
//  private int row = -1;
  private int col = -1;
  private int indentLevel = 0;
  private int indentIfLevel = 0;
  private int indentLoopLevel = 0;
  private boolean inError = false;

  protected ScriptCell() {

  }

  protected ScriptCell(Script script, String value, int col) {
    init(script, value, col);
  }

  private void init(Script script, String value, int col) {
    this.script = script;
    this.value = value.trim();
    this.col = col;
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

  protected ScriptCell asImage() {
    if (CellType.IMAGE.equals(cellType)) {
      return this;
    }
    if (isEmpty() || "@".equals(value)) {
      value = "@?";
      imagename = "";
    } else if (!value.startsWith("@") && !value.startsWith("$")) {
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

  protected void capture(TableCell tCell) {
    if (isEmpty() || value.startsWith("@")) {
      asImage();
      imagename = value.replace("@", "").replace("?", "");
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
                imagename = "?" + imagename;
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

  protected void show(TableCell tCell) {
    if (!isEmpty()) {
      if (asImage().isValid()) {
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
          value = "@?" + imagename;
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
          value = "@?" + imagename;
        }
      }
    }
  }

  private void loadPicture() {
    File fPicture = new File(script.getScriptPath().getParentFile(), imagename + ".png");
    if (fPicture.exists()) {
      picture = new Picture(fPicture.getAbsolutePath());
    }
  }

  private boolean existsPicture() {
    File fPicture = new File(script.getScriptPath().getParentFile(), imagename + ".png");
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
    boolean valid = SX.isSet(value);
    if (valid && isImage()) {
      valid &= !value.contains("?");
    }
    return valid;
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

  protected void lineHide(int[] selectedRows) {
    int currentDataRow = selectedRows[0];
    ScriptCell firstCell = script.dataCell(currentDataRow, Script.commandCol - 1);
    if (selectedRows.length == 1) {
      if (firstCell.isFirstHidden()) {
        firstCell.setHidden(0);
        for (List<ScriptCell> line : firstCell.getHiddenData()) {
          script.data.add(++currentDataRow, line);
        }
        firstCell.setHiddenData(null);
      }
    } else {
      firstCell.setHidden(selectedRows.length);
      firstCell.setHiddenData(script.createData(currentDataRow + 1, currentDataRow + selectedRows.length - 1));
    }
    script.table.tableCheckContent();
    select(selectedRows[0] + 1, Script.numberCol);
  }

  private int hiddenCount = 0;

  private List<List<ScriptCell>> hiddenData = null;

  protected void setHidden(int count) {
    hiddenCount = count;
  }

  protected void setHiddenData(List<List<ScriptCell>> hiddenData) {
    this.hiddenData = hiddenData;
  }

  protected List<List<ScriptCell>> getHiddenData() {
    return hiddenData;
  }

  protected int getHidden() {
    return hiddenCount;
  }

  protected boolean isFirstHidden() {
    return hiddenCount > 0;
  }

  protected boolean isHiddenBody() {
    return hiddenCount < 0;
  }

  protected void lineNew(int[] selectedRows) {
    lineNew(selectedRows, null);
  }

  protected void lineNew(int[] selectedRows, String token) {
    int numLines = selectedRows.length;
    int firstNewLine = selectedRows[numLines - 1] + 1;
    int currentDataRow = firstNewLine - 1;
    int selectCol = Script.commandCol;
    for (int n = 0; n < numLines; n++) {
      List<ScriptCell> line = new ArrayList<>();
      int lineAdded = currentDataRow + n + 1;
      line.add(new ScriptCell(script, "", Script.commandCol));
      script.data.add(lineAdded, line);
      if (SX.isSet(token)) {
        script.resetLines();
        boolean success = script.addCommandTemplate(token, new TableCell(script, firstNewLine, Script.commandCol), null);
        if (success) {
          selectCol = Script.commandCol + 1;
          break;
        } else {
          token = null;
        }
      }
    }
    script.table.tableCheckContent();
    select(firstNewLine, selectCol);
  }

  protected void lineDelete(int[] selectedRows) {
    saveLines(selectedRows);
    int currentRow = selectedRows[0];
    for (int delRow : selectedRows) {
      script.data.remove(currentRow);
    }
    script.table.tableCheckContent();
    select(selectedRows[0] - 1, Script.numberCol);
  }

  void select(int row, int col) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        script.table.setSelection(Math.max(0, row), col);
      }
    }).start();
  }

  private void saveLines(int[] rows) {
    script.savedLines.clear();
    for (int sourceRow : rows) {
      script.savedLines.add(script.data.get(sourceRow));
    }
  }

  protected void lineEmpty(int[] selectedRows) {
    int currentRow = selectedRows[0];
    saveLines(selectedRows);
    for (int sourceRow : selectedRows) {
      for (ScriptCell cell : script.data.get(sourceRow)) {
        cell.resetAll();
      }
    }
    script.table.tableCheckContent();
    select(currentRow, Script.commandCol);
  }

  protected void lineCopy(int[] selectedRows) {
    saveLines(selectedRows);
  }

  protected void lineRun(int[] selectedRows) {
    script.runScript(selectedRows[0], selectedRows[selectedRows.length - 1]);
  }

  protected void lineInsert(int[] selectedRows) {
    int firstNewLine = selectedRows[selectedRows.length - 1] + 1;
    int numLines = script.savedLines.size();
    int currentRow = selectedRows[selectedRows.length - 1];
    for (int n = 0; n < numLines; n++) {
      script.data.add(currentRow + n + 1, script.savedLines.remove(0));
    }
    script.table.tableCheckContent();
    script.table.setSelection(firstNewLine, Script.numberCol);
  }

  public String toString() {
    return String.format("Cell: (?,%d) %s i(%d %d %d) h%d", col, value, indentLevel, indentIfLevel, indentLoopLevel, hiddenCount);
  }
}
