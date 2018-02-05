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

  public PopUpMenus(Script script) {
    this.script = script;
    log = script.log;
  }

  Script script;

  class PopUpMenu extends JPopupMenu {

    PopUpMenu parent = null;

    boolean isHeader = false;

    JMenuItem createMenuItem(String name, Object ref) {
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

    JMenuItem createMenuItem(PopUpMenu subMenu) {
      return createMenuItem(subMenu, null);
    }

    JMenuItem createMenuItem(PopUpMenu subMenu, String name) {
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

    void createMenuSeperator() {
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

    public void pop() {
      boolean inHeader = isHeader;
      if (!inHeader && SX.isNotNull(parent)) {
        inHeader = parent.isHeader;
      }
      int row = script.table.getSelectedRows()[0];
      int col = script.table.getSelectedColumns()[0];
      if (inHeader) {
        row = 0;
        script.select(row, 0);
        script.lastPopInHeader = true;
      }
      Rectangle cellRectangle = script.table.getCellRect(row, col, false);
      int x = cellRectangle.x;
      int y = cellRectangle.y;
      if (!inHeader) {
        y += cellRectangle.height;
      }
      show(script.table, x, y);
    }
  }

  protected void command() {
    new Command().pop();
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
      add(createMenuItem("Focus fo", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  private class Blocks extends PopUpMenu {

    public Blocks(PopUpMenu parentMenu) {
      parent = parentMenu;
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
      if (isCommand) {
        add(createMenuItem("Break br", this));
        add(createMenuItem("BreakIf bi", this));
        add(createMenuItem("Continue co", this));
        add(createMenuItem("ContinueIf ci", this));
      }
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
      add(createMenuItem("Import", this));
      add(createMenuItem("RunScript", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  private class Testing extends PopUpMenu {

    public Testing(PopUpMenu parentMenu) {
      parent = parentMenu;
      add(createMenuItem("Testing", this));
    }

    public void addCommand(String menuItem) {
      script.addCommandTemplate(menuItem);
    }
  }

  protected void action() {
    Action action = new Action(false);
    action.pop();
  }

  protected void headerAction() {
    new Action(true).pop();
  }

  private class Action extends PopUpMenu {

    public Action(boolean isHeader) {
      this();
      this.isHeader = isHeader;
      this.init();
    }

    private Action() {
    }

    public void init() {
      add(createMenuItem(new Global(this)));
      createMenuSeperator();
      add(createMenuItem("NewLines +", this));
      if (!isHeader) {
        add(createMenuItem("DeleteLines -", this));
        add(createMenuItem("EmptyLines e", this));
        add(createMenuItem("CopyLines c", this));
      }
      add(createMenuItem("InsertLines i", this));
      createMenuSeperator();
      if (isHeader) {
        add(createMenuItem("UnhideAll", this));
        add(createMenuItem("HideAll", this));
      } else {
        add(createMenuItem("HideUnhide h", this));
        add(createMenuItem("ResetHidden -", this));
      }
      if (!isHeader) {
        add(createMenuItem(new Blocks(this), "Surround"));
      }
    }

    public void newLines(ActionEvent ae) {
      script.lineNew();
    }

    public void deleteLines(ActionEvent ae) {
      script.lineDelete();
    }

    public void emptyLines(ActionEvent ae) {
      script.lineEmpty();
    }

    public void copyLines(ActionEvent ae) {
      script.lineCopy();
    }

    public void insertLines(ActionEvent ae) {
      script.lineInsert();
    }

    public void hideUnhide(ActionEvent ae) {
      script.lineHide();
    }

    public void unhideAll(ActionEvent ae) {
      script.lineUnhideAll();
    }

    public void hideAll(ActionEvent ae) {
      script.lineHideAll();
    }

    public void resetHidden(ActionEvent ae) {
      script.lineResetHidden();
    }
  }

  private class Global extends PopUpMenu {

    public Global(PopUpMenu parentMenu) {
      parent = parentMenu;
      add(createMenuItem("Assist F1", this));
      add(createMenuItem("Save F2", this));
      add(createMenuItem("Open F3", this));
      add(createMenuItem("Run F4", this));
    }

    public void assist(ActionEvent e) {
      script.assist();
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
  }

  protected void notimplemented() {
    new Notimplemented().pop();
  }

  private class Notimplemented extends PopUpMenu {

    public Notimplemented() {
      add(createMenuItem(new Global(this)));
    }
  }
}
