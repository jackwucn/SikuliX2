/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.run;

import com.sikulix.api.Do;
import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Runner {

  static SXLog log = SX.getSXLog("SX.Runner");

  public enum ScriptType {FROMUNKNOWN, JAVASCRIPT, PYTHON, RUBY, APPLESCRIPT, FROMJAR, FROMNET}

  public enum ScriptOption {WITHTRACE}

  private static Map<ScriptType, String> scriptTypes = new HashMap<>();

  static {
    scriptTypes.put(ScriptType.JAVASCRIPT, ".js");
    scriptTypes.put(ScriptType.PYTHON, ".py");
    scriptTypes.put(ScriptType.RUBY, ".rb");
  }

  static int NOTYETRUN = -99999999;
  static int RUNWITHERROR = NOTYETRUN;

  static URL scriptPath = null;
  static String inJarFolderSX = "/SX_Scripts";


  public static URL getScriptPath() {
    return scriptPath;
  }

  public static boolean setScriptPath(Object... args) {
    scriptPath = Content.asURL(args);
    return SX.isNotNull(scriptPath);
  }

  public static File[] getScriptFile(File fScriptFolder) {
    if (fScriptFolder == null) {
      return null;
    }
    String scriptName;
    String scriptType = "";
    String fpUnzippedSkl = null;
    File[] scriptFiles = null;

    if (fScriptFolder.getName().endsWith(".skl") || fScriptFolder.getName().endsWith(".zip")) {
      fpUnzippedSkl = Content.unzipSKL(fScriptFolder.getAbsolutePath());
      if (fpUnzippedSkl == null) {
        return null;
      }
      scriptType = "sikuli-zipped";
      fScriptFolder = new File(fpUnzippedSkl);
    }

    int pos = fScriptFolder.getName().lastIndexOf(".");
    if (pos == -1) {
      scriptName = fScriptFolder.getName();
      scriptType = "sikuli-plain";
    } else {
      scriptName = fScriptFolder.getName().substring(0, pos);
      scriptType = fScriptFolder.getName().substring(pos + 1);
    }

    boolean success = true;
    if (!fScriptFolder.exists()) {
      if ("sikuli-plain".equals(scriptType)) {
        fScriptFolder = new File(fScriptFolder.getAbsolutePath() + ".sikuli");
        if (!fScriptFolder.exists()) {
          success = false;
        }
      } else {
        success = false;
      }
    }
    if (!success) {
      log.error("Not a valid Sikuli script project:\n%s", fScriptFolder.getAbsolutePath());
      return null;
    }
    if (scriptType.startsWith("sikuli")) {
      scriptFiles = fScriptFolder.listFiles(new FileFilterScript(scriptName + "."));
      if (scriptFiles == null || scriptFiles.length == 0) {
        log.error("Script project %s \n has no script file %s.xxx", fScriptFolder, scriptName);
        return null;
      }
    } else if ("jar".equals(scriptType)) {
      log.error("Sorry, script projects as jar-files are not yet supported;");
      //TODO try to load and run as extension
      return null; // until ready
    }
    return scriptFiles;
  }

  private static class FileFilterScript implements FilenameFilter {
    private String scriptName;

    public FileFilterScript(String scriptName) {
      this.scriptName = scriptName;
    }

    @Override
    public boolean accept(File dir, String fileName) {
      return fileName.startsWith(scriptName);
    }
  }

  public static ReturnObject run(Object... args) {
    if (args.length == 0) {
      log.error("run: no args");
      return null;
    }
    RunBox runBox = new RunBox(args);
    if (runBox.isValid()) {
      log.trace("starting run: %s", args[0]);
      new Thread(runBox).start();
      runBox.running = true;
      while (runBox.running) {
        SX.pause(1);
      }
      log.trace("ending run: %s with %s", args[0], runBox.getReturnObject());
      return runBox.getReturnObject();
    }
    return null;
  }

  private static class RunBox implements Runnable {

    Object[] args;
    int firstUserArg = 0;
    boolean running = false;
    boolean valid = false;
    ScriptType type = ScriptType.FROMUNKNOWN;
    String scriptName = "";
    URL scriptURL = null;
    String script = "";
    ReturnObject returnObject = null;

    public RunBox(Object[] args) {
      this.args = args;
      if (ScriptOption.WITHTRACE.equals(args[args.length - 1])) {
        log.on(SXLog.TRACE);
      }
      init();
    }

    private void init() {
      int firstarg = 0;
      if (args[0] instanceof ScriptType) {
        if (args.length > 1) {
          firstarg = 1;
          type = (ScriptType) args[0];
        }
      }
      if (args[firstarg] instanceof String) {
        firstUserArg = firstarg + 1;
        if (ScriptType.JAVASCRIPT.equals(type)) {
          scriptName = "givenAsText";
          setValid();
          script = (String) args[firstarg];
          return;
        }
        if (ScriptType.APPLESCRIPT.equals(type)) {
          scriptName = "givenAsText";
          setValid();
          script = (String) args[firstarg];
          return;
        }
        if (SX.isNotNull(type) && !type.toString().startsWith("FROM")) {
          log.error("RunBox.init: %s not implemented", type);
          return;
        }
        scriptName = (String) args[firstarg];
        String args1 = "";
        String args2 = "";
        if (args.length > firstarg + 1 && args[firstarg + 1] instanceof String) {
          args1 = (String) args[firstarg + 1];
          firstUserArg++;
        }
        if (args.length > firstarg + 2 && args[firstarg + 2] instanceof String) {
          args2 = (String) args[firstarg + 2];
          firstUserArg++;
        }
        if (ScriptType.FROMUNKNOWN.equals(type) || ScriptType.FROMJAR.equals(type)) {
          String scriptFolder = args1;
          String classReference = args2;
          script = getScriptFromJar(scriptName, scriptFolder, classReference);
          if (SX.isSet(script)) {
            setValid();
            return;
          }
        }
        if (ScriptType.FROMUNKNOWN.equals(type) || ScriptType.FROMNET.equals(type)) {
          String scriptFolder = args1;
          String httpRoot = args2;
          script = getScriptFromNet(scriptName, scriptFolder, httpRoot);
          if (SX.isSet(script)) {
            setValid();
            return;
          }
        }
      } else if (args[firstarg] instanceof File) {

      } else if (args[firstarg] instanceof URL) {

      } else {
        log.error("RunBox.init: invalid args (arg0: %s)", args[0]);
      }
      if (SX.isNotNull(scriptURL)) {
        //TODO load script
      }
      if (isValid()) {
        log.trace("Runbox: init: success for: %s", args[firstarg]);
      }
    }

    private String getScriptFromJar(String scriptName, String scriptFolder, String classReference) {
      String scriptText = "";
      if (SX.isNotSet(classReference)) {
        if (SX.isNotSet(scriptFolder)) {
          scriptFolder = inJarFolderSX;
        }
        scriptFolder += "/" + scriptName;
        for (ScriptType scriptType : scriptTypes.keySet()) {
          String scriptFile = scriptName + scriptTypes.get(scriptType);
          try {
            scriptText = Content.extractResourceToString(scriptFolder, scriptFile);
          } catch (Exception ex) {
            continue;
          }
          if (SX.isSet(scriptText)) {
            type = scriptType;
            break;
          }
          scriptText = "";
        }
      } else {
        log.error("RunBox: getScriptFromJar: non-SX classReference not implemented");
      }
      return scriptText;
    }

    private String getScriptFromNet(String scriptName, String scriptFolder, String httpRoot) {
      String scriptText = "";
      if (SX.isNotSet(httpRoot)) {
        httpRoot = SX.getSXWEBDOWNLOAD();
      }
      if (SX.isNotSet(scriptFolder)) {
        scriptFolder = "/Scripts";
      }
      URL url = Content.asURL(httpRoot, scriptFolder + "/" + scriptName);
      if (SX.isNotNull(url)) {
        for (ScriptType scriptType : scriptTypes.keySet()) {
          String scriptFile = scriptName + scriptTypes.get(scriptType);
          scriptText = Content.downloadScriptToString(Content.asURL(url, scriptFile));
          if (SX.isSet(scriptText)) {
            type = scriptType;
            break;
          }
          scriptText = "";
        }
      }
      if (SX.isNotSet(scriptText)) {
        log.error("getScriptFromNet: script not valid: %s", url);
      }
      return scriptText;
    }

    public boolean isValid() {
      return valid;
    }

    public void setValid() {
      valid = true;
    }

    public ReturnObject getReturnObject() {
      return returnObject;
    }

    @Override
    public void run() {
      returnObject = new ReturnObject(NOTYETRUN);
      if (ScriptType.JAVASCRIPT.equals(type)) {
        runJS();
      } else if (ScriptType.APPLESCRIPT.equals(type)) {
        runAS();
      }
      running = false;
    }

    private boolean runJS() {
      ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
      String scriptBefore = "var Do = Java.type('com.sikulix.api.Do');\n";
      if (log.isLevel(SXLog.TRACE)) {
        scriptBefore += "print('Hello from JavaScript: SikuliX support loaded');\n";
      }
      String scriptText = scriptBefore;
      scriptText += script;
      log.trace("%s: running script %s", ScriptType.JAVASCRIPT, scriptName);
      if (log.isLevel(SXLog.TRACE)) {
        log.p(script);
        log.p("---------- end of script");
      }
      try {
        engine.eval(scriptText);
      } catch (ScriptException e) {
        log.trace("%s: error: %s", ScriptType.JAVASCRIPT, e.getMessage());
        returnObject = new ReturnObject(false);
        return false;
      }
      log.trace("%s: ending run", ScriptType.JAVASCRIPT);
      returnObject = new ReturnObject(true);
      return true;
    }

    private void runAS() {
      if (!SX.isMac()) {
        log.error("Applescript run: not on a Mac system");
        return;
      }
      String scriptBefore = "";
      String scriptText = scriptBefore;
      scriptText += script;
      log.trace("%s: running script %s", ScriptType.APPLESCRIPT, scriptName);
      if (log.isLevel(SXLog.TRACE) || log.isGlobalLevel(SXLog.TRACE)) {
        log.p(script);
        log.p("---------- end of script");
      }
      String osascriptShebang = "#!/usr/bin/osascript\n";
      scriptText = osascriptShebang + scriptText;
      File aFile = Content.createTempFile("script");
      aFile.setExecutable(true);
      Content.writeStringToFile(scriptText, aFile);
      String retVal = Do.runcmd(new String[]{aFile.getAbsolutePath()});
      returnObject = new ReturnObject(ScriptType.APPLESCRIPT, retVal);
      if (returnObject.isSuccess()) {
        log.trace("Applescript run: success");
      } else {
        log.trace("Applescript run: no success: %s", returnObject.getLoad());
      }
    }
  }

  public static class ReturnObject {

    private ReturnObject() {
    }

    public ReturnObject(boolean success) {
      if (!success) {
        rCode = 1;
      }
    }

    public ReturnObject(int returnCode) {
      rCode = returnCode;
      if (rCode == NOTYETRUN) {
        load = "Script not yet run";
      }
    }

    public ReturnObject(ScriptType type, Object rObject) {
      if (ScriptType.APPLESCRIPT.equals(type)) {
        String[] parts = rObject.toString().split("\n");
        rCode = RUNWITHERROR;
        try {
          rCode = Integer.parseInt(parts[0]);
        } catch (Exception ex) {
          load = rObject.toString();
        }
        if (rCode != RUNWITHERROR) {
          load = rObject.toString().substring(parts[0].length()).trim();
        }
      }
    }

    private int rCode = 0;

    public boolean isSuccess() {
      return rCode == 0;
    }

    private Object load = null;

    public boolean hasLoad() {
      return SX.isNotNull(load);
    }

    public void setLoad(Object load) {
      this.load = load;
    }

    public Object getLoad() {
      return load;
    }

    @Override
    public String toString() {
      return String.format("(%d) %s", rCode, load);
    }
  }
}
