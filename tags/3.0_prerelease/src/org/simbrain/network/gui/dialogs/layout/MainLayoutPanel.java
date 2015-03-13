/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs.layout;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.HexagonalGridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.layouts.LineLayout;
import org.simbrain.util.widgets.DropDownTriangle;
import org.simbrain.util.widgets.DropDownTriangle.UpDirection;

/**
 * All the functions of a layout selector in one panel. MainLayoutPanel lets the
 * user switch between layout types and optionally displays the layout panels
 * accordingly. Designed to be a complete panel for setting layouts which can
 * easily be added to any panel/dialog in Simbrain.
 *
 * @author ztosi
 *
 */
@SuppressWarnings("serial")
public class MainLayoutPanel extends JPanel {

    /** Default Visibility of layout parameters. */
    private static final boolean DEFAULT_DP_TRIANGLE_VISIBILITY = true;

    /** Default starting layout (Grid Layout). */
    private static final Layout DEFAULT_INITIAL_LAYOUT = new GridLayout();

    /** A holder for the currently displayed panel. */
    private AbstractLayoutPanel layoutPanel;

    /** A map tying layout panels to their names for use in a combo box . */
    private final LinkedHashMap<String, AbstractLayoutPanel> panelMap =
            new LinkedHashMap<String, AbstractLayoutPanel>();

    {
        // Populate the panel map
        panelMap.put(new GridLayout().getDescription(), new GridLayoutPanel(
                new GridLayout()));
        panelMap.put(new HexagonalGridLayout().getDescription(),
                new HexagonalGridLayoutPanel(new HexagonalGridLayout()));
        panelMap.put(new LineLayout().getDescription(), new LineLayoutPanel(
                new LineLayout()));
    }

    /** A combo box for selecting the type of layout. */
    private final JComboBox<String> layoutCb = new JComboBox<String>(panelMap
            .keySet().toArray(new String[panelMap.size()]));

    /**
     * A drop-down triangle for showing or hiding the layout parameters.
     * Parameters are hidden by default
     */
    private final DropDownTriangle layoutParameterReveal;

    /**
     * A boolean value for setting whether or not the panel has a drop-down
     * triangle.If no, then by default the layout parameters are visible. This
     * is for cases where a layout panel is necessary, but where even the option
     * to hide the parameters would be inappropriate or aesthetically poor.
     */
    private final boolean revealOption;

    /**
     * Flag to determine whether the panel should have an apply button that
     * allows the current layout to be immediately applied to the layout's
     * neurons.
     */
    private boolean useApplyButton;

    /**
     * The apply button. It's action must be supplied by external users of this
     * class.
     */
    private JButton applyButton = new JButton("Apply");

    /**
     * A reference to the parent window for resizing.
     */
    private final Window parent;

    /**
     * Creates the main layout panel with default values.
     * @param parent the parent window, used for resizing
     */
    public MainLayoutPanel(Window parent) {
        this(DEFAULT_DP_TRIANGLE_VISIBILITY, parent);
    }

    /**
     * Creates the main layout panel with default initially selected layout, but
     * where the reveal option can be set.
     *
     * @param revealOption if false, then everything shows up; if true then it's
     *            hidden with a drop-down triangle to reveal
     * @param parent the parent window, used for resizing
     */
    public MainLayoutPanel(boolean revealOption, Window parent) {
        this(DEFAULT_INITIAL_LAYOUT, revealOption, parent);
    }

    /**
     * Creates the main layout panel.
     *
     * @param initialLayout the initially selected layout
     * @param revealOption whether or not displaying layout parameters is
     *            optional
     * @param parent the parent window, used for resizing
     */
    public MainLayoutPanel(Layout initialLayout, boolean revealOption,
            Window parent) {
        this.revealOption = revealOption;
        this.parent = parent;
        layoutParameterReveal = new DropDownTriangle(UpDirection.LEFT, false,
                "Settings", "Settings", parent);
        layoutCb.setSelectedItem(initialLayout.getDescription());
        layoutPanel = panelMap.get(initialLayout.getDescription());
        layoutPanel.setNeuronLayout(initialLayout);

        initializeLayout();
        addListeners();
    }

    /**
     * Lays out the contents of the panel.
     */
    private void initializeLayout() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        if (revealOption) { // Lay out the panel with a drop-down triangle.
            JPanel topPanel = new JPanel();
            topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
            topPanel.add(layoutCb);
            topPanel.add(Box.createHorizontalStrut(100));
            topPanel.add(layoutParameterReveal);
            topPanel.setAlignmentX(CENTER_ALIGNMENT);
            topPanel.setBorder(padding);
            add(topPanel);
        } else { // Lay out the panel without a drop-down triangle
            layoutCb.setAlignmentX(RIGHT_ALIGNMENT);
            layoutCb.setBorder(padding);
            add(layoutCb);
        }

        // Layout panel is visible regardless of triangle state if the reveal
        // option is false. Otherwise display the layout panel based on the
        // state of the drop-down triangle.
        layoutPanel.setVisible(layoutParameterReveal.isDown() || !revealOption);
        layoutPanel.setAlignmentX(CENTER_ALIGNMENT);
        add(layoutPanel);
        if (revealOption) {
            setBorder(BorderFactory.createTitledBorder("Layout"));
        }

        // Handle apply button
        if (useApplyButton) {
            JPanel applyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            applyPanel.add(applyButton);
            add(Box.createRigidArea(new Dimension(0, 10)));
            // TODO Can't get add(new Box.Filler...) to work to push apply
            // button to bottom of panel
            add(applyPanel);
        }

    }

    /**
     * Adds listeners to the dialog
     */
    private void addListeners() {

        // Change the layout panel based on the selection in the combo box
        layoutCb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                layoutPanel = panelMap.get(layoutCb.getSelectedItem());
                repaintPanel();
                parent.pack();
            }

        });

        // Reveal or hide the layout panel
        layoutParameterReveal.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent arg0) {

                layoutPanel.setVisible(layoutParameterReveal.isDown());
                repaint();
                parent.pack();

            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

        });

    }

    /**
     * Called to repaint the panel based on changes in the to the selected
     * neuron type.
     */
    public void repaintPanel() {
        removeAll();
        initializeLayout();
        repaint();
    }

    /**
     * Called externally to change values in the layout reflecting changes in
     * the dialog.
     */
    public void commitChanges() {
        layoutPanel.commitChanges();
    }

    /**
     * Sets the neuron layout using a layout object.
     *
     * @param layout the layout to set
     */
    public void setCurrentLayout(final Layout layout) {
        layoutCb.setSelectedItem(layout.getDescription());
        panelMap.get(layoutCb.getSelectedItem()).setNeuronLayout(layout);
    }

    /**
     * Returns the current layout being edited.
     *
     * @return the current layout
     */
    public Layout getCurrentLayout() {
        return layoutPanel.getNeuronLayout();
    }

    /**
     * @return the useApplyButton
     */
    public boolean isUseApplyButton() {
        return useApplyButton;
    }

    /**
     * Set whether to use the apply button. If true, then an action listener for
     * the button must also be supplied. If false set the action listener to
     * null.
     *
     * @param useApplyButton the useApplyButton to set
     * @param action the action to invoke when pressing the button
     */
    public void setUseApplyButton(boolean useApplyButton,
            final ActionListener action) {
        this.useApplyButton = useApplyButton;
        if (action != null) {
            applyButton.addActionListener(action);
        }
        repaintPanel();
    }
}
