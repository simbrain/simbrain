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

import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Panel to add a speech effector to an entity.
 *
 * @author Lam Nguyen
 *
 */
public class SpeechEffectorPanel extends AbstractEffectorPanel {

    /** Text field to edit uttered phrase. */
    private JTextField phrase = new JTextField("Hi!");

    /** Text field to edit threshold above which effector is activated. */
    private JTextField threshold = new JTextField("" + 0.1);

    /** Entity to which a speech effector is being added. */
    private RotatingEntity entity;

    /**
     * Default constructor.
     *
     * @param entity the entity to which a speech effector is added.
     */
    public SpeechEffectorPanel(OdorWorldEntity entity) {
        this.entity = (RotatingEntity) entity;
        addItem("Utterance", phrase);
        addItem("Threshold", threshold);
        setVisible(true);
    }

    @Override
    public void commitChanges() {
        entity.addEffector(new Speech(entity, phrase.getText(), Double.parseDouble(threshold.getText())));
        if (phrase.getText().length() > 10) {
            checkPhrase();
        }
    }

    /** Save changes to an edited speech effector. */
    public void commitChanges(Speech effector) {
        effector.setPhrase(phrase.getText());
        effector.setLabel("Say: \"" + phrase.getText() + "\"");
        effector.setThreshold(Double.parseDouble(threshold.getText()));
        effector.getParent().getParentWorld()
        .fireEntityChanged(effector.getParent());
        if (phrase.getText().length() > 10) {
            checkPhrase();
        }
    }

    /** Fill in appropriate text fields when speech effector is being modified. */
    public void fillFieldValues(Speech effector) {
        phrase.setText("" + effector.getPhrase());
        threshold.setText("" + effector.getThreshold());
    }

    /** Displays message when utterance is above 10 char. */
    private void checkPhrase() {
        JOptionPane.showOptionDialog(null, "Speech utterance is greater than 10 chars! Not guaranteed to render correctly.", "Warning",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, null, null);
    }
}
