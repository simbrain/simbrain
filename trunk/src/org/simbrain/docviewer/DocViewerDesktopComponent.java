/*
 * Part of Simbrain--a java-based neural network kit
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
package org.simbrain.docviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.workspace.component_actions.CloseAction;
import org.simbrain.workspace.component_actions.OpenAction;
import org.simbrain.workspace.component_actions.SaveAction;
import org.simbrain.workspace.component_actions.SaveAsAction;
import org.simbrain.workspace.gui.GuiComponent;

/**
 * A very simple component which displays html and allows it to be edited. Uses
 * a JEditorPane to display html and an RSSyntaxTextArea to edit it.
 */
public class DocViewerDesktopComponent extends GuiComponent<DocViewerComponent> {

    /** Main text area. */
    private final JEditorPane textArea = new JEditorPane();

    /** Menu Bar. */
    private JMenuBar menuBar = new JMenuBar();

    /** File menu for saving and opening world files. */
    private JMenu file = new JMenu("File");

    /** Main text area. */
    private final RSyntaxTextArea htmlEditor = new RSyntaxTextArea();

    /**
     * Constructor the gui component.
     */
    public DocViewerDesktopComponent(GenericFrame frame,
            DocViewerComponent component) {
        super(frame, component);
        setPreferredSize(new Dimension(500, 400));
        setLayout(new BorderLayout());

        // File Menu
        menuBar.add(file);
        file.add(new OpenAction(this));
        file.add(new SaveAction(this));
        file.add(new SaveAsAction(this));
        file.addSeparator();
        JMenuItem item = new JMenuItem("Import html...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser _fileChooser = new JFileChooser();
                int retval = _fileChooser.showOpenDialog(textArea);
                if (retval == JFileChooser.APPROVE_OPTION) {
                    File f = _fileChooser.getSelectedFile();
                    try {
                        InputStream fis;
                        BufferedReader br;
                        fis = new FileInputStream(f);
                        br = new BufferedReader(new InputStreamReader(fis,
                                Charset.forName("UTF-8")));
                        htmlEditor.read(br, null);
                        textArea.setText(htmlEditor.getText());
                        DocViewerDesktopComponent.this.getWorkspaceComponent()
                                .setText(htmlEditor.getText());
                        htmlEditor.setCaretPosition(0);
                    } catch (IOException ioex) {
                        System.out.println(e);
                    }
                }
            }
        });
        file.add(item);
        file.addSeparator();
        file.add(new CloseAction(this.getWorkspaceComponent()));

        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("Help");
        Action helpAction = new ShowHelpAction("Pages/DocEditor.html");
        helpItem.setAction(helpAction);
        helpMenu.add(helpItem);
        menuBar.add(helpMenu);

        getParentFrame().setJMenuBar(menuBar);

        textArea.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        textArea.setContentType("text/html");
        textArea.setEditable(false);
        textArea.setText(((DocViewerComponent) this.getWorkspaceComponent())
                .getText());

        final JScrollPane sp = new JScrollPane(textArea);

        JTabbedPane tabs = new JTabbedPane();
        htmlEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
        htmlEditor.setCodeFoldingEnabled(true);
        htmlEditor.setAntiAliasingEnabled(true);
        final RTextScrollPane sp2 = new RTextScrollPane(htmlEditor);
        sp2.setFoldIndicatorEnabled(true);
        add(sp2);

        tabs.addTab("View", sp);
        tabs.addTab("Edit", sp2);

        add("Center", tabs);

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

        // Listen for tab changed events. Synchronize the editor and the
        // display tabs on these events.
        // TODO: listen for changes in the editor and only update the display
        // when changes occur
        ChangeListener changeListener = new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent
                        .getSource();
                int index = sourceTabbedPane.getSelectedIndex();
                // Assumes index of view tab is 0
                if (index == 0) {
                    textArea.setText(htmlEditor.getText());
                    DocViewerDesktopComponent.this.getWorkspaceComponent()
                            .setText(htmlEditor.getText());
                }
            }
        };
        tabs.addChangeListener(changeListener);

        // Respond to clicks on hyper-links by opening a web page in the defautl
        // browser
        HyperlinkListener l = new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    try {
                        if (e.getURL() != null) {
                            System.out.println(e.getURL().toURI());
                            Desktop.getDesktop().browse(e.getURL().toURI());
                        }
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (URISyntaxException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }

            }

        };
        textArea.addHyperlinkListener(l);
    }

    @Override
    public void postAddInit() {
        htmlEditor.setText(this.getWorkspaceComponent().getText());
        textArea.setCaretPosition(0);
    }

    @Override
    public void closing() {
    }

    @Override
    protected void update() {
    }
}
