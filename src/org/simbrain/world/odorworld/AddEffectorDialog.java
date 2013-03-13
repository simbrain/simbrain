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
 * effectorDialog is a dialog box for adding effectors to Odor World.
 */

public class AddEffectorDialog extends StandardDialog implements ActionListener {

	/** String of effector types. */

	String[] effectors = { "StraightMovement", "Turning"};

	private OdorWorldEntity entity;

	private RotatingEntity rotatingEntity;

	/** Select effector type. */
	private JComboBox effectorType = new JComboBox(effectors);

	private AbstractEffectorPanel currentEffectorPanel;

	/** Main dialog box. */
	private Box mainPanel = Box.createVerticalBox();

	/** Panel for setting effector type. */
	private LabelledItemPanel typePanel = new LabelledItemPanel();

	/** effector Dialog add effector constructor. */
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
			currentEffectorPanel = new StraightEffectorPanel(rotatingEntity);
			mainPanel.add(currentEffectorPanel);
		} else if (effectorType.getSelectedItem() == "Turning") {
			cleareffectorPanel();
			currentEffectorPanel = new TurningEffectorPanel(rotatingEntity);
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
