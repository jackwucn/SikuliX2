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
    init(script, value, row, col);
  }

  private void init(Script script, String value, int row, int col) {
    this.script = script;
    this.value = value.trim();
    this.row = row;
    this.col = col;
  }

  protected int getRow() {
    return row;
  }

  protected int getCol() {
    return col;
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

  protected void capture() {
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
            script.table.setValueAt(value, row, col);
            script.getWindow().setVisible(true);
          }
        }).start();
      } else {
        script.getWindow().setVisible(true);
      }
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
    if (!isEmpty()) {
      if (asImage().isValid()) {
        loadPicture();
        if (SX.isNotNull(picture)) {
          new Thread(new Runnable() {
            @Override
            public void run() {
              picture.show(1);
              script.getWindow().setVisible(true);
              picture = null;
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
          script.table.setValueAt(value, row, col);
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

  protected void lineSet(String... items) {
    if (items.length == 0) {
      script.data.set(row, new ArrayList<>());
    } else {
      int col = 1;
      for (String item : items) {
        script.dataCellSet(row, col++, item);
      }
    }
  }

  protected void lineAdd(String command) {
    script.data.add(row + 1, new ArrayList<>());
    script.dataCellSet(row + 1, 1, command);
    script.table.tableHasChanged();
    script.table.setSelection(row, 1);
  }

  private List<Integer> getSelectedDataRows(int[] selectedRows) {
    List<Integer> selectedDataRows = new ArrayList<>();
    int currentDataRow = script.lines.get(selectedRows[0]);
    int nextDataRow, nextTableRow;
    for (int n = 0; n < selectedRows.length; n++) {
      nextTableRow = selectedRows[n] + 1;
      try {
        nextDataRow = script.lines.get(nextTableRow);
        while (currentDataRow < nextDataRow) {
          selectedDataRows.add(currentDataRow++);
        }
      } catch (Exception ex) {
        selectedDataRows.add(currentDataRow++);
        break;
      }
    }
    return selectedDataRows;
  }

  protected void lineHide(int[] selectedRows) {
    List<Integer> selectedDataRows = getSelectedDataRows(selectedRows);
    int currentDataRow = selectedDataRows.get(0);
    if (selectedRows.length == 1) {
      ScriptCell firstCell = script.dataCell(currentDataRow, Script.commandCol);
      if (firstCell.isFirstHidden()) {
        int count = firstCell.getHidden();
        for (int ix = currentDataRow; ix < currentDataRow + count; ix++) {
          script.dataCell(ix, Script.commandCol).setHidden(0);
        }
        currentDataRow--;
      }
    } else {
      int count = selectedDataRows.size();
      script.dataCell(currentDataRow, Script.commandCol).setHidden(count);
      for (int ix = currentDataRow + 1; ix < currentDataRow + count; ix++) {
        script.dataCell(ix, Script.commandCol).setHidden(-1);
      }
    }
    script.table.tableCheckContent();
    select(selectedRows[0] + 1, Script.numberCol);
  }

  private int hiddenCount = 0;

  private void setHidden(int count) {
    hiddenCount = count;
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
    List<Integer> selectedDataRows = getSelectedDataRows(selectedRows);
    int currentDataRow = selectedDataRows.get(selectedDataRows.size() - 1);
    int selectCol = Script.commandCol;
    for (int n = 0; n < numLines; n++) {
      List<ScriptCell> line = new ArrayList<>();
      int lineAdded = currentDataRow + n + 1;
      line.add(new ScriptCell(script, "", lineAdded, Script.commandCol));
      script.data.add(lineAdded, line);
      if (SX.isSet(token)) {
        script.resetLines();
        boolean success = script.addCommandTemplate(token,
                script.tableCell(currentDataRow + 1, Script.commandCol));
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
    List<Integer> selectedDataRows = getSelectedDataRows(selectedRows);
    saveLines(selectedDataRows);
    int currentRow = selectedDataRows.get(0);
    for (int delRow : selectedDataRows) {
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

  private void saveLines(List<Integer> rows) {
    script.savedLines.clear();
    for (int sourceRow : rows) {
      List<ScriptCell> copyRow = new ArrayList<>();
      for (ScriptCell cell : script.data.get(sourceRow)) {
        copyRow.add(new ScriptCell(script, cell.get(), -1, -1));
      }
      script.savedLines.add(copyRow);
    }
  }

  protected void lineEmpty(int[] selectedRows) {
    List<Integer> selectedDataRows = getSelectedDataRows(selectedRows);
    int currentRow = selectedDataRows.get(0);
    saveLines(selectedDataRows);
    for (int sourceRow : selectedDataRows) {
      for (ScriptCell cell : script.data.get(sourceRow)) {
        cell.resetAll();
      }
    }
    script.table.tableCheckContent();
    select(currentRow, Script.commandCol);
  }

  protected void lineCopy(int[] selectedRows) {
    saveLines(getSelectedDataRows(selectedRows));
  }

  protected void lineRun(int[] selectedRows) {
    List<Integer> selectedDataRows = getSelectedDataRows(selectedRows);
    script.runScript(selectedDataRows.get(0), selectedDataRows.get(selectedDataRows.size() - 1));
  }

  List<ScriptCell> getSavedLine(int rowTarget) {
    List<ScriptCell> savedLine = script.savedLines.remove(0);
    List<ScriptCell> savedClean = new ArrayList<>();
    for (ScriptCell cell : savedLine) {
      savedClean.add(new ScriptCell(script, cell.get(), rowTarget, cell.getCol()));
    }
    return savedClean;
  }

  protected void lineInsert(int[] selectedRows) {
    int firstNewLine = selectedRows[selectedRows.length - 1] + 1;
    int numLines = script.savedLines.size();
    List<Integer> selectedDataRows = getSelectedDataRows(selectedRows);
    int currentRow = selectedDataRows.get(selectedDataRows.size() - 1);
    for (int n = 0; n < numLines; n++) {
      script.data.add(currentRow + n + 1, getSavedLine(currentRow + n + 1));
    }
    script.table.tableCheckContent();
    script.table.setSelection(firstNewLine, Script.numberCol);
  }

  public String toString() {
    return String.format("Cell: (%d,%d) %s i(%d %d %d) h%d", row, col, value, indentLevel, indentIfLevel, indentLoopLevel, hiddenCount);
  }
}
