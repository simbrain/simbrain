package org.simbrain.network.gui.dialogs.connect;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.simbrain.network.connections.ConnectionUtilities;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.dialogs.synapse.CombinedSynapseInfoPanel;
import org.simbrain.util.widgets.EditablePanel;

/**
 * A panel allowing synapse learning rules to be set and random weights to be
 * activated/adjusted, designed with segregation of inhibitory and excitatory
 * weights in mind.
 * 
 * @author ztosi
 * 
 */
public class SynapsePropertiesPanel extends JPanel implements EditablePanel {

    /**
     * A synapse info panel containing basic synapse properties and synapse
     * update rule properties for excitatory synapses.
     */
    private CombinedSynapseInfoPanel excitatoryInfoPanel;

    /**
     * A synapse info panel containing basic synapse properties and synapse
     * update rule properties for inhibitory synapses.
     */
    private CombinedSynapseInfoPanel inhibitoryInfoPanel;

    private JButton exApplyButton = new JButton("Apply");

    private JButton inApplyButton = new JButton("Apply");

    /**
     * A template excitatory synapse used to store committed information if a
     * synapse group doesn't have any connections yet.
     */
    private final Synapse templateExcitatorySynapse;

    /**
     * A template inhibitory synapse used to store committed information if a
     * synapse group doesn't have any connections yet.
     */
    private final Synapse templateInhibitorySynapse;

    /** The synapse group being edited. Null, if editing loose synapses. */
    private final SynapseGroup synapseGroup;

    /** The main panel which contains the other panels. */
    private final JPanel mainPanel = new JPanel();

    /** Whether or not this is a creation panel. */
    private final boolean creationPanel;

    /**
     * 
     * @param parent
     * @param synapseGroup
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
        final Window parent, final SynapseGroup synapseGroup) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent,
            synapseGroup);
        spp.initApplyListeners();
        return spp;
    }

    /**
     * 
     * @param parent
     * @param synapses
     * @return
     */
    public static SynapsePropertiesPanel createSynapsePropertiesPanel(
        final Window parent, final Collection<Synapse> synapses) {
        SynapsePropertiesPanel spp = new SynapsePropertiesPanel(parent,
            synapses);
        spp.initApplyListeners();
        return spp;
    }

    /**
     * 
     * @param parentWindow
     * @param synapseGroup
     */
    private SynapsePropertiesPanel(final Window parentWindow,
        final SynapseGroup synapseGroup) {
        this.synapseGroup = synapseGroup;
        creationPanel = synapseGroup.isEmpty();
        templateExcitatorySynapse = synapseGroup.getExcitatoryPrototype();
        templateInhibitorySynapse = synapseGroup.getInhibitoryPrototype();
        Set<Synapse> excitatorySynapses;
        Set<Synapse> inhibitorySynapses;
        excitatorySynapses = synapseGroup.hasExcitatory()
            ? synapseGroup.getExcitatorySynapses()
            : Collections.singleton(synapseGroup.getExcitatoryPrototype());
        inhibitorySynapses = synapseGroup.hasInhibitory()
            ? synapseGroup.getInhibitorySynapses()
            : Collections.singleton(synapseGroup.getInhibitoryPrototype());
        excitatoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(excitatorySynapses, parentWindow);
        inhibitoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(inhibitorySynapses, parentWindow);
        init();
    }

    /**
     * 
     * @param parentWindow
     * @param synapses
     */
    private SynapsePropertiesPanel(final Window parentWindow,
        final Collection<Synapse> synapses) {
        synapseGroup = null;
        templateExcitatorySynapse = Synapse.getTemplateSynapse();
        templateInhibitorySynapse = Synapse.getTemplateSynapse();
        creationPanel = synapses.isEmpty();
        Collection<Synapse> excitatorySynapses = ConnectionUtilities
            .getExcitatorySynapses(synapses);
        Collection<Synapse> inhibitorySynapses = ConnectionUtilities
            .getInhibitorySynapses(synapses);
        excitatoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(excitatorySynapses, parentWindow);
        inhibitoryInfoPanel = CombinedSynapseInfoPanel
            .createCombinedSynapseInfoPanel(inhibitorySynapses, parentWindow);
        init();
    }

    private void init() {
        // Excitatory Border
        int redShadow = 0;
        byte bitmask = 0x7F;
        // RGB: 0x7F, 0, 0
        redShadow = redShadow | (bitmask << 16);
        // Color.RED as highlight, redShadow as shadow
        Border redBorder =
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED,
                Color.RED, new Color(redShadow));
        Border exBorder = BorderFactory.createTitledBorder(redBorder,
            "Excitatory");

        // Inhibitory Border
        int blueShadow = 0;
        // RGB: 0, 0, 0x7F
        blueShadow = blueShadow | bitmask;
        Border blueBorder =
            BorderFactory.createEtchedBorder(EtchedBorder.LOWERED,
                Color.BLUE, new Color(blueShadow));
        Border inBorder = BorderFactory.createTitledBorder(blueBorder,
            "Inhibitory");

        // Layout panels
        BoxLayout bxLayout = new BoxLayout(mainPanel, BoxLayout.X_AXIS);
        mainPanel.setLayout(bxLayout);

        Box inBox = Box.createVerticalBox();
        inBox.setAlignmentY(Component.TOP_ALIGNMENT);
        inBox.add(inhibitoryInfoPanel);
        JPanel inApButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inApButtonPanel.add(inApplyButton);
        inBox.add(Box.createVerticalGlue());
        inBox.add(inApButtonPanel);
        inApButtonPanel.setVisible(!creationPanel);

        inBox.add(new JPanel());
        inBox.setBorder(inBorder);

        Box exBox = Box.createVerticalBox();
        exBox.setAlignmentY(Component.TOP_ALIGNMENT);
        exBox.add(excitatoryInfoPanel);
        JPanel exApButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exApButtonPanel.add(exApplyButton);
        exBox.add(Box.createVerticalGlue());
        exBox.add(exApButtonPanel);
        exApButtonPanel.setVisible(!creationPanel);
        exBox.add(new JPanel());
        exBox.setBorder(exBorder);

        mainPanel.add(inBox);
        mainPanel.add(Box.createHorizontalGlue(), bxLayout);
        mainPanel.add(Box.createHorizontalStrut(10), bxLayout);
        mainPanel.add(exBox);
        this.add(mainPanel);

    }

    private void initApplyListeners() {
        exApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                excitatoryInfoPanel.commitChanges();
                double exStrength = excitatoryInfoPanel.getStrength();
                if (!Double.isNaN(exStrength) && synapseGroup != null) {
                    synapseGroup.setAllExcitatoryStrengths(exStrength);
                }
            }
        });
        inApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inhibitoryInfoPanel.commitChanges();
                double inStrength = inhibitoryInfoPanel.getStrength();
                if (!Double.isNaN(inStrength) && synapseGroup != null) {
                    synapseGroup.setAllInhibitoryStrengths(inStrength);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public boolean commitChanges() {
        boolean success = true;
        success &= excitatoryInfoPanel.commitChanges();
        success &= inhibitoryInfoPanel.commitChanges();
        return success;
    }

    /**
     * Does nothing. This panel contains two CombinedSynapseInfo panels, both of
     * which call their own fillFieldValues() method in their constructors.
     * Since there are no fields outside these panels in this panel
     */
    @Override
    public void fillFieldValues() {
    }

    @Override
    public JPanel getPanel() {
        return mainPanel;
    }

}
