/*
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.scripteditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJInternalFrame;

/**
 * An editor for beanshell scripts with syntax highlighting.
 *
 * Uses RSyntaxTextArea by Robert Futrell. See http://fifesoft.com/rsyntaxtextarea/
 *
 * @author Jeff Yoshimi
 */
public class ScriptEditor extends JPanel {

    /** Default script directory. */
    private static final String DEFAULT_SCRIPT_DIRECTORY = "."
            + System.getProperty("file.separator") + "scripts"
            + System.getProperty("file.separator") + "scriptmenu";

    /** Script directory. */
    private String scriptDirectory = DEFAULT_SCRIPT_DIRECTORY;

    /** Reference to file (for saving). */
    private File scriptFile;

    /** Main text area. */
    private final RSyntaxTextArea textArea;

    /** Default width. */
    private final int DEFAULT_WIDTH = 70;

    /** Default height. */
    private final int DEFAULT_HEIGHT = 25;

    /** Memory for last search string used in find and replace. */
    private String lastSearch = "";

    /** Memory for last replace string used in find and replace. */
    private String lastReplace = "";


    /**
     * Construct the main frame.
     */
    public ScriptEditor() {
        textArea = new RSyntaxTextArea(DEFAULT_HEIGHT, DEFAULT_WIDTH);
        initPanel();
    }

    /**
     * Initialize the script editor panel with some initial text.
     *
     * @param initialText the initial text.
     */
    public ScriptEditor(String initialText) {
        textArea = new RSyntaxTextArea(DEFAULT_HEIGHT, DEFAULT_WIDTH);
        initPanel();
        setText(initialText);
        textArea.setCaretPosition(0);
    }

    /**
     * Initialize with initial text and a default directory.
     *
     * @param initialText initial text
     * @param fileDirectory initial file directory
     */
    public ScriptEditor(String initialText, String fileDirectory) {
        this(initialText);
        scriptDirectory = fileDirectory;
    }

    /**
     * Perform required initialization.
     */
    private void initPanel() {
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        final RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setFoldIndicatorEnabled(true);
        add(sp);

        // Force component to fill up parent panel
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Component component = e.getComponent();
                sp.setPreferredSize(new Dimension(component.getWidth(),
                        component.getHeight()));
                sp.revalidate();
            }
        });

    }

    /**
     * @return the textArea
     */
    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    /**
     * Sets text of the underlying text area.
     *
     * @param text text to set.
     */
    public void setText(final String text) {
        textArea.setText(text);
    }

    /**
     * Returns the main text (in the underlying text area).
     *
     * @return the displayed in the text area
     */
    public String getText() {
        return textArea.getText();
    }

    /**
     * @return the scriptFile
     */
    public File getScriptFile() {
        return scriptFile;
    }

    /**
     * @param scriptFile the scriptFile to set
     */
    public void setScriptFile(File scriptFile) {
        this.scriptFile = scriptFile;
    }

    // Methods for obtaining frames which contain a Script Editor panel.

    /**
     * Returns an internal frame for editing a script.
     *
     * @return internal frame
     */
    public GenericJInternalFrame getInternalFrame() {
        GenericJInternalFrame frame = new GenericJInternalFrame();
        initFrame(frame, new ScriptEditor());
        return frame;
    }

    /**
     * Returns a standard dialog for editing a script.
     *
     * @param editor the panel to display in the dialog
     * @return the dialog
     */
    public StandardDialog getDialog(final ScriptEditor editor) {
        StandardDialog dialog = new StandardDialog();
        initFrame(dialog, editor);
        return dialog;
    }

    /**
     * Initialize the frame with the provided panel.
     *
     * @param frame frame to initialize
     * @param editor the panel to display in the frame
     */
    private void initFrame(final GenericFrame frame,
            final ScriptEditor editor) {
        final JPanel mainPanel = new JPanel(new BorderLayout());
        createAttachMenuBar(frame, editor);
        mainPanel.add("North", getToolbarOpenClose(frame, editor));
        mainPanel.add("Center", editor);
        frame.setContentPane(mainPanel);
        if (editor.getScriptFile() != null) {
            frame.setTitle(editor.getScriptFile().getName());
        }
    }

    /**
     * Creates the menu bar.
     */
    protected void createAttachMenuBar(final GenericFrame frame,
            final ScriptEditor editor) {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setAction(getOpenScriptAction(frame, editor));
        fileMenu.add(openItem);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAction(getSaveScriptAction(frame, editor));
        fileMenu.add(saveItem);
        JMenuItem saveAsItem = new JMenuItem("Save");
        saveAsItem.setAction(getSaveScriptAsAction(frame, editor));
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                frame.dispose();
            }
        });
        fileMenu.add(closeItem);
        bar.add(fileMenu);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem findReplaceItem = new JMenuItem("Find / Replace...");
        findReplaceItem.setAction(getFindReplaceAction(frame, editor));
        editMenu.add(findReplaceItem);
        bar.add(editMenu);

        frame.setJMenuBar(bar);
    }

    /**
     * Return a toolbar with buttons for opening from and saving to .bsh files.
     *
     * @return the toolbar
     */
    protected JToolBar getToolbarOpenClose(final GenericFrame frame,
            final ScriptEditor editor) {
        JToolBar toolbar = new JToolBar();
        toolbar.add(getOpenScriptAction(frame, editor));
        toolbar.add(getSaveScriptAction(frame, editor));
        return toolbar;
    }

    /**
     * Returns the action for opening script files.
     */
    private Action getOpenScriptAction(final GenericFrame frame,
            final ScriptEditor editor) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
                putValue(NAME, "Open Script (.bsh)...");
                putValue(SHORT_DESCRIPTION, "Open");
                putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                        KeyEvent.VK_O, Toolkit.getDefaultToolkit()
                                .getMenuShortcutKeyMask()));
            }

            @Override
            public void actionPerformed(ActionEvent e) {

                SFileChooser fileChooser = new SFileChooser(
                        scriptDirectory, "Edit Script", "bsh");
                final File scriptFile = fileChooser.showOpenDialog();
                if (scriptFile == null) {
                    return;
                }

                frame.setTitle(scriptFile.getName());
                try {
                    BufferedReader r = new BufferedReader(new FileReader(
                            scriptFile));
                    editor.setScriptFile(scriptFile);
                    editor.getTextArea().read(r, null);
                    r.close();
                    editor.getTextArea().setCaretPosition(0);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }
            }

        };
    }

    /**
     * Returns the action for saving script files.
     */
    private Action getSaveScriptAction(final GenericFrame frame,
            final ScriptEditor editor) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.png"));
                putValue(SHORT_DESCRIPTION, "save");
                putValue(Action.NAME, "Save");
                putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                        KeyEvent.VK_S, Toolkit.getDefaultToolkit()
                                .getMenuShortcutKeyMask()));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                File scriptFile = editor.getScriptFile();
                if (scriptFile == null) {
                    SFileChooser fileChooser = new SFileChooser(
                            scriptDirectory, "Edit Script", "bsh");
                    scriptFile = fileChooser.showSaveDialog();
                    if (scriptFile == null) {
                        return;
                    }
                    editor.setScriptFile(scriptFile);
                    frame.setTitle(editor.getScriptFile().getName());
                }

                try {
                    BufferedWriter r = new BufferedWriter(new FileWriter(
                            scriptFile));
                    editor.getTextArea().write(r);
                    r.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }
            }

        };
    }

    /**
     * Returns the action for saving script files.
     */
    private Action getSaveScriptAsAction(final GenericFrame frame,
            final ScriptEditor editor) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SMALL_ICON, ResourceManager.getImageIcon("SaveAs.png"));
                putValue(SHORT_DESCRIPTION, "save");
                putValue(Action.NAME, "Save as...");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                SFileChooser fileChooser = new SFileChooser(
                        scriptDirectory, "Edit Script", "bsh");
                File scriptFile = fileChooser.showSaveDialog();
                if (scriptFile == null) {
                    return;
                }
                editor.setScriptFile(scriptFile);
                try {
                    BufferedWriter r = new BufferedWriter(new FileWriter(
                            scriptFile));
                    editor.getTextArea().write(r);
                    frame.setTitle(editor.getScriptFile().getName());
                    r.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    UIManager.getLookAndFeel().provideErrorFeedback(editor);
                }

            }

        };
    }

    /**
     * Returns the action for finding and replacing text
     */
    private Action getFindReplaceAction(final GenericFrame frame,
            final ScriptEditor editor) {
        return new AbstractAction() {

            // Initialize
            {
                putValue(SHORT_DESCRIPTION, "Find/Replace");
                putValue(Action.NAME, "Find/Replace...");
                putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                        KeyEvent.VK_F, Toolkit.getDefaultToolkit()
                                .getMenuShortcutKeyMask()));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                showFindReplaceDialog();
            }

        };
    }

    /**
     * Show the find/replace dialog
     */
    private void showFindReplaceDialog() {

        JFrame frame = new JFrame();
        FindReplaceDialog dialog = new FindReplaceDialog(frame, this);
        frame.setContentPane(dialog);
        frame.setTitle("Find / Replace");
        frame.setVisible(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /**
     * @return the lastSearch
     */
    public String getLastSearchString() {
        return lastSearch;
    }

    /**
     * @param lastSearch the lastSearch to set
     */
    public void setLastSearchString(String lastSearch) {
        this.lastSearch = lastSearch;
    }

    /**
     * @return the lastReplace
     */
    public String getLastReplaceString() {
        return lastReplace;
    }

    /**
     * @param lastReplace the lastReplace to set
     */
    public void setLastReplaceString(String lastReplace) {
        this.lastReplace = lastReplace;
    }


}
