/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.core.Content;
import com.sikulix.core.SX;
import com.sikulix.run.Runner;

import javax.swing.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ScriptTemplate {

  static Map<String, String[]> commandTemplates = new HashMap<>();

  static List<String> createMethods = new ArrayList<>();

  static void initTemplates() {
    createTemplates();
    Method[] methods = ScriptTemplate.class.getMethods();
    for (Method method : methods) {
      if (method.getName().startsWith("create")) {
        createMethods.add(method.getName());
      }
    }
  }

  private static void createTemplates() {
    commandTemplates.put("find", new String[]{"", "@what?", "{where}", "result"});
    commandTemplates.put("f", new String[]{"find"});
    commandTemplates.put("wait", new String[]{"", "wait-time", "@what?", "{where}", "result"});
    commandTemplates.put("w", new String[]{"wait"});
    commandTemplates.put("vanish", new String[]{"", "wait-time", "@what?", "{where}", "result"});
    commandTemplates.put("v", new String[]{"vanish"});
    commandTemplates.put("findAll", new String[]{"", "@what?", "{where}", "result-list"});
    commandTemplates.put("fa", new String[]{"findAll"});
    commandTemplates.put("findBest", new String[]{"", "@@what?", "{where}", "result"});
    commandTemplates.put("fb", new String[]{"findBest"});
    commandTemplates.put("findAny", new String[]{"", "@@what?", "{where}", "result-list"});
    commandTemplates.put("fy", new String[]{"findAny"});
    commandTemplates.put("click", new String[]{"", "@what?", "{where}", "{offset [x,y]}", "{keys}", "result"});
    commandTemplates.put("c", new String[]{"click"});
    commandTemplates.put("clickRight", new String[]{"", "@what?", "{where}", "{offset [x,y]}", "result"});
    commandTemplates.put("cr", new String[]{"clickRight"});
    commandTemplates.put("clickDouble", new String[]{"", "@what?", "{where}", "{offset [x,y]}", "result"});
    commandTemplates.put("cd", new String[]{"clickDouble"});
    commandTemplates.put("hover", new String[]{"", "@what?", "{where}", "{offset [x,y]}", "result"});
    commandTemplates.put("h", new String[]{"hover"});
    commandTemplates.put("write", new String[]{"", "@what?", "{where}", "{offset [x,y]}", "{keys}"});
    commandTemplates.put("wr", new String[]{"write"});
    commandTemplates.put("hotkey", new String[]{"", "{keys}", "{function}"});
    commandTemplates.put("hk", new String[]{"hotkey"});
    commandTemplates.put("focus", new String[]{"", "appname"});
    commandTemplates.put("fo", new String[]{"focus"});

    commandTemplates.put("if", new String[]{"", "{condition}", "{script}"});
    commandTemplates.put("ifNot", new String[]{"", "{condition}", "{script}"});
    commandTemplates.put("in", new String[]{"ifNot"});
    commandTemplates.put("endif", new String[]{""});
    commandTemplates.put("else", new String[]{"", "{script}"});
    commandTemplates.put("e", new String[]{"else"});
    commandTemplates.put("elif", new String[]{"", "{condition}", "{script}"});
    commandTemplates.put("ei", new String[]{"elif"});
    commandTemplates.put("elifNot", new String[]{"", "{condition}", "{script}"});
    commandTemplates.put("en", new String[]{"elifNot"});
    commandTemplates.put("ifElse", new String[]{"", "{condition}", "{script}", "{script}", "result"});
    commandTemplates.put("ie", new String[]{"ifElse"});
    commandTemplates.put("setIf", new String[]{"", "{condition}", "$?", "{script}"});
    commandTemplates.put("si", new String[]{"setIf"});
    commandTemplates.put("setIfNot", new String[]{"", "{condition}", "$?", "{script}"});
    commandTemplates.put("sn", new String[]{"setIfNot"});

    commandTemplates.put("loop", new String[]{"", "{condition}", "{script}"});
    commandTemplates.put("l", new String[]{"loop"});
    commandTemplates.put("loopWith", new String[]{"", "$$?", "{script}"});
    commandTemplates.put("lw", new String[]{"loopWith"});
    commandTemplates.put("loopFor", new String[]{"", "{count step from}", "{script}"});
    commandTemplates.put("lf", new String[]{"loopFor"});
    commandTemplates.put("endloop", new String[]{""});
    commandTemplates.put("break", new String[]{"", ""});
    commandTemplates.put("br", new String[]{"break"});
    commandTemplates.put("breakIf", new String[]{"", "{condition}"});
    commandTemplates.put("bi", new String[]{"breakIf"});
    commandTemplates.put("continue", new String[]{"", ""});
    commandTemplates.put("co", new String[]{"continue"});
    commandTemplates.put("ContinueIf", new String[]{"", "{condition}"});
    commandTemplates.put("ci", new String[]{"ContinueIf"});
    commandTemplates.put("Exit", new String[]{"", "{value}"});
    commandTemplates.put("ex", new String[]{"Exit"});

    commandTemplates.put("print", new String[]{"", "variable..."});
    commandTemplates.put("p", new String[]{"print"});
    commandTemplates.put("printf", new String[]{"", "{template}", "variable..."});
    commandTemplates.put("pf", new String[]{"printf"});
    commandTemplates.put("log", new String[]{"", "{template}", "variable..."});
    commandTemplates.put("pop", new String[]{"", "message", "result"});
    commandTemplates.put("use", new String[]{"", "{element}", "result"});

    commandTemplates.put("import", new String[]{"", "scriptname", "parameter..."});

    commandTemplates.put("image", new String[]{"", "@?", "similar", "{offset [x,y]}", "result"});
    commandTemplates.put("$I", new String[]{"=@?", "similar", "{offset [x,y]}", "result"});
    commandTemplates.put("$$I", new String[]{"=imageList", "@@?", "{[image,image,...]}"});
    commandTemplates.put("imageList", new String[]{"", "@@?", "{[image,image,...]}"});
    commandTemplates.put("variable", new String[]{"", "$?", "{expression}"});
    commandTemplates.put("$", new String[]{"?", "{expression}"});
    commandTemplates.put("option", new String[]{"", "key", "{value}"});
    commandTemplates.put("$O", new String[]{"=option", "key", "{value}"});
    commandTemplates.put("where", new String[]{"", "$R?", "{[x,y,w,h]}"});
    commandTemplates.put("$R", new String[]{"?", "{[x,y,w,h]}"});
    commandTemplates.put("location", new String[]{"", "$L?", "{[x,y]}"});
    commandTemplates.put("$L", new String[]{"?", "{[x,y]}"});
    commandTemplates.put("array", new String[]{"", "$$?", "{[item,item,...]}"});
    commandTemplates.put("$$", new String[]{"=array", "$$?", "{[item,item,...]}"});
    commandTemplates.put("function", new String[]{"", "$F?", "{script}", "parameter..."});
    commandTemplates.put("$F", new String[]{"?", "{function}"});
    commandTemplates.put("endfunction", new String[]{""});
    commandTemplates.put("/", new String[]{"continuation", ""});
    commandTemplates.put("#", new String[]{"comment", ""});
    commandTemplates.put("{", new String[]{"={script}", "result"});
  }

  protected static String createTip(int row, int col) {
    String tip = null;
    if (col == 0) {
      tip = "line number";
    } else {
    }
    return tip;
  }

  static String convertScript(Script script, List<List<ScriptCell>> scriptData, File scriptFolder, boolean shouldTrace) {
    return convertScript(script, Runner.ScriptType.JAVASCRIPT, scriptData, scriptFolder, shouldTrace);
  }

  static String convertScript(Script script, Runner.ScriptType type,
                              List<List<ScriptCell>> scriptData, File scriptFolder, boolean shouldTrace) {
    String msgAutogen = "//*** (%d) autogenerated\n%s\n";
    String snippet = Content.extractResourceToString("Javascript", "sikulix.js");
    snippet += "Do.setBundlePath(\"" + scriptFolder.getAbsolutePath() + "\");\n";
    Integer lineNumber = 0;
    if (shouldTrace) {
      snippet += "log.on(SX.TRACE); //via global trace\n";
      ScriptTemplate.shouldTrace = true;
    }
    for (List<ScriptCell> line : scriptData) {
      String command = line.get(0).get();
      lineNumber++;
      if (command.contains("#")) {
        continue;
      }
      if (createMethods.contains("create" + command)) {
        Object[] parameters = new Object[]{line, lineNumber};
        try {
          Object snippetLine = ScriptTemplate.class.getMethod("create" + command, new Class[]{new ArrayList<ScriptCell>().getClass(), Integer.class})
                  .invoke(null, line, lineNumber);
          if (!((String) snippetLine).startsWith("//ERROR")) {
            snippet += String.format(msgAutogen, lineNumber, snippetLine);
          } else {
            snippet += snippetLine;
          }
        } catch (Exception e) {
          script.log.error("convertScript: command error: (%d) %s (%s)", lineNumber, command, e.getMessage());
          snippet += createErrorLine((ArrayList<ScriptCell>) line, lineNumber, "command error: " + e.getMessage());
        }
      } else {
        snippet += createErrorLine((ArrayList<ScriptCell>) line, lineNumber, "command invalid");
      }
    }
    return snippet;
  }

  public static String createErrorLine(ArrayList<ScriptCell> line, Integer lineNumber, String reason) {
    String snippet = String.format("//ERROR (%d): ", lineNumber);
    String sep = "";
    String sLine = "";
    for (ScriptCell cell : line) {
      sLine += sep + cell.get().trim();
      sep = " | ";
    }
    return snippet + sLine + " (" + reason + ")" + "\n";
  }

  public static String createfind(ArrayList<ScriptCell> line, Integer lineNumber) {
    String snippet = "$LINENUMBER = " + lineNumber + ";";
    String what = evalWhat(line, 1);
    if (SX.isNull(what)) {
      return createErrorLine(line, lineNumber, "no image");
    }
    String where = evalWhere(line, 2);
    String result = line.get(3).get();
    String template = " %s = find(%s, %s);";
    snippet += String.format(template, result, what, where);
    return createFinal(snippet, " log.trace(\"(#d) find: #s\", %s, %s);", lineNumber, result);
  }

  public static String createfindAll(ArrayList<ScriptCell> line, Integer lineNumber) {
    return createfind(line, lineNumber)
            .replace("= find(", "= findAll(")
            .replace(") find:", ") findAll:");
  }

  private static String createFinal(String statement, String logStatement, Object... args) {
    logStatement = String.format(logStatement, args).replaceAll("#", "%");
    return statement + logStatement;
  }

  public static String createwait(ArrayList<ScriptCell> line, Integer lineNumber) {
    String snippet = "$LINENUMBER = " + lineNumber + ";";
    String what = evalWhat(line, 2);
    if (SX.isNull(what)) {
      return createErrorLine(line, lineNumber, "no image");
    }
    String where = evalWhere(line, 3);
    int waitTime = evalWaitTime(line, 1);
    String result = line.get(4).get();
    String template = " %s = wait(%s, %s, %d);";
    snippet += String.format(template, result, what, where, waitTime, result);
    return createFinal(snippet, " log.trace(\"(#d) wait: #s\", %s, %s);", lineNumber, result);
  }

  public static String createvanish(ArrayList<ScriptCell> line, Integer lineNumber) {
    return createwait(line, lineNumber)
            .replace("= wait(", "= waitVanish(")
            .replace(") wait:", ") waitVanish:");
  }

  private static String evalWhat(List<ScriptCell> line, int col) {
    String what = line.get(col).get();
    if (what.contains("?")) {
      what = null;
    } else {
      what = "\"" + what.replace("@", "") + "\"";
    }
    return what;
  }

  private static String evalWhere(List<ScriptCell> line, int col) {
    String where = line.get(col).get();
    if (where.contains("{where}")) {
      where = null;
    }
    return where;
  }

  private static int evalWaitTime(List<ScriptCell> line, int col) {
    String waitTime = line.get(col).get();
    if (waitTime.contains("wait-time")) {
      return -1;
    } else {
      return Integer.parseInt(waitTime);
    }
  }

  private static boolean shouldTrace;

  public static String createoption(ArrayList<ScriptCell> line, Integer lineNumber) {
    String snippet = "$LINENUMBER = " + lineNumber + ";";
    String key = line.get(1).get();
    String value = line.get(2).get();
    if ("log".equals(key) && "trace".equals(value)) {
      if (!shouldTrace) {
        snippet += " log.on(SX.TRACE);\n";
        snippet += String.format(" log.trace(\"(%d) option: log = trace\");", lineNumber);
        return snippet;
      }
    }
    snippet += String.format(" //option: %s = %s", key, value);
    return snippet;
  }
}
