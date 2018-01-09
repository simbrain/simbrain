package org.simbrain.network.gui.dialogs.synapse;

import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.SimbrainConstants.Polarity;
import org.simbrain.util.Utils;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.EditablePanel;
import org.simbrain.util.widgets.YesNoNull;

/**
 * A panel containing more detailed generic information about synapse. Generally
 * speaking, this panel is not meant to exist in a dialog by itself, it is a set
 * of commonly used (hence generic) synapse value fields which is shared by
 * multiple complete dialogs.
 *
 * Values included are: Strength, label, frozen, upper / lower bounds, increment
 *
 * @author ztosi
 * @author jyoshimi
 *
 */
public class GeneralSynapsePropertiesPanel extends JPanel
        implements EditablePanel {
    
    /** The synapses being modified. */
    private final Collection<Synapse> synapseList;

    /** Id Label. */
    private final JLabel idLabel = new JLabel();

    /** Strength field. */
    private final JFormattedTextField tfStrength = new JFormattedTextField();

    /**
     * A switch for determining whether or not the synapse will send a weighted
     * input.
     */
    private final YesNoNull synapseEnabled = new YesNoNull(
        "Enabled", "Disabled");

    /** Freeze synapse field. */
    private YesNoNull frozenDD = new YesNoNull();

    // TODO: Implement...
    // private TristateDropDown clippingDD = new TristateDropDown();

    /** Increment field. */
    private JFormattedTextField tfIncrement = new JFormattedTextField();

    /** Upper bound field. */
    private JFormattedTextField tfUpBound = new JFormattedTextField();

    /** Lower bound field. */
    private JFormattedTextField tfLowBound = new JFormattedTextField();

    /** Delay field. */
    private JFormattedTextField tfDelay = new JFormattedTextField();

    /**
     * A triangle that switches between an up (left) and a down state Used for
     * showing/hiding extra synapse data.
     */
    private final DropDownTriangle detailTriangle;

    /**
     * The extra data panel. Includes: increment, upper bound, lower bound, and
     * priority.
     */
    private final JPanel detailPanel = new JPanel();

    /** Parent reference so pack can be called. */
    private final Window parent;

    /**
     * If true, displays ID info and other fields that would only make sense to
     * show if only one neuron was being edited. This value is set automatically
     * unless otherwise specified at construction.
     */
    private boolean displayIDInfo;
    
    /**
     * Creates a basic synapse info panel. Here the whether or not ID info is
     * displayed is manually set. This is the case when the number of synapses
     * (such as when adding multiple synapses) is unknown at the time of
     * display. In fact this is probably the only reason to use this factory
     * method over {@link #createBasicSynapseInfoPanel(Collection, Window)}.
     *
     * @param synapses
     *            the synapses whose information is being displayed/made
     *            available to edit on this panel
     * @param parent
     *            the parent window for dynamic resizing
     * @param displayIDInfo
     *            whether or not to display ID info
     * @return A basic synapse info panel with the specified parameters
     */
    public static GeneralSynapsePropertiesPanel createPanel(
        final Collection<Synapse> synapses, final Window parent,
        final boolean displayIDInfo) {
        GeneralSynapsePropertiesPanel panel = new GeneralSynapsePropertiesPanel(synapses,
            parent, displayIDInfo);
        panel.addListeners();
        return panel;
    }

    
    /**
     * Creates a basic synapse info panel. Here whether or not to display ID
     * info is automatically set based on the state of the synapse list.
     *
     * @param synapses
     *            the synapses whose information is being displayed/made
     *            available to edit on this panel
     * @param parent
     *            the parent window for dynamic resizing.
     * @return A basic synapse info panel with the specified parameters
     */
    public static GeneralSynapsePropertiesPanel createPanel(
        final Collection<Synapse> synapseList, final Window parent) {
        return createPanel(synapseList, parent,
                !(synapseList == null || synapseList.size() != 1));
    }

    
    /**
     * Construct the panel.
     *
     * @param synapseList
     *            the synapse list
     * @param parent
     *            the parent window
     * @param displayIDInfo
     *            whether to display ids
     */
    private GeneralSynapsePropertiesPanel(final Collection<Synapse> synapseList,
        final Window parent, final boolean displayIDInfo) {
        this.synapseList = synapseList;
        this.parent = parent;
        this.displayIDInfo = displayIDInfo;
        detailTriangle = new DropDownTriangle(UpDirection.LEFT, false, "More",
            "Less", parent);
        initializeLayout();
    }


    /**
     * Lays out the panel.
     */
    private void initializeLayout() {
        GridLayout gl = new GridLayout(0, 2);
        gl.setVgap(5);
        setLayout(gl);
        add(new JLabel("Frozen: "));
        add(frozenDD);
        // add(new JLabel("Clipping: "));
        // add(clippingDD);
        add(new JLabel("Upper Bound:"));
        add(tfUpBound);
        add(new JLabel("Lower Bound"));
        add(tfLowBound);
        add(new JLabel("Increment:"));
        add(tfIncrement);
        add(new JLabel("Delay:"));
        add(tfDelay);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    /**
     * Fills the values of the text fields based on the corresponding values of
     * the synapses to be edited.
     * 
     * @param synapseCollection
     */
    public void fillFieldValues(Collection<Synapse> synapseCollection) {
        Synapse synapseRef = synapseCollection.iterator().next();
        // Handle Upper Bound
        if (!NetworkUtils.isConsistent(synapseCollection, Synapse.class,
                "getUpperBound")) {
            tfUpBound.setValue(SimbrainConstants.NULL_STRING);
        } else {
            tfUpBound.setValue(synapseRef.getUpperBound());
        }
        // Handle Lower Bound
        if (!NetworkUtils.isConsistent(synapseCollection, Synapse.class,
                "getLowerBound")) {
            tfLowBound.setValue(SimbrainConstants.NULL_STRING);
        } else {
            tfLowBound.setValue(synapseRef.getLowerBound());
        }
        // Handle Increment
        if (!NetworkUtils.isConsistent(synapseCollection, Synapse.class,
                "getIncrement")) {
            tfIncrement.setValue(SimbrainConstants.NULL_STRING);
        } else {
            tfIncrement.setValue(synapseRef.getIncrement());
        }
        // Handle Delay
        if (synapseRef.getDelay() < 0 || !NetworkUtils
                .isConsistent(synapseCollection, Synapse.class, "getDelay")) {
            tfDelay.setValue(SimbrainConstants.NULL_STRING);
        } else {
            tfDelay.setValue(synapseRef.getDelay());
        }
        // Handle Frozen
        if (!NetworkUtils.isConsistent(synapseCollection, Synapse.class,
                "isFrozen")) {
            frozenDD.setNull();
        } else {
            frozenDD.setSelectedIndex(synapseRef.isFrozen() ? 0 : 1);
        }

    }

    /**
     * 
     * @param synapseGroup
     * @param polarity
     */
    public void fillFieldValues(SynapseGroup synapseGroup, Polarity polarity) {
        frozenDD.setSelected(synapseGroup.isFrozen(polarity));
        double increment = synapseGroup.getIncrement(polarity);
        if (!Double.isNaN(increment)) {
            tfIncrement.setValue(Double.toString(increment));
        } else {
            tfIncrement.setValue(SimbrainConstants.NULL_STRING);
        }
        double upBound = synapseGroup.getUpperBound(polarity);
        if (!Double.isNaN(upBound)) {
            tfUpBound.setValue(Double.toString(upBound));
        } else {
            tfUpBound.setValue(SimbrainConstants.NULL_STRING);
        }
        double lowBound = synapseGroup.getLowerBound(polarity);
        if (!Double.isNaN(lowBound)) {
            tfLowBound.setValue(Double.toString(lowBound));
        } else {
            tfLowBound.setValue(SimbrainConstants.NULL_STRING);
        }
        Integer delay = synapseGroup.getDelay(polarity);
        if (delay != null && delay != -1) {
            tfDelay.setValue(delay.toString());
        } else {
            tfDelay.setValue(SimbrainConstants.NULL_STRING);
        }
    }

    /**
     * Uses the values from text fields to alter corresponding values in the
     * synapse(s) being edited. Called externally to apply changes.
     * 
     * @param synapses
     */
    public void commitChanges(Collection<Synapse> synapses) {
        // Upper Bound
        double uB = Utils.doubleParsable(tfUpBound);
        if (!Double.isNaN(uB)) {
            for (Synapse s : synapses) {
                s.setUpperBound(uB);
            }
        }
        // Lower Bound
        double lB = Utils.doubleParsable(tfLowBound);
        if (!Double.isInfinite(lB)) {
            for (Synapse s : synapses) {
                s.setLowerBound(lB);
            }
        }
        // Increment
        double increment = Utils.doubleParsable(tfIncrement);
        if (!Double.isNaN(increment)) {
            for (Synapse s : synapses) {
                s.setIncrement(increment);
            }
        }
        // Delay
        double delay = Utils.doubleParsable(tfDelay);
        if (!Double.isNaN(delay)) {
            int dly = (int) delay;
            for (Synapse s : synapses) {
                s.setDelay(dly);
            }
        }
        // Frozen ?
        boolean frozen = frozenDD.getSelectedIndex() == YesNoNull.getTRUE();
        if (frozenDD.getSelectedIndex() != YesNoNull.getNULL()) {
            for (Synapse s : synapses) {
                s.setFrozen(frozen);
            }
        }
    }
    
    /**
     * Add listeners.
     */
    private void addListeners() {

        // Add a listener to display/hide extra editable neuron data
        detailTriangle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Repaint to show/hide extra data
                detailPanel.setVisible(detailTriangle.isDown());
                detailPanel.repaint();
                parent.pack();
                parent.setLocationRelativeTo(null);
            }
        });
    }
    
    @Override
    public void fillFieldValues() {
        // See GeneralNeuronPropertiesPanel

    }

    @Override
    public boolean commitChanges() {
        // TODO  See GeneralNeuronPropertiesPanel
        return false;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

}
