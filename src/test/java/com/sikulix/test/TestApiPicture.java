/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.test;

import com.sikulix.api.Do;
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
public class TestApiPicture {

  static SXLog log = SX.getSXLog("SX_TestApiPicture");

  private static String defaultImagePath = "SX_Images";
  private static String mavenRoot = "target/classes";
  private static String jarImagePathDefault = "." + "/" + defaultImagePath;
  private static String jarImagePathClass = "com.sikulix.testjar.Testjar" + "/" + defaultImagePath;
  private static String gitRoot = "https://raw.githubusercontent.com/RaiMan/SikuliX2/master";
  private static String gitImagePath = gitRoot + "/src/main/resources/" + defaultImagePath;
  private static String imageNameDefault = "sikulix2";

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
  public void test_010_loadImageFromFile() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    boolean success = Do.setBundlePath(mavenRoot, defaultImagePath);
    String result = "BundlePath: " + Do.getBundlePath();
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = String.format("(%s) Image %s from %s", img.getTimeToLoad(), img.getName(), img.getURL());
      if (log.isGlobalLevel(SXLog.TRACE)) {
        img.show();
      }
    }
    assert success;
    currentTest.setResult(result);
  }

  @Test
  public void test_011_loadImageFromJarByClass() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    boolean success = Do.setBundlePath(jarImagePathClass);
    String result = "BundlePath: " + Do.getBundlePath();
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = String.format("(%s) Image %s from %s", img.getTimeToLoad(), img.getName(), img.getURL());
      if (log.isGlobalLevel(SXLog.TRACE)) {
        img.show();
      }
    }
    assert success;
    currentTest.setResult(result);
  }

  @Test
  public void test_012_loadImageFromHttp() {
    currentTest = new SXTest();
    if (currentTest.shouldNotRun()) {
      return;
    }
    boolean success = Do.setBundlePath(gitImagePath);
    String result = "BundlePath: " + Do.getBundlePath();
    Picture img = new Picture(imageNameDefault);
    success &= img.isValid();
    if (success) {
      result = String.format("(%s) Image %s from %s", img.getTimeToLoad(), img.getName(), img.getURL());
      if (log.isGlobalLevel(SXLog.TRACE)) {
        img.show();
      }
    }
    assert success;
    currentTest.setResult(result);
  }


}
