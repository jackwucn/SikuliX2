package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PopUpMenus {

  SXLog log;

  class PopUpMenu extends JPopupMenu {

    PopUpMenu parent = null;
    PopUpMenu parentSub = null;
    Component comp = null;
    int row;
    int col;
    int x;
    int y;

    boolean isHeader() {
      return row < 0;
    }

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

    void pop(Component comp, int row, int col) {
      this.comp = comp;
      this.row = row;
      this.col = col;
      Rectangle cellRectangle = script.table.getCellRect(row, col, false);
      this.x = cellRectangle.x;
      this.y = cellRectangle.y;
      if (!isHeader()) {
        this.y += cellRectangle.height;
      }
      show(comp, x, y);
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

  protected void command(int row, int col) {
    new Command().pop(table, row, col);
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
      add(createMenuItem(new Scripting(this)));
      add(createMenuItem(new Testing(this)));
    }
  }

  private class Finding extends PopUpMenu {

    public Finding(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Find f", this));
      add(createMenuItem("Wait w", this));
      add(createMenuItem("FindAll fa", this));
      add(createMenuItem("FindBest fb", this));
      add(createMenuItem("FindAny fy", this));
      add(createMenuItem("Vanish v", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  private class Mouse extends PopUpMenu {

    public Mouse(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Click c", this));
      add(createMenuItem("ClickRight cr", this));
      add(createMenuItem("ClickDouble cd", this));
      add(createMenuItem("Hover h", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  private class Keyboard extends PopUpMenu {

    public Keyboard(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Write wr", this));
      add(createMenuItem("Hotkey hk", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  private class Window extends PopUpMenu {

    public Window(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Focus fo", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  private class Blocks extends PopUpMenu {

    public Blocks(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      String parentName = parent.getClass().getSimpleName();
      Boolean isCommand = "Command".equals(parentName);
      add(createMenuItem("If", this));
      add(createMenuItem("IfNot in", this));
      if (isCommand) {
        add(createMenuItem("Else e", this));
        createMenuSeperator();
        add(createMenuItem("Elif ei", this));
        add(createMenuItem("ElifNot en", this));
      }
      createMenuSeperator();
      add(createMenuItem("Loop l", this));
      add(createMenuItem("LoopFor lf", this));
      add(createMenuItem("LoopWith lw", this));
      add(createMenuItem("Break br", this));
      add(createMenuItem("BreakIf bi", this));
      add(createMenuItem("Continue co", this));
      add(createMenuItem("ContinueIf ci", this));
      createMenuSeperator();
      add(createMenuItem("Function $f", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }

  }

  private class Scripting extends PopUpMenu {

    public Scripting(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Image $i", this));
      add(createMenuItem("ImageList $$I", this));
      add(createMenuItem("Region $R", this));
      add(createMenuItem("Location $L", this));
      add(createMenuItem("Use ", this));
      createMenuSeperator();
      add(createMenuItem("Variable $", this));
      add(createMenuItem("Array $$", this));
      add(createMenuItem("Option $o", this));
      createMenuSeperator();
      add(createMenuItem("SetIf si", this));
      add(createMenuItem("SetIfNot sn", this));
      add(createMenuItem("IfElse ie", this));
      createMenuSeperator();
      add(createMenuItem("Import im", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }

  }

  private class Testing extends PopUpMenu {

    public Testing(PopUpMenu parentMenu) {
      parent = parentMenu;
      parentSub = this;
      add(createMenuItem("Testing", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }

  }

  protected void action(int row, int col) {
    new Action(row, col).pop(table, row, col);
  }

  private class Action extends PopUpMenu {

    public Action(int row, int col) {
      add(createMenuItem(new Global(this)));
      createMenuSeperator();
      add(createMenuItem("NewLines +", this));
      if (!isHeader()) {
        add(createMenuItem("DeleteLines -", this));
        add(createMenuItem("EmptyLines e", this));
        add(createMenuItem("CopyLines c", this));
      }
      add(createMenuItem("InsertLines i", this));
      createMenuSeperator();
      if (isHeader()) {
        add(createMenuItem("UnhideAll", this));
        add(createMenuItem("HideAll", this));
      } else {
        add(createMenuItem("HideUnhide h", this));
      }
      if (!isHeader()) {
        add(createMenuItem(new Blocks(this), "Surround"));
      }
      createMenuSeperator();
      add(createMenuItem("RunLines r", this));
    }

    ScriptCell getCell() {
      return data.get(row).get(0);
    }

    public void newLines(ActionEvent ae) {
      getCell().lineNew(table.getSelectedRows());
    }

    public void deleteLines(ActionEvent ae) {
      getCell().lineDelete(table.getSelectedRows());
    }

    public void emptyLines(ActionEvent ae) {
      getCell().lineEmpty(table.getSelectedRows());
    }

    public void copyLines(ActionEvent ae) {
      getCell().lineCopy(table.getSelectedRows());
    }

    public void insertLines(ActionEvent ae) {
      getCell().lineInsert(table.getSelectedRows());
    }

    public void hideUnhide(ActionEvent ae) {
      script.lineHide(table.getSelectedRows());
    }

    public void unhideAll(ActionEvent ae) {
      getCell().lineUnhideAll();
    }

    public void hideAll(ActionEvent ae) {
      getCell().lineHideAll();
    }

    public void runLines(ActionEvent ae) {
      getCell().lineRun(table.getSelectedRows());
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
      script.assist(row, col);
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
      script.find(row, col);
    }
  }

  protected void notimplemented(int row, int col) {
    new Notimplemented().pop(table, row, col);
  }

  private class Notimplemented extends PopUpMenu {

    public Notimplemented() {
      add(createMenuItem(new Global(this)));
    }
  }
}
