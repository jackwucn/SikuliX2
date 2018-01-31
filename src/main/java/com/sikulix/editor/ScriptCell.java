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

  private int tableRow = -1;

  public int getTableRow() {
    return tableRow;
  }

  public void setTableRow(int tableRow) {
    this.tableRow = tableRow;
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

  private ScriptCell copy() {
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

  private int countInnerHidden(int hiddenCount, int currentDataLine) {
    hiddenCount--;
    int hiddenLinesCount = 0;
    while (hiddenCount > 0) {
      int count = getLineHiddenCount(currentDataLine);
      currentDataLine += count;
      hiddenLinesCount += count;
      hiddenCount--;
    }
    return hiddenLinesCount;
  }

  private int getLineHiddenCount(int lineNumber) {
    ScriptCell cell = script.allData.get(lineNumber).get(0);
    int hiddenCount = cell.getHidden();
    if (cell.isFirstHidden() && hiddenCount > 0) {
      return countInnerHidden(hiddenCount, lineNumber + 1) + 1;
    } else {
      return 1;
    }
  }

  protected void lineHide(int[] selectedRows) {
    int currentTableRow = selectedRows[0];
    ScriptCell firstCell = script.data.get(currentTableRow).get(Script.commandCol - 1);
    int shouldSelectFirst = currentTableRow;
    int shouldSelectLast = currentTableRow;
    boolean shouldHide = false;
    int selectedCount = selectedRows.length;
    if (selectedCount == 1) {
      if (firstCell.isFirstHidden()) {
        int hCount = firstCell.getHidden();
        firstCell.setHidden(-hCount);
        if (hCount > 0) {
          hCount -= 1;
          int currentDataLine = script.data.get(currentTableRow).get(0).getRow() + 1;
          shouldSelectLast = shouldSelectFirst + hCount;
          while (hCount > 0) {
            List<ScriptCell> line = script.allData.get(currentDataLine);
            currentTableRow++;
            line.get(0).setRow(currentDataLine);
            script.data.add(currentTableRow, line);
            hCount--;
            currentDataLine++;
            int hiddenCount = line.get(0).getHidden();
            if (line.get(0).isFirstHidden()) {
              if (hiddenCount > 0) {
                currentDataLine += countInnerHidden(hiddenCount, currentDataLine);
              } else {
                hCount += (-hiddenCount) - 1;
                shouldSelectLast += (-hiddenCount) - 1;
              }
            }
          }
        } else {
          shouldHide = true;
          selectedCount = -hCount;
        }
      }
    } else {
      shouldHide = true;
    }
    if (shouldHide) {
      currentTableRow++;
      int hiddenLinesCount = 1;
      while (selectedCount > 1) {
        List<ScriptCell> line = script.data.remove(currentTableRow);
        hiddenLinesCount++;
        if (line.get(0).isFirstHidden()) {
          if (line.get(0).getHidden() < 0) {
            selectedCount += (-line.get(0).getHidden()) - 1;
            hiddenLinesCount -= (-line.get(0).getHidden()) - 1;
          }
        }
        selectedCount--;
      }
      firstCell.setHidden(hiddenLinesCount);
    }
    script.table.tableCheckContent();
    script.table.setLineSelection(shouldSelectFirst, shouldSelectLast);
  }

  protected void lineUnhideAll() {
    script.data.clear();
    int lineNumber = 0;
    for (List<ScriptCell> line : script.allData) {
      int hiddenCount = line.get(0).getHidden();
      if (hiddenCount > 0) {
        line.get(0).setHidden(-hiddenCount);
      }
      script.data.add(line);
    }
    script.checkContent();
    script.table.tableHasChanged();
    script.table.setSelection(0, 0);
  }

  protected void lineHideAll() {
    String theScript = script.scriptToString();
    script.data.clear();
    script.allData.clear();
    script.stringToScript(theScript);
    script.checkContent();
    script.table.tableHasChanged();
    script.table.setSelection(0, 0);
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

  private void select(int row, int col) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        script.table.setSelection(Math.max(0, row), col);
      }
    }).start();
  }

  protected boolean isLineEmpty() {
    for (ScriptCell cell : script.data.get(row)) {
      if (!cell.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  protected void lineNew(int[] selectedRows) {
    int numLines = selectedRows.length;
    int firstNewLine = selectedRows[numLines - 1] + 1;
    if (isHeader()) {
      firstNewLine = 0;
    }
    script.lineAdd(firstNewLine, numLines);
    script.table.tableCheckContent();
    select(firstNewLine, Script.commandCol);
  }

  private List<Integer> getDataLines(int[] selectedRows) {
    List<Integer> rows = new ArrayList<>();
    rows.add(script.data.get(selectedRows[0]).get(0).getRow());
    int lastLine = script.data.get(selectedRows[selectedRows.length - 1]).get(0).getRow();
    if (lineIsFirstCollapsed(lastLine)) {
      lastLine = script.allData.size() - 1;
    }
    rows.add(lastLine);
    return rows;
  }

  private boolean lineIsFirstCollapsed(int row) {
    return script.allData.get(row).get(0).isFirstCollapsed();
  }

  private List<Integer> saveLines(List<Integer> lines) {
    script.savedLines.clear();
    int dataLine = lines.get(0);
    script.savedLines.add(script.allData.remove(dataLine));
    while (dataLine < lines.get(1)) {
      script.savedLines.add(script.allData.remove(++dataLine));
    }
    return lines;
  }

  private List<Integer> copyLines(List<Integer> lines) {
    script.savedLines.clear();
    int currentLine = lines.get(0);
    script.savedLines.add(copyLine(script.allData.get(currentLine)));
    while (currentLine < lines.get(1)) {
      script.savedLines.add(copyLine(script.allData.get(++currentLine)));
    }
    return lines;
  }

  private List<ScriptCell> copyLine(List<ScriptCell> line) {
    List<ScriptCell> lineCopy = new ArrayList<>();
    for (ScriptCell cell : line) {
      lineCopy.add(cell.copy());
    }
    return lineCopy;
  }

  protected void lineCopy(int[] selectedRows) {
    copyLines(getDataLines(selectedRows));
  }

  protected void lineDelete(int[] selectedRows) {
    List<Integer> rows = saveLines(getDataLines(selectedRows));
    for (int delRow : selectedRows) {
      script.data.remove(selectedRows[0]);
    }
    int lineCount = rows.get(1) - rows.get(0) + 1;
    for (int row = selectedRows[0]; row < script.data.size(); row++) {
      script.changeRow(row, -lineCount);
    }
    script.table.tableCheckContent();
    select(selectedRows[0] - 1, Script.numberCol);
  }

  protected void lineEmpty(int[] selectedRows) {
    List<Integer> lines = saveLines(getDataLines(selectedRows));
    int dataLine = lines.get(0);
    for (int n = dataLine; n <= lines.get(1); n++) {
      lineNew(new int[]{dataLine - 1});
    }
    script.table.tableCheckContent();
    select(selectedRows[0], Script.commandCol);
  }

  protected void lineReset(int[] selectedRows) {
    int currentRow = selectedRows[0];
    String command = script.data.get(currentRow).get(0).get();
    saveLines(getDataLines(selectedRows));
    //TODO lineNew(new int[]{currentRow - 1}, command);
    script.table.tableCheckContent();
    select(currentRow, Script.commandCol);
  }

  protected void lineRun(int[] selectedRows) {
    script.runScript(selectedRows[0], selectedRows[selectedRows.length - 1]);
  }

  protected void lineInsert(int[] selectedRows) {
    int numLines = script.savedLines.size();
    if (isHeader()) {
      for (int n = 0; n < numLines; n++) {
        script.data.add(n, script.savedLines.remove(0));
      }
      script.table.tableCheckContent();
      select(0, 0);
      return;
    }
    int firstNewLine = selectedRows[selectedRows.length - 1] + 1;
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
