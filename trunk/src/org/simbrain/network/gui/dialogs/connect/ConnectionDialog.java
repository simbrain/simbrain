package org.simbrain.network.gui.dialogs.connect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>ConnectionDialog</b> is a dialog box for setting connection types and properties.
 */
public class ConnectionDialog extends StandardDialog implements ActionListener {

    /** Select connection type. */
    private JComboBox cbConnectionType = new JComboBox(new String[]{"All to All", "One to One", "Sparse"});

    /** Main dialog box. */
    private Box mainPanel = Box.createVerticalBox();

    /** Panel for setting connection type. */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /** Panel for setting connection properties. */
    private AbstractConnectionPanel optionsPanel;

    /**
     * Connection dialog default constructor.
     *
     */
    public ConnectionDialog() {
        init();
    }

    /**
     * Initialize default constructor.
     *
     */
    private void init() {
        setTitle("Connect Neurons");

        initConnectionType();

        cbConnectionType.addActionListener(this);

        typePanel.addItem("Connection Type", cbConnectionType);
        mainPanel.add(typePanel);
        mainPanel.add(optionsPanel);
        setContentPane(mainPanel);
    }

    /** @see StandardDialog */
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Initialize the connection panel based upon the current connection type.
     */
    private void initConnectionType() {
//        if (NetworkPreferences.getConnectionType().equalsIgnoreCase("All to All")) {
//            cbConnectionType.setSelectedIndex(0);
//            optionsPanel = new AllToAllPanel();
//            optionsPanel.fillFieldValues();
//        } else if (NetworkPreferences.getConnectionType().equalsIgnoreCase("One to One")) {
//            cbConnectionType.setSelectedIndex(1);
//            optionsPanel = new OneToOnePanel();
//            optionsPanel.fillFieldValues();
//        } else if (NetworkPreferences.getConnectionType().equals("Sparse")) {
//            cbConnectionType.setSelectedIndex(2);
//            optionsPanel = new SparsePanel();
//            optionsPanel.fillFieldValues();
//        }
    }

    /**
     * Respond to neuron type changes.
     * @param e Action event.
     */
    public void actionPerformed(final ActionEvent e) {
        if (cbConnectionType.getSelectedItem().equals("All to All")) {
            mainPanel.remove(optionsPanel);
            optionsPanel = new AllToAllPanel();
            optionsPanel.fillDefaultValues();
            mainPanel.add(optionsPanel);
        } else if (cbConnectionType.getSelectedItem().equals("One to One")) {
            mainPanel.remove(optionsPanel);
            optionsPanel = new OneToOnePanel();
            optionsPanel.fillDefaultValues();
            mainPanel.add(optionsPanel);
        } else if (cbConnectionType.getSelectedItem().equals("Sparse")) {
            mainPanel.remove(optionsPanel);
            optionsPanel = new SparsePanel();
            optionsPanel.fillDefaultValues();
            mainPanel.add(optionsPanel);
        }

        pack();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
//        NetworkPreferences.setConnectionType(cbConnectionType.getSelectedItem().toString());
        optionsPanel.commitChanges();
    }
}
