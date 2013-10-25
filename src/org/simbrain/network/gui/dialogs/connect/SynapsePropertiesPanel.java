package org.simbrain.network.gui.dialogs.connect;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.connections.ConnectNeurons;
import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.gui.dialogs.RandomPanelNetwork;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.AbstractSynapsePanel;
import org.simbrain.network.gui.dialogs.synapse.plasticity_panels.ClampedSynapseRulePanel;
import org.simbrain.util.LinkIcon;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.SwitchableActionListener;
import org.simbrain.util.SwitchablePropertyChangeListener;
import org.simbrain.util.randomizer.Randomizer;

/**
 * A panel allowing synapse learning rules to be set and random weights to be activated/adjusted,
 * designed with segregation of inhibitory and excitatory weights in mind.
 * 
 * @author ztosi
 *
 */
public class SynapsePropertiesPanel extends JPanel{

    /** Null string. */
    public static final String NULL_STRING = "...";
	
    /** 
     * A combo-box containing select-able synapse learning rules for
     * excitatory synapses.
     */
	private JComboBox<String> exSynTypes =
			new JComboBox<String>(Synapse.getRuleList());
	
    /** 
     * A combo-box containing select-able synapse learning rules for
     * inhibitory synapses.
     */
	private JComboBox<String> inSynTypes =
			new JComboBox<String>(Synapse.getRuleList());	
	

	/**
	 * Listens to changes to the type of excitatory synapse selected in the 
	 * combo-box, brining up the corresponding synapse panel and adjusting
	 * the template excitatory synapse accordingly.
	 * This Listener is switchable 
	 * @see org.simbrain.util.SwitchableActionListener.java
	 */
    private SwitchableActionListener exSynListener;
    
	/**
	 * Listens to changes to the type of inhibitory synapse selected in the 
	 * combo-box, brining up the corresponding synapse panel and adjusting
	 * the template inhibitory synapse accordingly.
	 * This Listener is switchable 
	 * @see org.simbrain.util.SwitchableActionListener.java
	 */
	private SwitchableActionListener inSynListener;


	/**
	 * Listens to changes as to whether or not excitatory weights will be
	 * randomized. Responsible for synchronizing changes to excitatory
	 * random fields to inhibitory random fields when the attributes are 
	 * linked.
	 * This Listener is switchable 
	 * @see org.simbrain.util.SwitchablePropertyChangeListener.java
	 */
    private SwitchablePropertyChangeListener randExListener;
    
	/**
	 * Listens to changes as to whether or not inhibitory weights will be
	 * randomized. Responsible for synchronizing changes to inhibitory
	 * random fields to excitatory random fields when the attributes are 
	 * linked.
	 * This Listener is switchable 
	 * @see org.simbrain.util.SwitchablePropertyChangeListener.java
	 */
    private SwitchablePropertyChangeListener randInListener;
  
    /**
     * A random panel to set the range and distribution of excitatory strengths.
     */
    private RandomPanelNetwork exRandPanel = new RandomPanelNetwork(true);

    /**
     * A random panel to set the range and distribution of inhibitory strengths.
     */
    private RandomPanelNetwork inRandPanel = new RandomPanelNetwork(true);
    
    /**
     * A synapse panel, set to the selected synapse learning rule and displayed
     * upon selection.
     */
    private AbstractSynapsePanel synapsePanel = new ClampedSynapseRulePanel();
	
    /**
     * A checkbox for whether or not inhibitory weights will be randomized.
     */
	private JCheckBox inRandCheck = new JCheckBox();
	
    /**
     * A checkbox for whether or not excitatory weights will be randomized.
     */
	private JCheckBox exRandCheck = new JCheckBox();
	
	/**
	 * A link icon for displaying and editing whether or not random-fields are
	 * linked/synchronized. If changing from an un-linked to a linked state,
	 * values from the most recently edited side is copied to the other side.
	 */
	private LinkIcon randLink;
	
	/**
	 * A link icon for displaying and editing whether or not the synapse types
	 * are linked/syncronized.If changing from an un-linked to a linked state,
	 * values from the most recently edited side is copied to the other side.
	 */
	private LinkIcon typeLink;
	
	/**
	 * The connect neurons object which will be edited when changes are
	 * committed to this panel.
	 */
	private ConnectNeurons connection;
	
	/**
	 * A template excitatory synapse, allowing synapse learning rule values to
	 * be edited in this panel without having to commit them until such a time
	 * as committing changes would be desirable.
	 */
	private Synapse exTemplateSynapse = Synapse.getTemplateSynapse();
	
	/**
	 * A template inhibitory synapse, allowing synapse learning rule values to
	 * be edited in this panel without having to commit them until such a time
	 * as committing changes would be desirable.
	 */
	private Synapse inTemplateSynapse = Synapse.getTemplateSynapse();
	
	/**
	 * The last time the excitatory synapse type was interacted with, used for
	 * determining the direction of synchronization when changing from an
	 * un-linked to a linked state.
	 */
	private long lastExSynTypeEdit = 0L;
	
	/**
	 * The last time the inhibitory synapse type was interacted with, used for
	 * determining the direction of synchronization when changing from an
	 * un-linked to a linked state.
	 */
	private long lastInSynTypeEdit = 0L;
	
	/**
	 * The last time any fields in the excitatory random weights area was
	 * interacted with, used for determining the direction of synchronization
	 * when changing from an un-linked to a linked state.
	 */
	private long lastExRandEdit = 0L;
	
	/**
	 * The last time any fields in the inhibitory random weights area was
	 * interacted with, used for determining the direction of synchronization
	 * when changing from an un-linked to a linked state.
	 */
	private long lastInRandEdit = 0L;

	
	/**
	 * Creates the synapse properties sub-panel for editing how and if weights
	 * are randomized, as well as the synapse type.
	 * @param connection the connection object which will be altered when
	 * values from this panel are committed.
	 */
	public SynapsePropertiesPanel(ConnectNeurons connection){
		this.connection = connection;
		fillFieldValues();
		initializeListeners();
		addListeners();
		initializeLayout();	
	}
	
	/**
	 * Initializes the panel's layout 
	 */
	private void initializeLayout(){
        
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		this.setLayout(layout);	
		
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(3, 3, 3, 3);		
		gbc.weightx = 1.0;
		gbc.gridwidth = 2;
		
		JPanel exRChSub = new JPanel(new FlowLayout());
		exRChSub.add(new JLabel("Random Weights:"));
		exRChSub.add(exRandCheck);
		
		JPanel exR = new JPanel(new BorderLayout());
		exR.add(exRChSub, BorderLayout.NORTH);
		exRandPanel.setPreferredSize(new Dimension((int)exSynTypes.
				getPreferredSize().getWidth(),
				(int)exRandPanel.getPreferredSize().getHeight()));
		exRandCheck.setSelected(false);
		exRandPanel.setEnabled(false);
		exRandPanel.getTfLowBound().setText("0.1");
		exR.add(exRandPanel, BorderLayout.SOUTH);	
		exR.setBorder(BorderFactory.createLineBorder(new Color(255, 50, 50)));	
		this.add(exR, gbc);
			
		gbc.gridx = 2;
		gbc.gridwidth = 1;
		this.add(randLink, gbc);
		
		JPanel inRChSub = new JPanel(new FlowLayout());
		inRChSub.add(new JLabel("Random Weights:"));
		inRChSub.add(inRandCheck);
		
		JPanel inR = new JPanel(new BorderLayout());
		inR.add(inRChSub, BorderLayout.NORTH);
		inRandPanel.setPreferredSize(new Dimension((int)inSynTypes.
				getPreferredSize().getWidth(),
				(int)inRandPanel.getPreferredSize().getHeight()));
		inRandCheck.setSelected(false);
		inRandPanel.setEnabled(false);
		inRandPanel.getTfUpBound().setText("-0.1");
		inR.add(inRandPanel, BorderLayout.SOUTH);	
		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		inR.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 255)));
		this.add(inR, gbc);
	
		gbc.gridx = 0;
		gbc.gridy = 1;
		this.add(exSynTypes, gbc);
		gbc.insets = new Insets(10, 3, 10, 3);
		exSynTypes.setBackground(new Color(255, 200, 200));
		
		gbc.gridx = 2;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(3, 3, 3, 3);
		this.add(typeLink, gbc);		
		
		gbc.gridx = 3;
		gbc.gridwidth = 2;
		gbc.insets = new Insets(10, 3, 10, 3);
		this.add(inSynTypes, gbc);
		inSynTypes.setBackground(new Color(200, 200, 255));
		
	}	
    
    /**
     * Initializes the the class-variable listeners
     */
    private void initializeListeners() {
    	
    	//TODO: Repetitive code within move to an external method, class, etc?
    	 exSynListener =
    	    		new SwitchableActionListener() {
    		   	 public void actionPerformed(ActionEvent e) {
    		   		Object selected = exSynTypes.getSelectedItem();
    	            if (selected != NULL_STRING && isEnabled()) {
    	            	synapsePanel = Synapse.
    	            			RULE_MAP.get(exSynTypes.getSelectedItem());
    	                synapsePanel.fillDefaultValues();
    	                if(!(synapsePanel instanceof
    	                		ClampedSynapseRulePanel)){ 	                	
	    	                StandardDialog sdsp = new StandardDialog(){    	
	    	                	@Override
	    	                	protected void closeDialogOk(){
	    	                		super.closeDialogOk();
	    	                		synapsePanel.commitChanges(
	    	                				exTemplateSynapse);
	    	                	}
	    	                };
	    	                sdsp.setContentPane(synapsePanel);
	    	                sdsp.pack();
	    	                sdsp.setLocationRelativeTo(inSynTypes);
	    	                sdsp.setVisible(true);
    	                } else {
    	                	synapsePanel.commitChanges(exTemplateSynapse);
    	                }
    	            }
    	            if(!typeLink.isLinked()){
    	            	lastExSynTypeEdit = System.nanoTime();
    	            } else {
    	            	inSynListener.disable();
    	            	inSynTypes.setSelectedItem(selected);
    	            	synapsePanel.commitChanges(inTemplateSynapse);
    	            	inSynListener.enable();
    	            }
    		    }	   	 
    	    };

    	inSynListener =
    			new SwitchableActionListener() {

    	   	 public void actionPerformed(ActionEvent e) {
    	            Object selected = inSynTypes.getSelectedItem();
    	            if (selected != NULL_STRING && isEnabled()) {
    	            	synapsePanel = Synapse.
    	            			RULE_MAP.get(inSynTypes.getSelectedItem());
    	                synapsePanel.fillDefaultValues();
    	                if(!(synapsePanel instanceof
    	                		ClampedSynapseRulePanel)){ 	                	
	    	                StandardDialog sdsp = new StandardDialog(){    	
	    	                	@Override
	    	                	protected void closeDialogOk(){
	    	                		super.closeDialogOk();
	    	                		synapsePanel.commitChanges(
	    	                				inTemplateSynapse);
	    	                	}
	    	                };
	    	                sdsp.setContentPane(synapsePanel);
	    	                sdsp.pack();
	    	                sdsp.setLocationRelativeTo(inSynTypes);
	    	                sdsp.setVisible(true);  
    	                } else {
    	                	synapsePanel.commitChanges(
    	                			inTemplateSynapse);
    	                }
    	            }
    	
    	            if(!typeLink.isLinked()){
    	            	lastInSynTypeEdit = System.nanoTime();
    	            } else {
    	            	exSynListener.disable();
    	            	exSynTypes.setSelectedItem(selected);
    	            	synapsePanel.commitChanges(exTemplateSynapse);
    	            	exSynListener.enable();
    	            }
    	        }
    		   	 

        };
    	
        randExListener = 
    			new SwitchablePropertyChangeListener(){

    		@Override
    		public void propertyChange(PropertyChangeEvent arg0) {
				if(Double.parseDouble(exRandPanel.getTfLowBound().getText()) < 0.0) {
					exRandPanel.getTfLowBound().setText("0.0");
				}
				if(Double.parseDouble(exRandPanel.getTfUpBound().getText()) < 0.0) {
					exRandPanel.getTfUpBound().setText("0.0");
				}
    			if(randLink.isLinked() && isEnabled()) {
    				syncRandExToIn();

    			}		
    			
    			if(!randLink.isLinked()) {
    				lastExRandEdit = System.nanoTime();
    			}
    		}		
    		
    		
    	};
        
    	randInListener =
    			new SwitchablePropertyChangeListener(){

    		@Override
    		public void propertyChange(PropertyChangeEvent arg0) {
				if(Double.parseDouble(inRandPanel.getTfUpBound().getText()) > 0.0) {
					inRandPanel.getTfUpBound().setText("0.0");
				}
				if(Double.parseDouble(inRandPanel.getTfLowBound().getText()) > 0.0) {
					inRandPanel.getTfLowBound().setText("0.0");
				}
    			if(randLink.isLinked() && isEnabled()) {
    				syncRandInToEx();
    			}
    			
    			if(!randLink.isLinked()) {
    				lastInRandEdit = System.nanoTime();
    			}
    		}		
    	};
    	
    }
    
    /**
     * Adds listeners to their respective components.
     * NOTE: initializeListeners() must be called prior to this method.
     */
    private void addListeners(){
        exSynTypes.addActionListener(exSynListener);

        inSynTypes.addActionListener(inSynListener);
        
        exRandPanel.addPropertyChangeListenerToFields(randExListener);
        
        inRandPanel.addPropertyChangeListenerToFields(randInListener);

        inRandCheck.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
            	inRandPanel.setEnabled(inRandCheck.isSelected());
                connection.setEnableInhibitoryRandomization(
                   		inRandCheck.isSelected());
	                if(!randLink.isLinked()){
	                	lastInRandEdit = System.nanoTime();
	                }
                } 
        });

        exRandCheck.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {                             
                exRandPanel.setEnabled(exRandCheck.isSelected());         
                connection.setEnableExcitatoryRandomization(
                  		exRandCheck.isSelected());      
                if(!randLink.isLinked()){
                	lastExRandEdit = System.nanoTime();
                }
            }
        });
    
        
        typeLink = new LinkIcon(false){
			@Override
			public void mouseClicked(MouseEvent e) {
				changeState();
				if(isLinked()) {					
					if(lastExSynTypeEdit < lastInSynTypeEdit){
						inSynListener.actionPerformed(null);
					} else {
						exSynListener.actionPerformed(null);
					}
					
				}
			}
		};
		
		randLink = new LinkIcon(false) {
			@Override
			public void mouseClicked(MouseEvent e) {
				changeState();
				if(isLinked()) {				
					if(lastExRandEdit < lastInRandEdit) {
						syncRandInToEx();
					} else {
						syncRandExToIn();
					}
				}
			}
		};
    }
    
    /**
     * Copies field values from the excitatory random panel to the inhibitory
     * random panel.
     */
    private void syncRandExToIn(){
    	Randomizer dummy = new Randomizer();
    	exRandPanel.commitRandom(dummy);
    	randInListener.disable();
    	inRandPanel.fillFieldValues(dummy.mirrorCopy());
    	inRandCheck.setSelected(exRandCheck.isSelected());
    	inRandPanel.setEnabled(exRandCheck.isSelected());
    	randInListener.enable();
    }
    
    /**
     * Copies field values from the inhibitory random panel to the excitatory
     * random panel.
     */
    private void syncRandInToEx(){
    	Randomizer dummy = new Randomizer();
    	inRandPanel.commitRandom(dummy);
    	randExListener.disable();
    	exRandPanel.fillFieldValues(dummy.mirrorCopy());
    	exRandCheck.setSelected(inRandCheck.isSelected());
    	exRandPanel.setEnabled(inRandCheck.isSelected());
    	randExListener.enable();
    }
    
    /**
     * Set default values.
     */
    public void fillFieldValues() {
        inRandPanel.fillFieldValues(connection.getInhibitoryRandomizer());
        exRandPanel.fillFieldValues(connection.getExcitatoryRandomizer());
    }
    
    /**
     * Fills fields with values from a pre-existing connect neurons object.
     * This panel's connection variable is set to the new connect neurons 
     * object by this method.
     * @param connection the connection from which field values are derived.
     */
    public void fillFieldValues(ConnectNeurons connection) {
    	
    	randExListener.disable();
    	randInListener.disable();
    	exSynListener.disable();
    	inSynListener.disable();
    	
    	resetLinks();
    	this.connection = connection;
    	exRandPanel.fillFieldValues(connection.getExcitatoryRandomizer());
    	inRandPanel.fillFieldValues(connection.getInhibitoryRandomizer());
    	
    	exSynTypes.setSelectedItem(connection.getBaseExcitatorySynapse().
    			getLearningRule().getDescription());
    	inSynTypes.setSelectedItem(connection.getBaseInhibitorySynapse().
    			getLearningRule().getDescription());
    	
    	randExListener.enable();
    	randInListener.enable();
    	exSynListener.enable();
    	inSynListener.enable();
    	
    }
    
    /**
     * Resets the links and all associated timers to an un-linked and zeroed
     * state.
     */
    public void resetLinks(){
    	lastInSynTypeEdit = 0L;	
    	lastExSynTypeEdit = 0L;   	
    	lastExRandEdit = 0L;	
    	lastInRandEdit = 0L;
    	randLink.setState(false);
    	typeLink.setState(false);	
    }
    
    /**
     * Committs the changes made to this panel to the connection object.
     */
    public void commitChanges(){
    	connection.setBaseExcitatorySynapse(exTemplateSynapse);
    	connection.setBaseInhibitorySynapse(inTemplateSynapse);
        connection.setEnableInhibitoryRandomization(inRandCheck.isSelected());
        connection.setEnableExcitatoryRandomization(exRandCheck.isSelected());
    	if(exRandCheck.isSelected())
    		exRandPanel.commitRandom(connection.getExcitatoryRandomizer());
    	if(inRandCheck.isSelected())
    		inRandPanel.commitRandom(connection.getInhibitoryRandomizer());
    }

    /**
     * Test main: for rapid prototyping
     * @param args
     */
    public static void main(String[] args) {
    	Sparse s = new Sparse();
    	SynapsePropertiesPanel span = new SynapsePropertiesPanel(s);
    	JFrame frame = new JFrame();
    	frame.setContentPane(span);
    	
    	frame.pack();
    	frame.setLocationRelativeTo(null);
    	frame.setVisible(true);
    	
    }
    
}
