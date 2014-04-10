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
package org.simbrain.util.randomizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.Utils;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.widgets.LabelledItem;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * A panel representing a given probability distribution.
 * 
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class ProbDistPanel {

    /** TODO: REALLY need to find somewhere (<b>one place</b>) to put this....*/
    public static final String NULL_STRING = "..."; 

    /** The default number of spaces given to each text field. */
    private static final int DEFAULT_TF_SIZE = 20;

    /** Upper bound field. */
    private JTextField tfUpBound = new JTextField(DEFAULT_TF_SIZE);

    /** Lower bound field. */
    private JTextField tfLowBound = new JTextField(DEFAULT_TF_SIZE);

    /** Mean value field. */
    private JTextField tfParam1 = new JTextField(DEFAULT_TF_SIZE);

    /** Standard deviation field. */
    private JTextField tfParam2 = new JTextField(DEFAULT_TF_SIZE);

    /** Clipping combo box. */
    private TristateDropDown tsClipping =
            new TristateDropDown();

    /** The panel where all items are placed. */
    private JPanel mainPanel = new JPanel();

    /** The probability distribution this panel supports. */
    private final ProbDistribution pdf;

    /**
     * Creates a panel within this class that is globally accessible
     * representing an editor for a randomizer with a specific probability
     * distribution.
     * @param pdf the probability distribution the main panel will represent
     */
    public ProbDistPanel(ProbDistribution pdf) {
        this.pdf = pdf;
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(new LabelledItem (pdf.getParam1Name(), tfParam1));
        if (pdf.getParam2Name() != null) {
            mainPanel.add(new LabelledItem(pdf.getParam2Name(), tfParam2));
        }
        if (!pdf.equals(ProbDistribution.UNIFORM)) {
            mainPanel.add(new LabelledItem("Ceiling", tfUpBound));
            mainPanel.add(new LabelledItem("Floor", tfLowBound));

            tsClipping.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tfUpBound.setEnabled(tsClipping.isSelected());
                    tfLowBound.setEnabled(tsClipping.isSelected());
                }

            });
            mainPanel.add(new LabelledItem("Clipping", tsClipping));
        } else {
            mainPanel.add(Box.createVerticalStrut(5
                    * (tfUpBound.getPreferredSize().height + 6)));
        }
        fillDefaultValues();
    }

    protected ProbDistPanel() {
        pdf = null;
    }

    /**
     * Populates the fields with current values.
     *
     * @param randomizers List of randomizers
     */
    public void fillFieldValues(final ArrayList<Randomizer> randomizers) {
        Randomizer rand = (Randomizer) randomizers.get(0);
        if (randomizers.size() == 1) {
            fillFieldValues(rand);
            return;
        }

        if (NetworkUtils.isConsistent(randomizers,
                Randomizer.class, "getParam1")) {
            tfParam1.setText(Double.toString(rand.getParam1()));
        } else {
            tfParam1.setText(NULL_STRING);
        }

        if (NetworkUtils.isConsistent(randomizers,
                Randomizer.class, "getParam2")) {
            tfParam2.setText(Double.toString(rand.getParam2()));
        } else {
            tfParam2.setText(NULL_STRING);
        }

        if (!pdf.equals(ProbDistribution.UNIFORM)) {
            if (NetworkUtils.isConsistent(randomizers,
                    Randomizer.class, "getLowerBound")) {
                tfLowBound.setText(Double.toString(rand.getLowerBound()));
            } else {
                tfLowBound.setText(NULL_STRING);
            }
            if (NetworkUtils.isConsistent(randomizers, Randomizer.class,
                    "getUpperBound")) {
                tfUpBound.setText(Double.toString(rand.getUpperBound()));
            } else {
                tfUpBound.setText(NULL_STRING);
            }
            if (NetworkUtils.isConsistent(randomizers, Randomizer.class,
                    "getClipping")) {
                tsClipping.setSelected(rand.getClipping());
            } else {
                tsClipping.setNull();
            }
        }
    }

    /**
     * Fills fields with values from a Random Source.
     *
     * @param rand
     */
    public void fillFieldValues(Randomizer rand) {
        if (!rand.getPdf().equals(pdf)) {
            throwBadPdfException();
        }
        tsClipping.setSelected(rand.getClipping());
        tfLowBound.setText(Double.toString(rand.getLowerBound()));
        tfUpBound.setText(Double.toString(rand.getUpperBound()));
        tfParam1.setText(Double.toString(rand.getParam1()));
        tfParam2.setText(Double.toString(rand.getParam2()));
        tfLowBound.setEnabled(rand.getClipping());
        tfUpBound.setEnabled(rand.getClipping());
    }

    /**
     * Fills fields with default values.
     */
    public void fillDefaultValues() {
        tsClipping.setSelected(false);
        tfLowBound.setText(Double.toString(pdf.getDefaultLowBound()));
        tfUpBound.setText(Double.toString(pdf.getDefaultUpBound()));
        tfParam1.setText(Double.toString(pdf.getDefaultParam1()));
        tfParam2.setText(Double.toString(pdf.getDefaultParam2()));
        tfLowBound.setEnabled(false);
        tfUpBound.setEnabled(false);
    }

    /**
     * Called externally when dialog is being closed.
     *
     * @param rand Random source
     */
    public void commitRandom(final Randomizer rand) {
        rand.setPdf(pdf);

        double param1 = Utils.doubleParsable(tfParam1);
        if (tfParam1.isEnabled() && !Double.isNaN(param1)) {
            rand.setParam1Consistent(pdf.getParam1Name(), param1);
        }

        double param2 = Utils.doubleParsable(tfParam2);
        if (tfParam2.isEnabled() && !Double.isNaN(param2)) {
            rand.setParam2Consistent(pdf.getParam2Name(), param2);
        }
        if (!tsClipping.isNull()) {
            rand.setClipping(tsClipping.isSelected());
            if (tsClipping.isSelected()) {
                double upperBound = Utils.doubleParsable(tfUpBound);
                if (!Double.isNaN(upperBound)) {
                    rand.setUpperBound(upperBound);
                }
                double lowerBound = Utils.doubleParsable(tfLowBound);
                if (!Double.isNaN(lowerBound)) {
                    rand.setLowerBound(lowerBound);
                }
            }

        }
    }

    /**
     * Enable or disable the upper and lower bounds fields depending on state of
     * rounding button.
     */
    public void checkBounds() {
        tfLowBound.setEnabled(tsClipping.isSelected());
        tfUpBound.setEnabled(tsClipping.isSelected());
    }

    public void setEnabled(boolean enabled) {

        boolean boundConditions = enabled
                && pdf.equals(ProbDistribution.UNIFORM)
                && tsClipping.isSelected();

        tfParam1.setEnabled(enabled);
        tfParam2.setEnabled(enabled);

        tfUpBound.setEnabled(boundConditions);
        tfLowBound.setEnabled(boundConditions);
        tsClipping.setEnabled(boundConditions);
    }


    /**
     * @return the probability distribution represented by this panel
     */
    public ProbDistribution getPdf() {
        return pdf;
    }

    /**
     * @return Returns the isUseBoundsBox.
     */
    public TristateDropDown getTsClipping() {
        return tsClipping;
    }

    /**
     * @return Returns the tfLowBound.
     */
    public JTextField getTfLowBound() {
        return tfLowBound;
    }

    /**
     * @return Returns the tfUpBound.
     */
    public JTextField getTfUpBound() {
        return tfUpBound;
    }

    /**
     * @return the actual gui panel supporting the probability distribution
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * 
     * @param pc
     */
    public void addPropertyChangeListenerToFields(PropertyChangeListener pc) {
        tfUpBound.addPropertyChangeListener(pc);
        tfLowBound.addPropertyChangeListener(pc);
        tfParam1.addPropertyChangeListener(pc);
        tfParam2.addPropertyChangeListener(pc);
        tsClipping.addPropertyChangeListener(pc);
    }

    /**
     * Contains the specific error message that occurs if one tries to 
     * use this panel to modify a Randomizer with a different Probability
     * distribution. 
     * @throws an IllegalArgumentException if this class is passed a randomizer
     * which cannot be altered sensibly by this class because it has a different
     * probability distribution function
     */
    private static void throwBadPdfException() {
        throw new IllegalArgumentException("Random panel was initialized" +
                " for one type of probability distribution, while a" +
                " randomizer with a different probability distribution" +
                " is being used to fill its feilds");
    }


}
