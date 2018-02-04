package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.run.Runner;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;

public class Script implements TableModelListener {

  protected static final SXLog log = SX.getSXLog("SX.SCRIPTEDITOR");
  protected static final int numberCol = 0;
  protected static final int commandCol = 1;
  protected static final int firstParamCol = 2;

  private JFrame window;

  protected JFrame getWindow() {
    return window;
  }

  private Font customFont = new Font("Helvetica Bold", Font.PLAIN, 18);
  PopUpMenus popUpMenus = null;
  PopUpWindow popUpWindow = null;

  ScriptTable table = null;
  public Rectangle rectTable = null;
  int maxCol = 7;

  protected ScriptTable getTable() {
    return table;
  }

  JTextField status = new JTextField();
  boolean lastPopInHeader = false;

  private File fScriptFolder = new File(SX.getSXSTORE(), "scripteditor");
  private File fScript = new File(fScriptFolder, "script.txt");

  protected File getScriptPath() {
    return fScript;
  }

  //  List<Integer> lines = new ArrayList<>();
  List<List<ScriptCell>> allData = new ArrayList<>();
  List<List<ScriptCell>> data = new ArrayList<>();
  List<List<ScriptCell>> savedLines = new ArrayList<>();
  String savedCellText = "";

  protected List<List<ScriptCell>> getData() {
    return data;
  }

  boolean shouldTrace = false;

  protected Script getScript() {
    return this;
  }

  public Script(String[] args) {

    if (args.length > 0 && "trace".equals(args[0])) {
      log.on(SXLog.TRACE);
      shouldTrace = true;
    }

    ToolTipManager.sharedInstance().setEnabled(false);

    window = new JFrame("SikuliX - ScriptEditor");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);

    ScriptTemplate.initTemplates();
    loadScript();

    table = new ScriptTable(this, new ScriptTableModel(this, maxCol, data));

    table.setAutoCreateColumnsFromModel(false);

    FontMetrics metrics = table.getFontMetrics(customFont);
    table.setRowHeight(metrics.getHeight() + 4); // set row height to match font

    table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.setCellSelectionEnabled(true);

    Rectangle monitor = SX.getSXLOCALDEVICE().getMonitor();
    int tableW = (int) (monitor.width * 0.8);
    int tableH = (int) (monitor.height * 0.7);
    if (shouldTrace) {
      tableH = (int) (monitor.height * 0.4);
    }
    Dimension tableDim = new Dimension(tableW, tableH);
    rectTable = new Rectangle();
    rectTable.setSize(tableDim);
//    rectTable.setLocation(monitor.width / 8, monitor.height / 9);
    rectTable.setLocation(50, 80);

    table.setTableHeader(new JTableHeader(table.getColumnModel()) {
      @Override
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = 30;
        return d;
      }
    });

    table.getTableHeader().setBackground(new Color(230, 230, 230));

    int tableCols = table.getColumnCount();
    int col0W = 70;
    int col1W = 150;
    int colLast = 250;
    table.getColumnModel().getColumn(0).setPreferredWidth(col0W);
    table.getColumnModel().getColumn(1).setPreferredWidth(col1W);
    table.getColumnModel().getColumn(maxCol).setPreferredWidth(colLast);
    int colsW = (tableW - col0W - col1W - colLast) / (tableCols - 3);
    for (int n = 2; n < tableCols - 1; n++) {
      table.getColumnModel().getColumn(n).setPreferredWidth(colsW);
    }

    table.setGridColor(Color.LIGHT_GRAY);
    table.setShowGrid(true);
//    table.setShowHorizontalLines(true);

    table.setPreferredScrollableViewportSize(tableDim);
    table.setFillsViewportHeight(true);

//    table.getModel().addTableModelListener(this);

//    ListSelectionModel listSelectionModel = table.getSelectionModel();
//    listSelectionModel.addListSelectionListener(this);
//    table.setSelectionModel(listSelectionModel);

    table.addMouseListener(new MyMouseAdapter(1, this));
    table.getTableHeader().addMouseListener(new MyMouseAdapter(0, this));
    checkContent();
    collectVariables();

    JScrollPane scrollPane = new JScrollPane(table);
    panel.add(scrollPane);

    status.setText("statusline");
    status.setEditable(false);
    status.setMinimumSize(new Dimension(tableW, 30));
    status.setMaximumSize(new Dimension(tableW, 30));
    status.setPreferredSize(new Dimension(tableW, 30));
    panel.add(status);

    window.setContentPane(panel);

    window.pack();
    window.setLocation(rectTable.x, rectTable.y);
    window.setVisible(true);
    table.setSelection(0, 0);

    popUpMenus = new PopUpMenus(this);

    popUpWindow = new PopUpWindow();

    new Thread(() -> makeStatusLine()).start();
  }

  public void makeStatusLine() {
    boolean statusPause = false;
    String currentText = "";
    while (true) {
      int row = table.getSelectedRows()[0];
      int col = table.getSelectedColumns()[0];
      String statusText = "";
      try {
        String text = "";
        ScriptCell commandCell = getScript().commandTableCell(row);
        if (col == 0 && isLineFirstHidden(row)) {
          int hiddenCount = commandCell.getHidden();
          text += " hidden: " + hiddenCount;
          if (hiddenCount < 0) {
            text += " ... press - to reset hidden";
          }
        }
        if (commandCell.isFunction() && commandCell.getIndent() > 0) {
          statusText = " !!! ERROR: function definition not at indent 0";
        }
        if (col > 0) {
          ScriptCell dCell = getScript().getTableCell(row, col);
          if (SX.isNotNull(dCell)) {
            String cellText = dCell.get();
            if (cellText.startsWith("@")) {
              if (!cellText.startsWith("@@")) {
                text += " IMAGE: " + cellText;
                if (cellText.contains("?")) {
                  text += " - needs capture - use F1";
                } else {
                  text += " - F1 to show - F5 to find - SPACE to recapture";
                }
              }
            } else if (dCell.getInitial().startsWith("{")) {
              text += " press SPACE to edit script snippet";
            }
          } else {
            text += " empty - start typing to fill";
          }
        }
        String newText = String.format("(%d, %d)%s%s", commandCell.getRow() + 1, col, text, " " + statusText);
        if (!currentText.equals(newText)) {
          status.setText(newText);
          currentText = newText;
        }
      } catch (Exception ex) {
        log.trace("STATUS-ERROR: %s", ex.getMessage());
        statusPause = true;
      }
      SX.pause(0.5);
      if (statusPause) {
        statusPause = false;
        SX.pause(1);
      }
    }
  }
  //  @Override
//  public void valueChanged(ListSelectionEvent e) {
//    log.trace("valueChanged(ListSelectionEvent)");
//  }

  @Override
  public void tableChanged(TableModelEvent e) {
//    log.trace("tableChanged: ");
  }

  private class MyMouseAdapter extends MouseAdapter {

    private int type = 1;
    private Script script = null;

    private boolean inBody() {
      return type > 0;
    }

    private boolean inHeader() {
      return type < 1;
    }

    private MyMouseAdapter() {
    }

    public MyMouseAdapter(int type, Script script) {
      this.type = type;
      this.script = script;
    }

    public void mouseReleased(MouseEvent me) {
      lastPopInHeader = false;
      Point where = me.getPoint();
      int clickCount = me.getClickCount();
      int row = table.rowAtPoint(where);
      int col = table.columnAtPoint(where);
      int button = me.getButton();
      int[] selectedRows = table.getSelectedRows();
      if (inBody()) {
//        log.trace("clicked: R%d C%d B%d {%d ... %d}",
//                row, col, button, selectedRows[0], selectedRows[selectedRows.length - 1]);
      } else {
        log.trace("clicked: Header C%d B%d #%d", col, button, clickCount);
      }
      if (inHeader() && col == Script.numberCol && button == 1) {
        if (table.getSelectedRows().length > 1) {
          table.setSelection(0, 0);
        } else {
          table.setLineSelection(0, data.size() - 1);
        }
        return;
      }
      if (inBody() && button > 1) {
        if (selectedRows.length < 2) {
          table.setSelection(row, col);
        }
      }
      if (button == 3) {
        if (inBody()) {
          if (col == 0) {
            popUpMenus.action();
          } else if (col == 1) {
            popUpMenus.command();
          } else {
            //popUpMenus.notimplemented();
          }
        } else {
          if (col < 2) {
            popUpMenus.headerAction();
          }
        }
        return;
      }
      if (inBody() && col == 0 && button == 1 && clickCount > 1) {
        if (isLineFirstHidden(row)) {
          lineHide(new int[]{row});
        }
      }
    }
  }

//  public void tableChanged(TableModelEvent e) {
//      log.trace("tableChanged:");
//  }

  private int findNext(int row) {
    ScriptCell cell = commandTableCell(row);
    int indent = cell.getLoopIndent() - 1;
    int lastLine = data.size() - 1;
    String cellText = cell.get();
    String searchText = "endloop";
    if (cellText.contains("function")) {
      searchText = "endfunction";
    }
    while (!(++row > lastLine)) {
      if (commandTableCell(row).get().contains(searchText)) {
        if (cellText.contains("function")) {
          break;
        } else if (indent == commandTableCell(row).getLoopIndent()) {
          break;
        }
      }
    }
    return row;
  }

  private int findPrevious(int row) {
    ScriptCell cell = commandTableCell(row);
    int indent = cell.getIndent();
    String cellText = cell.get();
    String searchText = "if ifNot";
    boolean isIf = true;
    if (cellText.contains("loop")) {
      searchText = "loop";
      isIf = false;
    } else if (cellText.contains("function")) {
      searchText = "function";
      isIf = false;
    }
    while (!(--row < 0)) {
      if (isLineFirstCollapsed(row)) {
        continue;
      }
      if (isIf) {
        if (searchText.contains(commandTableCell(row).get()) && indent == commandTableCell(row).getIndent()) {
          break;
        }
      } else if (commandTableCell(row).get().contains(searchText) && indent == commandTableCell(row).getIndent()) {
        break;
      }
    }
    return row;
  }

  private int findNextForIf(int row, String command) {
    int indent = commandTableCell(row).getIfIndent();
    int endIfIndent = indent - 1;
    boolean endIfOnly = command.contains("endif");
    int lastLine = data.size() - 1;
    while (!(++row > lastLine)) {
      if (commandTableCell(row).get().contains("endif") && (commandTableCell(row).getIfIndent() == endIfIndent)) {
        break;
      }
      if (!endIfOnly) {
        if (commandTableCell(row).get().contains("else") || commandTableCell(row).get().contains("elif")) {
          if (indent == commandTableCell(row).getIfIndent()) {
            break;
          }
        }
      }
    }
    return row;
  }

  private int findIfForEndIf(int row) {
    int indent = commandTableCell(row).getIfIndent() + 1;
    while (!(--row < 0)) {
      if (commandTableCell(row).get().startsWith("if") && !commandTableCell(row).get().contains("ifElse")
              && commandTableCell(row).getIfIndent() == indent) {
        break;
      }
    }
    return row;
  }

  ScriptCell commandTableCell(int row) {
    return data.get(row).get(0);
  }

  boolean isLineFirstHidden(int row) {
    return commandTableCell(row).isFirstHidden();
  }

  protected boolean isLineFirstCollapsed(int row) {
    return allData.get(row).get(0).isFirstCollapsed();
  }

  protected boolean isLineFirstNotCollapsed(int row) {
    return commandTableCell(row).isFirstNotCollapsed();
  }

  boolean isLineEmpty(List<ScriptCell> line) {
    if (line.size() == 0) {
      return true;
    } else {
      for (ScriptCell cell : line) {
        if (SX.isSet(cell.get())) {
          return false;
        }
      }
      return true;
    }
  }

  boolean isLineEmpty(int tableRow) {
    return isLineEmpty(data.get(tableRow));
  }

  boolean isRowValid(int row) {
    return row > -1;
  }

  protected void assist() {
    int row = table.getSelectedRows()[0];
    int col = table.getSelectedColumns()[0];
    ScriptCell commandCell = commandTableCell(row);
    String command = commandCell.get();
    ScriptCell cell = getTableCell(row, col);
    String cellText = cell.get();
    int[] selectedRows = table.getSelectedRows();
    log.trace("F1: (%d,%d) %s (%d, %d, %d)",
            row, col, cellText,
            commandCell.getIndent(), commandCell.getIfIndent(), commandCell.getLoopIndent());
    if (col < 2) {
      if (!isLineFirstHidden(row)) {
        int firstRow = row;
        int lastRow = -1;
        if (command.startsWith("loop") || command.startsWith("function")) {
          lastRow = findNext(firstRow);
          if (col > 0) {
            firstRow = -1;
          }
        } else if (command.startsWith("endloop") || command.startsWith("endfunction")) {
          lastRow = findPrevious(firstRow);
          if (col > 0) {
            firstRow = -1;
          }
        } else {
          if (col == 0) {
            if (command.startsWith("if")) {
              lastRow = findNextForIf(firstRow, "endif");
            }
            if (command.startsWith("endif")) {
              lastRow = findPrevious(firstRow);
            }
          } else {
            if (command.startsWith("if") || command.startsWith("elif") || command.startsWith("else")) {
              lastRow = findNextForIf(firstRow, "else");
            }
            if (command.startsWith("endif")) {
              lastRow = findIfForEndIf(firstRow);
            }
            firstRow = -1;
          }
        }
        if (isRowValid(lastRow)) {
          if (isRowValid(firstRow)) {
            table.setLineSelection(firstRow, lastRow);
          } else {
            table.setSelection(lastRow, commandCol);
          }
        }
      } else {
        lineHide(selectedRows);
      }
      return;
    }
    if (cell.isImage()) {
      if (cellText.contains("?")) {
        cell.capture(row, col);
      } else {
        cell.show(row, col);
      }
      return;
    }
    if (cellText.startsWith("$")) {
      if (cellText.startsWith("$?")) {
        // set value/block
      } else {
        // go to def/display
      }
      return;
    }
  }

  protected void find(int row, int col) {

  }

  protected void logLine(List<ScriptCell> line, String command, int lineNumber) {
    ScriptCell commandCell = line.get(0);
    String hidden = "";
    if (commandCell.isFirstHidden()) {
      int hiddenCount = commandCell.getHidden();
      if (hiddenCount > 0) {
        hidden = "+" + hiddenCount;
      } else {
        hidden += hiddenCount;
      }
    }
    log.trace("%3d%3s %s", lineNumber, hidden, command);
  }

  protected void stringToScript(String theScript) {
    int lineNumber = 0;
    for (String line : theScript.split("\\n")) {
      List<ScriptCell> aLine = new ArrayList<>();
      int colCount = 1;
      String[] cells = line.split("\\t");
      String command = cells[0];
      for (String cellText : cells) {
        cellText = cellText.trim();
        String resultTarget = "$V";
        if (cellText.contains(resultTarget)) {
          int resultCount = -1;
          try {
            resultCount = Integer.parseInt(cellText
                    .replace(resultTarget, "").replace("$", "").trim());
          } catch (Exception ex) {
            cellText += "?";
          }
          if (resultCount >= resultsCounter) {
            resultsCounter = resultCount + 1;
          }
        } else if (cellText.startsWith("#")) {
          if (cellText.startsWith("# h ")) {
            try {
              aLine.get(0).setHidden(Integer.parseInt(cellText.split(" ")[2]));
            } catch (Exception ex) {
            }
          }
          cellText = null;
        }
        if (SX.isNotNull(cellText)) {
          aLine.add(new ScriptCell(this, cellText, colCount));
          if (++colCount > maxCol) {
            break;
          }
        }
      }
      if (aLine.size() > 0) {
        allData.add(aLine);
        logLine(aLine, command, lineNumber++);
      }
    }
    if (allData.size() > 0) {
      List<Integer> firstHideLines = new ArrayList<>();
      Integer row = 0;
      for (List<ScriptCell> line : allData) {
        addData(line, row);
        ScriptCell cell = line.get(0);
        if (cell.isFirstHidden() && cell.getHidden() > 0) {
          firstHideLines.add(0, row);
        }
        row++;
      }
      while (firstHideLines.size() > 0) {
        Integer hrow = firstHideLines.get(0);
        ScriptCell firstHiddenCell = data.get(hrow).get(0);
        int firstLine = hrow + 1;
        int lastLine = hrow + firstHiddenCell.getHidden();
        for (int n = firstLine; n < lastLine; n++) {
          data.remove(firstLine);
        }
        firstHideLines.remove(0);
      }
    }
  }

  private void addData(List<ScriptCell> line, int row) {
    data.add(line);
    line.get(0).setRow(row);
  }

  protected void loadScript() {
    allData.clear();
    data.clear();
    resultsCounter = 0;
    stringToScript(com.sikulix.core.Content.readFileToString(fScript));
    if (SX.isNotNull(table)) {
      checkContent();
      table.setSelection(0, 1);
    }
  }

  protected String scriptToString() {
    String theScript = "";
    for (List<ScriptCell> line : allData) {
      String sLine = "";
      String sTab = "";
      String indent = line.get(0).getIndent() > 0 ?
              String.join("", Collections.nCopies(line.get(0).getIndent(), " ")) : "";
      for (ScriptCell cell : line) {
        sLine += sTab + cell.get();
        sTab = "\t";
      }
      if (SX.isSet(sLine)) {
        if (line.get(0).isFirstHidden()) {
          sLine += sTab + "# h " + Math.abs(line.get(0).getHidden());
        }
        theScript += indent + sLine + "\n";
      }
    }
    return theScript;
  }

  protected void saveScript() {
    String theScript = scriptToString();
    if (SX.isSet(theScript)) {
      fScript.getParentFile().mkdirs();
      com.sikulix.core.Content.writeStringToFile(theScript, fScript);
    }
  }

  protected void runScript(int lineFrom) {
    runScript(lineFrom, lineFrom);
  }

  protected void runScript(int lineFrom, int lineTo) {
    window.setVisible(false);
    lineFrom = 0; //lines.get(lineFrom);
    lineTo = allData.size() - 1; //lines.get(lineTo);
    String snippet = ScriptTemplate.convertScript(this, allData, fScriptFolder, shouldTrace);
    if (shouldTrace) {
      Runner.run(Runner.ScriptType.JAVASCRIPT, snippet, Runner.ScriptOption.WITHTRACE);
    } else {
      Runner.run(Runner.ScriptType.JAVASCRIPT, snippet);
    }
    window.setVisible(true);
  }

  int resultsCounter = 0;

  List<String> variables = new ArrayList<>();

  List<String> images = new ArrayList<>();

  protected void collectVariables() {
    for (List<ScriptCell> line : data) {
      if (line.size() == 0) {
        continue;
      }
      for (ScriptCell cell : line) {
        String cellText = cell.get();
        if (cellText.startsWith("@")) {
          if (!cellText.startsWith("@?")) {
            String imageName = cellText.substring(1);
            if (!images.contains(imageName)) {
              images.add(imageName);
            }
          }
        } else if (cellText.startsWith("$")) {
          String imageName = cellText.substring(1);
          if (!variables.contains(imageName)) {
            variables.add(imageName);
          }
        }
      }
    }
  }

  protected void checkContent() {
    log.trace("checkContent started");
    int currentIndent = 0;
    int currentIfIndent = 0;
    int currentLoopIndent = 0;
    int currentFunctionIndent = 0;
    boolean hasElse = false;
    for (List<ScriptCell> line : allData) {
      if (line.size() == 0) {
        log.trace("checkContent: line.size() == 0");
      }
      ScriptCell commandCell = line.get(0);
      String command = commandCell.get().trim();
      if (SX.isNotSet(command)) {
        continue;
      }
      commandCell.reset();
      if (SX.isNotNull(ScriptTemplate.commandTemplates.get(command))) {
        if (command.startsWith("if") && !command.contains("ifElse")) {
          currentIfIndent++;
          commandCell.setIndent(currentIndent, currentIfIndent, -1);
          currentIndent++;
        } else if (command.startsWith("elif")) {
          if (currentIfIndent > 0 && !hasElse) {
            currentIndent--;
            commandCell.setIndent(currentIndent, currentIfIndent, -1);
            currentIndent++;
          } else {
            commandCell.setError();
          }
        } else if (command.startsWith("else")) {
          if (currentIfIndent > 0) {
            hasElse = true;
            currentIndent--;
            commandCell.setIndent(currentIndent, currentIfIndent, -1);
            currentIndent++;
          } else {
            commandCell.setError();
          }
        } else if (command.startsWith("endif")) {
          if (currentIfIndent > 0) {
            hasElse = false;
            currentIndent--;
            currentIfIndent--;
            commandCell.setIndent(currentIndent, currentIfIndent, -1);
          } else {
            commandCell.setError();
          }
        } else if (command.startsWith("loop")) {
          currentLoopIndent++;
          commandCell.setIndent(currentIndent, -1, currentLoopIndent);
          currentIndent++;
        } else if (command.startsWith("endloop")) {
          if (currentLoopIndent > 0) {
            currentIndent--;
            currentLoopIndent--;
            commandCell.setIndent(currentIndent, -1, currentLoopIndent);
          } else {
            commandCell.setError();
          }
        } else if (command.startsWith("function")) {
          if (currentIndent > 0) {
            log.trace("checkContent: not at indent 0: %s", commandCell);
          }
          currentFunctionIndent++;
          commandCell.setIndent(currentIndent, -1, -1);
          currentIndent++;
        } else if (command.startsWith("endfunction")) {
          if (currentFunctionIndent > 0) {
            currentIndent--;
            currentFunctionIndent--;
            commandCell.setIndent(currentIndent, -1, -1);
          } else {
            commandCell.setError();
          }
        } else {
          commandCell.setIndent(currentIndent, currentIfIndent, currentLoopIndent);
        }
      } else if (commandCell.get().startsWith("@")) {

      } else if (commandCell.get().startsWith("$")) {

      } else if (commandCell.get().contains("#")) {

      } else {
        log.trace("checkContent: invalid: %s", commandCell);
      }
    }
    table.tableHasChanged();
    String sLines = "";
    for (List<ScriptCell> line : data) {
      sLines += line.get(0).getRow() + ",";
    }
    log.trace("checkContent finished (%s)", sLines);
  }

  protected boolean addCommandTemplate(String command) {
    boolean success = false;
    int[] selectedRows = table.getSelectedRows();
    if (SX.isNull(selectedRows)) {
      return success;
    }
    int firstRow = selectedRows[0];
    int lastRow = selectedRows[selectedRows.length - 1];
    int[] selectedCols = table.getSelectedColumns();
    if (SX.isNull(selectedCols)) {
      return success;
    }
    command = command.trim();
    if (command.length() == 2 && command.startsWith("$") || command.length() == 3 && command.startsWith("$$")) {
      command = command.substring(0, command.length() - 1) + command.substring(command.length() - 1).toUpperCase();
    }
    String[] commandLine = ScriptTemplate.commandTemplates.get(command);
    if (SX.isNotNull(commandLine)) {
      if (commandLine.length == 1) {
        command = commandLine[0];
        commandLine = ScriptTemplate.commandTemplates.get(command);
      }
      commandLine = commandLine.clone();
      if (commandLine[0].startsWith("=")) {
        commandLine[0] = commandLine[0].substring(1);
      } else {
        commandLine[0] = command + commandLine[0];
      }
    }
    if (selectedCols[0] == numberCol) {
      log.trace("addCommandTemplate: should surround with: %s", command);
        lineAdd(firstRow, 1);
        lineSet(firstRow, commandLine);
        lastRow += 2;
        lineAdd(lastRow, 1);
        lineSet(lastRow, command.startsWith("if") ? "endif" :
                (command.startsWith("loop") ? "endloop" : "endfunction"));
        success = true;
    } else if (isLineEmpty(firstRow)) {
      if (SX.isNotNull(commandLine)) {
        int lineLast = commandLine.length - 1;
        String lineEnd = commandLine[lineLast];
        if (lineEnd.startsWith("result")) {
          commandLine[lineLast] = (lineEnd.contains("-list") ? "$$V" : "$V") + resultsCounter++;
        }
        lineSet(firstRow, commandLine);
        if (command.startsWith("if") && !command.contains("ifElse")) {
          lineAdd(firstRow + 1, 1);
          lineSet(firstRow + 1, "endif");
        } else if (command.startsWith("loop")) {
          lineAdd(firstRow + 1, 1);
          lineSet(firstRow + 1, "endloop");
        } else if (command.startsWith("function")) {
          lineAdd(firstRow + 1, 1);
          lineSet(firstRow + 1, "endfunction");
        } else {
        }
      } else {
        lineSet(firstRow, command + "?");
      }
      success = true;
    }
    if (success) {
      checkContent();
      table.setLineSelection(firstRow, lastRow);
    }
    return success;
  }

  protected ScriptCell getTableCell(int row, int col) {
    List<ScriptCell> line = data.get(row);
    col = Math.max(0, col - 1);
    if (col > line.size() - 1) {
      return null;
    }
    return line.get(col);
  }

  protected void changeRow(int dataRow, int change) {
    int newRow = data.get(dataRow).get(0).getRow() + change;
    data.get(dataRow).get(0).setRow(newRow);
  }

  protected void lineAdd(int firstTableLine, int lineCount) {
    int firstDataLine;
    boolean shouldAppend = false;
    if (firstTableLine == 0) {
      firstTableLine = firstDataLine = 0;
    } else if (firstTableLine > data.size() - 1) {
      shouldAppend = true;
      firstDataLine = allData.size();
    } else {
      firstDataLine = data.get(firstTableLine).get(0).getRow();
    }
    for (int currentLine = 0; currentLine < lineCount; currentLine++) {
      List<ScriptCell> line = new ArrayList<>();
      if (shouldAppend) {
        data.add(line);
        allData.add(line);
      } else {
        data.add(firstTableLine + currentLine, line);
        allData.add(firstDataLine + currentLine, line);
      }
      line.add(new ScriptCell(this, "", commandCol, firstDataLine + currentLine));
    }
    for (int n = firstTableLine + lineCount; n < data.size(); n++) {
      changeRow(n, lineCount);
    }
  }

  protected void lineSet(int row, String... items) {
    int col = 0;
    for (String item : items) {
      cellSet(row, col++, item);
    }
  }

  private List<Integer> getDataLines(int[] selectedRows) {
    List<Integer> rows = new ArrayList<>();
    rows.add(data.get(selectedRows[0]).get(0).getRow());
    int lastLine = selectedRows[selectedRows.length - 1] + 1;
    if (lastLine > data.size() - 1) {
      lastLine = data.get(data.size() - 1).get(0).getRow();
      if (isLineFirstCollapsed(lastLine)) {
        lastLine = allData.size();
      }
    } else {
      lastLine = data.get(lastLine).get(0).getRow() - 1;
    }
    rows.add(lastLine);
    return rows;
  }

  private List<Integer> saveLines(List<Integer> lines) {
    savedLines.clear();
    int dataLine = lines.get(0);
    int removeLine = dataLine;
    savedLines.add(allData.remove(dataLine));
    while (dataLine < lines.get(1)) {
      savedLines.add(allData.remove(removeLine));
      dataLine++;
    }
    return lines;
  }

  boolean isHeader() {
    return false; //TODO isHeader
  }

  protected void lineNew() {
    lineNew(table.getSelectedRows());
  }

  protected void lineNew(int[] selectedRows) {
    int numLines = selectedRows.length;
    int firstNewLine = selectedRows[numLines - 1] + 1;
    if (lastPopInHeader) {
      firstNewLine = 0;
    }
    lineAdd(firstNewLine, numLines);
    table.tableCheckContent();
    select(firstNewLine, Script.commandCol);
  }

  private List<Integer> copyLines(List<Integer> lines) {
    savedLines.clear();
    int currentLine = lines.get(0);
    savedLines.add(copyLine(allData.get(currentLine)));
    while (currentLine < lines.get(1)) {
      savedLines.add(copyLine(allData.get(++currentLine)));
    }
    return lines;
  }

  private List<ScriptCell> copyLine(List<ScriptCell> line) {
    List<ScriptCell> lineCopy = new ArrayList<>();
    for (ScriptCell cell : line) {
      lineCopy.add(cell.copy());
    }
    return lineCopy;
  }

  protected void lineCopy() {
    lineCopy(table.getSelectedRows());
  }

  protected void lineCopy(int[] selectedRows) {
    copyLines(getDataLines(selectedRows));
  }

  private String lineStatus = "";

  private void createLineStatus() {
    lineStatus = "";
    int tableLine = 1;
    for (List<ScriptCell> line : data) {
      String command = line.get(0).get();
      int dataLine = line.get(0).getRow();
      int hiddenCount = line.get(0).getHidden();
      String sLine = String.format("%4d -> %4d | %10s | H%d\n", tableLine, dataLine, command, hiddenCount);
      lineStatus += sLine;
      tableLine++;
    }
  }

  protected void lineDelete() {
    lineDelete(table.getSelectedRows());
  }

  protected void lineDelete(int[] selectedRows) {
    List<Integer> rows = saveLines(getDataLines(selectedRows));
    for (int delRow : selectedRows) {
      List<ScriptCell> line = data.remove(selectedRows[0]);
    }
    int lineCount = rows.get(1) - rows.get(0) + 1;
    int row = selectedRows[0];
    while (row < data.size()) {
      changeRow(row++, -lineCount);
    }
    table.tableCheckContent();
    select(selectedRows[0] - 1, Script.numberCol);
  }

  protected void lineResetHidden() {
    lineResetHidden(table.getSelectedRows()[0]);
  }

  protected void lineResetHidden(int selectedRow) {
    commandTableCell(selectedRow).setHidden(0);
    table.tableCheckContent();
    select(selectedRow, Script.numberCol);
  }

  protected void lineEmpty() {
    lineEmpty(table.getSelectedRows());
  }

  protected void lineEmpty(int[] selectedRows) {
    int lineCount = allData.size();
    lineDelete(selectedRows);
    lineCount -= allData.size();
    int[] newRow = new int[]{selectedRows[0] - 1};
    for (int n = 0; n < lineCount; n++) {
      lineNew(newRow);
    }
    table.tableCheckContent();
    select(selectedRows[0], Script.commandCol);
  }

  protected void lineReset() {
    lineReset(table.getSelectedRows());
  }

  protected void lineReset(int[] selectedRows) {
    int currentRow = selectedRows[0];
    String command = data.get(currentRow).get(0).get();
    saveLines(getDataLines(selectedRows));
    //TODO lineNew(new int[]{currentRow - 1}, command);
    table.tableCheckContent();
    select(currentRow, Script.commandCol);
  }

  protected void lineRun() {
    lineRun(table.getSelectedRows());
  }

  protected void lineRun(int[] selectedRows) {
    runScript(selectedRows[0], selectedRows[selectedRows.length - 1]);
  }

  protected void lineInsert() {
    lineInsert(table.getSelectedRows());
  }

  protected void lineInsert(int[] selectedRows) {
    int numLines = savedLines.size();
    int firstNewLine = selectedRows[selectedRows.length - 1] + 1;
    if (lastPopInHeader) {
      firstNewLine = 0;
    }
    lineAdd(firstNewLine, numLines);
    int currentLine = data.get(firstNewLine).get(0).getRow();
    for (int n = 0; n < numLines; n++) {
      List<ScriptCell> line = allData.get(currentLine + n);
      List<ScriptCell> newLine = savedLines.get(n);
      line.get(0).set(newLine.get(0).get());
      for (int nc = 1; nc < newLine.size(); nc++) {
        cellSet(line, nc, newLine.get(nc).get());
      }
    }
    table.tableCheckContent();
    table.setSelection(firstNewLine, Script.numberCol);
  }

  protected void select(int row, int col) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        table.setSelection(Math.max(0, row), col);
      }
    }).start();
  }

  protected void cellSet(int row, int col, String item) {
    List<ScriptCell> line = data.get(row);
    cellSet(line, col, item);
  }

  protected void cellSet(List<ScriptCell> line, int col, String item) {
    if (col > line.size() - 1) {
      for (int n = line.size(); n <= col; n++) {
        line.add(new ScriptCell(this, "", n + 1));
      }
    }
    line.get(col).set(item);
    if (col > 0 && SX.isSet(item)) {
      line.get(col).setInitial(item);
    }
  }

  protected void lineHide() {
    lineHide(table.getSelectedRows());
  }

  protected void lineHide(int[] selectedRows) {
    int currentTableRow = selectedRows[0];
    ScriptCell firstCell = data.get(currentTableRow).get(commandCol - 1);
    int shouldSelectFirst = currentTableRow;
    int shouldSelectLast = currentTableRow;
    boolean shouldHide = false;
    int selectedCount = selectedRows.length;
    if (selectedCount == 1) {
      if (firstCell.isFirstHidden()) {
        int hCount = firstCell.getHidden();
        firstCell.setHidden(-hCount);
        if (hCount > 0) {
          hCount -= 1;
          int currentDataLine = data.get(currentTableRow).get(0).getRow() + 1;
          shouldSelectLast = shouldSelectFirst + hCount;
          while (hCount > 0) {
            List<ScriptCell> line = allData.get(currentDataLine);
            currentTableRow++;
            line.get(0).setRow(currentDataLine);
            data.add(currentTableRow, line);
            hCount--;
            currentDataLine++;
            int hiddenCount = line.get(0).getHidden();
            if (line.get(0).isFirstHidden()) {
              if (hiddenCount > 0) {
                currentDataLine += countInnerHidden(hiddenCount, currentDataLine);
              } else {
                hCount += (-hiddenCount) - 1;
                shouldSelectLast += (-hiddenCount) - 1;
              }
            }
          }
        } else {
          shouldHide = true;
          selectedCount = -hCount;
        }
      }
    } else {
      shouldHide = true;
    }
    if (shouldHide) {
      currentTableRow++;
      int hiddenLinesCount = 1;
      while (selectedCount > 1) {
        List<ScriptCell> line = data.remove(currentTableRow);
        hiddenLinesCount++;
        if (line.get(0).isFirstHidden()) {
          if (line.get(0).getHidden() < 0) {
            selectedCount += (-line.get(0).getHidden()) - 1;
            hiddenLinesCount -= (-line.get(0).getHidden()) - 1;
          }
        }
        selectedCount--;
      }
      firstCell.setHidden(hiddenLinesCount);
    }
    table.tableCheckContent();
    table.setLineSelection(shouldSelectFirst, shouldSelectLast);
  }

  private int countInnerHidden(int hiddenCount, int currentDataLine) {
    hiddenCount--;
    int hiddenLinesCount = 0;
    while (hiddenCount > 0) {
      int count = getLineHiddenCount(currentDataLine);
      currentDataLine += count;
      hiddenLinesCount += count;
      hiddenCount--;
    }
    return hiddenLinesCount;
  }

  private int getLineHiddenCount(int lineNumber) {
    ScriptCell cell = allData.get(lineNumber).get(0);
    int hiddenCount = cell.getHidden();
    if (cell.isFirstHidden() && hiddenCount > 0) {
      return countInnerHidden(hiddenCount, lineNumber + 1) + 1;
    } else {
      return 1;
    }
  }

  protected void lineUnhideAll() {
    data.clear();
    int lineNumber = 0;
    for (List<ScriptCell> line : allData) {
      int hiddenCount = line.get(0).getHidden();
      if (hiddenCount > 0) {
        line.get(0).setHidden(-hiddenCount);
      }
      data.add(line);
    }
    checkContent();
    table.tableHasChanged();
    table.setSelection(0, 0);
  }

  protected void lineHideAll() {
    String theScript = scriptToString();
    data.clear();
    allData.clear();
    stringToScript(theScript);
    checkContent();
    table.tableHasChanged();
    table.setSelection(0, 0);
  }

  protected void editBox(int row, int col) {
    ScriptCell cell = data.get(row).get(col);
    String initialText = cell.getInitial();
    String[] text = new String[]{cell.get(), initialText};
    boolean shouldEdit = false;
    if (text[0].startsWith("{")) {
      String token = text[0].substring(1, text[0].length() - 1);
      text[0] = "//--- enter a valid JavaScript " + token + "\n";
      text[0] += "//--- CTRL-ESC to save - ESC to cancel\n";
      shouldEdit = true;
    } else if (!text[0].startsWith("$") && !text[0].startsWith("@") && initialText.startsWith("{")) {
      shouldEdit = true;
    }
    if (shouldEdit) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          popUpWindow.showCell(getScript(), row, col, text);
        }
      });
    }
  }
}


