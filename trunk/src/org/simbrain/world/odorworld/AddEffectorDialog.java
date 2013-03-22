/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.world.odorworld;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.ShowHelpAction;
import org.simbrain.util.StandardDialog;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * EffectorDialog is a dialog box for adding effectors to Odor World.
 *
 * @author Lam Nguyen
 *
 */

public class AddEffectorDialog extends StandardDialog implements ActionListener {

	/** String of effector types. */
	private String[] effectors = {"StraightMovement", "Turning", "Speech"};

	/** Entity to which effector is being added. */
	private OdorWorldEntity entity;

	/** Instantiated entity to which effector is being added. */
	private RotatingEntity rotatingEntity;

	/** Select effector type. */
	private JComboBox effectorType = new JComboBox(effectors);

	/** Panel that changes to a specific effector panel. */
	private AbstractEffectorPanel currentEffectorPanel;

	/** Main dialog box. */
	private Box mainPanel = Box.createVerticalBox();

	/** Panel for setting effector type. */
	private LabelledItemPanel typePanel = new LabelledItemPanel();

	/** Effector Dialog add effector constructor. */
	public AddEffectorDialog(OdorWorldEntity entity) {
		this.entity = entity;
		this.rotatingEntity = (RotatingEntity) entity;
		init("Add effector");
	}

	/**
	 * Initialize default constructor.
	 */
	private void init(String title) {
		setTitle(title);
		effectorType.addActionListener(this);
        typePanel.addItem("Effector Type", effectorType);
        effectorType.setSelectedItem("SmellEffector");
        ShowHelpAction helpAction = new ShowHelpAction(
                "Pages/Worlds/OdorWorld/OdorWorld.html"); // todo: put in specific page
        addButton(new JButton(helpAction));
        initPanel();
        mainPanel.add(typePanel);
        mainPanel.add(currentEffectorPanel);
        setContentPane(mainPanel);
	}

	@Override
	protected void closeDialogOk() {
		super.closeDialogOk();
		commitChanges();
	}

	/**
	 * Initialize the effector Dialog Panel based upon the current effector type.
	 */
	private void initPanel() {
		if (effectorType.getSelectedItem() == "StraightMovement") {
			cleareffectorPanel();
			setTitle("Add a straight movement effector");
			currentEffectorPanel = new StraightEffectorPanel(rotatingEntity);
			mainPanel.add(currentEffectorPanel);
		} else if (effectorType.getSelectedItem() == "Turning") {
			cleareffectorPanel();
			setTitle("Add a turning effector");
			currentEffectorPanel = new TurningEffectorPanel(rotatingEntity);
			mainPanel.add(currentEffectorPanel);
		} else if (effectorType.getSelectedItem() == "Speech") {
            cleareffectorPanel();
            setTitle("Add a speech effector");
            currentEffectorPanel = new SpeechEffectorPanel(rotatingEntity);
            mainPanel.add(currentEffectorPanel);
        }
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Remove current panel, if any.
	 */
	private void cleareffectorPanel() {
		if (currentEffectorPanel != null) {
			mainPanel.remove(currentEffectorPanel);
		}
	}

	/**
	 *
	 * @param e Action event.
	 */
	public void actionPerformed(final ActionEvent e) {
		initPanel();
	}

	/**
	 * Called externally when the dialog is closed, to commit any changes made.
	 */
	public void commitChanges() {
		currentEffectorPanel.commitChanges();
	}
}
