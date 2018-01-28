package org.simbrain.network.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NeuronGroupSelectionPanel extends JPanel implements ActionListener {

    private JScrollPane scrollPane;

    private JList neuronGroups;

    private DefaultListModel model;

    public NeuronGroupSelectionPanel(JList neuronGroups) {
        this.neuronGroups = neuronGroups;
        initializeLayout();
    }

    private void initializeLayout() {

        // Set up the JList
        model = new DefaultListModel();
        neuronGroups.setModel(model);

        scrollPane = new JScrollPane(neuronGroups);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        this.setPreferredSize(new Dimension(150, 300));
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);

    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub

    }

    public DefaultListModel getModel() {
        return model;
    }

    public void setModel(DefaultListModel model) {
        this.model = model;
    }

    public JList getNeuronGroups() {
        return neuronGroups;
    }

    public void setNeuronGroups(JList neuronGroups) {
        this.neuronGroups = neuronGroups;
    }

    public static void main(String[] args) {
        JFrame demo = new JFrame();

        NeuronGroupSelectionPanel p = new NeuronGroupSelectionPanel(new JList());
        p.getModel().addElement("timmy");
        // JPanel pan = new JPanel();
        // pan.add(p, new FlowLayout());
        demo.add(p);

        demo.pack();
        demo.setVisible(true);

    }

}
