/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXTest;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.sikuli.script.Location;
import org.sikuli.script.Region;
import org.sikuli.script.Screen;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestScriptRegion {

  static SXLog log = SX.getSXLog("SX_TestScriptRegion");

  @BeforeClass
  public static void setUpClass() {
    log.on(SXLog.INFO);
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
    log.info("%s", currentTest);
  }

  private SXTest currentTest;

  @Ignore
  public void test_000_template() {
    //currentTest = new SXTest();
    currentTest = new SXTest().onlyLocal();
    if (currentTest.shouldNotRun()) {
      return;
    }
    currentTest.setResult("test000 template");
  }

  @Ignore
  public void test_300_oldAPI_Basic() {
    currentTest = new SXTest().onlyLocal();
    if (currentTest.shouldNotRun()) {
      return;
    }
    if (!SX.isHeadless()) {
      String result = "";
      Screen.showMonitors();
      Screen scr = new Screen();
      assert scr.getID() == 0;
      assert Element.equalsRectangle(scr, Do.on().getRectangle());
      scr.hover();
      Location center = scr.getCenter();
      //TODO assert Do.isMouseposition(hook, center.x, center.y);
      Element grow = center.grow(scr.w / 3, scr.h / 3);
      grow.show(3);
      result = "Screen basics: " + scr.toString();
      if (Do.getDevice().getNumberOfMonitors() > 1) {
        scr = new Screen(1);
        scr.hover();
        center = scr.getCenter();
        //TODO assert Do.isMouseposition(hook, center.x, center.y);
        grow = center.grow(scr.w / 3, scr.h / 3);
        grow.show(3);
        result += " with second monitor";
      }
      currentTest.setResult(result);
    }
  }

  @Test
  public void test_300_oldAPI_Region() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    currentTest.setResult("create new Region");
    Region region = new Region(100, 100, 100, 100);
    assert region.isValid() && region instanceof Region : "new Region(100, 100, 100, 100)";
    region = Region.create(100, 100, 100, 100);
    assert region.isValid() && region instanceof Region : "Region.create(100, 100, 100, 100)";
    region = Region.create(-200, -200, 100, 100);
    assert !region.isValid() && region instanceof Region : "Region.create(-200, -200, 100, 100)";
  }

}
