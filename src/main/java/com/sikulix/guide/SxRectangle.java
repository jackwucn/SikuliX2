/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */

/**
 *
 */
package com.sikulix.guide;

import com.sikulix.api.Element;

import java.awt.*;

public class SxRectangle extends Visual {

  public SxRectangle(Element region) {
    super();
    init(region);
  }

  public SxRectangle(Visual comp) {
    super();
    init(comp.getRegion());
  }

  public SxRectangle() {
    super();
    init(null);
  }

  private void init(Element region) {
    if (region != null) {
      targetRegion = region;
    } else {
      targetRegion = Element.create(0, 0, 2*stroke, 2*stroke);
    }
    setColor(Color.RED);
  }

  @Override
  public void updateComponent() {
    setActualBounds(targetRegion.getRectangle());
    setForeground(colorFront);
    super.setLocationRelative(Layout.OVER);
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    Stroke pen = new BasicStroke(stroke);
    g2d.setStroke(pen);
    g2d.drawRect(0, 0, getActualWidth() - 1, getActualHeight() - 1);
  }
}
