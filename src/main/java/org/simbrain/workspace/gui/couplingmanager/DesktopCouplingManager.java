package org.simbrain.workspace.gui.couplingmanager;

import kotlin.Pair;
import org.simbrain.util.ResourceManager;
import org.simbrain.util.SwingUtilsKt;
import org.simbrain.util.Theme;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.MismatchedAttributesException;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.gui.CouplingListPanel;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer;
import smile.math.matrix.Matrix;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.List;

/**
 * GUI dialog for creating couplings.
 */
public class DesktopCouplingManager extends JPanel {

    /**
     * Flag to ensure that only one dialog is opened at a time.
     */
    public static boolean isVisible;

    /**
     * List of producers.
     */
    private final AttributePanel producerPanel;

    /**
     * List of consumers.
     */
    private final AttributePanel consumerPanel;

    /**
     * Methods for making couplings.
     */
    private final String[] tempStrings = {"One to One", "One to Many"};

    /**
     * Methods for making couplings.
     */
    private final JComboBox<String> couplingMethodComboBox = new JComboBox<String>(tempStrings);

    /**
     * Reference to desktop.
     */
    private final SimbrainDesktop desktop;

    /**
     * Creates and displays the coupling manager.
     *
     * @param desktop reference to parent desktop
     */
    public DesktopCouplingManager(final SimbrainDesktop desktop) {
        super(new BorderLayout(Theme.componentGap, Theme.sectionGap));
        SwingUtilsKt.applyDialogPadding(this);
        this.desktop = desktop;
        isVisible = true;

        // Left Panel
        producerPanel = new AttributePanel(desktop.getWorkspace(), ProducerOrConsumer.Producing);
        producerPanel.setBorder(Theme.sectionBorder("Producers"));

        // Right Panel
        consumerPanel = new AttributePanel(desktop.getWorkspace(), ProducerOrConsumer.Consuming);
        consumerPanel.setBorder(Theme.sectionBorder("Consumers"));

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.componentGap, 0));
        var bluePairs = makeLegend("Text", String.class);
        var greenPairs = makeLegend("Array", double[].class);
        var orangePairs = makeLegend("Matrix", Matrix.class);
        var blackPairs = makeLegend("Number", double.class);
        legend.add(bluePairs.getFirst());
        legend.add(bluePairs.getSecond());
        legend.add(greenPairs.getFirst());
        legend.add(greenPairs.getSecond());
        legend.add(orangePairs.getFirst());
        legend.add(orangePairs.getSecond());
        legend.add(blackPairs.getFirst());
        legend.add(blackPairs.getSecond());

        // Bottom controls (trailing)
        JButton helpButton = new JButton(new ShowHelpAction("https://docs.simbrain.net/docs/workspace/couplings.html"));
        JButton addCouplingsButton = new JButton("Add Coupling(s)");
        addCouplingsButton.setToolTipText("Create couplings from currently selected producers and consumers");
        addCouplingsButton.setIcon(ResourceManager.getSmallIcon("menu_icons/plus.png"));
        addCouplingsButton.setActionCommand("addCouplings");
        addCouplingsButton.addActionListener((e) -> addCouplings());

        JPanel trailingControls = SwingUtilsKt.buttonRow(
            new Component[]{helpButton, couplingMethodComboBox, addCouplingsButton},
            FlowLayout.RIGHT,
            Theme.componentGap
        );

        JPanel bottomPanel = new JPanel(new BorderLayout(Theme.componentGap, 0));
        bottomPanel.add(legend, BorderLayout.WEST);
        bottomPanel.add(trailingControls, BorderLayout.EAST);

        // Center panel with couplings
        JComponent couplingList = new CouplingListPanel(desktop, desktop.getWorkspace().getCouplings());
        couplingList.setBorder(Theme.sectionBorder("Couplings"));

        // Main Panel
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, Theme.sectionGap, Theme.sectionGap));
        centerPanel.add(producerPanel);
        centerPanel.add(couplingList);
        centerPanel.add(consumerPanel);
        centerPanel.setPreferredSize(new Dimension(800, 400));
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(bottomPanel, BorderLayout.SOUTH);
    }

    private Pair<JLabel, JPanel> makeLegend(String label, Type dataType) {
        final int dimensions = 10;
        JLabel colorLabel = new JLabel(label);
        colorLabel.setForeground(getColor(dataType));
        JPanel colorBox = new JPanel();
        colorBox.setBackground(getColor(dataType));
        colorBox.setPreferredSize(new Dimension(dimensions, dimensions));
        return new Pair<>(colorLabel, colorBox);
    }

    /**
     * Add couplings using the selected method.
     */
    private void addCouplings() {
        List<Producer> producers = (List<Producer>) producerPanel.getSelectedAttributes();
        List<Consumer> consumers = (List<Consumer>) consumerPanel.getSelectedAttributes();

        if ((producers.size() == 0) || (consumers.size() == 0)) {
            JOptionPane.showMessageDialog(null, "You must select at least one consuming and producing attribute\nto create couplings.", "No Attributes Selected Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String couplingMethod = (String) couplingMethodComboBox.getSelectedItem();
            if (couplingMethod.equalsIgnoreCase("One to One")) {
                desktop.getWorkspace().getCouplingManager().createOneToOneCouplings(producers, consumers);
            } else if (couplingMethod.equalsIgnoreCase("One to Many")) {
                desktop.getWorkspace().getCouplingManager().createOneToManyCouplings(producers, consumers);
            }
        } catch (MismatchedAttributesException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Unmatched Attributes", JOptionPane.WARNING_MESSAGE, null);
        }
    }

    /**
     * Associates attribute and coupling data types (classes) with colors used
     * in displaying attributes and couplings.
     *
     * @param dataType the data type to associate with a color
     * @return the color associated with a data type
     */
    public static Color getColor(Type dataType) {
        if (dataType == double.class) {
            return Color.black;
        } else if (dataType == double[].class) {
            return Color.green.darker().darker();
        } else if (dataType == String.class) {
            return Color.blue.brighter();
        } else if (dataType == Matrix.class) {
            return new Color(255, 140, 0);
        }
        return Color.black;
    }

}
