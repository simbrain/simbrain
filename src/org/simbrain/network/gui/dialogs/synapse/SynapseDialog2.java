package org.simbrain.network.gui.dialogs.synapse;

import java.awt.Frame;
import java.util.List;

import javax.swing.JScrollPane;

import org.simbrain.network.core.Synapse;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

public class SynapseDialog2 extends StandardDialog {

    /** The synapses being modified. */
    private final List<Synapse> synapseList;

    /**
     * Private constructor without frame.
     * 
     * @param sns the logical synapses being adjusted
     */
    public SynapseDialog2(final List<Synapse> sns) {
        this.synapseList = sns;
        initializeLayout();
    }
    
    public SynapseDialog2(final Frame parent, final List<Synapse> sns) {
        super(parent, "Synapse Dialog");
        this.synapseList = sns;
        initializeLayout();
    }
    
    /**
     * Initializes the components on the panel.
     */
    private void initializeLayout() {
        setTitle("Synapse Dialog");
        
        //AnnotatedPropertyEditor synEditor = new AnnotatedPropertyEditor(synapseList.get(0));
        //JScrollPane scroller = new JScrollPane(synEditor);
        //scroller.setBorder(null);
        //setContentPane(synEditor);
        //this.addButton(helpButton);
    }
    
}
