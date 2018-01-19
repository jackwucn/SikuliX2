package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  ScriptTable table = null;
  public Rectangle rectTable = null;
  int maxCol = 6;

  protected ScriptTable getTable() {
    return table;
  }

  private File fScript = new File(SX.getSXSTORE(), "scripteditor/script.txt");

  protected File getScriptPath() {
    return fScript;
  }

  List<Integer> lines = new ArrayList<>();

  List<List<ScriptCell>> data = new ArrayList<>();
  List<List<ScriptCell>> savedLines = new ArrayList<>();

  protected List<List<ScriptCell>> getData() {
    return data;
  }

  boolean shouldTrace = false;

  public Script(String[] args) {

    if (args.length > 0 && "trace".equals(args[0])) {
      log.on(SXLog.TRACE);
      shouldTrace = true;
    }

    window = new JFrame("SikuliX - ScriptEditor");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JPanel panel = new JPanel(new GridLayout(1, 0));
    panel.setOpaque(true);

    initTemplates();
    loadScript();

    table = new ScriptTable(this, new ScriptTableModel(this, maxCol, data));

    table.setAutoCreateColumnsFromModel(false);

    FontMetrics metrics = table.getFontMetrics(customFont);
    table.setRowHeight(metrics.getHeight() + 4); // set row height to match font

    table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.setCellSelectionEnabled(true);

    Rectangle monitor = SX.getSXLOCALDEVICE().getMonitor();
    Dimension tableDim = new Dimension((int) (monitor.width * 0.8), (int) (monitor.height * 0.8));
    rectTable = new Rectangle();
    rectTable.setSize(tableDim);
    rectTable.setLocation(tableDim.width / 8, tableDim.height / 9);

    table.setTableHeader(new JTableHeader(table.getColumnModel()) {
      @Override
      public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = 30;
        return d;
      }
    });

    table.getTableHeader().setBackground(new Color(230, 230, 230));

    int tableW = tableDim.width;
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

    window.setContentPane(panel);

    window.pack();
    window.setLocation(rectTable.x, rectTable.y);
    window.setVisible(true);
    table.setSelection(0, 0);

    popUpMenus = new PopUpMenus(this);
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
      Point where = me.getPoint();
      int row = table.rowAtPoint(where);
      int col = table.columnAtPoint(where);
      TableCell cell = new TableCell(script, row, col);
      if (inHeader()) {
        cell.setHeader();
      }
      int button = me.getButton();
      int[] selectedRows = table.getSelectedRows();
      if (inBody()) {
        log.trace("clicked: R%d C%d B%d {%d ... %d}",
                row, col, button, selectedRows[0], selectedRows[selectedRows.length - 1]);
      } else {
        log.trace("clicked: Header C%d B%d", col, button);
      }
      if (inHeader() && col == Script.numberCol && button == 1) {
        if (table.getSelectedRows().length > 1) {
          script.table.setSelection(0, 0);
        } else {
          script.table.setLineSelection(0, script.getLastValidLine());
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
            popUpMenus.action(cell);
          } else if (col == 1) {
            popUpMenus.command(cell);
          } else {
            popUpMenus.notimplemented(cell);
          }
        } else {
          popUpMenus.action(cell);
        }
        return;
      }
    }

  }

//  public void tableChanged(TableModelEvent e) {
//      log.trace("tableChanged:");
//  }

  String savedCellText = "";

  protected ScriptCell evalDataCell(TableCell tCell) {
    return evalDataCell(tCell.row, tCell.col);
  }

  protected ScriptCell evalCommandCell(TableCell tCell) {
    return evalDataCell(tCell.row, 1);
  }

  protected ScriptCell evalDataCell(int tableRow, int tableCol) {
    int dataCol = tableCol == 0 ? 0 : tableCol - 1;
    if (tableRow < 0) {
      tableRow = 0;
    }
    int dataRow = lines.get(tableRow);
    if (dataRow < 0) {
      return null;
    }
    if (tableCol > data.get(dataRow).size()) {
      return null;
    }
    return dataCell(dataRow, dataCol);
  }

  protected ScriptCell dataCell(int dataRow, int dataCol) {
    List<ScriptCell> line = data.get(dataRow);
//    if (dataCol > line.size() -1) {
//      for (int n = line.size(); n <= dataCol; n++) {
//        line.add(new ScriptCell(this, "", dataRow, n + 1));
//      }
//    }
    return line.get(dataCol);
  }

  protected void dataCellSet(int dataRow, int tableCol, String item) {
    int dataCol = tableCol == 0 ? 0 : tableCol - 1;
    List<ScriptCell> line = data.get(dataRow);
    if (dataCol > line.size() - 1) {
      for (int n = line.size(); n <= dataCol; n++) {
        line.add(new ScriptCell(this, "", n + 1));
      }
    }
    line.get(dataCol).set(item);
  }

  protected void setValueAt(String text, TableCell cell) {
    table.getModel().setValueAt(text, cell.row, cell.col);
    table.setSelection(cell.row, cell.col);
  }

  private TableCell findNextForLoop(TableCell tCell) {
    ScriptCell cell = evalDataCell(tCell);
    int indent = cell.getLoopIndent() - 1;
    int lastLine = getLastValidLine();
    while (true) {
      tCell = tCell.nextRow();
      if (tCell.row > lastLine) {
        tCell = new TableCell(this, lastLine);
        break;
      }
      cell = evalDataCell(tCell);
      String cellText = cell.get();
      if (cellText.contains("endloop")) {
        if (indent == cell.getLoopIndent()) {
          break;
        }
      }
    }
    return tCell;
  }

  private TableCell findNextForIf(TableCell tCell, String command) {
    ScriptCell cell = evalDataCell(tCell);
    int indent = cell.getIfIndent();
    int endIfIndent = indent - 1;
    boolean endIfOnly = command.contains("endif");
    int lastLine = getLastValidLine();
    while (true) {
      tCell = tCell.nextRow();
      if (tCell.row > lastLine) {
        tCell = new TableCell(this, lastLine);
        break;
      }
      cell = evalDataCell(tCell);
      String cellText = cell.get();
      if (cellText.contains("endif") && (cell.getIfIndent() == endIfIndent)) {
        break;
      }
      if (!endIfOnly) {
        if (cellText.contains("else") || cellText.contains("elif")) {
          if (indent == cell.getIfIndent()) {
            break;
          }
        }
      }
    }
    return tCell;
  }

  private TableCell findIfForEndIf(TableCell tCell) {
    ScriptCell cell = evalDataCell(tCell);
    int indent = cell.getIfIndent() + 1;
    while (true) {
      tCell = tCell.previousRow();
      if (tCell.row < 0) {
        tCell = new TableCell(this, 0);
        break;
      }
      cell = evalDataCell(tCell);
      String cellText = cell.get();
      if (cellText.startsWith("if") && !cellText.contains("ifElse") && cell.getIfIndent() == indent) {
        break;
      }
    }
    return tCell;
  }

  protected void assist(TableCell tCell) {
    ScriptCell cell;
    String cellText;
    int[] selectedRows = table.getSelectedRows();
    if (selectedRows.length > 1) {
      tCell = new TableCell(this, selectedRows[0], numberCol);
    }
    if (!tCell.isItemCol()) {
      cell = evalCommandCell(tCell);
      if (!cell.isFirstHidden()) {
        TableCell firstCell = new TableCell(this, tCell.row, commandCol);
        TableCell lastCell = null;
        if (cell.get().startsWith("loop")) {
          lastCell = findNextForLoop(firstCell);
        } else {
          if (tCell.isLineNumber()) {
            if (cell.get().startsWith("if")) {
              lastCell = findNextForIf(firstCell, "endif");
            }
          } else {
            if (cell.get().startsWith("if") || cell.get().startsWith("elif") || cell.get().startsWith("else")) {
              lastCell = findNextForIf(firstCell, "else");
            }
            if (cell.get().startsWith("endif")) {
              lastCell = findIfForEndIf(firstCell);
            }
            firstCell = null;
          }
        }
        if (SX.isNotNull(lastCell)) {
          if (SX.isNull(firstCell)) {
            table.setSelection(lastCell.row, 1);
          } else {
            table.setLineSelection(firstCell.row, lastCell.row);
          }
        }
        return;
      }
    }
    cell = evalDataCell(tCell);
    cellText = cell.get();
    Script.log.trace("F1: (%d,%d) %s (%d, %d, %d)",
            tCell.row, tCell.col, cellText,
            cell.getIndent(), cell.getIfIndent(), cell.getLoopIndent());
    if (cellText.startsWith("@")) {
      if (cellText.startsWith("@?")) {
        cell.capture(tCell);
      } else {
        cell.show(tCell);
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

  protected void loadScript() {
    data.clear();
    resultsCounter = 0;
    int hiddenCount = 0;
    String theScript = com.sikulix.core.Content.readFileToString(fScript);
    for (String line : theScript.split("\\n")) {
      List<ScriptCell> aLine = new ArrayList<>();
      int colCount = 1;
      for (String cellText : line.split("\\t")) {
        String resultTarget = "$R";
        if (cellText.contains(resultTarget)) {
          int resultCount = -1;
          try {
            resultCount = Integer.parseInt(cellText.replace(resultTarget, "").trim());
          } catch (Exception ex) {
            cellText += "?";
          }
          if (resultCount >= resultsCounter) {
            resultsCounter = resultCount + 1;
          }
        } else if (cellText.startsWith("#")) {
          if (cellText.startsWith("# h ")) {
            try {
              hiddenCount = Integer.parseInt(cellText.split(" ")[2]);
              if (hiddenCount > 1) {
                aLine.get(0).setHidden(hiddenCount--);
                hiddenCount = -hiddenCount;
              }
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
        if (hiddenCount != 0) {
          if (hiddenCount > 0) {
            aLine.get(0).setHidden(-1);
            hiddenCount--;
          } else {
            hiddenCount = -hiddenCount;
          }
        }
        data.add(aLine);
      }
    }
    if (SX.isNotNull(table)) {
      checkContent();
      table.setSelection(0, 1);
    }
  }

  protected void saveScript() {
    String theScript = "";
    for (List<ScriptCell> line : data) {
      String sLine = "";
      String sTab = "";
      for (ScriptCell cell : line) {
        sLine += sTab + cell.get();
        sTab = "\t";
      }
      if (SX.isSet(sLine)) {
        if (line.get(0).isFirstHidden()) {
          sLine += sTab + "# h " + line.get(0).getHidden();
        }
        theScript += sLine + "\n";
      }
    }
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
    if (lineFrom < 0) {
      lineTo = data.size() - 1;
    }
    for (int n = lineFrom; n <= lineTo; n++) {
      String sLine = "";
      String sep = "";
      for (ScriptCell cell : data.get(n)) {
        sLine += sep + cell.get().trim();
        sep = " | ";
      }
      log.trace("runscript: (%4d) %s", n, sLine);
    }
    //SX.pause(2);
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

  protected boolean addCommandTemplate(String command, TableCell tCell, int[] selectedRows) {
    command = command.trim();
    String[] commandLine = commandTemplates.get(command);
    commandLine = commandLine.clone();
    commandLine[0] = command + commandLine[0];
    boolean success = false;
    TableCell tCellFirst = null;
    TableCell tCellLast = null;
    if (SX.isNotNull(selectedRows) && tCell.col == numberCol) {
      log.trace("addCommandTemplate: should surround with: %s", command);
      tCellFirst = new TableCell(this, selectedRows[0]);
      boolean addedBefore = tCellFirst.previousRow().lineAdd(commandLine);
      if (addedBefore) {
        tCellFirst.row += 1;
      }
      resetLines();
      tCellLast = new TableCell(this, selectedRows[selectedRows.length - 1] + 1);
      if (tCellLast.row > getLastValidLine()) {
        tCellLast.row = getLastValidLine();
      }
      tCellLast.nextRow().lineAdd(command.startsWith("if") ? "endif" : "endloop");
      success = true;
    } else if (tCell.isLineEmpty()) {
      if (SX.isNotNull(commandLine)) {
        int lineLast = commandLine.length - 1;
        String lineEnd = commandLine[lineLast];
        if ("result".equals(lineEnd)) {
          commandLine[lineLast] = "$R" + resultsCounter++;
        }
        tCell.lineSet(commandLine);
        if (command.startsWith("if") && !command.contains("ifElse")) {
          tCell.lineAdd("endif");
        } else if (command.startsWith("loop")) {
          tCell.lineAdd("endloop");
        } else {
        }
        table.setSelection(tCell.row, 2);
      } else {
        tCell.lineSet(new String[]{command + "?"});
        table.setSelection(tCell.row, 1);
      }
      success = true;
    }
    if (success) {
      checkContent();
      if (SX.allNotNull(tCellFirst, tCellLast)) {
        table.setLineSelection(tCellFirst.row, tCellLast.row);
      }
    }
    return success;
  }

  protected void resetLines() {
    int nextDataLine = 0;
    int ixLines = 0;
    lines.clear();
    for (int ix = 0; ix < data.size(); ix++) {
      lines.add(-1);
    }
    for (List<ScriptCell> line : data) {
      ScriptCell cell = line.get(0);
      if (!cell.isHiddenBody()) {
        lines.set(ixLines++, nextDataLine);
      }
      if (cell.isFirstHidden()) {
        nextDataLine += cell.getHidden();
      } else if (!cell.isHiddenBody()) {
        nextDataLine++;
      }
    }
  }

  protected int getLastValidLine() {
    int validLine = -1;
    for (int line : lines) {
      if (line < 0) {
        break;
      }
      validLine++;
    }
    return validLine;
  }

  protected void checkContent() {
    log.trace("checkContent started");
    int currentIndent = 0;
    int currentIfIndent = 0;
    int currentLoopIndent = 0;
    boolean hasElse = false;
    resetLines();
    for (List<ScriptCell> line : data) {
      ScriptCell cell = line.get(0);
      if (line.size() == 0) {
        continue;
      }
      cell.reset();
      String command = cell.get().trim();
      if (SX.isNotNull(commandTemplates.get(command))) {
        if (command.startsWith("if") && !command.contains("ifElse")) {
          currentIfIndent++;
          cell.setIndent(currentIndent, currentIfIndent, -1);
          currentIndent++;
        } else if (command.startsWith("elif")) {
          if (currentIfIndent > 0 && !hasElse) {
            currentIndent--;
            cell.setIndent(currentIndent, currentIfIndent, -1);
            currentIndent++;
          } else {
            cell.setError();
          }
        } else if (command.startsWith("else")) {
          if (currentIfIndent > 0) {
            hasElse = true;
            currentIndent--;
            cell.setIndent(currentIndent, currentIfIndent, -1);
            currentIndent++;
          } else {
            cell.setError();
          }
        } else if (command.startsWith("endif")) {
          if (currentIfIndent > 0) {
            hasElse = false;
            currentIndent--;
            currentIfIndent--;
            cell.setIndent(currentIndent, currentIfIndent, -1);
          } else {
            cell.setError();
          }
        } else if (command.startsWith("loop")) {
          currentLoopIndent++;
          cell.setIndent(currentIndent, -1, currentLoopIndent);
          currentIndent++;
        } else if (command.startsWith("endloop")) {
          if (currentLoopIndent > 0) {
            currentIndent--;
            currentLoopIndent--;
            cell.setIndent(currentIndent, -1, currentLoopIndent);
          } else {
            cell.setError();
          }
        } else {
          cell.setIndent(currentIndent, currentIfIndent, currentLoopIndent);
        }
      }
      if (cell.get().startsWith("@")) {

      }
      if (cell.get().startsWith("$")) {

      }
      //log.trace("checkContent: %s", cell);
    }
    table.tableHasChanged();
    String sLines = "";
    for (int ix : lines) {
      sLines += ix + ",";
    }
    log.trace("checkContent finished (%s)", sLines);
  }

  Map<String, String[]> commandTemplates = new HashMap<>();

  private void initTemplates() {
    commandTemplates.put("find", new String[]{"", "@?", "result"});
    commandTemplates.put("wait", new String[]{"", "wait-time", "@?", "result"});
    commandTemplates.put("vanish", new String[]{"", "wait-time", "@?", "result"});
    commandTemplates.put("findAll", new String[]{"", "@?", "result"});
    commandTemplates.put("findBest", new String[]{"", "$?listVariable", "result"});
    commandTemplates.put("findAny", new String[]{"", "$?listVariable", "result"});
    commandTemplates.put("click", new String[]{"", "@?", "result"});
    commandTemplates.put("clickRight", new String[]{"", "@?", "result"});
    commandTemplates.put("clickDouble", new String[]{"", "@?", "result"});
    commandTemplates.put("hover", new String[]{"", "@?", "result"});

    commandTemplates.put("if", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("ifNot", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("endif", new String[]{""});
    commandTemplates.put("else", new String[]{"", "{block}"});
    commandTemplates.put("elif", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("elifNot", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("ifElse", new String[]{"", "{condition}", "{block}", "{block}", "result"});

    commandTemplates.put("loop", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("loopWith", new String[]{"", "$?listVariable", "{block}"});
    commandTemplates.put("loopFor", new String[]{"", "$?count $?step $?from", "{block}"});
    commandTemplates.put("endloop", new String[]{""});

    commandTemplates.put("print", new String[]{"", "variable..."});
    commandTemplates.put("printf", new String[]{"", "template", "variable..."});
    commandTemplates.put("log", new String[]{"", "template", "variable..."});
    commandTemplates.put("pop", new String[]{"", "message", "result"});

    commandTemplates.put("image", new String[]{"", "@?", "$?similar", "$?offset"});
    commandTemplates.put("variable", new String[]{"", "$V?", "{block}"});
    commandTemplates.put("listVariable", new String[]{"", "$L?", "$?item..."});
    commandTemplates.put("function", new String[]{"", "$F?", "{block}", "$?parameter..."});
    commandTemplates.put("/", new String[]{"continuation"});
    commandTemplates.put("#", new String[]{"comment"});
  }

  protected void editBox(ScriptCell cell) {
    Script.log.trace("EditBox: should open");
  }
}


