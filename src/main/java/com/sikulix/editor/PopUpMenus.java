package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PopUpMenus {

  SXLog log;

  class PopUpMenu extends JPopupMenu {

    PopUpMenu parent = null;
    PopUpMenu parentSub = null;
    Component comp = null;
    TableCell cell = null;
    int x;
    int y;
    int[] selectedRows = new int[0];

    public JMenuItem createMenuItem(String name, Object ref) {
      int index = name.indexOf("!");
      String showName = name;
      if (index > -1) {
        showName = name.substring(0, index);
      }
      if ("?".equals(name)) {
        name = "NotImplemented";
      }
      Character menuChar = name.substring(0, 1).toUpperCase().toCharArray()[0];
      JMenuItem item = new JMenuItem(showName);
      item.setMnemonic(menuChar);
      item.addActionListener(new MenuAction(name, ref));
      return item;
    }

    public JMenuItem createMenuItem(PopUpMenu subMenu) {
      return createMenuItem(subMenu, null);
    }

    public JMenuItem createMenuItem(PopUpMenu subMenu, String name) {
      if (SX.isNull(name)) {
        name = subMenu.getClass().getSimpleName();
      }
      Character menuChar = name.substring(0, 1).toUpperCase().toCharArray()[0];
      name += "...";
      JMenuItem item = new JMenuItem(name);
      item.setMnemonic(menuChar);
      item.addActionListener(new MenuAction(name, subMenu));
      return item;
    }

    public void createMenuSeperator() {
      addSeparator();
    }

    class MenuAction implements ActionListener {

      Method actMethod = null;
      String actMethodName = "";
      int actMethodParams = 1;
      String action;
      Object actClass = null;
      boolean actIsParent = false;

      public MenuAction(String name, Object ref) {
        Class[] noParams = new Class[0];
        Class[] methodParams = new Class[1];
        String function = (name.substring(0, 1).toLowerCase() + name.substring(1));
        if (name.contains("...")) {
          function = function.replace("...", "");
          actIsParent = true;
        }
        int index = function.indexOf(" ");
        if (index > -1) {
          function = function.substring(0, index);
        }
        index = function.indexOf("!");
        if (index > -1) {
          name = function.substring(0, index);
          function = function.substring(index + 1);
        }
        if (actIsParent) {
          actClass = ref;
          actMethodParams = 0;
          try {
            actMethodName = "pop";
            actMethod = actClass.getClass().getMethod(actMethodName, noParams);
          } catch (NoSuchMethodException ex) {
          }
          action = function + "." + actMethodName;
        } else {
          try {
            methodParams[0] = Class.forName("java.awt.event.ActionEvent");
            actClass = ref;
          } catch (ClassNotFoundException ex) {
          }
          if (SX.isNotNull(actClass)) {
            try {
              action = function;
              actMethod = actClass.getClass().getMethod(function, methodParams);
            } catch (NoSuchMethodException ex) {
            }
            if (SX.isNull(actMethod)) {
              actMethodName = "addCommand";
              methodParams[0] = String.class;
              try {
                actMethod = actClass.getClass().getMethod(actMethodName, methodParams);
              } catch (NoSuchMethodException ex) {
              }
            }
          }
        }
        if (SX.isNull(actMethod)) {
          log.error("action missing: %s", function);
        }
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        if (actMethod != null) {
          String actionClassName = (actClass.getClass().getName() + "." + action);
          int hasDollar = actionClassName.indexOf("$");
          if (hasDollar > -1) {
            actionClassName = actionClassName.substring(hasDollar + 1);
          }
          try {
            log.trace("action: %s", actionClassName);
            Object[] params = new Object[actMethodParams];
            if (actMethodParams > 0) {
              if (actMethodName.isEmpty()) {
                params[0] = e;
              } else if ("addCommand".equals(actMethodName)) {
                params[0] = action;
              }
            }
            actMethod.invoke(actClass, params);
          } catch (Exception ex) {
            log.error("action %s return: %s", actionClassName, ex.getMessage());
          }
        }
      }
    }

    void pop(Component comp, TableCell cell) {
      this.comp = comp;
      this.cell = cell;
      Rectangle cellRectangle = cell.getRect();
      this.x = cellRectangle.x;
      this.y = cellRectangle.y + cellRectangle.height;
      show(comp, x, y);
    }

    public void pop() {
      if (SX.isNotNull(parent)) {
        parentSub.show(parent.comp, parent.x, parent.y);
      }
    }

    TableCell getCell() {
      if (SX.isNotNull(cell)) {
        return cell;
      }
      if (SX.isNotNull(parent)) {
        return parent.getCell();
      }
      log.error("no cell nor parent");
      return null;
    }

    ScriptCell getDataCell() {
      return script.evalDataCell(getCell());
    }

    int[] getSelectedRows() {
      int[] selectedRows = null;
      if (SX.isNotNull(parent)) {
        selectedRows = parent.selectedRows;
      }
      return selectedRows;
    }
  }

  public PopUpMenus(Script script) {
    this.script = script;
    log = script.log;
    table = script.getTable();
    data = script.getData();
  }

  Script script;
  ScriptTable table;

  List<List<ScriptCell>> data;
  List<ScriptCell> savedLine = new ArrayList<>();

  protected void command(TableCell cell) {
    new Command().pop(table, cell);
  }

  private class Command extends PopUpMenu {

    public Command() {
      add(createMenuItem(new Global(this)));
      createMenuSeperator();
      add(createMenuItem(new Finding(this)));
      add(createMenuItem(new Mouse(this)));
      add(createMenuItem(new Keyboard(this)));
      add(createMenuItem(new Window(this)));
      createMenuSeperator();
      add(createMenuItem(new Blocks(this)));
      add(createMenuItem(new Scripts(this)));
      add(createMenuItem(new Testing(this)));
    }
  }

  private class Finding extends PopUpMenu {

    public Finding(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Find", this));
      add(createMenuItem("Wait", this));
      add(createMenuItem("FindAll", this));
      add(createMenuItem("FindBest", this));
      add(createMenuItem("FindAny", this));
      add(createMenuItem("Vanish", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), null);
    }
  }

  private class Mouse extends PopUpMenu {

    public Mouse(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Click", this));
      add(createMenuItem("ClickRight", this));
      add(createMenuItem("ClickDouble", this));
      add(createMenuItem("Hover", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), null);
    }
  }

  private class Keyboard extends PopUpMenu {

    public Keyboard(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Write", this));
      add(createMenuItem("Hotkey", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), null);
    }
  }

  private class Window extends PopUpMenu {

    public Window(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Focus", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), null);
    }
  }

  private class Blocks extends PopUpMenu {

    public Blocks(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      String parentName = parent.getClass().getSimpleName();
      Boolean isCommand = "Command".equals(parentName);
      add(createMenuItem("If", this));
      add(createMenuItem("IfNot", this));
      if (isCommand) {
        add(createMenuItem("Else", this));
        add(createMenuItem("IfElse", this));
        createMenuSeperator();
        add(createMenuItem("Elif", this));
        add(createMenuItem("ElifNot", this));
      }
      createMenuSeperator();
      add(createMenuItem("Loop", this));
      add(createMenuItem("LoopFor", this));
      add(createMenuItem("LoopWith", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), getSelectedRows());
    }

  }

  private class Scripts extends PopUpMenu {

    public Scripts(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Image", this));
      add(createMenuItem("Variable", this));
      add(createMenuItem("ListVariable", this));
      add(createMenuItem("Function", this));
      createMenuSeperator();
      add(createMenuItem("Import", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), null);
    }

  }

  private class Testing extends PopUpMenu {

    public Testing(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Testing", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem, getCell(), null);
    }

  }

  protected void action(TableCell cell) {
    new Action().pop(table, cell);
  }

  private class Action extends PopUpMenu {

    public Action() {
      selectedRows = table.getSelectedRows();
      add(createMenuItem(new Global(this)));
      createMenuSeperator();
      add(createMenuItem("NewLines +", this));
      add(createMenuItem("DeleteLines -", this));
      add(createMenuItem("EmptyLines e", this));
      add(createMenuItem("CopyLines c", this));
      add(createMenuItem("InsertLines i", this));
      createMenuSeperator();
      add(createMenuItem("HideUnhide h", this));
      if (selectedRows.length > 0) {
        add(createMenuItem(new Blocks(this), "Surround"));
      }
      createMenuSeperator();
      add(createMenuItem("RunLines r", this));
    }

    public void newLines(ActionEvent ae) {
      getDataCell().lineNew(selectedRows);
    }

    public void deleteLines(ActionEvent ae) {
      getDataCell().lineDelete(selectedRows);
    }

    public void emptyLines(ActionEvent ae) {
      getDataCell().lineEmpty(selectedRows);
    }

    public void copyLines(ActionEvent ae) {
      getDataCell().lineCopy(selectedRows);
    }

    public void insertLines(ActionEvent ae) {
      getDataCell().lineInsert(selectedRows);
    }

    public void hideUnhide(ActionEvent ae) {
      getDataCell().lineHide(selectedRows);
    }

    public void runLines(ActionEvent ae) {
      getDataCell().lineRun(selectedRows);
    }
  }

  private class Global extends PopUpMenu {

    public Global(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Assist F1", this));
      add(createMenuItem("Save F2", this));
      add(createMenuItem("Open F3", this));
      add(createMenuItem("Run F4", this));
      add(createMenuItem("Find F5", this));
    }

    public void assist(ActionEvent e) {
      script.assist(getCell());
    }

    public void save(ActionEvent e) {
      script.saveScript();
    }

    public void open(ActionEvent e) {
      script.loadScript();
    }

    public void run(ActionEvent e) {
      script.runScript(-1);
    }

    public void find(ActionEvent e) {
      getDataCell().find();
    }
  }

  protected void notimplemented(TableCell cell) {
    new Notimplemented().pop(table, cell);
  }

  private class Notimplemented extends PopUpMenu {

    public Notimplemented() {
      add(createMenuItem(new Global(this)));
    }
  }
}
