package org.simbrain.network.gui.dialogs.neuron.rule_panels;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.dialogs.neuron.AbstractNeuronRulePanel;
import org.simbrain.network.neuron_update_rules.KuramotoRule;
import org.simbrain.util.LabelledItemPanel;

/**
 * @author Amanda Pandey <amanda.pandey@gmail.com>
 */
public class KuramotoRulePanel extends AbstractNeuronRulePanel {

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Main tab. */
    private LabelledItemPanel mainTab = new LabelledItemPanel();

    /** A reference to the neuron update rule being edited. */
    private static final KuramotoRule prototypeRule = new KuramotoRule();

    /**
     * Creates an instance of this panel.
     */
    public KuramotoRulePanel() {
        this.add(tabbedPane);
        JTextField slopeField = createTextField(
                (r) -> ((KuramotoRule) r).getSlope(),
                (r, val) -> ((KuramotoRule) r).setSlope((double) val));
        JTextField biasField = createTextField(
                (r) -> ((KuramotoRule) r).getBias(),
                (r, val) -> ((KuramotoRule) r).setBias((double) val));
        mainTab.addItem("Slope", slopeField);
        mainTab.addItem("Bias", biasField);
        mainTab.addItem("Add noise", this.getAddNoise());
        tabbedPane.add(mainTab, "Main");

        tabbedPane.add(getNoisePanel(), "Noise");

    }

    @Override
    protected final NeuronUpdateRule getPrototypeRule() {
        return prototypeRule.deepCopy();
    }

}
