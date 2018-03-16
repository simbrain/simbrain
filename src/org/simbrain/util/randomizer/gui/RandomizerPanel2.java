package org.simbrain.util.randomizer.gui;

import org.simbrain.network.core.Neuron;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.math.ProbDistributions.*;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.util.randomizer.Randomizer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

//TODO: Revisit name randomizer

/**
 * This is a panel that allows a randomizer or list of randomizers to be edited.
 * For example, a set of neurons might have different randomizers that all need to
 * be edited at once.  (Think more about this design...)
 */
public class RandomizerPanel2 extends JPanel {

    /**
     * The probability distributions being modified.
     */
    private final List<Randomizer> randomizerList; //TODO Rename

    /**
     * Combo box for choosing which distribution to use.
     */
    private final JComboBox<String> cbDistribution;

    /**
     * A reference to the parent window containing this panel for the purpose of
     * adjusting to different sized randomizer dialogs.
     */
    private final Window parent;

    /**
     * Main editor panel.
     */
    private AnnotatedPropertyEditor randomizerPanel;

    /**
     * A reference to the original panel, so that we can easily know if we are
     * writing to already existing randomizer or replacing them with
     * new rules.
     */
    private AnnotatedPropertyEditor startingPanel;

    /**
     * Associate names with property editor panels. Used in combo box.
     */
    private static final LinkedHashMap<String, AnnotatedPropertyEditor> DISTRIBUTION_MAP = new LinkedHashMap<>();

    static {
        DISTRIBUTION_MAP.put(new ExponentialDistribution().getName(), new AnnotatedPropertyEditor(new ExponentialDistribution()));
        DISTRIBUTION_MAP.put(new GammaDistribution().getName(), new AnnotatedPropertyEditor(new GammaDistribution()));
        DISTRIBUTION_MAP.put(new LogNormalDistribution().getName(), new AnnotatedPropertyEditor(new LogNormalDistribution()));
        DISTRIBUTION_MAP.put(new NormalDistribution().getName(), new AnnotatedPropertyEditor(new NormalDistribution()));
        DISTRIBUTION_MAP.put(new ParetoDistribution().getName(), new AnnotatedPropertyEditor(new ParetoDistribution()));
        DISTRIBUTION_MAP.put(new UniformDistribution().getName(), new AnnotatedPropertyEditor(new UniformDistribution()));
    }

    /**
     * Construct a panel to edit a single randomizer.
     *
     * @param rand   the randomizer
     * @param parent the parent window for nice resizing
     */
    public RandomizerPanel2(Randomizer rand, Window parent) {
        this(Collections.singletonList(rand), parent);
    }

    /**
     * Construct the randomize panel.
     *
     * @param randomizerList
     * @param parent
     */
    public RandomizerPanel2(List<Randomizer> randomizerList, Window parent) {
        this.randomizerList = randomizerList;
        this.parent = parent;
        cbDistribution = new JComboBox<String>(DISTRIBUTION_MAP.keySet().toArray(new String[DISTRIBUTION_MAP.size()]));
        checkRandomizerConsistency();
        startingPanel = randomizerPanel;
        addListeners();
        initializeLayout();
    }

    private void checkRandomizerConsistency() {


        //TODO: Ugly overlap with discrepancy case
        if(randomizerList.isEmpty()) {
            cbDistribution.addItem(SimbrainConstants.NULL_STRING);
            cbDistribution.setSelectedIndex(cbDistribution.getItemCount() - 1);
            // Simply to serve as an empty panel
            randomizerPanel = new AnnotatedPropertyEditor(Collections.emptyList());
            return;
        }

        Iterator<Randomizer> randomizerItr = randomizerList.iterator();
        Randomizer randomizerRef = randomizerItr.next();

        // Check whether the set of randomizers being edited are of the
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
            randomizerPanel = new AnnotatedPropertyEditor(Collections.emptyList());
        } else {
            // If they are the same type, use the appropriate editor panel.
            // Later if ok is pressed the values from that panel will be written
            // to the rules
            String distributionName = randomizerRef.getPdf().getName();
            randomizerPanel = DISTRIBUTION_MAP.get(distributionName);
//            randomizerPanel.fillFieldValues(randomizerList);
            cbDistribution.setSelectedItem(distributionName);
        }
    }

    private void initializeLayout() {

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel tPanel = new JPanel();
        tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.X_AXIS));
        tPanel.add(cbDistribution);
        int horzStrut = 30;

        // Create a minimum spacing
        tPanel.add(Box.createHorizontalStrut(horzStrut));

        // Give all extra space to the space between the components
        tPanel.add(Box.createHorizontalGlue());

        tPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tPanel.setBorder(padding);
        this.add(tPanel);

        this.add(Box.createRigidArea(new Dimension(0, 5)));

        randomizerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        randomizerPanel.setBorder(padding);
        randomizerPanel.setVisible(true);
        this.add(randomizerPanel);

        TitledBorder tb = BorderFactory.createTitledBorder("Randomizer");
        this.setBorder(tb);

    }

    private void addListeners() {

        // Change randomizer distribution
        cbDistribution.addActionListener(e -> {

            randomizerPanel = DISTRIBUTION_MAP.get(cbDistribution.getSelectedItem());

            // Is the current panel different from the starting panel?
            boolean replaceDistribution = randomizerPanel != startingPanel;


            // If so we have to fill the new panel with default values
            if (replaceDistribution) {
                randomizerPanel.fillDefaultValues();
            } else {
                // If not we can fill the new panel with values from the
                // neurons being edited.
                randomizerPanel.fillFieldValues(randomizerList);
            }

            // Tell the panel whether it will have to replace neuron
            // update rules or edit them upon commit.

            repaintPanel();
            repaint();
            parent.pack();
            parent.setLocationRelativeTo(null);
        });

    }

    /**
     * Called to repaint the panel based on changes in the to the selected
     * distribution.
     */
    public void repaintPanel() {
        removeAll();
        initializeLayout();
        repaint();
    }

    /**
     * Fill field values using specified list.
     *
     * @param randomizerList2 list of randomizers
     */
    public void fillFieldValues(List<Randomizer> randomizerList2) {
        randomizerPanel.fillFieldValues(randomizerList2);
    }

    /**
     * Commit changes to provided list of randomizers.
     *
     * @param randList list of randomizers
     */
    public void commitChanges(List<ProbabilityDistribution> randList) {
        randomizerPanel.commitChanges(randList);
    }

    /**
     * Fill fields to default values.
     */
    public void fillDefaultValues() {
        randomizerPanel.fillDefaultValues();
    }


    /**
     * Commit changes to randomizers
     */
    public boolean commitChanges() {
        ProbabilityDistribution selectedDistribution =
            (ProbabilityDistribution) randomizerPanel.getEditedObject();

        // If an inconsistent set of objects is being edited return with no action
        if (selectedDistribution == null) {
            return true;
        }

        for (Randomizer r : randomizerList) {
            // Only replace if this is a different rule (otherwise when
            // editing multiple rules with different parameter values which
            // have not been set those values will be replaced with the
            // default).
            if (!r.getPdf().getClass().equals(selectedDistribution.getClass())) {
                r.setPdf(selectedDistribution.deepCopy());
            }
        }

        List<EditableObject> distributionList = randomizerList.stream()
                .map(Randomizer::getPdf)
                .collect(Collectors.toList());
        startingPanel = randomizerPanel;
        randomizerPanel.commitChanges(distributionList);
        return true;
    }

    /**
     * Test main.
     */
    static JFrame frame = new JFrame();

    public static void main(String[] args) {
        Randomizer rd = new Randomizer(new NormalDistribution());
        RandomizerPanel2 rp = new RandomizerPanel2(rd, frame);
        rp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // rp.fillDefaultValues();
        frame.setContentPane(rp);
        frame.setVisible(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        rp.repaintPanel();
    }

}
