package com.sikulix.util;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.editor.Script;

import javax.swing.*;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PopUpMenu extends JPopupMenu {

  protected static final SXLog log = SX.getSXLog("SX.POPUPMENU");
  private static int logLevel = SXLog.TRACE;

  public Point pos = new Point();
  public int row = -1;
  public int col = -1;
  public JTable table = null;
  protected Component comp = null;

  private int menuCount = 0;
  private Map<String, Integer> menus = new HashMap<String, Integer>();

  protected void init(JTable table, Component comp, int x, int y) {
    this.table =table;
    this.comp = comp;
    pos = new Point(x, y);
    init1();
  }

  protected void init(PopUpMenu menu) {
    this.table =menu.table;
    this.comp =menu.comp;
    pos = menu.pos;
    init1();
  }

  private void init1() {
    this.row = table.rowAtPoint(pos);
    this.col = table.columnAtPoint(pos);
  }

  public JMenuItem createMenuItem(JMenuItem item, ActionListener listener) {
    item.addActionListener(listener);
    return item;
  }

  public JMenuItem createMenuItem(String name, ActionListener listener) {
    return createMenuItem(new JMenuItem(name), listener);
  }

  public JMenuItem createMenuItem(String name, Object ref) {
    return createMenuItem(new JMenuItem(name), new MenuAction(name, ref));
  }

  public void createMenuSeperator() {
    menuCount++;
    addSeparator();
  }

  public void doShow(Component comp, MouseEvent me) {
    show(comp, me.getX(), me.getY());
  }

  class MenuAction implements ActionListener {

    Method actMethod = null;
    String action;
    int menuPos;
    Object actClass = null;

    public MenuAction(String name) {
      Class[] paramsWithEvent = new Class[1];
      try {
        String function = name.substring(0, 1).toLowerCase() + name.substring(1);
        paramsWithEvent[0] = Class.forName("java.awt.event.ActionEvent");
        actClass = this;
        actMethod = this.getClass().getMethod(function, paramsWithEvent);
        action = name;
        menuPos = menuCount++;
        menus.put(name, menuPos);
      } catch (Exception ex) {
        log.error("action missing: %s (%s)", name, ex.getMessage());
      }
    }

    public MenuAction(String name, Object ref) {
      Class[] paramsWithEvent = new Class[1];
      try {
        String function = (name.substring(0, 1).toLowerCase() + name.substring(1)).replace("...", "");
        int index = function.indexOf(" ");
        if (index > -1) {
          function = function.substring(0, index);
        }
        paramsWithEvent[0] = Class.forName("java.awt.event.ActionEvent");
        actClass = ref;
        actMethod = actClass.getClass().getMethod(function, paramsWithEvent);
        action = function;
        menuPos = menuCount++;
        menus.put(name, menuPos);
      } catch (Exception ex) {
        log.error("action missing: %s (%s)", name, ex.getMessage());
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
          log.trace( "action: %s", actionClassName);
          Object[] params = new Object[1];
          params[0] = e;
          actMethod.invoke(actClass, params);
        } catch (Exception ex) {
          log.error( "action %s return: %s", actionClassName, ex.getMessage());
        }
      }
    }
  }
}
