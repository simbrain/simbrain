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
package org.simbrain.network.gui.dialogs.network;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.connect.CondensedConnectionPanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.AbstractSigmoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.ContinuousSigmoidalRulePanel;
import org.simbrain.network.gui.dialogs.neuron.rule_panels.DiscreteSigmoidalRulePanel;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simbrain.util.math.SquashingFunction;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;
import org.simbrain.util.widgets.ShowHelpAction;

/**
 *
 * A panel for creating and setting the parameters of an arbitrary Echo-State
 * Network.
 *
 * TODO: Include initial scaling factors of terms. TODO: Consider using
 * simplified continuous time ESN where only leak is important
 *
 * @author Zach Tosi
 *
 */
@SuppressWarnings("serial")
public class ESNCreationDialog extends StandardDialog {

    public static final String TITLE = "ESN Creation Wizard";

    public static final TimeType DEFAULT_TIME_TYPE = TimeType.DISCRETE;

    public static final SquashingFunction DEFAULT_INITIAL_FUNCTION = SquashingFunction.TANH;

    public static final double DEFAULT_SPECTRAL_RADIUS = 0.98;

    public static final int DEFAULT_RESERVOIR_POPULATION = 256;

    public static final int DEFAULT_INPUT_POPULATION = 1;

    public static final int DEFAULT_OUTPUT_POPULATION = 1;

    /** A combo box containing the two possible time types an ESN can have. */
    private JComboBox<TimeType> cbTimeType = new JComboBox<TimeType>(
            TimeType.values());

    /** The main panel to which all non-dialog specific components are added. */
    private Box mainPanel = Box.createVerticalBox();

    /**
     * A panel for containing global settings about the ESN. TODO: Add initial
     * scaling factor values.
     */
    private JPanel globalESNSettingsPanel = new JPanel();

    /**
     * A specific panel for the input layer. All it requires is a number since
     * the type must be linear.
     */
    private JPanel inputLayerPanel = new JPanel();

    /** The population (# neurons) in the input layer field. */
    private JTextField tfInputLayerPop = new JTextField(5);
    {
        // Aesthetic choice...
        tfInputLayerPop.setHorizontalAlignment(JTextField.RIGHT);
    }

    /** The neuron layer panel corresponding to the reservoir. */
    private NeuronLayerPanel reservoirPanel;

    /** The neuron layer panel corresponding to the output layer. */
    private NeuronLayerPanel outputPanel;

    /**
     * A connection panel governing the connections between the input and the
     * reservoir.
     */
    private CondensedConnectionPanel inToResPanel;

    /**
     * A connection panel governing the recurrent reservoir connections. By
     * default randomizers are on.
     */
    private CondensedConnectionPanel resToResPanel;

    /**
     * A connection panel governing the connections from the output to the
     * reservoir.
     */
    private CondensedConnectionPanel outToResPanel;

    /** The tabbed pane containing the connection panels. */
    private final JTabbedPane connectionPanels = new JTabbedPane();

    /**
     * The text field where the maximum eigenvalue of the recurrent weight
     * matrix is set.
     */
    private final JTextField spectralRadius = new JTextField(15);

    /**
     * Whether or not to create a connection from the output to the reservoir.
     * */
    private final JCheckBox allowOutToRes = new JCheckBox();

    /**
     * Whether or not the output should have its own recurrent weights. Seeing
     * as how these weights are trained the option to set their parameters is
     * not present to the user.
     */
    private final JCheckBox allowRecurrentOutputs = new JCheckBox();

    /**
     * Whether or not the input should connect directly to the output. Seeing as
     * how these weights are trained the option to set their parameters is not
     * present to the user.
     */
    private final JCheckBox directInputToOutput = new JCheckBox();

    /** The network panel where this is all taking place. */
    private final NetworkPanel networkPanel;

    /**
     * A static initializer. TODO: References to "this" escape in the
     * constructor, however constraints relating to the use of reflection to
     * summon this dialog prevent a safe static initializer from being used.
     *
     * @param network
     * @return
     */
    public static ESNCreationDialog createESNDialog(NetworkPanel network) {
        ESNCreationDialog ed = new ESNCreationDialog(network);
        // ed.initGlobalESNSettingsPanel();
        // ed.initConnectionPanels();
        // ed.init();
        // ed.initListeners();
        return ed;
    }

    /**
     * Creates an dialog for creating and setting the parameters of an Echo
     * State Network in a given networkPanel.
     *
     * @param networkPanel
     */
    public ESNCreationDialog(NetworkPanel networkPanel) {
        this.networkPanel = networkPanel;
        setTitle("ESN Creation Dialog");
        // TODO: BAD! But currently the way the action manager handles opening
        // this panel make it so that everything must happen in the constructor.
        // References to "this" escape before construction is finished as a
        // result.
        initGlobalESNSettingsPanel();
        initConnectionPanels();
        init();
        initListeners();

        // Set initial size of dialog based on screen size
        int height = (int) (.9 * Toolkit.getDefaultToolkit().getScreenSize().height);
        int width = this.getPreferredSize().width;
        this.setPreferredSize(new Dimension(width, height));

    }

    /**
     * Initialize the settings panel.
     */
    private void initGlobalESNSettingsPanel() {

        // Set up the global esn settings panel
        GridLayout gl = new GridLayout(0, 2);
        gl.setVgap(5);
        JPanel gESNSubPanel = new JPanel(gl);

        // ESN Settings: Time type
        gESNSubPanel.add(new JLabel("Time Type: "));
        gESNSubPanel.add(cbTimeType);

        // ESN Settings: Spectral Radius
        gESNSubPanel.add(new JLabel("Spectral Radius: "));
        spectralRadius.setText(Double.toString(DEFAULT_SPECTRAL_RADIUS));
        gESNSubPanel.add(spectralRadius);

        GridLayout gl2 = new GridLayout(0, 4);
        gl2.setVgap(10);
        JPanel chkBxSubPanel = new JPanel(gl2);

        // ESN Settings: Recurrent outputs
        chkBxSubPanel.add(new JLabel("Output\u2192 Output * "));
        chkBxSubPanel.add(allowRecurrentOutputs);

        // ESN Settings: Direct input to output
        chkBxSubPanel.add(new JLabel("Input\u2192 Output * "));
        chkBxSubPanel.add(directInputToOutput);

        // ESN Settings: Output to reservoir
        chkBxSubPanel.add(new JLabel("Output\u2192 Res. "));
        chkBxSubPanel.add(allowOutToRes);

        // Links the user to the help page for ESNs in case confusion arises
        // as to the fact that out->out and in->out do not spawn settings
        // panels like out->res.
        JLabel helpIcon = new JLabel(ResourceManager.getImageIcon("Help.png"));
        chkBxSubPanel.add(new JLabel("* Trained Weights"));
        chkBxSubPanel.add(helpIcon);
        helpIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** @see Runnable */
                    public void run() {
                        Utils.showHelpPage("Pages/Network/network/echostatenetwork.html");
                    }
                });

            }

        });

        gESNSubPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        chkBxSubPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        globalESNSettingsPanel = new JPanel(new BorderLayout());
        globalESNSettingsPanel.add(gESNSubPanel, BorderLayout.NORTH);
        globalESNSettingsPanel.add(chkBxSubPanel, BorderLayout.SOUTH);

    }

    /**
     * Initialize the connection panels.
     */
    private void initConnectionPanels() {
        // Create panels
        inToResPanel = new CondensedConnectionPanel(networkPanel, this,
                DEFAULT_RESERVOIR_POPULATION);
        inToResPanel.getConnectorPanel().setRecurrent(false);
        resToResPanel = new CondensedConnectionPanel(networkPanel, this,
                DEFAULT_RESERVOIR_POPULATION);
        resToResPanel.getConnectorPanel().setRecurrent(true);
        outToResPanel = new CondensedConnectionPanel(networkPanel, this,
                DEFAULT_RESERVOIR_POPULATION);
        outToResPanel.getConnectorPanel().setRecurrent(false);

        // Create aesthetic borders
        ((Box) inToResPanel.getMainPanel()).setBorder(BorderFactory
                .createEmptyBorder(10, 5, 5, 5));
        ((Box) resToResPanel.getMainPanel()).setBorder(BorderFactory
                .createEmptyBorder(10, 5, 5, 5));
        ((Box) outToResPanel.getMainPanel()).setBorder(BorderFactory
                .createEmptyBorder(10, 5, 5, 5));
    }

    /**
     * Create the main panel.
     */
    private void init() {

        globalESNSettingsPanel.setBorder(BorderFactory
                .createTitledBorder("Global Settings"));
        mainPanel.add(globalESNSettingsPanel);

        //mainPanel.add(Box.createVerticalStrut(5));

        Box neuronLayerPanels = Box.createVerticalBox();
        neuronLayerPanels.add(Box.createVerticalStrut(5));

        GridLayout gl = new GridLayout(1, 4);
        gl.setVgap(5);
        inputLayerPanel.setLayout(gl);
        inputLayerPanel.add(new JLabel("Neuron Type: "));
        JPanel inTypeSub = new JPanel(new FlowLayout());
        inTypeSub.add(new JLabel("N/A"));
        inputLayerPanel.add(inTypeSub);
        inputLayerPanel.add(new JLabel("Population: "));
        tfInputLayerPop.setText(Integer.toString(DEFAULT_INPUT_POPULATION));
        JPanel inPopSub = new JPanel(new FlowLayout());
        inPopSub.add(tfInputLayerPop);
        inputLayerPanel.add(inPopSub);
        // Create a 5, 5, 5, 5 insets...
        inputLayerPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        JPanel insetPanel = new JPanel(new BorderLayout());
        insetPanel.add(inputLayerPanel, BorderLayout.CENTER);
        insetPanel.setBorder(BorderFactory.createTitledBorder("Input"));
        neuronLayerPanels.add(insetPanel);

        reservoirPanel = NeuronLayerPanel.createNeuronLayerPanel(this,
                (TimeType) cbTimeType.getSelectedItem(),
                DEFAULT_INITIAL_FUNCTION, DEFAULT_RESERVOIR_POPULATION);
        reservoirPanel.setBorder(BorderFactory.createTitledBorder("Reservoir"));
        neuronLayerPanels.add(reservoirPanel);

        outputPanel = NeuronLayerPanel.createNeuronLayerPanel(this,
                (TimeType) cbTimeType.getSelectedItem(),
                DEFAULT_INITIAL_FUNCTION, DEFAULT_OUTPUT_POPULATION);
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));
        neuronLayerPanels.add(outputPanel);

        neuronLayerPanels.setBorder(BorderFactory.createTitledBorder(
                "Neuron Layer Settings"));
        //neuronLayerPanels.add(Box.createVerticalStrut(10));

        // The jpanel set to the dialog's background color addresses
        // a problem where transparent components allowed odd colors
        // to show on some systems
        JPanel neuronPanelHolder = new JPanel(new BorderLayout());
        neuronPanelHolder.setBackground(this.getBackground());
        neuronPanelHolder.add("Center", neuronLayerPanels);
        mainPanel.add(neuronPanelHolder);

        connectionPanels.add(inToResPanel.getMainPanel(), "Input \u2192 Res.");
        connectionPanels.add(resToResPanel.getMainPanel(), "Res. \u2192 Res.");
        connectionPanels.setBorder(BorderFactory
                .createTitledBorder("Synaptic Connection Settings"));

        JPanel connectionPanelHolder = new JPanel();
        connectionPanelHolder.setBackground(this.getBackground());
        connectionPanelHolder.add(connectionPanels);
        mainPanel.add(connectionPanelHolder);


        JScrollPane scroller = new JScrollPane(mainPanel);
        scroller.setBorder(null);

        this.setContentPane(scroller);

        // Add a help button
        JButton helpButton = new JButton("Help");
        Action helpAction = new ShowHelpAction(
                "Pages/Network/network/echostatenetwork.html");
        helpButton.setAction(helpAction);
        this.addButton(helpButton);

    }

    /**
     * Add listeners.
     */
    private void initListeners() {

        // Adds a CondensedConnectionPanel to the tabbed pane if back-weights
        // are allowed
        allowOutToRes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (allowOutToRes.isSelected()) {
                    connectionPanels.add(outToResPanel.getMainPanel(),
                            "Out \u2192 Res.");
                } else {
                    connectionPanels.remove(outToResPanel.getMainPanel());
                }
                connectionPanels.repaint();
            }
        });

        // Change the rule panel and repaint/pack if the time type is
        // changed
        JComboBox<TimeType> ttCb = cbTimeType;
        ttCb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reservoirPanel.setTimeType((TimeType) cbTimeType
                        .getSelectedItem());
            }
        });

        reservoirPanel.getTfPopulation().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void focusLost(FocusEvent e) {
                Integer numTargs = Utils.parseInteger(reservoirPanel
                        .getTfPopulation());
                if (numTargs == null) {
                    return;
                }
                inToResPanel.getConnectorPanel().setNumTargs(numTargs);
                resToResPanel.getConnectorPanel().setNumTargs(numTargs);
                outToResPanel.getConnectorPanel().setNumTargs(numTargs);
            }

        });

    }

    /**
     * Create an ESN from the data entered in this panel.
     *
     * @return and ESN based on the data in the panel
     */
    public EchoStateNetwork commitChanges() {

        Network net = networkPanel.getNetwork();

        // Create constituent neuron groups...

        // Input layer
        Integer numIns = Utils.parseInteger(tfInputLayerPop);
        if (numIns == null) {
            throw new IllegalArgumentException("Non-Number input population.");
        }
        NeuronGroup inputLayer = new NeuronGroup(net, numIns);
        if (inputLayer.size() > inputLayer.getGridThreshold()) {
            inputLayer.setLayout(new org.simbrain.network.layouts.GridLayout());
        }
        inputLayer.applyLayout();

        // Reservoir layer
        NeuronGroup reservoirLayer = reservoirPanel.createLayerFromData();
        // for (Neuron n : reservoirLayer.getNeuronList()) {
        // ((NoisyUpdateRule) n.getUpdateRule()).setAddNoise(true);
        // }
        if (reservoirLayer.size() > reservoirLayer.getGridThreshold()) {
            reservoirLayer
                    .setLayout(new org.simbrain.network.layouts.GridLayout());
        }
        reservoirLayer.applyLayout();

        // Reservoir's synapse group must be initialized alongside the reservoir
        // neuron group
        SynapseGroup resSynapses = resToResPanel.createSynapseGroup(
                reservoirLayer, reservoirLayer);
        resSynapses.setLabel("Res \u2192 Res");

        // Output layer
        NeuronGroup outputLayer = outputPanel.createLayerFromData();
        outputLayer.applyLayout();
        if (outputLayer.size() > outputLayer.getGridThreshold()) {
            outputLayer
                    .setLayout(new org.simbrain.network.layouts.GridLayout());
        }

        // Make a bare ESN (no neuron or synapse groups)
        EchoStateNetwork esn = new EchoStateNetwork(net,
                networkPanel.getLastClickedPosition());

        // Set gloabl ESN settings
        esn.setBackWeights(allowOutToRes.isSelected());
        esn.setDirectInOutWeights(directInputToOutput.isSelected());
        esn.setRecurrentOutWeights(allowRecurrentOutputs.isSelected());
        esn.setTimeType((TimeType) cbTimeType.getSelectedItem());
        double maxEig = Utils.doubleParsable(spectralRadius);
        if (Double.isNaN(maxEig)) {
            throw new IllegalArgumentException("Non-Number spectral radius.");
        }

        // Initialize Neuron Groups
        esn.initializeInputLayer(inputLayer);
        esn.initializeReservoir(reservoirLayer, resSynapses, maxEig);
        esn.initializeOutput(outputLayer);

        // Initialize Synapse Groups
        SynapseGroup itr = inToResPanel.createSynapseGroup(inputLayer,
                reservoirLayer);
        itr.setLabel("In \u2192 Res");
        esn.addSynapseGroup(itr);

        SynapseGroup rto = SynapseGroup.createSynapseGroup(reservoirLayer,
                outputLayer, 0.5);
        rto.setLabel("Res \u2192 Out");
        esn.addSynapseGroup(rto);
        if (allowRecurrentOutputs.isSelected()) {
            SynapseGroup oto = SynapseGroup.createSynapseGroup(outputLayer,
                    outputLayer, 0.5);
            oto.setLabel("Out \u2192 Out");
            esn.addSynapseGroup(oto);
        }
        if (allowOutToRes.isSelected()) {
            SynapseGroup otr = outToResPanel.createSynapseGroup(outputLayer,
                    reservoirLayer);
            otr.setLabel("Out \u2192 Res.");
            esn.addSynapseGroup(otr);
        }
        if (directInputToOutput.isSelected()) {
            SynapseGroup ito = SynapseGroup.createSynapseGroup(inputLayer,
                    outputLayer, 0.5);
            ito.setLabel("In \u2192 Out");
            esn.addSynapseGroup(ito);
        }
        esn.positionLayers();

        return esn;

    }

    /**
     * {@inheritDoc} If any fields are invalid does not close dialog. In such a
     * case a pop-up dialog is used to indicate that there was an error and the
     * user has the opportunity to correct the mistake.
     */
    @Override
    public void closeDialogOk() {
        try {
            EchoStateNetwork esn = commitChanges();
            networkPanel.getNetwork().addGroup(esn);
            super.closeDialogOk(); // close dialog normally
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ex.printStackTrace();
            // Something went wrong... user probably put a non-sensical value
            // in one of the fields.
            JOptionPane.showMessageDialog(new JFrame(), "BUILD FAILURE:"
                    + "\nOne or more entered parameters are invalid. "
                    + "\nCheck all fields for invalid variable assignments.",
                    "Network Creation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *
     * A panel for creating neuron layers appropriate for ESNs... that is ones
     * which only specify a population and constrain the update rule to be
     * sigmoidal. Here as a convenience.
     *
     * @author Zach Tosi
     *
     */
    private static class NeuronLayerPanel extends JPanel {

        public static final Color TANH_COLOR = new Color(20, 100, 0);

        public static final Color LOG_COLOR = new Color(120, 120, 0);

        public static final Color ARCT_COLOR = new Color(120, 0, 120);

        private JTextField tfPopulation = new JTextField(5);

        {
            tfPopulation.setHorizontalAlignment(JTextField.RIGHT);
        }

        /** The label that displays the type of squashing function being used. */
        private JLabel typeLabel;

        /**
         * The triangle that when clicked displays or hides {@link #rulePanel}
         * so that specific attributes of the neuron update rule can be set.
         */
        private DropDownTriangle editTriangle;

        /** The sigmoidal rule panel: either discrete or continuous. */
        private AbstractSigmoidalRulePanel rulePanel;

        /** The parent dialog for resizing. */
        private final ESNCreationDialog parent;

        /**
         *
         * @param parent
         * @param timeType
         * @param initialFunc
         * @param initPop
         * @return
         */
        public static NeuronLayerPanel createNeuronLayerPanel(
                ESNCreationDialog parent, TimeType timeType,
                SquashingFunction initialFunc, int initPop) {
            NeuronLayerPanel nlp = new NeuronLayerPanel(parent, timeType,
                    initialFunc, initPop);
            nlp.setTimeType(timeType);
            nlp.rulePanel.getCbImplementation().setSelectedItem(initialFunc);
            nlp.initListeners();
            nlp.init();
            // TODO: Cleanup/generalize
            nlp.typeLabel.setForeground(TANH_COLOR);
            return nlp;
        }

        /**
         *
         * @param parent
         * @param timeType
         * @param initialFunc
         * @param initPop
         */
        private NeuronLayerPanel(ESNCreationDialog parent, TimeType timeType,
                SquashingFunction initialFunc, int initPop) {
            this.parent = parent;
            tfPopulation.setText(Integer.toString(initPop));
            editTriangle = new DropDownTriangle(UpDirection.LEFT, false,
                    "Edit", "Edit", parent);

        }

        /**
         *
         */
        private void init() {

            this.setLayout(new BorderLayout());

            // Add the basic info (pop and type label) panel
            GridLayout gl = new GridLayout(1, 4);
            gl.setVgap(5);
            JPanel basicPanel = new JPanel(gl);
            basicPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
            basicPanel.add(new JLabel("Neuron Type: "));
            typeLabel = new JLabel(rulePanel.getCbImplementation()
                    .getSelectedItem().toString());
            JPanel tSub = new JPanel(new FlowLayout());
            tSub.add(typeLabel);
            basicPanel.add(tSub);
            basicPanel.add(new JLabel("Population: "));
            JPanel tfSub = new JPanel(new FlowLayout());
            tfSub.add(tfPopulation);
            basicPanel.add(tfSub);
            this.add(basicPanel, BorderLayout.NORTH);

            // Add the drop-down triangle
            JPanel editWrapper = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            editWrapper
                    .setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 15));
            editWrapper.add(editTriangle);
            this.add(editWrapper, BorderLayout.CENTER);

            // Add the rule panel
            rulePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            rulePanel.setVisible(editTriangle.isDown());
            this.add(rulePanel, BorderLayout.SOUTH);

            resetLabelColors();
            repaint();
        }

        /**
         *
         * @return
         */
        public JTextField getTfPopulation() {
            return tfPopulation;
        }

        /**
         *
         */
        private void initListeners() {

            // Make the extra details about the implementation visible/invisible
            editTriangle.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    rulePanel.setVisible(editTriangle.isDown());
                    repaint();
                    parent.pack();
                }

                // Does nothing...
                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                }

                @Override
                public void mouseExited(MouseEvent e) {
                }
            });

        }

        /**
         *
         */
        public void resetLabelColors() {
            JComboBox impCb = rulePanel.getCbImplementation();
            SquashingFunction func = SquashingFunction.getFunctionFromIndex(impCb
                    .getSelectedIndex());
            typeLabel.setText(func.toString());
            // TODO: Cleanup/generalize
            if (func == SquashingFunction.TANH) {
                typeLabel.setForeground(TANH_COLOR);
            } else if (func == SquashingFunction.LOGISTIC) {
                typeLabel.setForeground(LOG_COLOR);
            } else {
                // TODO: Better assertion. Check ALL Squashing functions.
                assert func == SquashingFunction.ARCTAN : "No such squashing function";
                typeLabel.setForeground(ARCT_COLOR);
            }
        }

        /**
         *
         * @return
         * @throws IllegalArgumentException
         */
        public NeuronGroup createLayerFromData()
                throws IllegalArgumentException {
            Integer numNeurons = Utils.parseInteger(tfPopulation);
            if (numNeurons == null) {
                throw new IllegalArgumentException("Non-number population"
                        + " value.");
            }
            ArrayList<Neuron> neurons = new ArrayList<Neuron>(numNeurons);
            for (int i = 0; i < numNeurons; i++) {
                neurons.add(new Neuron(parent.networkPanel.getNetwork()));
            }
            rulePanel.commitChanges(neurons);
            return new NeuronGroup(parent.networkPanel.getNetwork(), neurons);
        }

        /**
         * Changes the time type and alters the update rule panel accordingly.
         * Also resizes/repaints the frame/panel.
         *
         * @param timeType
         */
        public void setTimeType(TimeType timeType) {
            if (timeType == TimeType.DISCRETE) {
                rulePanel = new DiscreteSigmoidalRulePanel();
            } else {
                rulePanel = new ContinuousSigmoidalRulePanel();
            }
            // Update the Type Label based on the selection in the combobox
            final JComboBox impCb = rulePanel.getCbImplementation();
            impCb.setSelectedIndex(SquashingFunction.getIndexFromFunction(DEFAULT_INITIAL_FUNCTION));
            impCb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SquashingFunction func = SquashingFunction
                            .getFunctionFromIndex(impCb.getSelectedIndex());
                    typeLabel.setText(func.toString());
                    // TODO: Cleanup/generalize
                    resetLabelColors();
                    repaint();
                }
            });
            removeAll();
            init();
            revalidate();
            parent.pack();
        }

    }

    /**
     * Main function to test dialog.
     * @param args
     */
    public static void main(String[] args) {
        ESNCreationDialog dialog = ESNCreationDialog
                .createESNDialog(new NetworkPanel(new Network()));
        dialog.setLocationRelativeTo(null);
        dialog.pack();
        dialog.setVisible(true);
    }

}
