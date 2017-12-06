/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.Element;
import com.sikulix.api.Picture;
import com.sikulix.api.Target;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.core.SXTest;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.opencv.core.Mat;

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestApiElement {

  static SXLog log = SX.getSXLog("SX_TestApiElement");

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
    currentTest = new SXTest();
    currentTest = new SXTest().onlyLocal();
    if (currentTest.shouldNotRun()) {
      return;
    }
    currentTest.setResult("test template");
  }

  @Test
  public void test_010_elementConstructors() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    Element elem = new Element();
    String result = "Element();";
    assert Element.eType.ELEMENT.equals(elem.getType());
    Picture img = new Picture();
    result += " Picture();";
    assert Element.eType.PICTURE.equals(img.getType());
    Target tgt = new Target();
    result += " Target();";
    assert Element.eType.TARGET.equals(tgt.getType());
    tgt = new Target(img);
    result += " Target(image);";
    assert Element.eType.TARGET.equals(tgt.getType());
    tgt = new Target(tgt);
    result += " Target(target);";
    assert Element.eType.TARGET.equals(tgt.getType());
    Mat aMat = tgt.getContent();
    tgt = new Target(aMat);
    result += " Target(mat);";
    assert Element.eType.TARGET.equals(tgt.getType());
    tgt = new Target(img, 0.95, new Element(2, 3));
    result += " Target(image, 0.95, new Element(2,3));";
    assert Element.eType.TARGET.equals(tgt.getType());
    currentTest.setResult(result);
  }
}
