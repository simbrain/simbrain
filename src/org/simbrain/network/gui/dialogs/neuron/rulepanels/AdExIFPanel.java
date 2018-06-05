//package org.simbrain.network.gui.dialogs.neuron.rulepanels;
//
//import javax.swing.JTabbedPane;
//
//import org.simbrain.network.neuron_update_rules.AdExIFRule;
//import org.simbrain.util.LabelledItemPanel;
//import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
//
//public class AdExIFPanel extends AnnotatedPropertyEditor {
//
//    /** Organize the giant cauldron of params for this rule. */
//    private JTabbedPane tabbedPane;
//
//    public AdExIFPanel(AdExIFRule rule) {
//        super(rule);
//    }
//
//    @Override
//    protected void initPanel() {
//        super.initPanel();
//        this.removeAll();
//        tabbedPane = new JTabbedPane();
//        this.add(tabbedPane);
//
//        LabelledItemPanel membraneVoltagePanel  = new LabelledItemPanel();
//        membraneVoltagePanel.addItem("Peak Voltage (mV)", getWidget("Peak Voltage (mV)").component);
//        membraneVoltagePanel.addItem("Threshold voltage (mV)", getWidget("Threshold voltage (mV)").component);
//        membraneVoltagePanel.addItem("Reset voltage (mV)", getWidget("Reset voltage (mV)").component);
//        membraneVoltagePanel.addItem("Capacitance (μF)", getWidget("Capacitance (μF)").component);
//        membraneVoltagePanel.addItem("Background Current (nA)", getWidget("Background Current (nA)").component);
//        membraneVoltagePanel.addItem("Slope Factor", getWidget("Slope Factor").component);
//        membraneVoltagePanel.addItem("Add noise", getWidget("Add noise").component);
//
//        LabelledItemPanel currentTab = new LabelledItemPanel();
//        currentTab.addItem("Leak Conductance (nS)", getWidget("Leak Conductance (nS)").component);
//        currentTab.addItem("Max Ex. Conductance (nS)", getWidget("Max Ex. Conductance (nS)").component);
//        currentTab.addItem("Max In. Conductance (nS)", getWidget("Max In. Conductance (nS)").component);
//        currentTab.addItem("Leak Reversal (mV)", getWidget("Leak Reversal (mV)").component);
//        currentTab.addItem("Excitatory Reversal (mV)", getWidget("Excitatory Reversal (mV)").component);
//        currentTab.addItem("Inbitatory Reversal (mV)", getWidget("Inbitatory Reversal (mV)").component);
//
//        LabelledItemPanel adaptationTab = new LabelledItemPanel();
//        adaptationTab.addItem("Reset (nA)", getWidget("Reset (nA)").component);
//        adaptationTab.addItem("Coupling Const.", getWidget("Coupling Const.").component);
//        adaptationTab.addItem("Time constant (ms)", getWidget("Time constant (ms)").component);
//
//        tabbedPane.add(membraneVoltagePanel, "Membrane Voltage");
//        tabbedPane.add(currentTab, "Input Currents");
//        tabbedPane.add(adaptationTab, "Adaptation");
//    }
//}
