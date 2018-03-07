package org.simbrain.util.randomizer.gui;

import java.awt.Window;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.math.ProbDistribution;
import org.simbrain.util.math.PropDistribution.NormalDistribution;
import org.simbrain.util.math.PropDistribution.UniformDistribution;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.EditablePanel;

public class RandomizerPanel2 extends JPanel implements EditablePanel{
    
    /**
     * The neurons being modified.
     */
    private final List<Randomizer> randomizerList;

    private final JComboBox<String> cbDistribution;

    private static final LinkedHashMap<String, AnnotatedPropertyEditor> DISTRIBUTION_MAP = new LinkedHashMap<>();

    static {
        DISTRIBUTION_MAP.put(new NormalDistribution().getName(), new AnnotatedPropertyEditor(new NormalDistribution()));
        DISTRIBUTION_MAP.put(new UniformDistribution().getName(), new AnnotatedPropertyEditor(new UniformDistribution()));
    }

    /**
     * Distribution panel.
     */
    private AnnotatedPropertyEditor distributionPanel;
    
    public RandomizerPanel2(List<Randomizer> randomizerList, Window parent) {
        this.randomizerList = randomizerList;
        cbDistribution = new JComboBox<String>(DISTRIBUTION_MAP.keySet().toArray(new String[DISTRIBUTION_MAP.size()]));
    }

    
    private void checkNeuronConsistency() {

        // TODO: Better handling of mixed case with activity generators. Warn
        // against it
        // or if allowing it, change the shape of the neuron to match.

        Iterator<Randomizer> randomizerItr = randomizerList.iterator();
        Randomizer randomizerRef = randomizerItr.next();

        // Check whether the set of synapses being edited are of the
        // same type or not
        boolean discrepancy = false;
        while (randomizerItr.hasNext()) {
            if (!randomizerRef.getPdf().getClass().equals(randomizerItr.next().getPdf().getClass())) {
                discrepancy = true;
                break;
            }
        }

        if (discrepancy) {
            cbDistribution.addItem(SimbrainConstants.NULL_STRING);
            cbDistribution.setSelectedIndex(cbDistribution.getItemCount() - 1);
            // Simply to serve as an empty panel
            distributionPanel = new AnnotatedPropertyEditor(Collections.emptyList());
        } else {
            // If they are the same type, use the appropriate editor panel.
            // Later if ok is pressed the values from that panel will be written
            // to the rules
            String distributionName = randomizerRef.getPdf().getName();
            distributionPanel = DISTRIBUTION_MAP.get(distributionName);
            List<EditableObject> ruleList = randomizerList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());
            neuronRulePanel.fillFieldValues(ruleList);
            cbNeuronType.setSelectedItem(neuronName);
        }
    }
    
    @Override
    public void fillFieldValues() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean commitChanges() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public JPanel getPanel() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
