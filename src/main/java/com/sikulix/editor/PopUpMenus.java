package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.util.PopUpMenu;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class PopUpMenus {

  public PopUpMenus(Script script) {
    theScript = script;
    theTable = theScript.getTable();
    data = theScript.getData();
  }

  static Script theScript = null;
  static List<List<ScriptCell>> data = null;
  static ScriptTable theTable = null;

  static void tableChanged() {
    theTable.tableHasChanged();
  }

  static List<ScriptCell> savedLine = new ArrayList<>();

  public static class Command extends PopUpMenu {

    private static Command menu = null;

    public static Command pop(Component comp, int x, int y) {
      if (SX.isNull(menu)) {
        menu = new Command();
      }
      menu.init(theTable, comp, x, y, theScript.shouldTrace);
      menu.show(comp, x, y);
      return menu;
    }

    private Command() {
      add(createMenuItem("Global...", this));
      createMenuSeperator();
      add(createMenuItem("Finding...", this));
      add(createMenuItem("Mouse...", this));
      add(createMenuItem("Keyboard...", this));
      add(createMenuItem("Window...", this));
      createMenuSeperator();
      add(createMenuItem("Blocks...", this));

    }

    public void global(ActionEvent ae) {
      Global.pop(this);
    }

    public void finding(ActionEvent ae) {
      Finding.pop(this);
    }

    public void mouse(ActionEvent ae) {
      DefaultSub.pop(this);
    }

    public void keyboard(ActionEvent ae) {
      DefaultSub.pop(this);
    }

    public void window(ActionEvent ae) {
      DefaultSub.pop(this);
    }

    public void blocks(ActionEvent ae) {
      Blocks.pop(this);
    }
  }

  public static class Finding extends PopUpMenu {

    private static Finding menu = null;

    public static void pop(PopUpMenu parentMenu) {
      if (SX.isNull(menu)) {
        menu = new Finding();
      }
      menu.init(parentMenu, theScript.shouldTrace);
      menu.show(parentMenu.table, parentMenu.pos.x, parentMenu.pos.y);
    }

    private Finding() {
      add(createMenuItem("Find", this));
      add(createMenuItem("FindAll", this));
      add(createMenuItem("FindBest", this));
      add(createMenuItem("FindAny", this));
      add(createMenuItem("Vanish", this));
    }

    public void find(ActionEvent ae) {
      theScript.addCommandTemplate("#find", row, col);
    }

    public void vanish(ActionEvent ae) {
      theScript.addCommandTemplate("#vanish", row, col);
    }

    public void findAll(ActionEvent ae) {
      theScript.addCommandTemplate("#findAll", row, col);
    }

    public void findBest(ActionEvent ae) {
      theScript.addCommandTemplate("#findBest", row, col);
    }

    public void findAny(ActionEvent ae) {
      theScript.addCommandTemplate("#findAny", row, col);
    }
  }

  public static class Blocks extends PopUpMenu {

    private static Blocks menu = null;

    public static void pop(PopUpMenu parentMenu) {
      if (SX.isNull(menu)) {
        menu = new Blocks();
      }
      menu.init(parentMenu, theScript.shouldTrace);
      menu.show(parentMenu.table, parentMenu.pos.x, parentMenu.pos.y);
    }

    private Blocks() {
      add(createMenuItem("If!is", this));
      add(createMenuItem("IfNot", this));
      add(createMenuItem("Else!otherwise", this));
      add(createMenuItem("Elif", this));
      add(createMenuItem("ElifNot", this));
      add(createMenuItem("Loop", this));
      add(createMenuItem("LoopFor", this));
      add(createMenuItem("LoopWith", this));
    }

    public void is(ActionEvent ae) {
      theScript.addCommandTemplate("#if", row, col);
    }

    public void ifNot(ActionEvent ae) {
      theScript.addCommandTemplate("#ifNot", row, col);
    }

    public void otherwise(ActionEvent ae) {
      theScript.addCommandTemplate("#else", row, col);
    }

    public void elif(ActionEvent ae) {
      theScript.addCommandTemplate("#elif", row, col);
    }

    public void elifNot(ActionEvent ae) {
      theScript.addCommandTemplate("#elifNot", row, col);
    }

    public void loop(ActionEvent ae) {
      theScript.addCommandTemplate("#loop", row, col);
    }

    public void loopFor(ActionEvent ae) {
      theScript.addCommandTemplate("#loopFor", row, col);
    }

    public void loopWith(ActionEvent ae) {
      theScript.addCommandTemplate("#loopWith", row, col);
    }
  }

  public static class Action extends PopUpMenu {

    private static Action menu = null;

    public static Action pop(Component comp, int x, int y) {
      if (SX.isNull(menu)) {
        menu = new Action();
      }
      menu.init(theTable, comp, x, y, theScript.shouldTrace);
      menu.show(comp, x, y);
      return menu;
    }


    private Action() {
      add(createMenuItem("Global...", this));
      createMenuSeperator();
      add(createMenuItem("Indent >", this));
      add(createMenuItem("Dedent <", this));
      createMenuSeperator();
      add(createMenuItem("NewLine +", this));
      add(createMenuItem("DeleteLine", this));
      add(createMenuItem("EmptyLine", this));
      add(createMenuItem("CopyLine", this));
      add(createMenuItem("InsertLine", this));
      createMenuSeperator();
      add(createMenuItem("RunLine", this));
    }

    public void global(ActionEvent ae) {
      Global.pop(this);
    }

    public void indent(ActionEvent ae) {
      theScript.indent(row, col);
    }

    public void dedent(ActionEvent ae) {
      theScript.dedent(row, col);
    }

    public void newLine(ActionEvent ae) {
      data.add(row + 1, new ArrayList<>());
      tableChanged();
      table.setRowSelectionInterval(row + 1, row + 1);
      table.setColumnSelectionInterval(col + 1, col + 1);
    }

    public void deleteLine(ActionEvent ae) {
      savedLine = data.remove(row);
      tableChanged();
      int selRow = row - 1 < 0 ? row : row - 1;
      table.setRowSelectionInterval(selRow, selRow);
      table.setColumnSelectionInterval(col, col);
    }

    public void emptyLine(ActionEvent ae) {
      savedLine = theScript.cellAt(row, col + 1).setLine();
      tableChanged();
      table.setRowSelectionInterval(row, row);
      table.setColumnSelectionInterval(col, col);
    }

    public void copyLine(ActionEvent ae) {
      savedLine = data.get(row);
    }

    public void runLine(ActionEvent ae) {
      theScript.runScript(row, row);
    }

    public void insertLine(ActionEvent ae) {
      data.add(row + 1, savedLine);
      tableChanged();
      table.setRowSelectionInterval(row + 1, row + 1);
      table.setColumnSelectionInterval(col + 1, col + 1);
    }
  }

  public static class Global extends PopUpMenu {

    private static Global menu = null;

    public static void pop(PopUpMenu parentMenu) {
      if (SX.isNull(menu)) {
        menu = new Global();
      }
      menu.init(parentMenu, theScript.shouldTrace);
      menu.show(parentMenu.table, parentMenu.pos.x, parentMenu.pos.y);
    }

    private Global() {
      add(createMenuItem("Help F1", this));
      add(createMenuItem("Save... F2", this));
      add(createMenuItem("Open... F3", this));
      add(createMenuItem("Run F4", this));
      add(createMenuItem("Capture F5", this));
      add(createMenuItem("Show F6", this));
      add(createMenuItem("Find F7", this));
    }

    public void help(ActionEvent ae) {

    }

    public void save(ActionEvent ae) {
      theScript.saveScript();
    }

    public void open(ActionEvent ae) {
      theScript.loadScript();
    }

    public void run(ActionEvent ae) {
      if (col == 0) {
        theScript.runScript(0, data.size() - 1);
      } else {
        theScript.runScript(row, row);
      }
    }

    public void capture(ActionEvent ae) {
      if (col > 0) {
        theScript.cellAt(row, col).capture();
      }
    }

    public void show(ActionEvent ae) {
      if (col > 0) {
        theScript.cellAt(row, col).show();
      }
    }

    public void find(ActionEvent ae) {
      if (col > 0) {
        theScript.cellAt(row, col).find();
      }
    }

  }

  public static class Default extends PopUpMenu {

    private static Default menu = null;

    public static void pop(Component comp, int x, int y) {
      if (SX.isNull(menu)) {
        menu = new Default();
      }
      menu.init(theTable, comp, x, y, theScript.shouldTrace);
      menu.show(comp, x, y);
    }

    private Default() {
      add(createMenuItem("NotImplemented", this));
    }

    public void notImplemented(ActionEvent ae) {

    }
  }

  public static class DefaultSub extends PopUpMenu {

    private static DefaultSub menu = null;

    public static void pop(PopUpMenu parentMenu) {
      if (SX.isNull(menu)) {
        menu = new DefaultSub();
      }
      menu.init(parentMenu, theScript.shouldTrace);
      menu.show(parentMenu.table, parentMenu.pos.x, parentMenu.pos.y);
    }

    private DefaultSub() {
      add(createMenuItem("NotImplemented", this));
    }

    public void notImplemented(ActionEvent ae) {

    }
  }
}
