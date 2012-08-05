/*
 * Copyright (C) 2012 Mikel Artetxe
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package org.apertium.caffeine;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.HighlightPainter;
import org.apertium.Translator;

/**
 *
 * @author Mikel Artetxe
 */
public class ApertiumCaffeine extends javax.swing.JFrame {

    protected static final Preferences prefs = Preferences.userNodeForPackage(ApertiumCaffeine.class);
    protected static final FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.matches("apertium-[a-z][a-z][a-z]?-[a-z][a-z][a-z]?.jar");
        }
    };

    private HashMap<String, String> titleToBase;
    private HashMap<String, String> titleToMode;

    /** Creates new form ApertiumCaffeine */
    public ApertiumCaffeine() {
        initComponents();
    }

    public void init() {
        int x = prefs.getInt("boundsX", -1);
        int y = prefs.getInt("boundsY", -1);
        int width = prefs.getInt("boundsWidth", -1);
        int height = prefs.getInt("boundsHeight", -1);
        if (height > 0 && width > 0) setBounds(x, y, width, height);
        else setLocationRelativeTo(null);
        
        // We create a popup menu for the input text area
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem("Copy");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                inputTextArea.copy();
            }
        });
        popup.add(item);
        item = new JMenuItem("Cut");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                inputTextArea.cut();
            }
        });
        popup.add(item);
        item = new JMenuItem("Paste");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                inputTextArea.paste();
            }
        });
        popup.add(item);
        popup.addSeparator();
        item = new JMenuItem("Select all");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                inputTextArea.selectAll();
            }
        });
        popup.add(item);
        inputTextArea.setComponentPopupMenu(popup);
        
        // We create a popup menu for the output text area
        popup = new JPopupMenu();
        item = new JMenuItem("Copy");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                outputTextArea.copy();
            }
        });
        popup.add(item);
        popup.addSeparator();
        item = new JMenuItem("Select all");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                outputTextArea.selectAll();
            }
        });
        popup.add(item);
        outputTextArea.setComponentPopupMenu(popup);
        
        Translator.setParallelProcessingEnabled(false);
        
        File packagesDir = null;
        String packagesPath = prefs.get("packagesPath", null);
        if (packagesPath != null) packagesDir = new File(packagesPath);
        while (packagesDir == null || !packagesDir.isDirectory()) {
            String options[] = {"Create default directory", "Choose my own directory"};
            int answer = JOptionPane.showOptionDialog(null,
                    "It seems that this is the first time that you run the program.\n"
                    + "First of all, we need to set the directory in which to install the\n"
                    + "language pair packages.\n"
                    + "You can either create the default directory (a folder called \n"
                    + "\"Apertium Caffeine packages\" in your home directory) or select\n"
                    + "a custom one.\n",
                    "Welcome to Apertium Caffeine!",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                packagesDir = new File(new File(System.getProperty("user.home")), "Apertium Caffeine packages");
                packagesDir.mkdir();
                prefs.put("packagesPath", packagesDir.getPath());
                try {
                    new File(packagesDir, ".apertium-caffeine").createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (answer == 1) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setApproveButtonText("OK");
                fc.setDialogTitle("Choose a directory");
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    packagesDir = fc.getSelectedFile();
                    while (new File(packagesDir, ".apertium-omegat").exists()) {
                        JOptionPane.showMessageDialog(null,
                                "The selected directory is being used by Apertium OmegaT plug-in.\n"
                                + "Please, select a different one.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                            packagesDir = fc.getSelectedFile();
                        else System.exit(0);
                    }
                    prefs.put("packagesPath", packagesDir.getPath());
                    try {
                        new File(packagesDir, ".apertium-caffeine").createNewFile();
                    } catch (IOException ex) {
                        Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else System.exit(0);
            } else System.exit(0);
        }

        initModes(packagesDir);
        if (modesComboBox.getItemCount() == 0 &&
                JOptionPane.showConfirmDialog(null,
                "You don't have any language pair installed yet.\n"
                + "Would you like to install some now?",
                "We need language pairs!", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            try {
                new InstallDialog((Frame)null, true) {
                    @Override
                    protected void initStrings() {
                        STR_TITLE = "Install language pairs";
                        STR_INSTRUCTIONS = "Check the language pairs to install.";
                    }
                }.setVisible(true);
                initModes(packagesDir);
            } catch (IOException ex) {
                Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, ex, "Error", JOptionPane.ERROR_MESSAGE);
            }

        int idx = prefs.getInt("modesComboBox", 0);
        if (idx < 0) idx = 0;
        if (idx < modesComboBox.getItemCount())
            modesComboBox.setSelectedIndex(idx);

        boolean displayMarks = prefs.getBoolean("displayMarks", true);
        displayMarksCheckBox.setSelected(displayMarks);
        Translator.setDisplayMarks(displayMarks);

        boolean wrap = prefs.getBoolean("wrapLines", true);
        inputTextArea.setLineWrap(wrap);
        inputTextArea.setWrapStyleWord(wrap);
        outputTextArea.setLineWrap(wrap);
        outputTextArea.setWrapStyleWord(wrap);

        Translator.setCacheEnabled(true);

        inputTextArea.requestFocusInWindow();
        inputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {update();}
            public void removeUpdate(DocumentEvent e) {update();}
            public void changedUpdate(DocumentEvent e) {update();}
        });
        inputTextArea.setText(prefs.get("inputText", ""));

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                prefs.put("inputText", inputTextArea.getText());
                prefs.putInt("modesComboBox", modesComboBox.getSelectedIndex());
                prefs.putBoolean("displayMarks", displayMarksCheckBox.isSelected());
                prefs.putInt("boundsX", getBounds().x);
                prefs.putInt("boundsY", getBounds().y);
                prefs.putInt("boundsWidth", getBounds().width);
                prefs.putInt("boundsHeight", getBounds().height);
            }
        });

        if (prefs.getBoolean("checkUpdates", true))
            new Thread() {
                @Override
                public void run() {
                    try {
                        UpdateDialog ud = new UpdateDialog(ApertiumCaffeine.this, true);
                        if (ud.updatesAvailable() && JOptionPane.showConfirmDialog(ApertiumCaffeine.this,
                                "Updates are available for some language pairs!\n"
                                + "Would you like to install them now?",
                                "Updates found", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                            ud.setVisible(true);
                    } catch (IOException ex) {
                        Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
    }

    private static final Pattern replacePattern = Pattern.compile("\\B(\\*|#|@)\\b");
    private static final Pattern highlightPattern = Pattern.compile("\\B(\\*|#|@)(\\p{L}||\\p{N})*\\b");
    private static final HighlightPainter redPainter = new DefaultHighlightPainter(Color.RED);
    private static final HighlightPainter orangePainter = new DefaultHighlightPainter(Color.ORANGE);
    private boolean textChanged, translating;
    private void update() {
        if (modesComboBox.getSelectedIndex() == -1) return;
        textChanged = true;
        if (translating) return;
        translating = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (textChanged) {
                    textChanged = false;
                    try {
                        final String translation = Translator.translate(inputTextArea.getText());
                        final boolean hide = prefs.getBoolean("hideMarks", true);
                        if (displayMarksCheckBox.isSelected() && prefs.getBoolean("highlightMarkedWords", true)) {
                            int offset = 0;
                            outputTextArea.setText(hide ? replacePattern.matcher(translation).replaceAll("") : translation);
                            Matcher matcher = highlightPattern.matcher(translation);
                            while (matcher.find()) outputTextArea.getHighlighter().addHighlight(
                                    matcher.start() + (hide ? offset-- : offset), matcher.end() + offset,
                                    translation.charAt(matcher.start()) == '*' ? redPainter : orangePainter);
                        } else outputTextArea.setText(translation);
                    } catch (Exception ex) {
                        Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
                        JOptionPane.showMessageDialog(ApertiumCaffeine.this, ex, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                translating = false;
            }
        }).start();
    }

    private void initModes(File packagesDir) {
        titleToBase = new HashMap<String, String>();
        titleToMode = new HashMap<String, String>();
        File packages[] = packagesDir.listFiles(filter);
        for (File p : packages) {
            try {
                String base = p.getPath();
                Translator.setBase(base);
                for (String mode : Translator.getAvailableModes()) {
                    String title = Translator.getTitle(mode);
                    titleToBase.put(title, base);
                    titleToMode.put(title, mode);
                }
            } catch (Exception ex) {
                //Perhaps the directory contained a file that wasn't a valid package...
                Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        Object titles[] = titleToBase.keySet().toArray();
        Arrays.sort(titles);
        modesComboBox.setModel(new DefaultComboBoxModel(titles));
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        modesComboBox = new javax.swing.JComboBox();
        displayMarksCheckBox = new javax.swing.JCheckBox();
        settingsButton = new javax.swing.JButton();
        inputScrollPane = new javax.swing.JScrollPane();
        inputTextArea = new javax.swing.JTextArea();
        outputScrollPane = new javax.swing.JScrollPane();
        outputTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Apertium Caffeine");

        modesComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modesComboBoxActionPerformed(evt);
            }
        });

        displayMarksCheckBox.setText("Mark unknown words");
        displayMarksCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayMarksCheckBoxActionPerformed(evt);
            }
        });

        settingsButton.setText("Settings...");
        settingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsButtonActionPerformed(evt);
            }
        });

        inputTextArea.setColumns(20);
        inputTextArea.setRows(5);
        inputScrollPane.setViewportView(inputTextArea);

        outputTextArea.setColumns(20);
        outputTextArea.setEditable(false);
        outputTextArea.setRows(5);
        outputScrollPane.setViewportView(outputTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(outputScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(modesComboBox, 0, 314, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(displayMarksCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(settingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(inputScrollPane, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 598, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(modesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(settingsButton)
                    .addComponent(displayMarksCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outputScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void modesComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modesComboBoxActionPerformed
        try {
            Translator.setBase(titleToBase.get(modesComboBox.getSelectedItem()));
            Translator.setMode(titleToMode.get(modesComboBox.getSelectedItem()));
            update();
        } catch (Exception ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_modesComboBoxActionPerformed

    private void displayMarksCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayMarksCheckBoxActionPerformed
        Translator.setDisplayMarks(displayMarksCheckBox.isSelected());
        update();
    }//GEN-LAST:event_displayMarksCheckBoxActionPerformed

    private void settingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsButtonActionPerformed
        Object currentMode = modesComboBox.getSelectedItem();
        SettingsDialog md = new SettingsDialog(this, true);
        md.setVisible(true);
        initModes(new File(prefs.get("packagesPath", null)));
        modesComboBox.setSelectedItem(currentMode);
        modesComboBox.setSelectedIndex(modesComboBox.getSelectedIndex());

        boolean wrap = prefs.getBoolean("wrapLines", true);
        inputTextArea.setLineWrap(wrap);
        inputTextArea.setWrapStyleWord(wrap);
        outputTextArea.setLineWrap(wrap);
        outputTextArea.setWrapStyleWord(wrap);
    }//GEN-LAST:event_settingsButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        /*try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ApertiumCaffeine.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ApertiumCaffeine.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ApertiumCaffeine.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ApertiumCaffeine.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }*/
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                ApertiumCaffeine jframe = new ApertiumCaffeine();
                jframe.setVisible(true);
                jframe.init();
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox displayMarksCheckBox;
    private javax.swing.JScrollPane inputScrollPane;
    private javax.swing.JTextArea inputTextArea;
    private javax.swing.JComboBox modesComboBox;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JButton settingsButton;
    // End of variables declaration//GEN-END:variables
}
