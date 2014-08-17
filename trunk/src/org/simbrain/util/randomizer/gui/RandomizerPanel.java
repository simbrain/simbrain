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
package org.simbrain.util.randomizer.gui;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.LabelledItem;
import org.simbrain.util.widgets.TristateDropDown;

/**
 * <b>RandomizerPanel</b> an interface for setting parameters of a randomizer
 * object.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 */
public class RandomizerPanel extends JPanel {

    /** Distribution combo box. */
    private JComboBox<ProbDistribution> cbDistribution =
        new JComboBox<ProbDistribution>(ProbDistribution.values());

    // Initialize distribution.
    {
        cbDistribution.setSelectedItem(Randomizer.DEFAULT_DISTRIBUTION);
    }

    /**
     * A map between probability distributions and specific distribution panels.
     */
    private HashMap<ProbDistribution, ProbDistPanel> cardMap =
        new HashMap<ProbDistribution, ProbDistPanel>();

    /**
     * The main panel where all the different probability distribution panels
     * are stored as cards.
     */
    private JPanel cardPanel = new JPanel();

    private ItemListener cbItemListener = new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                ProbDistPanel rp = null;
                String str = SimbrainConstants.NULL_STRING;
                cardPanel.removeAll();
                if (!cbDistribution.getSelectedItem().equals(str)) {
                    rp = cardMap.get(cbDistribution.getSelectedItem());
                    cardPanel.add(rp.getPanel());
                }
                cardPanel.repaint();
                repaint();
                if (parent != null)
                    parent.pack();
            }
        }
    };

    private final Window parent;

    /**
     * This method is the default constructor. The parent window is set to null
     * and no auto-resizing will occur.
     */
    public RandomizerPanel() {
        parent = null;
        this.setLayout(new BorderLayout());
        initializeRandomPanels();
        layoutPanel();
        addInternalListeners();
    }

    /**
     * A constructor specifying a parent window for auto-resizing.
     *
     * @param parent
     */
    public RandomizerPanel(Window parent) {
        this.parent = parent;
        this.setLayout(new BorderLayout());
        initializeRandomPanels();
        layoutPanel();
        addInternalListeners();
    }

    /**
     * Lays out the panel...
     */
    private void layoutPanel() {
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout());
        //        GridBagConstraints gbc = new GridBagConstraints();
        //        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(new LabelledItem("Distribution: ", cbDistribution),
            BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);

        cbItemListener.itemStateChanged(new ItemEvent(cbDistribution,
            ItemEvent.SELECTED, cardPanel, ItemEvent.ITEM_STATE_CHANGED));
        repaint();
        revalidate();
    }

    /**
     * Adds all the possible probability distribution panels to the cardPanel as
     * cards (one on top of the other).
     */
    private void initializeRandomPanels() {
        for (ProbDistribution pd : ProbDistribution.values()) {
            ProbDistPanel rp = new ProbDistPanel(pd);
            cardMap.put(pd, rp);
        }
    }

    /**
     * Adds all internal listeners. Currently this just consists of an item
     * listener for the combo-box which switches to the correct "card" for the
     * given probability distribution.
     */
    private void addInternalListeners() {
        cbDistribution.addItemListener(cbItemListener);
    }

    /**
     * Populates the fields with current values.
     *
     * @param randomizers
     *            List of randomizers
     */
    public void fillFieldValues(final ArrayList<Randomizer> randomizers) {
        Randomizer rand = (Randomizer) randomizers.get(0);
        if (randomizers.size() == 1) {
            fillFieldValues(rand);
            return;
        }
        if (!NetworkUtils.isConsistent(randomizers, Randomizer.class,
            "getPdf")) {
            cbDistribution.setSelectedItem(SimbrainConstants.NULL_STRING);
            NullRandPanel nrp = new NullRandPanel();
            cardMap.put(null, nrp);
            cardPanel.removeAll();
            cardPanel.add(nrp.getPanel(), SimbrainConstants.NULL_STRING);
            cardPanel.repaint();
            if (parent != null)
                parent.pack();
        } else {
            ProbDistPanel rp = cardMap.get(rand.getPdf());
            cbDistribution.setSelectedItem(rand.getPdf());
            rp.fillFieldValues(rand);
            cardPanel.removeAll();
            cardPanel.add(rp.getPanel());
            cardPanel.repaint();
            if (parent != null)
                parent.pack();
        }

    }

    /**
     * Fills fields with values from a Random Source.
     *
     * @param rand
     */
    public void fillFieldValues(Randomizer rand) {
        ProbDistPanel rp = cardMap.get(rand.getPdf());
        cbDistribution.setSelectedItem(rand.getPdf());
        rp.fillFieldValues(rand);
        cardPanel.removeAll();
        cardPanel.add(rp.getPanel());
        cardPanel.repaint();
        if (parent != null)
            parent.pack();
    }

    /**
     * Fills fields with default values.
     */
    public void fillDefaultValues() {
        ProbDistPanel rp = cardMap.get(Randomizer.DEFAULT_DISTRIBUTION);
        cbDistribution.setSelectedItem((Randomizer.DEFAULT_DISTRIBUTION));
        rp.fillDefaultValues();
        cardPanel.removeAll();
        cardPanel.add(rp.getPanel());
        cardPanel.repaint();
        if (parent != null)
            parent.pack();
    }

    /**
     * Called externally when dialog is being closed.
     *
     * @param rand
     *            Random source
     */
    public void commitRandom(final Randomizer rand) {
        if (!cbDistribution.getSelectedItem()
            .equals(SimbrainConstants.NULL_STRING)) {
            ProbDistribution pdf =
                (ProbDistribution) cbDistribution.getSelectedItem();
            cardMap.get(pdf).commitRandom(rand);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        for (ProbDistPanel pdp : cardMap.values()) {
            pdp.getPanel().setVisible(visible);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(boolean enabled) {
        cbDistribution.setEnabled(enabled);
        if (!cbDistribution.getSelectedItem()
            .equals(SimbrainConstants.NULL_STRING)) {
            cardMap.get((ProbDistribution) cbDistribution.getSelectedItem())
                .setEnabled(enabled);
        }
    }

    /**
     * @return Returns the cbDistribution.
     */
    public JComboBox<ProbDistribution> getCbDistribution() {
        return cbDistribution;
    }

    /**
     * @return Returns the isUseBoundsBox.
     */
    public TristateDropDown getTsClipping() {
        if (cbDistribution.getSelectedItem()
            .equals(SimbrainConstants.NULL_STRING)) {
            return null;
        }
        return cardMap.get((ProbDistribution) cbDistribution
            .getSelectedItem()).getTsClipping();
    }

    /**
     * @return Returns the tfLowBound.
     */
    public JTextField getTfLowBound() {
        if (cbDistribution.getSelectedItem()
            .equals(SimbrainConstants.NULL_STRING)) {
            return null;
        }
        return cardMap.get((ProbDistribution) cbDistribution
            .getSelectedItem()).getTfLowBound();
    }

    /**
     * @return Returns the tfUpBound.
     */
    public JTextField getTfUpBound() {
        if (cbDistribution.getSelectedItem()
            .equals(SimbrainConstants.NULL_STRING)) {
            return null;
        }
        return cardMap.get((ProbDistribution) cbDistribution
            .getSelectedItem()).getTfUpBound();
    }

    /**
     * Provides a string summary of the field values.
     * @return
     */
    public String getSummary() {
        ProbDistribution pdf =
            (ProbDistribution) cbDistribution.getSelectedItem();
        ProbDistPanel pdp = cardMap.get(pdf);
        double param1 = pdp.getParam1FieldVal();
        double param2 = pdp.getParam2FieldVal();
        String newText = "";
        param1 = SimbrainMath.roundDouble(param1, 5);
        param2 = SimbrainMath.roundDouble(param2, 5);
        switch ((ProbDistribution) cbDistribution.getSelectedItem()) {
            case UNIFORM:
                newText = "Uniform: [" + param1 + ", " + param2 + "]";
                break;
            case NORMAL:
                newText = "Normal: \u03BC=" + param1 + " \u03C3=" + param2;
                break;
            case LOGNORMAL:
                newText = "Log-normal: \u03BC=" + param1 + " \u03C3=" + param2;
                break;
            case GAMMA:
                newText = "Gamma: k=" + param1 + " \u03B8=" + param2;
                break;
            case EXPONENTIAL:
                newText = "Exponential: \u03BB=" + param1;
                break;
            case PARETO:
                newText = "Pareto: \u03B1=" + param1 + " min=" + param2;
                break;
            default:
                newText = "???";
                break;
        }
        return newText;
    }

    /**
     *
     * @param pc
     */
    public void addPropertyChangeListenerToFields(PropertyChangeListener pc) {
        cbDistribution.addPropertyChangeListener(pc);
        if (!cbDistribution.getSelectedItem()
            .equals(SimbrainConstants.NULL_STRING)) {
            cardMap.get(cbDistribution.getSelectedItem())
                .addPropertyChangeListenerToFields(pc);
        }
    }

    /**
    *
    * @param pc
    */
    public void addFocusListenerToFields(FocusListener fl) {
        for (ProbDistPanel pdp : cardMap.values()) {
            pdp.addFocusListenerToFields(fl);
        }
    }

    /**
     * A null ProbDist panel which cannot under any circumstance actually edit
     * anything. It displays nothing and serves only as a place holder in
     * instances where multiple randomizers are selected with different
     * probability density functions.
     *
     * @author Zach Tosi
     */
    private class NullRandPanel extends ProbDistPanel {

        public NullRandPanel() {
            super();
        }

        public void fillFieldValues(Randomizer rand) {
            // Do nothing...
        }

        public void fillFieldValues(ArrayList<Randomizer> rands) {
            // Do nothing...
        }

        public void fillDefaultValues() {
            // Do nothing...
        }

        public void commitRandom(Randomizer rand) {
            // Do nothing...
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        RandomizerPanel rp = new RandomizerPanel(frame);
        rp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rp.fillDefaultValues();
        frame.setContentPane(rp);
        frame.setVisible(true);
        frame.pack();
    }
}
