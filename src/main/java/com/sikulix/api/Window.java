/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.api;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.run.Runner;

import java.awt.*;
import java.net.URL;

public class Window extends Element {

  private static eType eClazz = eType.WINDOW;

  public eType getType() {
    return eClazz;
  }

  private static SXLog log = SX.getSXLog("SX." + eClazz.toString());

  private String application = "";

  public String getAppName() {
    return application;
  }

  public Window() {
    if (SX.isMac()) {
      String script = SX.str(
      "tell application 'System Events'",
             "set activeApp to name of first application process whose frontmost is true",
             "activeApp",
             "end tell");
      Runner.ReturnObject returnObject = Runner.run(Runner.ScriptType.APPLESCRIPT, script);
      if (returnObject.isSuccess()) {
        application = (String) returnObject.getLoad();
      }
    }
  }

  public Window(String application) {
    this.application = application;
  }

  public String toFront() {
    if (SX.isMac()) {
      String script = SX.str(
      "tell app '%s' to activate",
             "tell application 'System Events'",
             "set activeApp to name of first application process whose frontmost is true",
             "activeApp",
             "end tell", "#", application);
      Runner.ReturnObject returnObject = Runner.run(Runner.ScriptType.APPLESCRIPT, script);
      if (returnObject.isSuccess()) {
        return (String) returnObject.getLoad();
      }
    }
    return "";
  }

  /**
   * open the given url in the standard browser
   *
   * @param url string representing a valid url
   * @return false on error, true otherwise
   */
  public static boolean openURL(String url) {
    try {
      URL u = new URL(url);
      Desktop.getDesktop().browse(u.toURI());
    } catch (Exception ex) {
      log.error("show in browser: bad URL: " + url);
      return false;
    }
    return true;
  }
}
