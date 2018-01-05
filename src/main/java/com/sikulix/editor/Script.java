package com.sikulix.editor;

import com.sikulix.api.Do;
import com.sikulix.api.Element;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;

public class Script implements TableModelListener, ListSelectionListener {

  protected static final SXLog log = SX.getSXLog("SX.SCRIPTEDITOR");

  private JFrame window;

  protected JFrame getWindow() {
    return window;
  }

  private Font customFont = new Font("Helvetica Bold", Font.PLAIN, 18);
  PopUpMenus popUpMenus = null;

  private ScriptTable table = null;
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

    table.getModel().addTableModelListener(this);

    ListSelectionModel listSelectionModel = table.getSelectionModel();
    listSelectionModel.addListSelectionListener(this);
    table.setSelectionModel(listSelectionModel);

    table.addMouseListener(new MyMouseAdapter(1));
    table.getTableHeader().addMouseListener(new MyMouseAdapter(0));

    JScrollPane scrollPane = new JScrollPane(table);
    panel.add(scrollPane);

    window.setContentPane(panel);

    window.pack();
    window.setLocation(rectTable.x, rectTable.y);
    window.setVisible(true);

    popUpMenus = new PopUpMenus(this);
  }

  private class MyMouseAdapter extends MouseAdapter {

    private int type = 1;

    private MyMouseAdapter() {
    }

    public MyMouseAdapter(int type) {
      this.type = type;
    }

    public void mouseReleased(MouseEvent me) {
      Point where = me.getPoint();
      int row = table.rowAtPoint(where);
      int col = table.columnAtPoint(where);
      int button = me.getButton();

      if (row < 0) {
        return;
      }
      if (type > 0) {
        log.trace("clicked: R%d C%d B%d", row, col, button);
      } else {
        log.trace("clicked: Header C%d B%d", col, button);
      }
      if (type > 0 && button > 1) {
        table.setSelection(row, col);
      }
      if (button == 3) {
        if (type > 0) {
          if (col == 1) {
            PopUpMenus.Command.pop(table, where.x, where.y);
          } else if (col == 0) {
            PopUpMenus.Action.pop(table, where.x, where.y);
          } else {
            PopUpMenus.Default.pop(table, where.x, where.y);
          }
        } else {

        }
        return;
      }
    }

  }

  @Override
  public void tableChanged(TableModelEvent e) {
    int firstRow = e.getFirstRow();
    int lastRow = e.getLastRow();
    int column = e.getColumn();
    String changed = "NO_CELL";
    if (firstRow == lastRow) {
      changed = data.get(firstRow).get(column - 1).get();
    }
    if (column > 0) {
      log.trace("changed: [%d, %d] %s", firstRow, column - 1, changed);
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (!e.getValueIsAdjusting()) {
      if (!e.toString().contains("={}")) {
        int[] rows = table.getSelectedRows();
        int[] cols = table.getSelectedColumns();
        if (rows.length > 0 && cols.length > 0) {
          //log.trace("selected: (%d, %d)", rows[0], cols[0]);
        }
      }
    }
  }

  Point currentCell = null;
  String savedCell = "";

  protected ScriptCell cellAt(int row, int col) {
    int lineCol = col - 1;
    if (row < 0) {
      row = 0;
    }
    if (row > data.size() - 1) {
      data.add(new ArrayList<>());
    }
    List<ScriptCell> line = data.get(row);
    if (lineCol > line.size() - 1) {
      for (int n = line.size(); n <= lineCol; n++) {
        line.add(new ScriptCell(this));
      }
    }
    return data.get(row).get(lineCol).set(row, col);
  }

  protected ScriptCell lineAt(int row) {
    return cellAt(row, 0);
  }

  protected static Element getCellClick(int row, int col, JFrame window, JTable table) {
    Point windowLocation = window.getLocation();
    Rectangle cell = table.getCellRect(row, col, false);
    windowLocation.x += cell.x + 10;
    windowLocation.y += cell.y + 70;
    return new Element(windowLocation);
  }

  void indent(int row, int col) {
    log.trace("action: indent");
    cellAt(row, 2).doIndent();
    table.tableHasChanged();
  }

  void dedent(int row, int col) {
    log.trace("action: dedent");
    cellAt(row, 2).doDedent();
    table.tableHasChanged();
  }

  protected void loadScript() {
    data.clear();
    resultsCounter = 0;
    String theScript = com.sikulix.core.Content.readFileToString(fScript);
    for (String line : theScript.split("\\n")) {
      List<ScriptCell> aLine = new ArrayList<>();
      int colCount = 1;
      for (String cellText : line.split("\\t")) {
        if (cellText.contains("=result")) {
          int count = -1;
          String resultCount = cellText.replace("=result", "").trim();
          try {
            count = Integer.parseInt(resultCount);
          } catch (Exception ex) {
            cellText += "?";
          }
          if (count >= resultsCounter) {
            resultsCounter = count + 1;
          }
        }
        aLine.add(new ScriptCell(this, cellText));
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

  protected void runScript(int lineFrom, int lineTo) {
    window.setVisible(false);
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

  protected boolean addCommandTemplate(String command, int row, int col) {
    boolean hasError = false;
    ScriptCell.BlockType blockType = null;
    if (cellAt(row, col).isLineEmpty()) {
      if (commandTemplates.size() == 0) {
        initTemplates();
      }
      command = command.trim();
      String[] commandLine = commandTemplates.get(command).clone();
      if (SX.isNull(commandLine)) {
        hasError = true;
      } else {
        commandLine[0] = commandLine[0] + command;
        int lineLast = commandLine.length - 1;
        String lineEnd = commandLine[lineLast];
        String block = "{block}";
        if ("result".equals(lineEnd)) {
          commandLine[lineLast] = "$R" + resultsCounter++;
        } else if (lineEnd.startsWith(block)) {
          String suffix = lineEnd.replace(block, "");
          if (lineEnd.length() > block.length()) {
            if ("if".equals(suffix)) {
              blockType = ScriptCell.BlockType.IF;
            } else if ("el".equals(suffix)) {
              blockType = ScriptCell.BlockType.ELSE;
            } else if ("ei".equals(suffix)) {
              blockType = ScriptCell.BlockType.ELIF;
            } else {
              log.error("addCommandTemplate: invalid {block} suffix: %s", suffix);
              hasError = true;
            }
          } else {
            blockType = ScriptCell.BlockType.SINGLE;
          }
          commandLine[lineLast] = block;
        }
      }
      if (hasError) {
        commandLine = new String[]{"#error!", commandLine[0]};
      }
      cellAt(row, col).setLine(blockType, commandLine);
      table.tableHasChanged(row, col);
      return true;
    }
    return false;
  }

  protected void checkContent(int row) {
    int currentIndent = 0;
    int currentIfIndent = 0;
    int rowIndex = -1;
    for (List<ScriptCell> line : data) {
      if (rowIndex < row) {
        rowIndex++;
        continue;
      }
      //TODO debug it
      ScriptCell command = line.get(0);
      command.resetIndent();
      command.setIndent(currentIndent);
      if (command.isBlock()) {
        if (command.isBlockType(ScriptCell.BlockType.SINGLE)) {
          command.setIndent(currentIndent);
          currentIndent++;
        } else {
          if (command.isBlockType(ScriptCell.BlockType.IF)) {
            command.setIndent(currentIndent);
            currentIndent++;
            currentIfIndent++;
            command.setIfIndent(currentIfIndent);
          } else {
            if (currentIfIndent < 1) {
              command.setIndent(currentIndent);
              command.setIfIndent(-1);
            } else {
              if (command.isBlockType(ScriptCell.BlockType.ELSE)) {
                command.setIndent(currentIndent - 1);
                currentIfIndent--;
                command.setIfIndent(currentIfIndent);
              } else if (command.isBlockType(ScriptCell.BlockType.ELIF)) {
                command.setIndent(currentIndent - 1);
                command.setIfIndent(currentIfIndent);
              }
            }
          }
        }
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

    commandTemplates.put("#if", new String[]{"", "{condition}", "{block}if"});
    commandTemplates.put("#ifNot", new String[]{"", "{condition}", "{block}if"});
    commandTemplates.put("#else", new String[]{"", "{block}el"});
    commandTemplates.put("#elif", new String[]{"", "{condition}", "{block}ei"});

    commandTemplates.put("#elifNot", new String[]{"", "{condition}", "{block}ei"});
    commandTemplates.put("#loop", new String[]{"", "{condition}", "{block}"});
    commandTemplates.put("#loopWith", new String[]{"", "listvariable", "{block}"});
    commandTemplates.put("#loopFor", new String[]{"", "count step from", "{block}"});

    commandTemplates.put("#print", new String[]{""});
    commandTemplates.put("#printf", new String[]{"", "template", "variable..."});
    commandTemplates.put("#log", new String[]{"", "template", "variable..."});
    commandTemplates.put("#pop", new String[]{"", "message", "result"});

    commandTemplates.put("#codeblock", new String[]{"", "{block}", "result"});
    commandTemplates.put("#function", new String[]{"", "$?", "{block}", "parameter..."});
  }

  protected void editBox(int row, int col) {
    Script.log.trace("EditBox: should open");
    ScriptCell cell = cellAt(row, Math.max(1, col));
  }
}


