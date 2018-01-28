package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;
import com.sikulix.run.Runner;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

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
  String statusText = "";
  boolean statusPause = false;

  private File fScriptFolder = new File(SX.getSXSTORE(), "scripteditor");
  private File fScript = new File(fScriptFolder, "script.txt");

  protected File getScriptPath() {
    return fScript;
  }

  List<Integer> lines = new ArrayList<>();
  List<List<ScriptCell>> allData = new ArrayList<>();

  List<List<ScriptCell>> data = new ArrayList<>();
  List<List<ScriptCell>> savedData = new ArrayList<>();
  List<List<ScriptCell>> savedLines = new ArrayList<>();
  int[] savedHiddenCount = new int[0];

  protected boolean isUnhidden() {
    return savedData.size() > 0;
  }

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

    new Thread(new Runnable() {
      int selectedRow = -1;
      int selectedCol = -1;

      @Override
      public void run() {
        while (true) {
          int row = table.getSelectedRow();
          int col = table.getSelectedColumn();
          if (row != selectedRow || col != selectedCol || !statusText.isEmpty()) {
            selectedRow = row;
            selectedCol = col;
            String text = "";
            TableCell tCell = new TableCell(getScript(), row, 1);
            ScriptCell dCell = null;
            if (tCell.isFirstHidden()) {
              text += " hidden:" + tCell.getDataCell().getHidden();
            }
            if (col > 0) {
              dCell = evalDataCell(row, col);
              if (SX.isNotNull(dCell)) {
                String cellText = dCell.get();
                if (cellText.startsWith("@")) {
                  if (!cellText.startsWith("@@")) {
                    text += " IMAGE:" + cellText;
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
            status.setText(String.format("(%d, %d)%s%s", lines.get(row) + 1, col, text, " " + statusText));
            statusText = "";
          }
          SX.pause(0.5);
          while (statusPause) {
            SX.pause(0.5);
          }
        }
      }
    }).start();
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
//        log.trace("clicked: R%d C%d B%d {%d ... %d}",
//                row, col, button, selectedRows[0], selectedRows[selectedRows.length - 1]);
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
          if (col < 2) {
            popUpMenus.action(cell);
          }
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
    ScriptCell cell = evalDataCell(tCell.row, tCell.col);
    if (tCell.isHeader()) {
      cell.setHeader();
    }
    return cell;
  }

  protected ScriptCell evalCommandCell(TableCell tCell) {
    return evalDataCell(tCell.row, 1);
  }

  protected ScriptCell evalDataCell(int tableRow, int tableCol) {
    int dataCol = tableCol == 0 ? 0 : tableCol - 1;
    if (tableRow < 0) {
      tableRow = 0;
    }
//    int dataRow = lines.get(tableRow);
    int dataRow = tableRow;
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
    return line.get(dataCol);
  }

  protected List<ScriptCell> dataRow(int dataRow) {
    return data.get(dataRow);
  }

  protected List<ScriptCell> dataRowRemove(int dataRow) {
    return data.remove(dataRow);
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
    if (dataCol > 0 && SX.isSet(item)) {
      line.get(dataCol).setInitial(item);
    }
  }

  protected void setValueAt(String text, TableCell cell) {
    table.getModel().setValueAt(text, cell.row, cell.col);
    table.setSelection(cell.row, cell.col);
  }

  private TableCell findNext(TableCell tCell) {
    ScriptCell cell = evalDataCell(tCell);
    int indent = cell.getLoopIndent() - 1;
    int lastLine = getLastValidLine();
    String cellText = cell.get();
    String searchText = "endloop";
    if (cellText.contains("function")) {
      searchText = "endfunction";
    }
    while (true) {
      tCell = tCell.nextRow();
      if (tCell.row > lastLine) {
        tCell = new TableCell(this, lastLine);
        break;
      }
      cell = evalDataCell(tCell);
      cellText = cell.get();
      if (cellText.contains(searchText)) {
        if (indent == cell.getLoopIndent()) {
          break;
        }
      }
    }
    return tCell;
  }

  private TableCell findPrevious(TableCell tCell) {
    ScriptCell cell = evalDataCell(tCell);
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
    while (true) {
      tCell = tCell.previousRow();
      if (tCell.isFirstHidden()) {
        continue;
      }
      if (tCell.row < 0) {
        tCell = new TableCell(this, 0);
        break;
      }
      cell = evalDataCell(tCell);
      cellText = cell.get().trim();
      if (isIf) {
        if (searchText.contains(cellText)) {
          if (indent == cell.getIndent()) {
            break;
          }
        }
      } else if (cellText.contains(searchText)) {
        if (indent == cell.getIndent()) {
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
        if (cell.get().startsWith("loop") || cell.get().startsWith("function")) {
          lastCell = findNext(firstCell);
        } else if (cell.get().startsWith("endloop") || cell.get().startsWith("endfunction")) {
          lastCell = findPrevious(firstCell);
        } else {
          if (tCell.isLineNumber()) {
            if (cell.get().startsWith("if")) {
              lastCell = findNextForIf(firstCell, "endif");
            }
            if (cell.get().startsWith("endif")) {
              lastCell = findPrevious(firstCell);
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
      } else {
        cell.lineHide(selectedRows);
      }
    }
    cell = evalDataCell(tCell);
    cellText = cell.get();
    if (cell.isImage()) {
      if (cellText.contains("?")) {
        cell.capture(tCell);
      } else {
        cell.show(tCell);
      }
      return;
    }
    log.trace("F1: (%d,%d) %s (%d, %d, %d)",
            tCell.row + 1, tCell.col, cellText,
            cell.getIndent(), cell.getIfIndent(), cell.getLoopIndent());
    if (cellText.startsWith("$")) {
      if (cellText.startsWith("$?")) {
        // set value/block
      } else {
        // go to def/display
      }
      return;
    }
  }

  protected void setStatus(String msg, Object... args) {
    statusText = String.format(msg, args);
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
              hiddenCount = Integer.parseInt(cellText.split(" ")[2]);
              if (hiddenCount > 1) {
                aLine.get(0).setHidden(hiddenCount);
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
        data.add(aLine);
      }
    }
    if (data.size() > 0) {
      List<Integer> firstHideLines = new ArrayList<>();
      Integer row = 0;
      for (List<ScriptCell> line : data) {
        ScriptCell cell = line.get(0);
        if (cell.isFirstHidden()) {
          firstHideLines.add(0, row);
        }
        row++;
      }
      int actualHidden = 0;
      while (firstHideLines.size() > 0) {
        Integer hrow = firstHideLines.get(actualHidden);
        ScriptCell firstHiddenCell = data.get(hrow).get(0);
        firstHiddenCell.setHiddenData(createData(hrow + 1, hrow + firstHiddenCell.getHidden() - 1));
        firstHideLines.remove(actualHidden);
      }
    }
    if (SX.isNotNull(table)) {
      checkContent();
      table.setSelection(0, 1);
    }
  }

  protected List<List<ScriptCell>> createData(int firstLine, int lastLine) {
    List<List<ScriptCell>> newData = new ArrayList<>();
    for (int n = firstLine; n <= lastLine; n++) {
      newData.add(dataRowRemove(firstLine));
    }
    return newData;
  }

  protected void saveScript() {
    String theScript = "";
    resetLines();
    for (List<ScriptCell> line : allData) {
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

  protected void resetLines() {
    int nextDataLine = 0;
    lines.clear();
    allData.clear();
    boolean someHidden = false;
    int first = 0;
    for (List<ScriptCell> line : data) {
      ScriptCell cell = line.get(0);
      lines.add(nextDataLine++);
      allData.add(line);
      if (cell.isFirstHidden()) {
        nextDataLine += cell.getHidden() - 1;
        if (!someHidden) {
          someHidden = true;
          first++;
        }
      }
    }
    while (someHidden) {
      for (int n = first; n < allData.size(); n++) {
        someHidden = false;
        List<ScriptCell> line = allData.get(n);
        ScriptCell cell = line.get(0);
        if (cell.isFirstHidden()) {
          someHidden = true;
          List<List<ScriptCell>> hiddenData = cell.getHiddenData();
          int hiddenLines = hiddenData.size();
          allData.addAll(n + 1, hiddenData);
          lines.addAll(n + 1, Arrays.asList(new Integer[hiddenLines]));
          first = n + 1;
          break;
        }
      }
    }
    for (int n = first; n < lines.size(); n++) {
      if (SX.isNull(lines.get(n))) {
        continue;
      }
      lines.set(n, n);
    }
    Predicate<Integer> removeNull = nLine -> nLine == null;
    lines.removeIf(removeNull);
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
    int currentFunctionIndent = 0;
    boolean hasElse = false;
    resetLines();
    for (List<ScriptCell> line : allData) {
      ScriptCell cell = line.get(0);
      if (line.size() == 0) {
        continue;
      }
      cell.reset();
      String command = cell.get().trim();
      if (SX.isNotNull(ScriptTemplate.commandTemplates.get(command))) {
        if (command.startsWith("if") && !command.contains("ifElse")) {
          currentIfIndent++;
          cell.setIndent(currentIndent, currentIfIndent, -1, -1);
          currentIndent++;
        } else if (command.startsWith("elif")) {
          if (currentIfIndent > 0 && !hasElse) {
            currentIndent--;
            cell.setIndent(currentIndent, currentIfIndent, -1, -1);
            currentIndent++;
          } else {
            cell.setError();
          }
        } else if (command.startsWith("else")) {
          if (currentIfIndent > 0) {
            hasElse = true;
            currentIndent--;
            cell.setIndent(currentIndent, currentIfIndent, -1, -1);
            currentIndent++;
          } else {
            cell.setError();
          }
        } else if (command.startsWith("endif")) {
          if (currentIfIndent > 0) {
            hasElse = false;
            currentIndent--;
            currentIfIndent--;
            cell.setIndent(currentIndent, currentIfIndent, -1, -1);
          } else {
            cell.setError();
          }
        } else if (command.startsWith("loop")) {
          currentLoopIndent++;
          cell.setIndent(currentIndent, -1, currentLoopIndent, -1);
          currentIndent++;
        } else if (command.startsWith("endloop")) {
          if (currentLoopIndent > 0) {
            currentIndent--;
            currentLoopIndent--;
            cell.setIndent(currentIndent, -1, currentLoopIndent, -1);
          } else {
            cell.setError();
          }
        } else if (command.startsWith("function")) {
          currentFunctionIndent++;
          cell.setIndent(currentIndent, -1, -1, currentFunctionIndent);
          currentIndent++;
        } else if (command.startsWith("endfunction")) {
          if (currentFunctionIndent > 0) {
            currentIndent--;
            currentFunctionIndent--;
            cell.setIndent(currentIndent, -1, -1, currentFunctionIndent);
          } else {
            cell.setError();
          }
        } else {
          cell.setIndent(currentIndent, currentIfIndent, currentLoopIndent, currentFunctionIndent);
        }
      } else if (cell.get().startsWith("@")) {

      } else if (cell.get().startsWith("$")) {

      } else if (cell.get().contains("#")) {

      } else {
        log.error("checkContent: %s", cell);
      }
    }
    table.tableHasChanged();
    String sLines = "";
    for (int ix : lines) {
      sLines += ix + ",";
    }
    log.trace("checkContent finished (%s)", sLines);
  }

  protected boolean addCommandTemplate(String command, TableCell tCell, int[] selectedRows) {
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
      tCellLast = new TableCell(this, selectedRows[selectedRows.length - 1]);
      tCellLast.nextRow().lineAdd(command.startsWith("if") ? "endif" : "endloop");
      success = true;
    } else if (tCell.isLineEmpty()) {
      if (SX.isNotNull(commandLine)) {
        int lineLast = commandLine.length - 1;
        String lineEnd = commandLine[lineLast];
        if (lineEnd.startsWith("result")) {
          commandLine[lineLast] = (lineEnd.contains("-list") ? "$$V" : "$V") + resultsCounter++;
        }
        tCell.lineSet(commandLine);
        if (command.startsWith("if") && !command.contains("ifElse")) {
          tCell.lineAdd("endif");
        } else if (command.startsWith("loop")) {
          tCell.lineAdd("endloop");
        } else if (command.startsWith("function")) {
          tCell.lineAdd("endfunction");
        } else {
        }
      } else {
        tCell.lineSet(new String[]{command + "?"});
      }
      tCell = new TableCell(this, tCell.row, 0);
      success = true;
    }
    if (success) {
      checkContent();
      if (SX.allNotNull(tCellFirst, tCellLast)) {
        table.setLineSelection(tCellFirst.row, tCellLast.row);
      } else {
        table.setSelection(tCell.row, tCell.col);
      }
    }
    return success;
  }

  protected void editBox(TableCell cell) {
    String initialText = cell.getDataCell().getInitial();
    String[] text = new String[]{cell.getDataCell().get(), initialText};
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
          popUpWindow.showCell(cell, text);
        }
      });
    }
  }
}


