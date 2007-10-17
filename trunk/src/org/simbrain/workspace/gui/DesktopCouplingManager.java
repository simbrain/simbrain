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
package org.simbrain.workspace.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.CouplingManager;

/**
 * Graphical element for managing coupling of objects.
 */
public class DesktopCouplingManager extends JPanel implements ActionListener, MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;

    private final CouplingManager manager;
    
    /** List of consumers for use in dialog. */
    private JList consumerJList = new JList();

    /** Combo box of available consumers. */
    private JComboBox consumerComboBox = new JComboBox();

    /** Reference to current component (on the consumer / coupling side). */
    private WorkspaceComponent currentComponent = null;

    /** List of producers for use in dialog. */
    private JList producerJList = new JList();

    /** Combo box of available producers. */
    private JComboBox producerComboBox = new JComboBox();

    /** Area of coupled items. */
    private CouplingTray couplingTray = new CouplingTray();
    /** model for CouplingTray */
    CouplingTray.CouplingList trayModel = new CouplingTray.CouplingList();
    
    /** Reference of parent frame. */
    private final JFrame frame;

    private final KeyAdapter couplingKeyAdapter = new KeyAdapter()
    {
        /**
         * {@inheritDoc}
         */
        public void keyPressed(final KeyEvent event) {
            int keyCode = event.getKeyCode();
            switch (keyCode) {
            case KeyEvent.VK_BACK_SPACE:        
                if (getCouplingTray().hasFocus()) {
                    deleteSelectedCouplings();
                }
                break;
            default:
                break;
            }
        }
    };
    
    /**
     * Default constructor. Creates and displays the coupling manager.
     * @param frame parent of panel.
     */
    public DesktopCouplingManager(final JFrame frame) {
        super();
        
        this.manager = new CouplingManager();
        this.frame = frame;
        
        GenericListModel<WorkspaceComponent> componentList 
            = new GenericListModel<WorkspaceComponent>(SimbrainDesktop.getInstance().getComponentList());

        ///////////////
        // CONSUMERS //
        ///////////////
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel leftButtonTray = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JScrollPane leftScrollPane = new JScrollPane(consumerJList);
        JButton jbLeftAddAll = new JButton("Add All >>");
        jbLeftAddAll.setActionCommand("addAllConsumers");
        jbLeftAddAll.addActionListener(this);
        JButton jbLeftAdd = new JButton("Add Selected >");
        jbLeftAdd.setActionCommand("addSelectedConsumers");
        jbLeftAdd.addActionListener(this);
        leftButtonTray.add(jbLeftAdd);
        leftButtonTray.add(jbLeftAddAll);
        Border leftBorder = BorderFactory.createTitledBorder("Consumers");
        leftPanel.setBorder(leftBorder);
        addConsumerContextMenu(consumerJList);
        consumerJList.setDragEnabled(true);
        consumerJList.setTransferHandler(new CouplingTransferHandler("consumers"));
        consumerJList.setCellRenderer(new ConsumerCellRenderer());
        consumerJList.addMouseListener(this);
        consumerJList.addMouseMotionListener(this);
        consumerJList.addKeyListener(couplingKeyAdapter);
        consumerComboBox.setModel(componentList);
        consumerComboBox.addActionListener(this);
        if (consumerComboBox.getModel().getSize() > 0) {
            consumerComboBox.setSelectedIndex(0);
            this.refreshComponentList(componentList.getElementAt(0), consumerComboBox);
//            this.refreshComponentList(components.get(0), consumerComboBox);
        }
        leftPanel.add("North", consumerComboBox);
        leftPanel.add("Center", leftScrollPane);
        leftPanel.add("South", leftButtonTray);

        ///////////////////
        // COUPLING TRAY //
        ///////////////////
        JPanel middlePanel = new JPanel(new BorderLayout());
        JScrollPane middleScrollPane = new JScrollPane();
        JPanel centerButtonTray = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton deleteButton = new JButton("Remove");
        deleteButton.addActionListener(this);
        centerButtonTray.add(deleteButton);
        addCouplingContextMenu(couplingTray);
        couplingTray.setModel(trayModel);
        couplingTray.setSize(new Dimension(250, 350));
        couplingTray.addKeyListener(couplingKeyAdapter);
        couplingTray.addMouseListener(this);
        couplingTray.addMouseMotionListener(this);
        Border centerBorder = BorderFactory.createTitledBorder("Couplings");
        middleScrollPane.setViewportView(couplingTray);
        middlePanel.setBorder(centerBorder);
        middlePanel.add("Center", middleScrollPane);
        middlePanel.add("South", centerButtonTray);

        ///////////////
        // PRODUCERS //
        ///////////////
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel rightButtonTray = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JScrollPane rightScrollPane = new JScrollPane(producerJList);
        JButton jbRightAddAll = new JButton("<< Bind All");
        jbRightAddAll.setActionCommand("bindAllProducers");
        jbRightAddAll.addActionListener(this);
        JButton jbRightAdd = new JButton("< Bind Selected");
        jbRightAdd.setActionCommand("bindSelectedProducers");
        jbRightAdd.addActionListener(this);
        rightButtonTray.add(jbRightAddAll);
        rightButtonTray.add(jbRightAdd);
        Border rightBorder = BorderFactory.createTitledBorder("Producers");
        rightPanel.setBorder(rightBorder);
        producerJList.setDragEnabled(true);
        producerJList.setTransferHandler(new CouplingTransferHandler("producers"));
        producerJList.setCellRenderer(new ProducerCellRenderer());
        producerJList.addKeyListener(couplingKeyAdapter);
        producerJList.addMouseListener(this);
        producerJList.addMouseMotionListener(this);
        addProducerContextMenu(producerJList);
        producerComboBox.setModel(componentList);
        producerComboBox.addActionListener(this);
        
        if (producerComboBox.getModel().getSize() > 0) {
            producerComboBox.setSelectedIndex(0);
//            this.refreshComponentList(componentList.getComponentList().get(0), producerComboBox);
            this.refreshComponentList(componentList.getElementAt(0), producerComboBox);
        }

        rightPanel.add("North", producerComboBox);
        rightPanel.add("Center", rightScrollPane);
        rightPanel.add("South", rightButtonTray);

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
        centerPanel.add(middlePanel);
        centerPanel.add(rightPanel);
        centerPanel.setPreferredSize(new Dimension(800, 400));
        this.setLayout(new BorderLayout());
        this.add("Center", centerPanel);
        this.add("South", bottomPanel);
    }

    ///////////////////////////////////////////////////////////////////
    // The next few methods are repeated for consumer and producer.  //
    //  Wasn't sure how to avoid this, but it works....               //
    ///////////////////////////////////////////////////////////////////

    /**
     * Custom consumer cell renderer which shows default attribute
     * name.
     */
    private class ConsumerCellRenderer extends DefaultListCellRenderer {
        
        private static final long serialVersionUID = 1L;

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
                    + consumer.getDefaultConsumingAttribute().getAttributeDescription());
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

        private static final long serialVersionUID = 1L;

        /** Consuming attribute. */
        private ConsumingAttribute<?> attribute;

        /** Selected list of consumers. */
        private ArrayList<Consumer> selectedConsumerList;

        /**
         * Constructs the consumer menu item.
         * @param attribute consuming attribute.
         * @param consumerList list of consumers.
         */
        public ConsumerMenuItem(final ConsumingAttribute<?> attribute, final ArrayList<Consumer> consumerList) {
            super(attribute.getAttributeDescription());
            this.attribute = attribute;
            this.selectedConsumerList = consumerList;
        }

        /**
         * Returns the consuming attribute.
         * @return consuming attribute
         */
        public ConsumingAttribute<?> getConsumingAttribute() {
            return attribute;
        }

        /**
         * Returns the list of selected consumers.
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
                for (ConsumingAttribute<?> attribute : consumer.getConsumingAttributes()) {
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
        
        private static final long serialVersionUID = 1L;

        /**
         * Producer cell renderer component.
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
                    + producer.getDefaultProducingAttribute().getAttributeDescription());
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
        
        private static final long serialVersionUID = 1L;

        /** Attribute of producer. */
        private ProducingAttribute<?> attribute;

        /** List of selected producers. */
        private ArrayList<Producer> selectedProducerList;

        /**
         * Constructs the menu for producers.
         * @param attribute producer attribute.
         * @param producerList list of selected producers.
         */
        public ProducerMenuItem(final ProducingAttribute<?> attribute, final ArrayList<Producer> producerList) {
            super(attribute.getAttributeDescription());
            this.attribute = attribute;
            this.selectedProducerList = producerList;
        }

        /**
         * Returns the producing attribute.
         * @return producing attribute.
         */
        public ProducingAttribute<?> getProducingAttribute() {
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
                for (ProducingAttribute<?> attribute : producer.getProducingAttributes()) {
                    ProducerMenuItem item = new ProducerMenuItem(attribute, producers);
                    item.addActionListener(this);
                    popup.add(item);
                }
            }
        }
        return popup;
    }

    /**
     * Refresh the main parts of coupling manager.
     * For consumer combo box, refresh consumer list + coupling tray.
     * For producer combo box, refresh producer list.
     *
     * @param component the workspace component being checked
     * @param comboBox the combo box upon which the refresh is based
     */
    private void refreshComponentList(final WorkspaceComponent component, final JComboBox comboBox) {
        if (component != null) {
            if (comboBox == consumerComboBox) {
                currentComponent = component;
                if (component.getCouplingContainer() != null) {
                    if (component.getCouplingContainer().getConsumers() != null) {
                        manager.refreshConsumers(component.getCouplingContainer());
                        consumerJList.setModel(new GenericListModel<Consumer>(manager.getConsumers()));
                    }
                    if (component.getCouplingContainer().getCouplings() != null) {
                        ArrayList<Coupling> couplings = new ArrayList<Coupling>(component.getCouplingContainer().getCouplings());
                        couplingTray.setModel(new CouplingTray.CouplingList(couplings));
                    }
                }
            } else if (comboBox == producerComboBox) {
                if (component.getCouplingContainer() != null) {
                    if (component.getCouplingContainer().getProducers() == null) {
                        producerJList.setModel(new DefaultComboBoxModel());
                    } else {
                        manager.refreshProducers(component.getCouplingContainer());
                        producerJList.setModel(new GenericListModel<Producer>(manager.getProducers()));
                    }
                }
            }
        }
    }

    /**
     * @see ActionListener.
     * @param event to listen.
     */
    public void actionPerformed(final ActionEvent event) {

        // Refresh component lists
        if (event.getSource() instanceof JComboBox) {
            WorkspaceComponent component = (WorkspaceComponent) ((JComboBox) event.getSource()).getSelectedItem();
            refreshComponentList(component, (JComboBox) event.getSource());
        }

        // Handle consumer attribute setting events
//        if (event.getSource() instanceof ConsumerMenuItem) {
//            ConsumerMenuItem item = (ConsumerMenuItem) event.getSource();
//            for (Consumer consumer : item.getSelectedConsumerList()) {
//                consumer.setDefaultConsumingAttribute(item.getConsumingAttribute());
//            }
//        }

        // Handle producer attribute setting events
//        if (event.getSource() instanceof ProducerMenuItem) {
//            ProducerMenuItem item = (ProducerMenuItem) event.getSource();
//            for (Producer producer : item.getSelectedProducerList()) {
//                producer.setDefaultProducingAttribute(item.getProducingAttribute());
//            }
//        }

        // Handle coupling tray menu items
        if (event.getSource() instanceof CouplingTrayMenuItem) {
            if (event.getActionCommand().equalsIgnoreCase("delete")) {
                deleteSelectedCouplings();
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
            } else if (button.getActionCommand().equalsIgnoreCase("remove")) {
                deleteSelectedCouplings();
            } else if (button.getActionCommand().equalsIgnoreCase("addAllConsumers")) {
                GenericListModel<Consumer> consumerList = (GenericListModel<Consumer>) consumerJList.getModel();
                for (Consumer consumer : consumerList) {
                    trayModel.addElement(new Coupling(consumer.getDefaultConsumingAttribute()));
                }
            } else if (button.getActionCommand().equalsIgnoreCase("addSelectedConsumers")) {
                ArrayList<Consumer> consumerList = this.getSelectedConsumers();
                for (Consumer consumer : consumerList) {
                    trayModel.addElement(new Coupling(consumer.getDefaultConsumingAttribute()));
                }
            } else if (button.getActionCommand().equalsIgnoreCase("bindAllProducers")) {
                GenericListModel<Producer> producerList = (GenericListModel<Producer>) producerJList.getModel();
                int i = couplingTray.getSelectedIndex();
                if (i == -1) {
                    return;
                }
                for (Producer producer : producerList) {
                    if (i < trayModel.getSize()) {
                        trayModel.bindElementAt(producer.getDefaultProducingAttribute(), i++);
                    }
                }
            } else if (button.getActionCommand().equalsIgnoreCase("bindSelectedProducers")) {
                ArrayList<Producer> producerList = getSelectedProducers();
                int i = couplingTray.getSelectedIndex();
                for (Producer producer : producerList) {
                    if (i < trayModel.getSize()) {
                        trayModel.bindElementAt(producer.getDefaultProducingAttribute(), i++);
                    }
                }
            }
        }
    }

    /**
     * Returns producers selected in producer list.
     * @return selected producers.
     */
    public ArrayList<Producer> getSelectedProducers() {
        ArrayList<Producer> ret = new ArrayList<Producer>();
        for (Object object : producerJList.getSelectedValues()) {
            ret.add((Producer) object);
        }
        return ret;
    }

    /**
     * Returns coupling selected in coupling list.
     * @return selected couplings.
     */
    public ArrayList<Coupling> getSelectedCouplings() {
        ArrayList<Coupling> ret = new ArrayList<Coupling>();
        for (Object object : couplingTray.getSelectedValues()) {
            ret.add((Coupling) object);
        }
        return ret;
    }

    /**
     * Returns consumers selected in consumer list.
     * @return selected consumers.
     */
    private ArrayList<Consumer> getSelectedConsumers() {
        ArrayList<Consumer> ret = new ArrayList<Consumer>();
        for (Object object : consumerJList.getSelectedValues()) {
            ret.add((Consumer) object);
        }
        return ret;
    }

    /**
     * Delete selected couplings from tray.
     */
    public void deleteSelectedCouplings() {
       ArrayList<Coupling> toDelete = getSelectedCouplings();
       CouplingTray.CouplingList list = (CouplingTray.CouplingList) couplingTray.getModel();
       for (Coupling coupling : toDelete) {
           list.remove(coupling);
       }
    }

    /**
     * Update workspace to reflect all changes that have been made.
     */
    private void applyChanges() {
        if (currentComponent != null) {
            if (currentComponent.getCouplingContainer() != null) {
                currentComponent.getCouplingContainer().getCouplings().clear();
                for (Coupling coupling : trayModel) {
                    currentComponent.getCouplingContainer().getCouplings().add(coupling);
                }
            }
        }
    }

    /**
     * Provides a menu for changing Producer attributes.
     *
     * @param list of items to have menu.
     */
    private void addCouplingContextMenu(final JList list) {
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent me) {

                ArrayList<Coupling> selectedCouplings = new ArrayList<Coupling>();
                if (list.getSelectedValues().length == 0) {
                    selectedCouplings.add((Coupling) list.getSelectedValue());
                } else {
                    for (int i = 0; i < list.getSelectedValues().length; i++) {
                        selectedCouplings.add((Coupling) list
                                .getSelectedValues()[i]);
                    }
                }

                JPopupMenu popup = getCouplingPopupMenu(selectedCouplings);
                if (SwingUtilities.isRightMouseButton(me)
                        || (me.isControlDown())) {
                    popup.show(list, me.getX(), me.getY());
                }
            }
        });
    }

    /**
     * Returns a menu populated by attributes.
     *
     * @param producers that need menu.
     * @return menu for list of producers.
     */
    private JPopupMenu getCouplingPopupMenu(final ArrayList<Coupling> couplings) {
        final JPopupMenu popup = new JPopupMenu();
        CouplingTrayMenuItem delete = new CouplingTrayMenuItem("Delete", couplings);
        delete.setActionCommand("delete");
        delete.addActionListener(this);
        popup.add(delete);
        return popup;
    }
    /**
     * Packages a coupling into a menu item.
     */
    private class CouplingTrayMenuItem extends JMenuItem {

        /** Coupling list. */
        private ArrayList<Coupling> couplingList;

        /**
         * Constructor.
         *
         * @param name passed to jmenuitem
         * @param couplingList the couplings to package
         */
        public CouplingTrayMenuItem(final String name, final ArrayList<Coupling> couplingList) {
            super(name);
            this.couplingList = couplingList;
        }

        /**
         * Returns reference to packaged coupling list.
         *
         * @return reference to coupling list.
         */
        public ArrayList<Coupling> getCouplingList() {
            return couplingList;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(final MouseEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseEntered(final MouseEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseExited(final MouseEvent arg0) {
    }

    /** For drag selecting JList items. */
    private int initialIndex;

    /**
     * {@inheritDoc}
     */
    public void mousePressed(final MouseEvent event) {
        if (consumerJList.hasFocus()) {
            if (consumerJList.getModel().getSize() > 0) {
                initialIndex = consumerJList.locationToIndex(event.getPoint());
                if (!consumerJList.getCellBounds(0, consumerJList.getModel().getSize() - 1).contains(event.getPoint())) {
                    consumerJList.clearSelection();
                }
            }
        } else if (producerJList.hasFocus()) {
            if (producerJList.getModel().getSize() > 0) {
                initialIndex = producerJList.locationToIndex(event.getPoint());
                if (!producerJList.getCellBounds(0, producerJList.getModel().getSize() - 1).contains(event.getPoint())) {
                    producerJList.clearSelection();
                }
            }
        } else if (couplingTray.hasFocus()) {
            if (couplingTray.getModel().getSize() > 0) {
                initialIndex = couplingTray.locationToIndex(event.getPoint());
                if (!couplingTray.getCellBounds(0, couplingTray.getModel().getSize() - 1).contains(event.getPoint())) {
                    couplingTray.clearSelection();
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(final MouseEvent event) {
        if (consumerJList.hasFocus()) {
            consumerJList.addSelectionInterval(initialIndex, consumerJList.locationToIndex(event.getPoint()));
        } else if (producerJList.hasFocus()) {
            producerJList.addSelectionInterval(initialIndex, producerJList.locationToIndex(event.getPoint()));
        } else if (couplingTray.hasFocus()) {
            couplingTray.addSelectionInterval(initialIndex, couplingTray.locationToIndex(event.getPoint()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(final MouseEvent arg0) {
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(final MouseEvent arg0) {
    }

    /**
     * @return the consumerJList.
     */
    public JList getConsumerJList() {
        return consumerJList;
    }

    /**
     * @return the producerJList.
     */
    public JList getProducerJList() {
        return producerJList;
    }

    /**
     * @return the coupling tray.
     */
    public CouplingTray getCouplingTray() {
        return couplingTray;
    }
}


