/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
import java.awt.Dialog;
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
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * <b>StandardDialog</b> implements a standard data entry dialog with "Ok" and
 * "Cancel" buttons. Subclasses can override the isDataValid(),
 * okButtonPressed(), and cancelButtonPressed() methods to perform
 * implementation specific processing.
 * <P>
 * By default, the dialog is modal, and has a JPanel with a
 * BorderLayout for its content pane.
 *
 * @author David Fraser
 * @author Michael Harris
 */
public class StandardDialog extends JDialog
{
    // Constants
    
	JPanel customButtonPanel = new JPanel();
	

	ActionListener actionListener = new ActionListener() {

	  public void actionPerformed(ActionEvent actionEvent) {

	     setVisible(false);

	  }
	};
	
    /** The spacing between components in pixels */
    private static final int COMPONENT_SPACING = 10;

    // Attributes

    /** Flag indicating if the "Cancel" button was pressed to close dialog */
    private boolean myIsDialogCancelled = true;

    /** The content pane for holding user components */
    private Container myUserContentPane;

    // Methods

    /**
     * This method is the default constructor.
     */
    public StandardDialog()
    {
        init();
    }

    /**
     * This method creates a StandardDialog with the given parent frame
     * and title.
     *
     * @param parent The parent frame for the dialog.
     * @param title The title to display in the dialog.
     */
    public StandardDialog(Frame parent, String title)
    {
        super(parent, title);

        init();
    }

    /**
     * This method creates a StandardDialog with the given parent dialog
     * and title.
     *
     * @param parent The parent dialog for the dialog.
     * @param title The title to display in the dialog.
     */
    public StandardDialog(Dialog parent, String title)
    {
        super(parent, title);

        init();
    }
    
    public void commit() {
    }
    public void returnToCurrentPrefs() {
    }
    public void setAsDefault() {
    }

    /**
     * This method sets up the default attributes of the dialog and
     * the content pane.
     */
    private void init()
    {
    	    setModal(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        

        // Setup the internal content pane to hold the user content pane
        // and the standard button panel


        JPanel internalContentPane = new JPanel();

        internalContentPane.setLayout(
            new BorderLayout(COMPONENT_SPACING, COMPONENT_SPACING));

        internalContentPane.setBorder(
            BorderFactory.createEmptyBorder(COMPONENT_SPACING,
                COMPONENT_SPACING, COMPONENT_SPACING, COMPONENT_SPACING));

        // Create the standard button panel with "Ok" and "Cancel"

        Action okAction = new AbstractAction("OK")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                if(isValidData())
                {
                    myIsDialogCancelled = false;

                    dispose();
                }
            }
        };

        Action cancelAction = new AbstractAction("Cancel")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                myIsDialogCancelled = true;

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

        // Initialise the user content pane with a JPanel

        setContentPane(new JPanel(new BorderLayout()));

        super.setContentPane(internalContentPane);

        // Finally, add a listener for the window close button.
        // Process this event the same as the "Cancel" button.

        WindowAdapter windowAdapter = new WindowAdapter()
        {
            public void windowClosing(WindowEvent windowEvent)
            {
                myIsDialogCancelled = true;

                dispose();
            }
        };

        addWindowListener(windowAdapter);
                
        this.getRootPane().registerKeyboardAction(actionListener,KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),0);
        
    }

    /**
     * This method gets the content pane for adding components.
     * Components should not be added directly to the dialog.
     *
     * @return the content pane for the dialog.
     */
    public Container getContentPane()
    {
        return myUserContentPane;
    }

    /**
     * This method sets the content pane for adding components.
     * Components should not be added directly to the dialog.
     *
     * @param contentPane The content pane for the dialog.
     */
    public void setContentPane(Container contentPane)
    {
        myUserContentPane = contentPane;

        super.getContentPane().add(myUserContentPane, BorderLayout.CENTER);
    }

    /**
     * This method returns <code>true</code> if the User cancelled
     * the dialog otherwise <code>false</code>. The dialog is cancelled
     * if the "Cancel" button is pressed or the "Close" window button is
     * pressed, or the "Escape" key is pressed. In other words, if the
     * User has caused the dialog to close by any method other than by
     * pressing the "Ok" button, this method will return <code>true</code>.
     */
    public boolean hasUserCancelled()
    {
        return myIsDialogCancelled;
    }

    /**
     * This method is used to validate the current dialog box. This method
     * provides a default response of <code>true</code>. This method should be
     * implemented by each dialog that extends this class.
     *
     * @return a boolean indicating if the data is valid.
     * <code>true</code> indicates that all of the fields were validated
     * correctly and <code>false</code> indicates the validation failed
     */
    protected boolean isValidData()
    {
        return true;
    }
    
    public void addButton(JButton theButton) {
    	customButtonPanel.add(theButton);
    	
    }

}
