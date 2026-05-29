package org.simbrain.workspace.gui.couplingmanager;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.SwingUtilsKt;
import org.simbrain.workspace.*;
import org.simbrain.workspace.events.WorkspaceComponentEvents;
import org.simbrain.workspace.events.WorkspaceEvents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static org.simbrain.util.SwingUtilsKt.getSwingDispatcher;

/**
 * Displays a panel with a JComboBox, which the user uses to select a component,
 * and a JList of attributes for that component.
 */
public class AttributePanel extends JPanel implements ActionListener, MouseListener {

    /**
     * Drop down box for workspace components.
     */
    private final ComponentDropDownBox componentComboBox;

    /**
     * List of Attributes in a specified Component.
     */
    private final JList attributeList;

    /**
     * List model.
     */
    private final DefaultListModel<Attribute> model;

    /* Whether this Panel displays producers or consumers. */
    public enum ProducerOrConsumer {
        Producing, Consuming
    }

    private final ProducerOrConsumer producerOrConsumer;

    /**
     * Panel for setting visibility of attribute types.
     */
    private AttributeTypePanel attributeTypePanel;

    /**
     * Creates a new attribute list panel.
     *
     * @param workspace reference to workspace
     * @param attributeType
     */
    public AttributePanel(Workspace workspace, ProducerOrConsumer attributeType) {
        super(new BorderLayout(0, org.simbrain.util.Theme.componentGap));
        this.producerOrConsumer = attributeType;

        // Set up attribute lists
        model = new DefaultListModel<Attribute>();
        attributeList = new JList<Attribute>(model);
        attributeList.setCellRenderer(new AttributeCellRenderer());
        attributeList.addMouseListener(this);

        // Component list box
        componentComboBox = new ComponentDropDownBox(workspace);
        componentComboBox.addActionListener(this);
        JPanel componentPanel = new JPanel();
        componentPanel.setLayout(new BorderLayout());
        componentPanel.add(componentComboBox, BorderLayout.WEST);
        add(componentPanel, BorderLayout.NORTH);

        // Scroll pane
        JScrollPane listScroll = new JScrollPane(attributeList);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(listScroll, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottomPanel = new JPanel();
        JButton button = new JButton("Set Visibility");
        button.addActionListener(evt -> showAttributeTypePanel());
        bottomPanel.add(button);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listen for attribute changes
        for (WorkspaceComponent component : workspace.getComponentList()) {
            addAttributeListener(component);
        }
    }

    private void showAttributeTypePanel() {
        if (attributeTypePanel != null) {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            StandardDialog dialog = new StandardDialog(parentWindow, "Set Visibility");
            dialog.setAsDoneDialog();
            dialog.setContentPane(attributeTypePanel);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);
                    refresh((WorkspaceComponent) componentComboBox.getSelectedItem());
                }
            });
            dialog.setModal(true);
            dialog.makeVisible();
        }
    }

    /**
     * Add a listener to the specified workspace component.
     *
     * @param component component to listen to
     */
    private void addAttributeListener(WorkspaceComponent component) {

        WorkspaceComponentEvents events = component.getEvents();

        var attributeContainerAddedCleanupHandler = events.getAttributeContainerAdded().on(getSwingDispatcher(), ac -> {
            if (isSelectedComponent(component)) {
                refresh(component);
            }
        });

        var attributeContainerRemovedCleanupHandler = events.getAttributeContainerRemoved().on(getSwingDispatcher(), ac -> {
            if (isSelectedComponent(component)) {
                refresh(component);
            }
        });

        // Add property change listener to detect when the panel is disposed
        SwingUtilsKt.onWindowClose(this, () -> {
            attributeContainerAddedCleanupHandler.invoke();
            attributeContainerRemovedCleanupHandler.invoke();
        });

    }

    /**
     * Returns true if the component is current, false otherwise.
     *
     * @param component the component to check
     * @return whether the component is current
     */
    private boolean isSelectedComponent(WorkspaceComponent component) {
        return component == componentComboBox.getSelectedItem();
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        // Refresh component list
        if (event.getSource() instanceof JComboBox source) {
            WorkspaceComponent component = (WorkspaceComponent) source.getSelectedItem();
            refresh(component);
        }
    }

    /**
     * Refresh attribute list.
     */
    private void refresh(WorkspaceComponent component) {
        if (component != null) {
            model.clear();
            if (producerOrConsumer == ProducerOrConsumer.Producing) {
                final var iterator = component.getCouplingManager().getVisibleProducers(component).iterator();
                while (iterator.hasNext()) {
                    final var producer = iterator.next();
                    model.addElement(producer);
                }
            } else {
                final var iterator = component.getCouplingManager().getVisibleConsumers(component).iterator();
                while (iterator.hasNext()) {
                    final var consumer = iterator.next();
                    model.addElement(consumer);
                }
            }
            attributeTypePanel = new AttributeTypePanel(component, producerOrConsumer);
        }
    }

    /**
     * Clear attribute list.
     */
    private void clearList() {
        model.clear();
    }

    /**
     * Returns selected attributes.
     *
     * @return list of selected attributes.
     */
    public List<? extends Attribute> getSelectedAttributes() {
        if (producerOrConsumer == ProducerOrConsumer.Producing) {
            List<Producer> ret = new ArrayList<>();
            for (Object object : attributeList.getSelectedValuesList()) {
                ret.add((Producer) object);
            }
            return ret;
        } else if (producerOrConsumer == ProducerOrConsumer.Consuming) {
            List<Consumer> ret = new ArrayList<>();
            for (Object object : attributeList.getSelectedValuesList()) {
                ret.add((Consumer) object);
            }
            return ret;
        }
        return null;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Custom attribute renderer for JList.
     */
    private class AttributeCellRenderer extends DefaultListCellRenderer {

        public java.awt.Component getListCellRendererComponent(JList list, Object object, int index, boolean isSelected, boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer) super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            // Set text color based on data type
            Attribute attribute = (Attribute) object;
            renderer.setForeground(DesktopCouplingManager.getColor(attribute.getType()));
            return renderer;
        }

    }

    /**
     * A JComboBox which listens to the workspace and updates accordingly.
     */
    private class ComponentDropDownBox extends JComboBox {

        /**
         * Reference to workspace.
         */
        private final Workspace workspace;

        /**
         * @param workspace the workspace
         */
        public ComponentDropDownBox(final Workspace workspace) {
            this.workspace = workspace;
            for (WorkspaceComponent component : workspace.getComponentList()) {
                this.addItem(component);
            }
            if (this.getModel().getSize() > 0) {
                this.setSelectedIndex(0);
                AttributePanel.this.refresh((WorkspaceComponent) this.getItemAt(0));
            }

            WorkspaceEvents events = workspace.getEvents();

            events.getComponentAdded().on(getSwingDispatcher(), wc -> {
                this.addItem(wc);
                addAttributeListener(wc);
            });

            events.getComponentRemoved().on(getSwingDispatcher(), wc -> {
                this.removeItem(wc);
                if (this.getItemCount() == 0) {
                    AttributePanel.this.clearList();
                }
            });

            events.getWorkspaceCleared().on(getSwingDispatcher(), () -> {
                this.removeAllItems();
                AttributePanel.this.clearList();
            });
        }

        public boolean clearWorkspace() {
            return false;
        }
    }
}
