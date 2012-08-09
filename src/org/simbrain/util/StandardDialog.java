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
package org.simbrain.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.simbrain.util.genericframe.GenericJDialog;

/**
 * <b>StandardDialog</b> implements a standard data entry dialog with "Ok" and
 * "Cancel" buttons. Special functionality associated with Simbrain has also
 * been added. Subclasses can override the isDataValid(), okButtonPressed(), and
 * cancelButtonPressed() methods to perform implementation specific processing.
 * <P>
 * By default, the dialog is modal, and has a JPanel with a BorderLayout for its
 * content pane.
 * </p>
 *
 * @author David Fraser
 * @author Michael Harris
 */
public class StandardDialog extends GenericJDialog {

    /**
     * When this flag is set to true, then whenever a StandardDialog is used, a
     * warning will be displayed first, notifying the user that an external
     * simulation is running.
     */
    private static boolean isRunning;

    /**
     * Use this flag to disable the use of run warnings (see isRunning)
     */
    private static final boolean USE_RUN_WARNINGS = true;

    /** Custom button panel. */
    private JPanel customButtonPanel = new JPanel();

    /** Action listener. */
    ActionListener actionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent actionEvent) {
            setVisible(false);
        }
    };

    /** The spacing between components in pixels. */
    private static final int COMPONENT_SPACING = 10;

    /** Flag indicating if the "Cancel" button was pressed to close dialog. */
    private boolean myIsDialogCancelled = true;

    /** The content pane for holding user components. */
    private Container myUserContentPane;

    /**
     * This method is the default constructor.
     */
    public StandardDialog() {
        super();
        init();
    }

    /**
     * This method creates a StandardDialog with the given parent frame and
     * title.
     *
     * @param parent The parent frame for the dialog.
     * @param title The title to display in the dialog.
     */
    public StandardDialog(final Frame parent, final String title) {
        super(parent, title);

        init();
    }

    /**
     * Commit any changes made.
     */
    public void commit() {
    }

    /**
     * Returns fields changed to current preferences.
     */
    public void returnToCurrentPrefs() {
    }

    /**
     * Sets changed fields as current preferences.
     */
    public void setAsDefault() {
    }

    /**
     * This method sets up the default attributes of the dialog and the content
     * pane.
     */
    private void init() {

        if (isRunning && USE_RUN_WARNINGS) {
            JOptionPane
                    .showMessageDialog(
                            null,
                            "WARNING: You are modifying system parameters while a simulation is running. \n "
                                    + "It is reccomended that you first stop the simulation using the stop button.\n"
                                    + " Some functions may not behave as they are supposed to.",
                            "Warning!", JOptionPane.WARNING_MESSAGE);
        }

        setModal(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Setup the internal content pane to hold the user content pane
        // and the standard button panel
        JPanel internalContentPane = new JPanel();

        internalContentPane.setLayout(new BorderLayout(COMPONENT_SPACING,
                COMPONENT_SPACING));

        internalContentPane.setBorder(BorderFactory.createEmptyBorder(
                COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING,
                COMPONENT_SPACING));

        // Create the standard button panel with "Ok" and "Cancel"
        Action okAction = new AbstractAction("OK") {
            public void actionPerformed(final ActionEvent actionEvent) {
                if (isValidData()) {
                    myIsDialogCancelled = false;
                    closeDialogOk();
                }
            }
        };

        Action cancelAction = new AbstractAction("Cancel") {
            public void actionPerformed(final ActionEvent actionEvent) {
                myIsDialogCancelled = true;
                closeDialogCancel();
                dispose();
            }
        };

        JPanel buttonPanel = new JPanel();

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JButton okButton = new JButton(okAction);
        buttonPanel.add(customButtonPanel);
        buttonPanel.add(okButton);
        buttonPanel.add(new JButton(cancelAction));

        getRootPane().setDefaultButton(okButton);

        internalContentPane.add(buttonPanel, BorderLayout.SOUTH);

        // Initialize the user content pane with a JPanel
        setContentPane(new JPanel(new BorderLayout()));

        super.setContentPane(internalContentPane);

        // Finally, add a listener for the window close button.
        // Process this event the same as the "Cancel" button.
        WindowAdapter windowAdapter = new WindowAdapter() {
            public void windowClosing(final WindowEvent windowEvent) {
                myIsDialogCancelled = true;
                closeDialogCancel();
            }
        };

        addWindowListener(windowAdapter);

        this.getRootPane().registerKeyboardAction(
                actionListener,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit
                        .getDefaultToolkit().getMenuShortcutKeyMask()), 0);

        // this.setAlwaysOnTop(true); //BREAKS SOME VERSIONS
        pack();

    }

    /**
     * Overrideen to perform specific clean up when dialog closed.
     */
    protected void closeDialogOk() {
        dispose();
    }

    /**
     * Overriden to perform specific clean up when dialog closed.
     */
    protected void closeDialogCancel() {
        dispose();
    }

    /**
     * This method gets the content pane for adding components. Components
     * should not be added directly to the dialog.
     *
     * @return the content pane for the dialog.
     */
    public Container getContentPane() {
        return myUserContentPane;
    }

    /**
     * This method sets the content pane for adding components. Components
     * should not be added directly to the dialog.
     *
     * @param contentPane The content pane for the dialog.
     */
    public void setContentPane(final Container contentPane) {
        myUserContentPane = contentPane;

        super.getContentPane().add(myUserContentPane, BorderLayout.CENTER);
    }

    /**
     * This method returns <code>true</code> if the User cancelled the dialog
     * otherwise <code>false</code>. The dialog is cancelled if the "Cancel"
     * button is pressed or the "Close" window button is pressed, or the
     * "Escape" key is pressed. In other words, if the User has caused the
     * dialog to close by any method other than by pressing the "Ok" button,
     * this method will return <code>true</code>.
     */
    public boolean hasUserCancelled() {
        return myIsDialogCancelled;
    }

    /**
     * This method is used to validate the current dialog box. This method
     * provides a default response of <code>true</code>. This method should be
     * implemented by each dialog that extends this class.
     *
     * @return a boolean indicating if the data is valid. <code>true</code>
     *         indicates that all of the fields were validated correctly and
     *         <code>false</code> indicates the validation failed
     */
    protected boolean isValidData() {
        return true;
    }

    /**
     * Adds a new button to the panel.
     *
     * @param theButton button to be added
     */
    public void addButton(final JButton theButton) {
        customButtonPanel.add(theButton);
    }

    /**
     * @param isRunning the isRunning to set
     */
    public static void setSimulationRunning(boolean isRunning) {
        StandardDialog.isRunning = isRunning;
    }

    /**
     * Center the dialog on the screen.
     */
    public void centerDialog() {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Dimension screenSize = toolkit.getScreenSize();
        final int x = (screenSize.width - getWidth()) / 2;
        final int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
    }
}
