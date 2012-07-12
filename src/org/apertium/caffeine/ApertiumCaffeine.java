/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ApertiumCaffeine.java
 *
 * Created on Jul 9, 2012, 1:44:57 PM
 */
package org.apertium.caffeine;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
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
import javax.swing.JOptionPane;
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
    
    protected static final Preferences prefs = Preferences.userNodeForPackage(Translator.class);
    
    private HashMap<String, String> titleToBase;
    private HashMap<String, String> titleToMode;

    /** Creates new form ApertiumCaffeine */
    public ApertiumCaffeine() {
        initComponents();
        
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
                    + "\"Apertium packages\" in your home directory) or select a custom one.\n",
                    "Welcome to Apertium Caffeine!",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                packagesDir = new File(new File(System.getProperty("user.home")), "Apertium packages");
                packagesDir.mkdir();
                prefs.put("packagesPath", packagesDir.getPath());
            } else if (answer == 1) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setApproveButtonText("OK");
                fc.setDialogTitle("Choose a directory");
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    packagesDir = fc.getSelectedFile();
                    prefs.put("packagesPath", packagesDir.getPath());
                }
            } else {
                System.exit(0);
            }
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
        
        boolean displayMarks = prefs.getBoolean("displayMarks", false);
        boolean displayAmbiguity = prefs.getBoolean("displayAmbiguity", false);
        displayMarksCheckBox.setSelected(displayMarks);
        displayAmbiguityCheckBox.setSelected(displayAmbiguity);
        Translator.setDisplayMarks(displayMarks);
        Translator.setDisplayAmbiguity(displayAmbiguity);
        
        boolean wrap = prefs.getBoolean("wrapLines", true);
        inputTextArea.setLineWrap(wrap);
        inputTextArea.setWrapStyleWord(wrap);
        outputTextArea.setLineWrap(wrap);
        outputTextArea.setWrapStyleWord(wrap);
        
        Translator.setCacheEnabled(true);
        
        inputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {update();}
            public void removeUpdate(DocumentEvent e) {update();}
            public void changedUpdate(DocumentEvent e) {update();}
        });
        inputTextArea.setText(prefs.get("inputText", ""));
        
        int x = prefs.getInt("boundsX", -1);
        int y = prefs.getInt("boundsY", -1);
        int width = prefs.getInt("boundsWidth", -1);
        int height = prefs.getInt("boundsHeight", -1);
        if (height > 0 && width > 0) setBounds(x, y, width, height);
        else setLocationRelativeTo(null);
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                prefs.put("inputText", inputTextArea.getText());
                prefs.putInt("modesComboBox", modesComboBox.getSelectedIndex());
                prefs.putBoolean("displayMarks", displayMarksCheckBox.isSelected());
                prefs.putBoolean("displayAmbiguity", displayAmbiguityCheckBox.isSelected());
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
    
    private static final HighlightPainter redPainter = new DefaultHighlightPainter(Color.RED);
    private static final HighlightPainter orangePainter = new DefaultHighlightPainter(Color.ORANGE);
    private static final HighlightPainter greenPainter = new DefaultHighlightPainter(Color.GREEN);
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
                        String translation = Translator.translate(inputTextArea.getText());
                        boolean unknown = displayMarksCheckBox.isSelected();
                        boolean ambiguity = displayAmbiguityCheckBox.isSelected();
                        boolean highlight = prefs.getBoolean("highlightMarkedWords", true);
                        boolean hide = prefs.getBoolean("hideMarks", true);
                        if (highlight && (unknown || ambiguity)) {
                            String pattern;
                            if (unknown && ambiguity) pattern = "(\\*|#|@|=)";
                            else if (unknown) pattern = "(\\*|#|@)";
                            else pattern = "=";
                            outputTextArea.setText(hide ? Pattern.compile("\\B" + pattern + "\\b").matcher(translation).replaceAll("") : translation);
                            int offset = 0;
                            Matcher matcher = Pattern.compile("\\B" + pattern + "(\\p{L}||\\p{N})*\\b").matcher(translation);
                            while (matcher.find()) {
                                HighlightPainter painter = null;
                                if (translation.charAt(matcher.start()) == '*')
                                    painter = redPainter;
                                else if (translation.charAt(matcher.start()) == '#')
                                    painter = orangePainter;
                                else if (translation.charAt(matcher.start()) == '@')
                                    painter = orangePainter;
                                else if (translation.charAt(matcher.start()) == '=')
                                    painter = greenPainter;
                                outputTextArea.getHighlighter().addHighlight(matcher.start() + (hide ? offset-- : offset), matcher.end() + offset, painter);
                            }
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
        File packages[] = packagesDir.listFiles();
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
        displayAmbiguityCheckBox = new javax.swing.JCheckBox();
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

        displayAmbiguityCheckBox.setText("Mark ambiguity");
        displayAmbiguityCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayAmbiguityCheckBoxActionPerformed(evt);
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
                        .addComponent(modesComboBox, 0, 186, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(displayMarksCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(displayAmbiguityCheckBox)
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
                    .addComponent(displayMarksCheckBox)
                    .addComponent(displayAmbiguityCheckBox))
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

    private void displayAmbiguityCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayAmbiguityCheckBoxActionPerformed
        Translator.setDisplayAmbiguity(displayAmbiguityCheckBox.isSelected());
        update();
    }//GEN-LAST:event_displayAmbiguityCheckBoxActionPerformed

    private void settingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsButtonActionPerformed
        Object currentMode = modesComboBox.getSelectedItem();
        ManageDialog md = new ManageDialog(this, true);
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
        /*try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ApertiumCaffeine.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
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
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new ApertiumCaffeine().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox displayAmbiguityCheckBox;
    private javax.swing.JCheckBox displayMarksCheckBox;
    private javax.swing.JScrollPane inputScrollPane;
    private javax.swing.JTextArea inputTextArea;
    private javax.swing.JComboBox modesComboBox;
    private javax.swing.JScrollPane outputScrollPane;
    private javax.swing.JTextArea outputTextArea;
    private javax.swing.JButton settingsButton;
    // End of variables declaration//GEN-END:variables
}