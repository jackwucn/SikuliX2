/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.core.SX;

import javax.swing.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class ScriptCell {

  enum CellType {COMMAND, IMAGE, SCRIPT, VARIABLE, LIST, MAP, IMAGELIST, IMAGEMAP, TEXT}
  enum BlockType {SINGLE, IF, ELSE, ELIF}

  private Script script;
  private String value = "";
  private CellType cellType = CellType.TEXT;
  private int row = -1;
  private int col = -1;
  private int indentLevel = 0;
  private int indentIfLevel = 0;

  protected ScriptCell() {

  }

  protected ScriptCell(Script script, String value, int row, int col) {
    this.script = script;
    this.value = value.trim();
    this.row = row;
    this.col = col;
  }

  protected String getIndentMarker() {
    if (indentLevel > 0) {
      return String.format(">%d", indentLevel);
    }
    return "";
  }

  protected void resetIndent() {
    indentLevel = indentIfLevel = 0;
  }

  protected void setIndent(int level) {
    if (level > -1) {
      indentLevel = level;
    }
  }

  protected void setIfIndent(int level) {
    if (level > -1) {
      indentIfLevel = level;
    }
  }

  protected int getIndent() {
    return indentLevel;
  }

  protected int getIfIndent() {
    return indentIfLevel;
  }

  private boolean block = false;
  private BlockType bType = null;

  void setBlock(BlockType bType) {
    block = true;
    this.bType = bType;
  }

  boolean isBlock() {
    return block;
  }

  boolean isBlockType(BlockType bType) {
    return this.bType.equals(bType);
  }

  protected ScriptCell asCommand(int row, int col) {
    if (!value.startsWith("#")) {
      value = "#" + value;
    }
    cellType = CellType.COMMAND;
    return this;
  }

  protected boolean isCommand() {
    return CellType.COMMAND.equals(cellType);
  }

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
          script.getTable().setValueAt(value, row, col);
          script.getWindow().setVisible(true);
        }
      }).start();
    }
  }

  protected void capture() {
    asImage().getImage();
  }

  protected Element getCellClick() {
    Point windowLocation = script.getWindow().getLocation();
    Rectangle cell = script.getTable().getCellRect(row, col, false);
    windowLocation.x += cell.x + 10;
    windowLocation.y += cell.y + 70;
    return new Element(windowLocation);
  }

  protected Rectangle getRect() {
    return script.getTable().getCellRect(row, col, false);
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
    asImage();
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
        script.getTable().setValueAt(value, row, col);
      }
    }
  }

  protected void find() {
    asImage();
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
        script.getTable().setValueAt(value, row, col);
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
    script.getTable().getModel().setValueAt(value, row, col);
    return this;
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

  protected List<ScriptCell> setLine(BlockType bType, String... items) {
    List<ScriptCell> oldLine = setLine(items);
    if (SX.isNotNull(bType)) {
      setBlock(bType);
    }
    return oldLine;
  }
}
