package org.simbrain.util.randomizer.gui;

import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.randomizer.Randomizer;
import org.simbrain.util.widgets.EditablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

//TODO: Avoid all neuron dependencies.
// This is a panel that allows a randomizer or list of randomizers to be edited.
// For example, a set of neurons might have different randomizers that all need to
// be edited at once.  (Think more about this design...)
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

    /**
     * Construct a panel to edit a single randomizer.
     *
     * @param rand the randomizer
     * @param parent the parent window for nice resizing
     */
    public RandomizerPanel2(Randomizer rand, Window parent) {
        this(Collections.singletonList(rand), parent);
    }

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
//            String distributionName = randomizerRef.getPdf().getName();
//            distributionPanel = DISTRIBUTION_MAP.get(distributionName);
//            List<EditableObject> ruleList = randomizerList.stream().map(Neuron::getUpdateRule).collect(Collectors.toList());
//            neuronRulePanel.fillFieldValues(ruleList);
//            cbNeuronType.setSelectedItem(neuronName);
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

    /**
     * Test main.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        Randomizer rand = new Randomizer();
        RandomizerPanel2 rp = new RandomizerPanel2(rand, frame);
        rp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // rp.fillDefaultValues();
        frame.setContentPane(rp);
        frame.setVisible(true);
        frame.pack();
    }

}
