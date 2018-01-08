package com.sikulix.editor;

import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Script {

  protected static final SXLog log = SX.getSXLog("SX.SCRIPTEDITOR");

  private JFrame window;

  protected JFrame getWindow() {
    return window;
  }

  private Font customFont = new Font("Helvetica Bold", Font.PLAIN, 18);
  PopUpMenus popUpMenus = null;

  ScriptTable table = null;
  public Rectangle rectTable = null;
  int maxCol = 7;

  protected ScriptTable getTable() {
    return table;
  }

  private File fScript = new File(SX.getSXSTORE(), "scripteditor/script.txt");

  protected File getScriptPath() {
    return fScript;
  }

  List<List<ScriptCell>> data = new ArrayList<>();
  List<List<ScriptCell>> savedLine = new ArrayList<>();

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
    int col1W = 90;
    int colLast = 400;
    table.getColumnModel().getColumn(0).setPreferredWidth(col0W);
    table.getColumnModel().getColumn(1).setPreferredWidth(col1W);
    table.getColumnModel().getColumn(maxCol).setPreferredWidth(colLast);
    int colsW = (tableW - col0W - col1W - colLast) / (tableCols - 3);
    for (int n = 2; n < tableCols - 1; n++) {
      table.getColumnModel().getColumn(n).setPreferredWidth(colsW);
    }

    table.setGridColor(Color.LIGHT_GRAY);
    table.setShowGrid(false);
    table.setShowHorizontalLines(true);

    table.setPreferredScrollableViewportSize(tableDim);
    table.setFillsViewportHeight(true);

//    table.getModel().addTableModelListener(this);
//
//    ListSelectionModel listSelectionModel = table.getSelectionModel();
//    listSelectionModel.addListSelectionListener(this);
//    table.setSelectionModel(listSelectionModel);

    table.addMouseListener(new MyMouseAdapter(1, this));
    table.getTableHeader().addMouseListener(new MyMouseAdapter(0, this));

    JScrollPane scrollPane = new JScrollPane(table);
    panel.add(scrollPane);

    window.setContentPane(panel);

    window.pack();
    window.setLocation(rectTable.x, rectTable.y);
    window.setVisible(true);
    table.setSelection(0, 0);

    popUpMenus = new PopUpMenus(this);
  }

  private class MyMouseAdapter extends MouseAdapter {

    private int type = 1;
    private Script script = null;

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
      ScriptCell cell = script.cellAt(row, col);
      int button = me.getButton();

      if (row < 0) {
        return;
      }
      int[] selectedRows = table.getSelectedRows();
      if (type > 0) {
        log.trace("clicked: R%d C%d B%d {%d ... %d}",
                row, col, button, selectedRows[0], selectedRows[selectedRows.length - 1]);
      } else {
        log.trace("clicked: Header C%d B%d", col, button);
      }
      if (type > 0 && button > 1) {
        if (selectedRows.length < 2) {
          table.setSelection(row, col);
        }
      }
      if (button == 3) {
        if (type > 0) {
          if (col == 0) {
            popUpMenus.action(cell);
          } else if (col == 1) {
            popUpMenus.command(cell);
          } else {
            popUpMenus.notimplemented(cell);
          }
        } else {
        }
        return;
      }
    }

  }

//  @Override
//  public void tableChanged(TableModelEvent e) {
//    int firstRow = e.getFirstRow();
//    int lastRow = e.getLastRow();
//    int column = e.getColumn();
//    String changed = "NO_CELL";
//    if (firstRow == lastRow) {
//      changed = data.get(firstRow).get(column - 1).get();
//    }
//    if (column > 0) {
//      log.trace("changed: [%d, %d] %s", firstRow, column - 1, changed);
//    }
//  }
//
//  @Override
//  public void valueChanged(ListSelectionEvent e) {
//    if (!e.getValueIsAdjusting()) {
//      if (!e.toString().contains("={}")) {
//        int[] rows = table.getSelectedRows();
//        int[] cols = table.getSelectedColumns();
//        if (rows.length > 0 && cols.length > 0) {
//          //log.trace("selected: (%d, %d)", rows[0], cols[0]);
//        }
//      }
//    }
//  }

  String savedCell = "";

  protected ScriptCell cellAt(int row, int col) {
    int dataCol = col == 0 ? 0 : col - 1;
    if (row < 0) {
      row = 0;
    }
    if (row > data.size() - 1) {
      data.add(new ArrayList<>());
    }
    List<ScriptCell> line = data.get(row);
    if (dataCol > line.size() - 1) {
      for (int n = line.size(); n <= dataCol; n++) {
        line.add(new ScriptCell(this, "", row, col));
      }
    }
    ScriptCell cell = data.get(row).get(dataCol);
    cell.set(row, col);
    return cell;
  }

  protected void loadScript() {
    data.clear();
    resultsCounter = 0;
    int rowCount = -1;
    String theScript = com.sikulix.core.Content.readFileToString(fScript);
    for (String line : theScript.split("\\n")) {
      List<ScriptCell> aLine = new ArrayList<>();
      int colCount = 1;
      rowCount++;
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
        }
        aLine.add(new ScriptCell(this, cellText, rowCount, colCount));
        if (++colCount > maxCol) {
          break;
        }
      }
      if (aLine.size() > 0) {
        data.add(aLine);
      }
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

  Map<String, String> images = new HashMap<>();

  protected boolean addCommandTemplate(String command, ScriptCell cell) {
    ScriptCell.BlockType blockType = null;
    if (cell.isLineEmpty()) {
      if (commandTemplates.size() == 0) {
        initTemplates();
      }
      command = command.trim();
      String[] commandLine = commandTemplates.get(command);
      if (SX.isNotNull(commandLine)) {
        commandLine = commandLine.clone();
        commandLine[0] = commandLine[0] + command;
        int lineLast = commandLine.length - 1;
        String lineEnd = commandLine[lineLast];
        String block = "{block}";
        if ("result".equals(lineEnd)) {
          commandLine[lineLast] = "$R" + resultsCounter++;
        } else if (lineEnd.startsWith(block)) {
          if (command.startsWith("#if")) {
            blockType = ScriptCell.BlockType.IF;
          } else if (command.startsWith("#else")) {
            blockType = ScriptCell.BlockType.ELSE;
          } else if (command.startsWith("#elif")) {
            blockType = ScriptCell.BlockType.ELIF;
          } else if (command.startsWith("#loop")) {
            blockType = ScriptCell.BlockType.LOOP;
          }
          commandLine[lineLast] = block;
        }
        cell.setLine(blockType, commandLine);
        if (ScriptCell.BlockType.IF.equals(blockType)) {
          cell.addLine("#endif");
        } else if (ScriptCell.BlockType.LOOP.equals(blockType)) {
          cell.addLine("#endloop");
        } else {
          table.tableHasChanged();
        }
        table.setSelection(cell.getRow(), 2);
      } else {
        cell.setLine(blockType, new String[]{command + "?"});
        table.tableHasChanged();
        table.setSelection(cell.getRow(), 1);
      }
      return true;
    }
    return false;
  }

  protected void checkContent(int row) {
    int currentIndent = 0;
    int currentIfIndent = 0;
    int rowIndex = 0;
    for (List<ScriptCell> line : data) {
      if (rowIndex != row) {
        rowIndex++;
        continue;
      }
      ScriptCell command = line.get(0);
      if (command.get().startsWith("#")) {
        command.resetIndent();
      } else if (command.get().startsWith("@")) {

      } else if (command.get().startsWith("$")) {
      } else {

      }
    }

  }

  Map<String, String[]> commandTemplates = new HashMap<>();

  private void initTemplates() {
    commandTemplates.put("#find", new String[]{"", "@?", "result"});
    commandTemplates.put("#wait", new String[]{"", "wait-time", "@?", "result"});
    commandTemplates.put("#vanish", new String[]{"", "wait-time", "@?", "result"});
    commandTemplates.put("#findAll", new String[]{"", "@?", "result"});
    commandTemplates.put("#findBest", new String[]{"", "@[?", "result"});
    commandTemplates.put("#findAny", new String[]{"", "@[?", "result"});
    commandTemplates.put("#click", new String[]{"", "@?", "result"});
    commandTemplates.put("#clickRight", new String[]{"", "@?", "result"});
    commandTemplates.put("#clickDouble", new String[]{"", "@?", "result"});
    commandTemplates.put("#hover", new String[]{"", "@?", "result"});

    commandTemplates.put("#if", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("#ifNot", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("#else", new String[]{"", "{block}"});
    commandTemplates.put("#elif", new String[]{"", "{condition}", "{block}"});

    commandTemplates.put("#elifNot", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("#loop", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("#loopWith", new String[]{"", "listvariable", "{block}"});
    commandTemplates.put("#loopFor", new String[]{"", "count step from", "{block}"});

    commandTemplates.put("#print", new String[]{""});
    commandTemplates.put("#printf", new String[]{"", "template", "variable..."});
    commandTemplates.put("#log", new String[]{"", "template", "variable..."});
    commandTemplates.put("#pop", new String[]{"", "message", "result"});

    commandTemplates.put("#function", new String[]{"", "$?", "{block}", "parameter..."});
  }

  protected void editBox(ScriptCell cell) {
    Script.log.trace("EditBox: should open");
  }
}


