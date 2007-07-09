package org.simbrain.workspace.couplingmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Graphical element for managing coupling of objects.
 */
public class CouplingManager extends JPanel implements ActionListener {

    /** List of consumers. */
    private ArrayList consumers;

    /** List of consumers for use in dialog. */
    private JList consumerJList = new JList();

    /** Combo box of available consumers. */
    private JComboBox consumerComboBox = new JComboBox();

    /** TODO: Remove when a method for saving all is finished. */
    private WorkspaceComponent currentComponent = null;

    /** List of producers. */
    private ArrayList producers;

    /** List of producers for use in dialog. */
    private JList producerJList = new JList();

    /** Combo box of available producers. */
    private JComboBox producerComboBox = new JComboBox();

    /** Area of coupled items. */
    private CouplingTray couplingTray = new CouplingTray();

    /** Reference of parent frame. */
    private final JFrame frame;

    /**
     * Default constructor. Creates and displays the coupling manager.
     * @param frame parent of panel.
     */
    public CouplingManager(final JFrame frame) {
        super();

        this.frame = frame;
        ComponentList componentList = new ComponentList(Workspace.getInstance().getComponentList());

        ///////////////
        // CONSUMERS //
        ///////////////
        JPanel leftPanel = new JPanel(new BorderLayout());
        Border leftBorder = BorderFactory.createTitledBorder("Consumers");
        leftPanel.setBorder(leftBorder);
        addConsumerContextMenu(consumerJList);
        consumerJList.setDragEnabled(true);
        consumerJList.setPreferredSize(new Dimension(150, 300));
        consumerJList.setTransferHandler(new CouplingTransferHandler("consumers"));
        consumerJList.setCellRenderer(new ConsumerCellRenderer());
        consumerComboBox.setModel(componentList);
        consumerComboBox.addActionListener(this);
        leftPanel.add("North", consumerComboBox);
        leftPanel.add("Center", consumerJList);

        ///////////////
        // TRAY      //
        ///////////////
        JPanel middleCenterPanel = new JPanel();
        CouplingList trayModel = new CouplingList();
        couplingTray.setModel(trayModel);
        couplingTray.setPreferredSize(new Dimension(250, 350));
        Border centerBorder = BorderFactory.createTitledBorder("Couplings");
        middleCenterPanel.setBorder(centerBorder);
        middleCenterPanel.add(couplingTray);

        ///////////////
        // PRODUCERS //
        ///////////////
        JPanel rightPanel = new JPanel(new BorderLayout());
        Border rightBorder = BorderFactory.createTitledBorder("Producers");
        rightPanel.setBorder(rightBorder);
        JPanel rightCenterPanel = new JPanel();
        producerJList.setDragEnabled(true);
        producerJList.setPreferredSize(new Dimension(150, 300));
        producerJList.setTransferHandler(new CouplingTransferHandler("producers"));
        producerJList.setCellRenderer(new ProducerCellRenderer());
        addProducerContextMenu(producerJList);
        producerComboBox.setModel(componentList);
        producerComboBox.addActionListener(this);
        rightCenterPanel.add(producerJList);
        rightPanel.add("North", producerComboBox);
        rightPanel.add("Center", producerJList);

        ////////////////
        // BOTTOM     //
        ////////////////
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        applyButton.setActionCommand("apply");
        applyButton.addActionListener(this);
        JButton okButton = new JButton("OK");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        bottomPanel.add(applyButton);
        bottomPanel.add(okButton);
        bottomPanel.add(cancelButton);

        ////////////////
        // MAIN PANEL //
        ////////////////
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        centerPanel.add(leftPanel);
        centerPanel.add(middleCenterPanel);
        centerPanel.add(rightPanel);
        centerPanel.setPreferredSize(new Dimension(800, 400));
        this.setLayout(new BorderLayout());
        this.add("Center", centerPanel);
        this.add("South", bottomPanel);
    }

    ///////////////////////////////////////////////////////////////////
    // The next few methods are repeated for consumer and producer.  //
    //  Wasn't sure how to avoid this, but it wors....               //
    ///////////////////////////////////////////////////////////////////

    /**
     * Custom consumer cell renderer which shows default attribute
     * name.
     */
    private class ConsumerCellRenderer extends DefaultListCellRenderer {
        /**
         * Returns the list of cell renderer components.
         * @param list Graphical object to be rendered.
         * @param object to be rendered.
         * @param index of object.
         * @param isSelected boolean value.
         * @param cellHasFocus boolean value.
         * @return Component to be rendered.
         * @overrides java.awt.Component
         */
        public java.awt.Component getListCellRendererComponent(final JList list,
                final Object object, final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer)
                    super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            Consumer consumer = (Consumer) object;
            renderer.setText(consumer.getConsumerDescription() + ":"
                    + consumer.getDefaultConsumingAttribute().getName());
            return renderer;
       }
    }

    /**
     * Provides a menu for changing consumer attributes.
     *
     * @param list to be managed
     */
    private void addConsumerContextMenu(final JList list) {
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent me) {

                ArrayList<Consumer> selectedConsumers = new ArrayList<Consumer>();
                if (list.getSelectedValues().length == 0) {
                    selectedConsumers.add((Consumer) list.getSelectedValue());
                } else {
                    for (int i = 0; i < list.getSelectedValues().length; i++) {
                        selectedConsumers.add((Consumer) list
                                .getSelectedValues()[i]);
                    }
                }

                JPopupMenu popup = getConsumerAttributePopupMenu(selectedConsumers);
                if (SwingUtilities.isRightMouseButton(me)
                        || (me.isControlDown())) {
                    popup.show(list, me.getX(), me.getY());
                }
            }
        });
    }

    /**
     * Packages a coupling into a menu item.
     */
    private class ConsumerMenuItem extends JMenuItem {

        /** Consuming attribute. */
        private ConsumingAttribute attribute;

        /** Selected list of consumers. */
        private ArrayList<Consumer> selectedConsumerList;

        /**
         * Constructs the consumer menu item.
         * @param attribute consuming attribute.
         * @param consumerList list of consumers.
         */
        public ConsumerMenuItem(final ConsumingAttribute attribute, final ArrayList<Consumer> consumerList) {
            super(attribute.getName());
            this.attribute = attribute;
            this.selectedConsumerList = consumerList;
        }

        /**
         * Returns the consuming attribute.
         * @return consuming attribute
         */
        public ConsumingAttribute getConsumingAttribute() {
            return attribute;
        }

        /**
         * Returns the list of selected sonsumers.
         * @return selected consumers.
         */
        public ArrayList<Consumer> getSelectedConsumerList() {
           return selectedConsumerList;
        }

    }

    /**
     * Returns a menu populated by attributes.
     *
     * @param consumers that will have popup menu.
     * @return popup menu.
     */
    private JPopupMenu getConsumerAttributePopupMenu(final ArrayList<Consumer> consumers) {
        final JPopupMenu popup = new JPopupMenu();
        Consumer consumer = consumers.get(0);
        if (consumer != null) {
            if (consumer.getConsumingAttributes() != null) {
                for (ConsumingAttribute attribute : consumer.getConsumingAttributes()) {
                    ConsumerMenuItem item = new ConsumerMenuItem(attribute, consumers);
                    item.addActionListener(this);
                    popup.add(item);
                }
            }
        }
        return popup;
    }

    // And now the producers....

    /**
     * Custom producer cell renderer which shows default attribute
     * name.
     */
    private class ProducerCellRenderer extends DefaultListCellRenderer {
        /**
         * Producer cell renerer component.
         * @param list to be rendered.
         * @param object to be added.
         * @param index of producer.
         * @param isSelected boolean value.
         * @param cellHasFocus boolean value.
         * @return rendered producers.
         * @overrides java.awt.Component
         */
        public java.awt.Component getListCellRendererComponent(final JList list, final Object object,
                final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer)
                    super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            Producer producer = (Producer) object;
            renderer.setText(producer.getProducerDescription() + ":"
                    + producer.getDefaultProducingAttribute().getName());
            return renderer;
       }
    }

    /**
     * Provides a menu for changing Producer attributes.
     *
     * @param list of items to have menu.
     */
    private void addProducerContextMenu(final JList list) {
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent me) {

                ArrayList<Producer> selectedProducers = new ArrayList<Producer>();
                if (list.getSelectedValues().length == 0) {
                    selectedProducers.add((Producer) list.getSelectedValue());
                } else {
                    for (int i = 0; i < list.getSelectedValues().length; i++) {
                        selectedProducers.add((Producer) list
                                .getSelectedValues()[i]);
                    }
                }

                JPopupMenu popup = getProducerAttributePopupMenu(selectedProducers);
                if (SwingUtilities.isRightMouseButton(me)
                        || (me.isControlDown())) {
                    popup.show(list, me.getX(), me.getY());
                }
            }
        });
    }

    /**
     * Packages a coupling into a menu item.
     */
    private class ProducerMenuItem extends JMenuItem {

        /** Attribute of producer. */
        private ProducingAttribute attribute;

        /** List of selected producers. */
        private ArrayList<Producer> selectedProducerList;

        /**
         * Constructs the menu for producers.
         * @param attribute producer attribute.
         * @param producerList list of selected producers.
         */
        public ProducerMenuItem(final ProducingAttribute attribute, final ArrayList<Producer> producerList) {
            super(attribute.getName());
            this.attribute = attribute;
            this.selectedProducerList = producerList;
        }

        /**
         * Returns the producing attribute.
         * @return producing attribute.
         */
        public ProducingAttribute getProducingAttribute() {
            return attribute;
        }

        /**
         * Returns the selected list of producers.
         * @return list of producers.
         */
        public ArrayList<Producer> getSelectedProducerList() {
           return selectedProducerList;
        }

    }

    /**
     * Returns a menu populated by attributes.
     *
     * @param producers that need menu.
     * @return menu for list of producers.
     */
    private JPopupMenu getProducerAttributePopupMenu(final ArrayList<Producer> producers) {
        final JPopupMenu popup = new JPopupMenu();
        Producer producer = producers.get(0);
        if (producer != null) {
            if (producer.getProducingAttributes() != null) {
                for (ProducingAttribute attribute : producer.getProducingAttributes()) {
                    ProducerMenuItem item = new ProducerMenuItem(attribute, producers);
                    item.addActionListener(this);
                    popup.add(item);
                }
            }
        }
        return popup;
    }

    /**
     * @see ActionListener.
     * @param event to listen.
     */
    public void actionPerformed(final ActionEvent event) {
        //System.out.println(event.getSource());
        // Refresh component lists
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event.getSource()).getSelectedItem();
            if (component != null) {
                if (event.getSource() == consumerComboBox) {
                    currentComponent = component;
                    if (component.getCouplingContainer() != null) {
                        if (component.getCouplingContainer().getConsumers() != null) {
                            consumers = new ArrayList(component.getCouplingContainer().getConsumers());
                            consumerJList.setModel(new ConsumerList(consumers));
                        }
                        if (component.getCouplingContainer().getCouplings() != null) {
                            ArrayList<Coupling> couplings = new ArrayList(component.
                                    getCouplingContainer().getCouplings());
                            couplingTray.setModel(new CouplingList(couplings));
                        }
                    }
                } else if (event.getSource() == producerComboBox) {
                    if (component.getCouplingContainer() != null) {
                        if (component.getCouplingContainer().getProducers() == null) {
                            producerJList.setModel(new DefaultComboBoxModel());
                        } else {
                            producers = new ArrayList(component.getCouplingContainer().getProducers());
                            producerJList.setModel(new ProducerList(producers));
                        }
                    }
                }
            }
        }

        // Handle consumer attribute setting events
        if (event.getSource() instanceof ConsumerMenuItem) {
            ConsumerMenuItem item = (ConsumerMenuItem) event.getSource();
            for (Consumer consumer : item.getSelectedConsumerList()) {
                consumer.setDefaultConsumingAttribute(item.getConsumingAttribute());
            }
        }

        // Handle producer attribute setting events
        if (event.getSource() instanceof ProducerMenuItem) {
            ProducerMenuItem item = (ProducerMenuItem) event.getSource();
            for (Producer producer : item.getSelectedProducerList()) {
                producer.setDefaultProducingAttribute(item.getProducingAttribute());
            }
        }

        // Handle Button Presses
        if (event.getSource() instanceof JButton) {
            JButton button = (JButton) event.getSource();
            if (button.getActionCommand().equalsIgnoreCase("apply")) {
                applyChanges();
            } else if (button.getActionCommand().equalsIgnoreCase("ok")) {
                this.applyChanges();
                frame.dispose();
            } else if (button.getActionCommand().equalsIgnoreCase("cancel")) {
                frame.dispose();
            }
        }
    }

    /**
     * Update workspace to reflect all changes that have been made.
     */
    private void applyChanges() {
        if (currentComponent != null) {
            if (currentComponent.getCouplingContainer() != null) {
                currentComponent.getCouplingContainer().getCouplings().clear();
                ArrayList<Coupling> couplings = ((CouplingList) couplingTray.getModel()).getCouplingList();
                for (Coupling coupling : couplings) {
                    currentComponent.getCouplingContainer().getCouplings().add(coupling);
                }
            }
        }
    }

    /**
     * A list of components used by the combo box.
     *
     * @author jyoshimi
     */
    class ComponentList extends AbstractListModel implements ComboBoxModel {

        /** List of components within the workspace. */
        private ArrayList<WorkspaceComponent> componentList = new ArrayList<WorkspaceComponent>();

        /** Workspace component that is selected. */
        private WorkspaceComponent selected;

        /**
         * Constructs a list of components.
         * @param components to be set.
         */
        public ComponentList(final ArrayList<WorkspaceComponent> components) {
            super();
            this.componentList = new ArrayList<WorkspaceComponent>(components);
        }

        /**
         * Returns the element at the specified location.
         * @param index of element.
         * @return object at location.
         */
        public Object getElementAt(final int index) {
            return componentList.get(index);
        }

        /**
         * Returns the size of the component list.
         * @return size of component list
         */
        public int getSize() {
            return componentList.size();
        }

        /**
         * @return the componentList
         */
        public ArrayList<WorkspaceComponent> getComponentList() {
            return componentList;
        }

        /**
         * @param componentList the componentList to set
         */
        public void setComponentList(final ArrayList<WorkspaceComponent> componentList) {
            this.componentList = componentList;
        }

        /**
         * Returns the selected item.
         * @return selected item.
         */
        public Object getSelectedItem() {
            return selected;
        }

        /**
         * Sets the selected item(s).
         * @param arg0 items to be set as selected.
         * //TODO: Check this stuff...
         */
        public void setSelectedItem(final Object arg0) {
            for (WorkspaceComponent component : componentList) {
                if (component == arg0) {
                    selected = component;
                }
            }
        }

    }

}


