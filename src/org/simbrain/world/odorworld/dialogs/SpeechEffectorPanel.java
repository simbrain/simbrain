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
package org.simbrain.world.odorworld.dialogs;

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
    private JTextField phrase = new JTextField();

    /** Text field to edit threshold above which effector is activated. */
    private JTextField threshold = new JTextField();

    /** Entity to which a speech effector is being added. */
    private RotatingEntity entity;

    /** Maximum characters currently allowed for a phrase. */
    private static final int MAX_PHRASE_LENGTH = 10;

    /**
     * Reference to speech effector. Initially null if this is a creation panel.
     */
    private Speech speechEffector;

    /** If true this is a creation panel. Otherwise it is an edit panel. */
    private boolean isCreationPanel;

    /**
     * Constructor for the case where an effector is being created.
     *
     * @param entity the entity to which a speech effector is added.
     */
    public SpeechEffectorPanel(final OdorWorldEntity entity) {
        this.entity = (RotatingEntity) entity;
        isCreationPanel = true;
        addItem("Utterance", phrase);
        addItem("Threshold", threshold);
        fillFieldValues();
    }

    /**
     * Constructor for the case where an effector is being edited.
     *
     * @param entity parent entity
     * @param effector effector to edit
     */
    public SpeechEffectorPanel(final OdorWorldEntity entity,
            final Speech effector) {
        this.entity = (RotatingEntity) entity;
        this.speechEffector = effector;
        isCreationPanel = false;
        addItem("Utterance", phrase);
        addItem("Threshold", threshold);
        fillFieldValues();
    }

    @Override
    public void commitChanges() {
        if (isCreationPanel) {
            entity.addEffector(new Speech(entity, phrase.getText(), Double
                    .parseDouble(threshold.getText())));
            if (phrase.getText().length() > MAX_PHRASE_LENGTH) {
                checkPhrase();
            }

        } else {
            speechEffector.setPhrase(phrase.getText());
            speechEffector.setLabel("Say: \"" + phrase.getText() + "\"");
            speechEffector
                    .setThreshold(Double.parseDouble(threshold.getText()));
            speechEffector.getParent().getParentWorld()
                    .fireEntityChanged(speechEffector.getParent());
            if (phrase.getText().length() > MAX_PHRASE_LENGTH) {
                checkPhrase();
            }

        }
    }

    @Override
    protected void fillFieldValues() {
        if (isCreationPanel) {
            phrase.setText("" + Speech.DEFAULT_PHRASE);
            threshold.setText("" + Speech.DEFAULT_THRESHOLD);
        } else {
            phrase.setText("" + speechEffector.getPhrase());
            threshold.setText("" + speechEffector.getThreshold());
        }
    }

    /** Displays message when utterance is above MAX_PHRASE_LENGTH char. */
    private void checkPhrase() {
        JOptionPane.showOptionDialog(null, "Speech utterance is greater than"
                + MAX_PHRASE_LENGTH
                + "chars! Not guaranteed to render correctly.", "Warning",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                null, null);
    }
}
