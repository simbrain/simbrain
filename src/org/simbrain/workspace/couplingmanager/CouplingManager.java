package org.simbrain.workspace.couplingmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ListDataListener;

import org.simbrain.workspace.*;

public class CouplingManager extends JPanel implements ActionListener {

    private ArrayList consumers;

    private JList consumerJList = new JList();

    private JComboBox consumerComboBox = new JComboBox();

    // TODO: Remove when a method for saving all is finished
    private WorkspaceComponent currentComponent = null;

    private ArrayList producers;

    private JList producerJList = new JList();

    private JComboBox producerComboBox = new JComboBox();

    private CouplingTray couplingTray = new CouplingTray();

    public CouplingManager() {
        super();

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
        consumerJList.setCellRenderer(new consumerCellRenderer());
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
        producerJList.setCellRenderer(new producerCellRenderer());
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
        JButton iterateButton = new JButton("Iterate");
        iterateButton.addActionListener(this);
        iterateButton.setActionCommand("iterate");
        JButton applyButton = new JButton("Apply");
        applyButton.setActionCommand("apply");
        applyButton.addActionListener(this);
        JButton okButton = new JButton("OK");
        okButton.setActionCommand("ok");
        okButton.addActionListener(this);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("cancel");
        cancelButton.addActionListener(this);
        bottomPanel.add(iterateButton);
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
    private class consumerCellRenderer extends DefaultListCellRenderer {
        public java.awt.Component getListCellRendererComponent(final JList list, final Object object, final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer)super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            Consumer consumer = (Consumer)object;
            renderer.setText(consumer.getConsumerDescription());
            return renderer;
       }
    }

    /**
     * Provides a menu for changing consumer attributes.
     *
     * @param list
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

        ConsumingAttribute attribute;

        ArrayList<Consumer> selectedConsumerList;

        public ConsumerMenuItem(final ConsumingAttribute attribute, final ArrayList<Consumer> consumerList) {
            super(attribute.getName());
            this.attribute = attribute;
            this.selectedConsumerList = consumerList;
        }

        public ConsumingAttribute getConsumingAttribute() {
            return attribute;
        }

        public ArrayList<Consumer> getSelectedConsumerList() {
           return selectedConsumerList;
        }

    }

    /**
     * Returns a menu populated by attributes.
     *
     * @param coupling
     * @return
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
    private class producerCellRenderer extends DefaultListCellRenderer {
        public java.awt.Component getListCellRendererComponent(final JList list, final Object object, final int index, final boolean isSelected, final boolean cellHasFocus) {
            DefaultListCellRenderer renderer = (DefaultListCellRenderer)super.getListCellRendererComponent(list, object, index, isSelected, cellHasFocus);
            Producer producer = (Producer)object;    
            renderer.setText(producer.getProducerDescription());
            return renderer;
       }
    }

    /**
     * Provides a menu for changing Producer attributes.
     *
     * @param list
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
        
        ProducingAttribute attribute;
        
        ArrayList<Producer> selectedProducerList;

        public ProducerMenuItem(final ProducingAttribute attribute, final ArrayList<Producer> producerList) {
            super(attribute.getName());
            this.attribute = attribute;
            this.selectedProducerList = producerList;
        }

        public ProducingAttribute getProducingAttribute() {
            return attribute;
        }

        public ArrayList<Producer> getSelectedProducerList() {
           return selectedProducerList;
        }

    }
    
    /**
     * Returns a menu populated by attributes 
     * 
     * @param coupling
     * @return
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

    public void actionPerformed(final ActionEvent event) {
        //System.out.println(event.getSource());
        // Refresh component lists
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event.getSource()).getSelectedItem();
            if (component != null) {
                if (event.getSource() == consumerComboBox) {
                    currentComponent = component;
                    if (component.getConsumers() != null) {
                        consumers = new ArrayList(component.getConsumers());
                        consumerJList.setModel(new ConsumerList(consumers));                        
                    }
                    if (component.getCouplings() != null) {
                        ArrayList<Coupling> couplings = new ArrayList(component.getCouplings());
                        couplingTray.setModel(new CouplingList(couplings));                        
                    }
                } else if (event.getSource() == producerComboBox) {
                    if (component.getProducers() == null) {
                        producerJList.setModel(new DefaultComboBoxModel());
                    } else {
                        producers = new ArrayList(component.getProducers());
                        producerJList.setModel(new ProducerList(producers));
                    }
                }
            }
        }

        // Handle consumer attribute setting events
        if (event.getSource() instanceof ConsumerMenuItem) {
            ConsumerMenuItem item = (ConsumerMenuItem)event.getSource();
            for (Consumer consumer : item.getSelectedConsumerList()) {
                consumer.setDefaultConsumingAttribute(item.getConsumingAttribute());            
            }
        }

        // Handle producer attribute setting events
        if (event.getSource() instanceof ProducerMenuItem) {
            ProducerMenuItem item = (ProducerMenuItem)event.getSource();
            for (Producer producer : item.getSelectedProducerList()) {
                producer.setDefaultProducingAttribute(item.getProducingAttribute());            
            }
        }

        // Handle Button Presses
        if (event.getSource() instanceof JButton) {
            JButton button = (JButton) event.getSource();
            if (button.getActionCommand().equalsIgnoreCase("iterate")) {
                Workspace.getInstance().globalUpdate();
            } else if (button.getActionCommand().equalsIgnoreCase("apply")) {
                applyChanges();
            } else if (button.getActionCommand().equalsIgnoreCase("ok")) {
                //System.exit(0);
            } else if (button.getActionCommand().equalsIgnoreCase("cancel")) {
                //this.getParent().di
            }
        }
    }

    /**
     * Update workspace to reflect all changes that have been made.
     */
    private void applyChanges() {
        if (currentComponent != null) {
            currentComponent.getCouplings().clear();
            ArrayList<Coupling> couplings = ((CouplingList)couplingTray.getModel()).getCouplingList();
            for (Coupling coupling : couplings) {
                currentComponent.getCouplings().add(coupling);
            }
        }
    }

    /**
     * A list of components used by the combo box.
     *
     * @author jyoshimi
     */
    class ComponentList extends AbstractListModel implements ComboBoxModel {

        private ArrayList<WorkspaceComponent> componentList = new ArrayList<WorkspaceComponent>();

        private WorkspaceComponent selected;

        public ComponentList(final ArrayList<WorkspaceComponent> components) {
            super();
            this.componentList = new ArrayList<WorkspaceComponent>(components);
        }

        public Object getElementAt(final int index) {
            return componentList.get(index);
        }

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

        public Object getSelectedItem() {
            return selected;
        }

        //TODO: Check this stuff...
        public void setSelectedItem(final Object arg0) {
            for (WorkspaceComponent component : componentList) {
                if (component == arg0) {
                    selected = component;
                }
            }
        }

    }

}


