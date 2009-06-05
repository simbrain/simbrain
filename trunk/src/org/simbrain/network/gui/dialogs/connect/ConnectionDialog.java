package org.simbrain.network.gui.dialogs.connect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;

import org.simbrain.network.connections.AllToAll;
import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.OneToOne;
import org.simbrain.network.connections.Radial;
import org.simbrain.network.connections.Sparse;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

/**
 * <b>ConnectionDialog</b> is a dialog box for setting connection types and properties.
 */
public class ConnectionDialog extends StandardDialog implements ActionListener {

    /** Connection type objects. */
    private ConnectNeurons[] connectionObjects = new ConnectNeurons[] {
            new AllToAll(), new OneToOne(), new Radial(), new Sparse() };

    /** Select connection type. */
    private JComboBox cbConnectionType = new JComboBox(connectionObjects);

    /** Main dialog box. */
    private Box mainPanel = Box.createVerticalBox();

    /** Panel for setting connection type. */
    private LabelledItemPanel typePanel = new LabelledItemPanel();

    /** Panel for setting connection properties. */
    private AbstractConnectionPanel optionsPanel;

    /**
     * Connection dialog default constructor.
     */
    public ConnectionDialog() {
        init();
    }

    /**
     * Initialize default constructor.
     */
    private void init() {
        setTitle("Connect Neurons");

        cbConnectionType.addActionListener(this);
        typePanel.addItem("Connection Type", cbConnectionType);
        cbConnectionType.setSelectedItem(connectionObjects[0]);
        initPanel();
        ConnectNeurons.connectionType = (ConnectNeurons)cbConnectionType.getSelectedItem();
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
    private void initPanel() {
        ConnectNeurons connection = (ConnectNeurons) cbConnectionType.getSelectedItem();
        if (connection instanceof AllToAll) {
            clearOptionPanel();
            optionsPanel = new AllToAllPanel((AllToAll) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        } else if (connection instanceof OneToOne) {
            clearOptionPanel();
            optionsPanel = new OneToOnePanel((OneToOne) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        } else if (connection instanceof Radial) {
            clearOptionPanel();
            optionsPanel = new RadialPanel((Radial) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        } else if (connection instanceof Sparse) {
            clearOptionPanel();
            optionsPanel = new SparsePanel((Sparse) connection);
            optionsPanel.fillFieldValues();
            mainPanel.add(optionsPanel);
        }
        pack();
        setLocationRelativeTo(null);
    }
    
    /**
     * Remove current panel, if any.
     */
    private void clearOptionPanel() {
        if (optionsPanel != null) {
            mainPanel.remove(optionsPanel);
        }
    }

    /**
     * Respond to neuron type changes.
     * @param e Action event.
     */
    public void actionPerformed(final ActionEvent e) {
         initPanel();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        ConnectNeurons.connectionType = (ConnectNeurons)cbConnectionType.getSelectedItem();
        optionsPanel.commitChanges();
    }
}
