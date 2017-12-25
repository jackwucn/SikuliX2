/*
 * Copyright (c) 2010-2017, sikuli.org, sikulix.com - MIT license
 */

/**
 *
 */
package com.sikulix.guide;

import com.sikulix.api.Element;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class SxCircle extends Visual {

  public SxCircle(Element region) {
    super();
    init(region);
  }

  public SxCircle(Visual comp) {
    super();
    init(comp.getRegion());
  }

  public SxCircle() {
    super();
    init(null);
  }

  private void init(Element region) {
    if (region != null) {
      targetRegion = region;
    } else {
      targetRegion = new Element(0, 0, 2*stroke, 2*stroke);
    }
    setColor(Color.RED);
  }

  @Override
  public void updateComponent() {
    setActualBounds(getTarget().getRectangle());
    setForeground(colorFront);
    super.setLocationRelative(Layout.OVER);
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
    Stroke pen = new BasicStroke((float) stroke);
    g2d.setStroke(pen);
    Rectangle r = new Rectangle(getActualBounds());
    r.grow(-(stroke-1), -(stroke-1));
    g2d.translate(stroke-1, stroke-1);
    Ellipse2D.Double ellipse = new Ellipse2D.Double(0, 0, r.width - 1, r.height - 1);
    g2d.draw(ellipse);
  }
}
