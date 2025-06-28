package org.simbrain.network.gui.dialogs;

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.util.Utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Hashtable;

/**
 * Panel for adjusting the connectivity between a source and target set of
 * neurons.
 * <p>
 * TODO: Not yet completed. This is temporary.
 */
public class ConnectivityAdjustmentPanel extends JPanel {

    /**
     * A slider for setting the sparsity of the connections.
     */
    private final JSlider sparsitySlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);

    /**
     * A text field for setting the sparsity of the connections.
     */
    private final JFormattedTextField sparsity = new JFormattedTextField(NumberFormat.getNumberInstance());

    /**
     * The number of target neurons.
     */
    private final int numTargs;

    private final JButton applyButton = new JButton("Apply");

    /**
     * A flag determining if an action was initiated by a user (useful for
     * reciprocal action listeners).
     */
    private boolean userFlag = true;

    private final Sparse connection;

    public ConnectivityAdjustmentPanel(final Sparse connection, final NetworkPanel networkPanel) {
        this.connection = connection;
        numTargs = networkPanel.getSelectionManager().filterSelectedModels(Neuron.class).size();
        // fillFieldValues();
        initializeSparseSlider();
        addChangeListeners();
        initializeLayout();
    }

    /**
     * Initializes the custom layout of the sparse panel.
     */
    private void initializeLayout() {

        JPanel sparseContainer = new JPanel();
        this.add(sparseContainer);
        JScrollPane pScroller = new JScrollPane(sparseContainer);
        this.add(pScroller, BorderLayout.CENTER);

        sparseContainer.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.gridheight = 4;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        sparseContainer.add(initializeSparseSubPanel(), gbc);

    }

    private JPanel initializeSparseSubPanel() {
        JPanel ssp = new JPanel();
        ssp.setLayout((new GridBagLayout()));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(10, 10, 0, 10);
        ssp.add(new JLabel("Percent Connectivity:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        ssp.add(sparsitySlider, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        ssp.add(new JLabel("Sparsity: "), gbc);

        gbc.gridx = 1;
        Dimension sSize = sparsity.getPreferredSize();
        sSize.width = 40;
        sparsity.setPreferredSize(sSize);
        ssp.add(sparsity, gbc);

        return ssp;
    }

    /**
     * Initializes the sparse slider.
     */
    private void initializeSparseSlider() {

        sparsitySlider.setMajorTickSpacing(10);
        sparsitySlider.setMinorTickSpacing(2);
        sparsitySlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> labelTable2 = new Hashtable<Integer, JLabel>();
        labelTable2.put(0, new JLabel("0%"));
        labelTable2.put(100, new JLabel("100%"));
        sparsitySlider.setLabelTable(labelTable2);
        sparsitySlider.setPaintLabels(true);
    }

    /**
     * Adds change listeners specific to sparse panel: Sparsity slider, sparsity
     * text field, and syns/source field.
     */
    private void addChangeListeners() {

        sparsitySlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting() && source == sparsitySlider) {
                    if (userFlag) {
                        userFlag = false;
                        double val = (double) (sparsitySlider.getValue()) / 100;
                        sparsity.setValue(val);
                    } else {
                        userFlag = true;
                    }

                }
            }

        });

        sparsity.addPropertyChangeListener("value", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getSource() == sparsity) {
                    if (userFlag) {
                        userFlag = false;
                        if (sparsity.getValue() != null) {
                            int sVal = (int) (((Number) sparsity.getValue()).doubleValue() * 100);
                            sparsitySlider.setValue(sVal);
                        }
                    } else {
                        userFlag = true;
                    }
                }
            }
        });

    }

    public void commitChanges() {
        double density = Utils.doubleParsable(sparsity.getText());
//        if (!Double.isNaN(density)) {
//            connection.adjustToSparsity(density);
//            if (density > connection.getConnectionDensity()) {
//                connection.addToSparsity(density);
//            } else if (density < connection.getConnectionDensity()) {
//                connection.removeToSparsity(density);
//            }
//        }
    }

    public void fillFieldValues() {
        if (connection == null || !(connection instanceof Sparse)) {
            sparsity.setValue(.01);

        } else {
            sparsity.setValue(connection.getConnectionDensity());
        }
    }

}
