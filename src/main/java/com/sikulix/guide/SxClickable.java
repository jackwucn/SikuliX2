/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */

/**
 *
 */
package com.sikulix.guide;

import com.sikulix.api.Element;

import java.awt.*;

public class SxClickable extends Visual {

  Color normalColor = new Color(1.0f, 1.0f, 0, 0.1f);
  Color mouseOverColor = new Color(1.0f, 0, 0, 0.1f);
  String name;
  Element region;
  boolean borderVisible = true;
  boolean mouseOverVisible = false;
  boolean mouseOver;

  public SxClickable(Element region) {
    this.region = region;
    if (region != null) {
      this.setActualBounds(region.getRectangle());
      this.setActualLocation(region.x, region.y);
    }
  }

  Point clickPoint = null;

  public SxClickable() {
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setBorderVisible(boolean borderVisible) {
    this.borderVisible = borderVisible;
  }

  public void setMouseOverVisible(boolean visible) {
    mouseOverVisible = visible;
  }

  public void setMouseOver(boolean mouseOver) {
    if (this.mouseOver != mouseOver) {
      if (this.mouseOver) {
        globalMouseExited();
      } else {
        globalMouseEntered();
      }
      Rectangle r = getBounds();
      this.getTopLevelAncestor().repaint(r.x, r.y, r.width, r.height);
    }
    this.mouseOver = mouseOver;
  }

  public boolean isMouseOver() {
    return mouseOver;
  }

  public void globalMouseMoved(Point p) {
  }

  public void globalMouseEntered() {
    mouseOver = true;
  }

  public void globalMouseExited() {
    mouseOver = false;
  }

  public void globalMouseClicked(Point p) {
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    g2d.setColor(new Color(1, 1, 1, 0.05f));
    g2d.fillRect(0, 0, getActualWidth() - 1, getActualHeight() - 1);
    if (mouseOverVisible) {
      if (mouseOver) {
        g2d.setColor(mouseOverColor);
      } else {
        g2d.setColor(normalColor);
      }
      g2d.fillRect(0, 0, getActualWidth() - 1, getActualHeight() - 1);
    }
  }
}
