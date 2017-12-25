/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */
package com.sikulix.util;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.lang.reflect.Method;

/**
 * INTERNAL USE
 * implements a transparent screen overlay for various purposes
 */
public class Overlay extends JFrame implements EventSubject {

  private static SXLog log = SX.getSXLog("SX.OVERLAY");

  private JPanel _panel = null;
  private Color _col = null;
  private Overlay _win = null;
  private Graphics2D _currG2D = null;
  private EventObserver _obs;

  public Overlay() {
    init(null, null);
  }

  public Overlay(Color col, EventObserver o) {
    init(col, o);
  }

  private void init(Color col, EventObserver observer) {
    setUndecorated(true);
    setAlwaysOnTop(true);
    if (col != null) {
      _obs = observer;
      _win = this;
      try {
        setBackground(col);
      } catch (Exception e) {
        log.error("setBackground: did not work");
      }
      _panel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
          if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            _currG2D = g2d;
            if (_obs != null) {
              _obs.update(_win);
            }
          } else {
            super.paintComponent(g);
          }
        }
      };
      _panel.setLayout(null);
      add(_panel);
    }
  }

  @Override
  public void setOpacity(float alpha) {
    try {
      Class<?> c = Class.forName("javax.swing.JFrame");
      Method m = c.getMethod("setOpacity", float.class);
      m.invoke(this, alpha);
    } catch (Exception e) {
      log.error("setOpacity: did not work");
    }
  }

  public JPanel getJPanel() {
    return _panel;
  }

  public Graphics2D getJPanelGraphics() {
    return _currG2D;
  }

  @Override
  public void addObserver(EventObserver o) {
    _obs = o;
  }

  @Override
  public void notifyObserver() {
    _obs.update(this);
  }

  public void close() {
    setVisible(false);
    dispose();
  }
}
