package com.sikulix.editor;

import com.sikulix.core.SX;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class PopUpWindow extends JFrame {

  TableCell cell = null;
  RSyntaxTextArea cellTextArea = null;
  String cellText = "";
  String resetText = "";

  public void showCell(TableCell cell, String[] text) {
    this.cell = cell;
    String editText = text[0];
    resetText = text[1];
    if (SX.isNotSet(text)) {
      editText = cell.script.evalDataCell(cell).get();
    }
    cellTextArea.setText(editText);
    setLocationRelativeTo(cell.script.getWindow());
    setVisible(true);
  }

  public PopUpWindow() {

    JPanel contentPane = new JPanel(new BorderLayout());
    RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
    textArea.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent e) {
        if (e.getExtendedKeyCode() == KeyEvent.VK_ESCAPE) {
          setVisible(false);
          int modifiers = e.getModifiers();
          if (modifiers == KeyEvent.CTRL_MASK) {
            cellText = textArea.getText();
            if (!checkText(cellText)) {
              cellText = resetText;
            }
            cell.script.table.setValueAt(cellText, cell.row, cell.col);
            cell.script.table.setSelection(cell.row, cell.col);
          }
          return;
        }
        super.keyTyped(e);
      }
    });
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
    textArea.setMarkOccurrences(true);
    contentPane.add(new RTextScrollPane(textArea));
    CompletionProvider provider = createCompletionProvider();
    AutoCompletion ac = new AutoCompletion(provider);
//    ((AbstractCompletionProvider)provider).setAutoActivationRules(true, null);
    ac.setAutoActivationEnabled(true);
    ac.install(textArea);
//    ac.setAutoCompleteSingleChoices(true);

    setContentPane(contentPane);
    setTitle("AutoComplete Demo");
    setUndecorated(true);
    setAlwaysOnTop(true);
    pack();
    cellTextArea = textArea;
  }

  boolean checkText(String text) {
    String[] lines = text.split("\n");
    String saveText = "";
    boolean shouldReset = true;
    for (int n = 0; n < lines.length; n++) {
      if (lines[n].startsWith("//---")) continue;
      String line = lines[n].trim();
      if (SX.isSet(line)) shouldReset = false;
      saveText += line + "\n";
    }
    if (!shouldReset) {
      cellText = saveText;
      return true;
    }
    return false;
  }

  /**
   * Create a simple provider that adds some Java-related completions.
   */
  private CompletionProvider createCompletionProvider() {

    // A DefaultCompletionProvider is the simplest concrete implementation
    // of CompletionProvider. This provider has no understanding of
    // language semantics. It simply checks the text entered up to the
    // caret position for a match against known completions. This is all
    // that is needed in the majority of cases.
    DefaultCompletionProvider provider = new JSCompletion();

    // Add completions for all Java keywords. A BasicCompletion is just
    // a straightforward word completion.
    provider.addCompletion(new BasicCompletion(provider, "abstract"));
    provider.addCompletion(new BasicCompletion(provider, "assert"));
    provider.addCompletion(new BasicCompletion(provider, "break"));
    provider.addCompletion(new BasicCompletion(provider, "case"));
    // ... etc ...
    provider.addCompletion(new BasicCompletion(provider, "transient"));
    provider.addCompletion(new BasicCompletion(provider, "try"));
    provider.addCompletion(new BasicCompletion(provider, "void"));
    provider.addCompletion(new BasicCompletion(provider, "volatile"));
    provider.addCompletion(new BasicCompletion(provider, "while"));

    provider.addCompletion(new BasicCompletion(provider, "click"));
    provider.addCompletion(new ShorthandCompletion(provider, "find", "#find|@?|$R"));

    // Add a couple of "shorthand" completions. These completions don't
    // require the input text to be the same thing as the replacement text.
    provider.addCompletion(new ShorthandCompletion(provider, "sysout",
            "System.out.println(", "System.out.println("));
    provider.addCompletion(new ShorthandCompletion(provider, "syserr",
            "System.err.println(", "System.err.println("));
    return provider;
  }

  private class JSCompletion extends DefaultCompletionProvider {

    protected boolean isValidChar(char ch) {
      return Character.isLetter(ch) || "_$@".contains("" + ch);
    }
  }
}