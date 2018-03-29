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
package org.simbrain.util.propertyeditor2;

import org.simbrain.network.gui.dialogs.neuron.NeuronPropertiesPanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.util.BiMap;
import org.simbrain.util.SimbrainConstants;
import org.simbrain.util.widgets.DropDownTriangle;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.List;

/**
 * Swing component that manages editing the types of a collection of objects,
 * where those objects have a field whose value can be objects of a variety of
 * specific types. The types are displayed using a combo-box and an {@link
 * AnnotatedPropertyEditor} sub panel for editing the objects themselves.  This
 * editor can be revealed using a drop-down triangle. Examples:
 * <p>
 * Neuron.neuronUpdateRule --> Linear, Integrateandfire, etc.
 * <p>
 * Synapse.learningRule --> Static, Hebbian,...
 * <p>
 * Synapse.spikeResponder --> JumpAndDecay
 * <p>
 * ProbabilityDistribution --> Uniform, Normal,...
 * <p>
 * When loading the editor, if the objects are in in a consistent state then
 * they can be edited as usual with an annotated property editor. If they are in
 * an inconsistent state (e.g. some neurons are linear, some are Decay), then a
 * "..." appears in the combo box and no editor panel is displayed. If "ok" is
 * pressed at this point nothing is written to the objects. If the combo box is
 * changed at any point the default values for the new type are shown, and
 * pressing ok writes those values to every object being edited.
 * <p>
 * HOW TO USE IT. To use this class (1) be sure designate the relevant type
 * superclass (e.g. NeuronUpdateRule, LearningRule, etc.) as a {@link
 * CopyableObject}, and (2) annotate the field itself (e.g.
 * Neuron.neuronUpdateRule) using the {@link org.simbrain.util.UserParameter}
 * annotation with "multi-state" set to true. Then (3) use it as part of a
 * higher level AnnotatedPropertyEditor.
 * <p>
 * The ObjectTypeEditor can be used on its own but is currently written to be
 * used inside a a higher level {@link AnnotatedPropertyEditor} as the widget
 * created by {@link org.simbrain.util.widgets.ParameterWidget} for fields with
 * the {@link org.simbrain.util.UserParameter} annotation with "multi-state" set
 * to true.  So the ObjecTypeEditor is both inside an AnnotatedPropertyEditor
 * and has one as a field, allowing for hierarchies of editors within one
 * another (e.g. editing a neuron's update rule, and within that, its noise
 * generator).
 */
public class ObjectTypeEditor extends JComponent {

    /**
     * The main collection of objects being edited
     */
    private List<CopyableObject> objectList;

    /**
     * The prototype object that is used to set the values of the object list
     * whe committing.
     */
    private CopyableObject prototypeObject;

    /**
     * The possible types of this object
     */
    private JComboBox<String> cbObjectType;

    /**
     * Editor panel for the set of objects (null panel if they are
     * inconsistent)
     */
    private AnnotatedPropertyEditor editorPanel;

    /**
     * Mapping from labels to types and back again.
     */
    private BiMap<String, Class> typeMap;

    /**
     * Label for border around editor
     */
    private String label;

    /**
     * For showing/hiding the property editor
     */
    private DropDownTriangle detailTriangle;

    /**
     * A reference to the parent window containing this panel for the purpose of
     * adjusting to different sized dialogs. Since this is a hassle this is used
     * as little as possible in favor of finding the parent at runtime.
     */
    private Window parent;

    /**
     * True if the combo box is changed from its initial state.
     */
    private boolean cbChanged;

    /**
     * Container for editor panel used to clear the panel on updates.
     */
    private JPanel editorPanelContainer;

    /**
     * Used to track whether the combo box has changed
     */
    private Object cbStartState;

    /**
     * Create the editor with a type map, but no list of objects yet.  The
     * editor is uninitalized!
     *
     * @param tm    type map
     * @param label label to use for display
     * @return the editor object
     */
    public static ObjectTypeEditor createUninitializedEditor(BiMap<String, Class> tm, String label) {
        return new ObjectTypeEditor(tm, label);
    }

    /**
     * Create the editor in an uninitalized state using only the type map.
     * Assumes objects will be loaded using the setObjects method.
     *
     * @param typeMap the type map
     */
    private ObjectTypeEditor(BiMap<String, Class> typeMap, String label) {
        this.typeMap = typeMap;
        this.label = label;
    }

    /**
     * Create the editor from a set of objects and a type map. Currently just
     * used by the test method.  When used in a higher level
     *
     * @param objectList the list of objects to edit
     * @param typeMap    the mapping from strings to types
     * @param parent     the parent window
     */
    private ObjectTypeEditor(List objectList, BiMap<String, Class> typeMap, Window parent) {
        this.objectList = objectList;
        this.parent = parent;
        this.typeMap = typeMap;
        setObjects(objectList);
    }

    /**
     * Initialize the editor with a list of objects.
     *
     * @param objects the list of objects to edit
     */
    public void setObjects(List<CopyableObject> objects) {
        this.objectList = objects;
        if (objectList.isEmpty()) {
            throw new IllegalStateException("Can't edit empty list of objects");
        }
        //TODO: Feels redundant with check in APE.fillFieldValues. Can this check be ommitted?
        boolean consistent = objectList.stream().allMatch(o -> o.getClass() == objectList.get(0).getClass());
        if (!consistent) {
            editorPanel = new AnnotatedPropertyEditor(Collections.emptyList());
        } else {
            editorPanel = new AnnotatedPropertyEditor(objectList);
        }
        layoutPanel();
    }

    /**
     * Initialize the panel.
     */
    private void layoutPanel() {

        // General layout setup
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Border padding = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        // Top Panel contains the combo box and detail triangle
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        TitledBorder tb = BorderFactory.createTitledBorder(label);
        this.setBorder(tb);
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.setBorder(padding);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(topPanel);

        // Set up the combo box
        cbObjectType = new JComboBox<String>();
        for (String label : typeMap.keySet()) {
            cbObjectType.addItem(label);
        }
        topPanel.add(cbObjectType);
        addDropDownListener();

        // Set up detail triangle
        detailTriangle = new DropDownTriangle(DropDownTriangle.UpDirection.LEFT, false, "Settings", "Settings", parent);
        detailTriangle.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent arg0) {
                if (parent == null) {
                    parent = SwingUtilities.getWindowAncestor(ObjectTypeEditor.this);
                }
                editorPanel.setVisible(detailTriangle.isDown());
                repaint();
                parent.pack();
            }

        });
        topPanel.add(Box.createHorizontalStrut(30));
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(detailTriangle);

        // Container for editor panel, so that it can easily be refreshed
        editorPanelContainer = new JPanel();
        this.add(editorPanelContainer);
        editorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        editorPanel.setBorder(padding);
        editorPanelContainer.add(editorPanel);

    }

    /**
     * Fill field values for {@link AnnotatedPropertyEditor} representing
     * objects being edited.
     */
    public void fillFieldValues() {
        editorPanel.fillFieldValues(objectList);
    }

    //TODO: Methods below are confusing / states of this dialog should be made more clear.
    // They are called once at startup (which "changes" the combo box, which in turn
    // makes it hard to track "first change" to combo box).  But then can also be
    // called at other times too, so they can't be assumed to just be for initialization.

    /**
     * Set the current object type being edited, and update the property editor
     * accordingly.
     * <p>
     * Should only be called once.
     *
     * @param object current object value
     */
    public void setValue(Object object) {
        cbObjectType.setSelectedItem(typeMap.getInverse((Class) object));
        cbStartState = cbObjectType.getSelectedItem();
        resetStuff();
    }

    /**
     * Set the editor to a null state.
     * <p>
     * Should only be called once.
     */
    public void setNull() {
        cbObjectType.removeAllItems();
        cbObjectType.addItem(SimbrainConstants.NULL_STRING);
        for (String label : typeMap.keySet()) {
            cbObjectType.addItem(label);
        }
        cbObjectType.setSelectedIndex(0);
        cbObjectType.repaint();
        Object cbStartState = cbObjectType.getSelectedItem();
        editorPanelContainer.removeAll(); // TODO More hacking
        resetStuff();
    }

    //TODO: besides the obviously bad name, this is managing some
    // state information.
    private void resetStuff() {
        editorPanel.setVisible(detailTriangle.isDown());
        prototypeObject = null; // This is needed for prototype mode to be correct
    }

    // TODO: This is used alongside start state to determine if the combo box
    // has chnaged. There must be a less ugly way to do this.
    private boolean neverChanged = true;

    // TODO: Make sure the below is called just once per component.

    /**
     * Initialize the combo box to react to events.
     */
    private void addDropDownListener() {

        cbObjectType.addActionListener(e -> {

            // Handle change logic.  We need to know when the drop down box is
            // changed.  If it is not change there is no need to change the
            // types of any objects when closing.
            if (neverChanged) {
                cbChanged = cbStartState != cbObjectType.getSelectedItem();
                if (!cbChanged) {
                    return;
                }
            }
            neverChanged = false;

            // TODO: The null string should not be around, so this should  not be
            // needed.
            if (cbObjectType.getSelectedItem() == SimbrainConstants.NULL_STRING) {
                return;
            }

            // Create a prototype object and refresh editor panel
            try {
                Class proto = typeMap.get((String) cbObjectType.getSelectedItem());
                // TODO: This shouldn't happen. Maybe remove and document any error that occurs.
                if (proto == null) {
                    return;
                }
                prototypeObject = (CopyableObject) proto.newInstance();
                editorPanel = new AnnotatedPropertyEditor(prototypeObject);
                editorPanelContainer.removeAll();
                editorPanelContainer.add(editorPanel);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            // Can't go back to null once leaving it
            cbObjectType.removeItem(SimbrainConstants.NULL_STRING);

            // TODO: Remove or at least simplify reliance on this utter crap
            // Maybe move to some utility class.  Like Simbrain.pack().
            if (parent == null) {
                parent = SwingUtilities.getWindowAncestor(this);
            }
            if (parent != null) {
                parent.pack();
            }

        });
    }

    /**
     * If true,then the combo box now holds a prototype object which will be
     * used to write the values of edited objects on close.
     *
     * @return true if in prototype mode, false otherwise
     */
    public boolean prototypeMode() {
        // TODO: Bad using null for state?
        return prototypeObject != null;
    }

    /**
     * The current value of this widget. It should be the object that can be
     * copied, or null.
     *
     * @return the object to be copied when done, or null.
     */
    public Object getValue() {
        if (cbObjectType.getSelectedItem() == SimbrainConstants.NULL_STRING) {
            return null;
        } else {
            if (prototypeMode()) {
                return prototypeObject;
            } else {
                return objectList.get(0);
            }
        }

    }

    /**
     * Helper to sync the prototype object to the current {@link AnnotatedPropertyEditor}
     * (whose state may have been set before the object was created).
     */
    public void syncPrototype() {
        editorPanel.commitChanges(Arrays.asList(prototypeObject));
    }

    /**
     * Commit any changes made.
     */
    public void commitChanges() {

        //System.out.println("ObjectTypeEditor.commitChanges");
        //System.out.println("Prototype Mode: " + prototypeMode());

        editorPanel.commitChanges(objectList);

    }

    /**
     * Test main.
     */
    public static void main(String[] args) {


        //LinearRule rule = new LinearRule();
        //rule.setBias(.5);
        //List objectList = Arrays.asList(rule);
        List objectList = Arrays.asList(new LinearRule(), new LinearRule());
        //List objectList = Arrays.asList(new LinearRule(), new ThreeValueRule() );

        System.out.println("Initialize:");
        System.out.println(Arrays.asList(objectList));

        JFrame frame = new JFrame();
        ObjectTypeEditor editor = new ObjectTypeEditor(objectList, NeuronPropertiesPanel.getTypeMap(), frame);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg) {
                System.out.println("After committ:");
                editor.commitChanges();
                System.out.println(Arrays.asList(objectList));
                //System.out.println(rule.getBias());
                frame.dispose();
            }
        });
        frame.setContentPane(editor);
        frame.setVisible(true);
        frame.pack();
    }


}
