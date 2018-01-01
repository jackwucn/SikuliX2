package com.sikulix.editor;

import javax.swing.*;

public class Editor {
  private static void createAndShowGUI() {
    JFrame frame = new JFrame("SikuliX - ScriptEditor");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Script script = new Script(frame);
    script.setOpaque(true);
    frame.setContentPane(script);

    frame.pack();
    frame.setLocation(script.rectTable.x, script.rectTable.y);
    frame.setVisible(true);
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
}
