/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.editor;

import com.sikulix.run.Runner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ScriptTemplate {

  static Map<String, String[]> commandTemplates = new HashMap<>();

  static List<String> createMethods = new ArrayList<>();

  static void initTemplates() {
    Method[] methods = ScriptTemplate.class.getMethods();
    for (Method method : methods) {
      if (method.getName().startsWith("create")) {
        createMethods.add(method.getName());
      }
    }

    commandTemplates.put("find", new String[]{"", "@?", "{region}", "result"});
    commandTemplates.put("f", new String[]{"find"});
    commandTemplates.put("wait", new String[]{"", "wait-time", "@?", "{region}", "result"});
    commandTemplates.put("w", new String[]{"wait"});
    commandTemplates.put("vanish", new String[]{"", "wait-time", "@?", "{region}", "result"});
    commandTemplates.put("v", new String[]{"vanish"});
    commandTemplates.put("findAll", new String[]{"", "@?", "{region}", "result-list"});
    commandTemplates.put("fa", new String[]{"findAll"});
    commandTemplates.put("findBest", new String[]{"", "@@?", "{region}", "result"});
    commandTemplates.put("fb", new String[]{"findBest"});
    commandTemplates.put("findAny", new String[]{"", "@@?", "{region}", "result-list"});
    commandTemplates.put("fy", new String[]{"findAny"});
    commandTemplates.put("click", new String[]{"", "@?", "{region}", "result"});
    commandTemplates.put("c", new String[]{"click"});
    commandTemplates.put("clickRight", new String[]{"", "@?", "{region}", "result"});
    commandTemplates.put("cr", new String[]{"clickRight"});
    commandTemplates.put("clickDouble", new String[]{"", "@?", "{region}", "result"});
    commandTemplates.put("cd", new String[]{"clickDouble"});
    commandTemplates.put("hover", new String[]{"", "@?", "{region}", "result"});
    commandTemplates.put("h", new String[]{"hover"});
    commandTemplates.put("write", new String[]{"", "@?", "{region}", "{keys}"});
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

    commandTemplates.put("print", new String[]{"", "variable..."});
    commandTemplates.put("p", new String[]{"print"});
    commandTemplates.put("printf", new String[]{"", "{template}", "variable..."});
    commandTemplates.put("pf", new String[]{"printf"});
    commandTemplates.put("log", new String[]{"", "{template}", "variable..."});
    commandTemplates.put("pop", new String[]{"", "message", "result"});
    commandTemplates.put("use", new String[]{"", "{region}", "result"});

    commandTemplates.put("import", new String[]{"", "scriptname", "parameter..."});

    commandTemplates.put("image", new String[]{"", "@?", "similar", "{offset [x,y]}"});
    commandTemplates.put("$I", new String[]{"=@?", "similar", "{offset [x,y]}"});
    commandTemplates.put("$$I", new String[]{"=imageList", "@@?", "{[image,image,...]}"});
    commandTemplates.put("imageList", new String[]{"", "@@?", "{[image,image,...]}"});
    commandTemplates.put("variable", new String[]{"", "$?", "{expression}"});
    commandTemplates.put("$", new String[]{"?", "{expression}"});
    commandTemplates.put("option", new String[]{"", "key", "{value}"});
    commandTemplates.put("$O", new String[]{"=option", "key", "{value}"});
    commandTemplates.put("region", new String[]{"", "$R?", "{[x,y,w,h]}"});
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

  static String convertScript(Script script, List<List<ScriptCell>> scriptData) {
    return convertScript(script, Runner.ScriptType.JAVASCRIPT, scriptData);
  }

  static String convertScript(Script script, Runner.ScriptType type, List<List<ScriptCell>> scriptData) {
    String snippet = "";
    Integer lineNumber = 0;
    for (List<ScriptCell> line : scriptData) {
      String command = line.get(0).get();
      if (createMethods.contains("create" + command)) {
        Object[] parameters = new Object[]{line, lineNumber};
        try {
          ScriptTemplate.class.getMethod("create" + command, new Class[]{new ArrayList<ScriptCell>().getClass(), Integer.class});
        } catch (NoSuchMethodException e) {
          script.log.error("convertScript: command not implemented: (%d) %s", lineNumber, command);
        }
      } else {
        script.log.error("convertScript: invalid command: (%d) %s", lineNumber, command);
      }
    }
    return snippet;
  }

  public static String createfind(ArrayList<ScriptCell> line, Integer lineNumber) {
    String snippet = "$LINENUMBER = " + lineNumber + ";";
    String what = null;
    String where = null;
    String template = " find(%s, %s);";
    snippet += String.format(template, what, where);
    return snippet;
  }
}
