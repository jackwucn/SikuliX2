package com.sikulix.editor;

import javax.swing.*;

public class Editor {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new Script(args);
      }
    });
  }
}
