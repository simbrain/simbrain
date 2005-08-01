package org.simbrain.workspace;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.dataworld.DataWorldFrame;
import org.simbrain.world.odorworld.OdorWorldFrame;




public class WorkspaceChangedDialog extends JDialog implements ActionListener {
	
	private LabelledItemPanel panel= new LabelledItemPanel();
	private ArrayList nCheckBoxList = new ArrayList();
	private ArrayList oCheckBoxList = new ArrayList();
	private ArrayList dCheckBoxList = new ArrayList();
	private ArrayList gCheckBoxList = new ArrayList();
	private ArrayList networkChangeList = new ArrayList();
	private ArrayList odorWorldChangeList = new ArrayList();
	private ArrayList dataWorldChangeList = new ArrayList();
	private ArrayList gaugeChangeList = new ArrayList();
	private Workspace parent;
	private boolean userCancelled = false;

	public WorkspaceChangedDialog(Workspace parent){
		networkChangeList = parent.getNetworkList().getChanges();
		odorWorldChangeList = parent.getOdorWorldChangeList();
		dataWorldChangeList = parent.getDataWorldChangeList();
		gaugeChangeList = parent.getGaugeChangeList();
		this.parent = parent;
		init();
	}
	
	public void init(){

		initPanel();
		
		this.getContentPane().setLayout(new BorderLayout());

		JButton ok = new JButton("OK");
		JButton cancel = new JButton("Cancel");
		getContentPane().add(ok);
		getContentPane().add(cancel);
		ok.addActionListener(this);
		ok.setActionCommand("ok");
		cancel.addActionListener(this);
		cancel.setActionCommand("cancel");
		
		getContentPane().add(BorderLayout.CENTER,panel);
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		northPanel.add(new JLabel("Select the resources to save:       "));
		getContentPane().add(BorderLayout.NORTH, northPanel);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(ok);
		buttonPanel.add(cancel);
		getContentPane().add(BorderLayout.SOUTH,buttonPanel);

		setTitle("Save Resources");
		pack();
		setLocationRelativeTo(null);
		setModal(true);
		setVisible(true);
	}
	
	public void initPanel(){
		for (int i = 0; i < networkChangeList.size(); i++){
			NetworkFrame save = (NetworkFrame)networkChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem("Network: " + save.getTitle(),checker);
			nCheckBoxList.add(i,checker);
		}
		for (int i = 0; i < odorWorldChangeList.size(); i++){
			OdorWorldFrame save = (OdorWorldFrame)odorWorldChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem("Odor-world: " + save.getTitle(),checker);
			oCheckBoxList.add(i,checker);
		}
		for (int i = 0; i < dataWorldChangeList.size(); i++){
			DataWorldFrame save = (DataWorldFrame)dataWorldChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem("Data-world: " + save.getTitle(),checker);
			dCheckBoxList.add(i,checker);
		}
		for (int i = 0; i < gaugeChangeList.size(); i++){
			GaugeFrame save = (GaugeFrame)gaugeChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem("Gauge: " + save.getTitle(),checker);
			gCheckBoxList.add(i,checker);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("cancel")) {
			userCancelled = true;
			dispose();
		} else if(e.getActionCommand().equals("ok")){
			for(int i = 0;i<nCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)nCheckBoxList.get(i);
				NetworkFrame testFrame = (NetworkFrame)networkChangeList.get(i);
				if(test.isSelected()){
					testFrame.getNetPanel().save();
				}
				testFrame.setChangedSinceLastSave(false);
			}
			for(int i = 0;i<oCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)oCheckBoxList.get(i);
				OdorWorldFrame testWorld = (OdorWorldFrame)odorWorldChangeList.get(i);
				if(test.isSelected()){
					testWorld.saveWorld(testWorld.getCurrentFile());
				}
				testWorld.setChangedSinceLastSave(false);
			}
			for(int i = 0;i<dCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)dCheckBoxList.get(i);
				DataWorldFrame testFrame = (DataWorldFrame)dataWorldChangeList.get(i);
				if(test.isSelected()){
					testFrame.saveWorld();
				}
				testFrame.setChangedSinceLastSave(false);
			}
			for(int i = 0;i<gCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)gCheckBoxList.get(i);
				GaugeFrame testFrame = (GaugeFrame)gaugeChangeList.get(i);
				if(test.isSelected()){
					testFrame.saveCombined();
				}
				testFrame.setChangedSinceLastSave(false);
			} 
			dispose();
		} 
		else { 
			dispose();
		}
	}
	

	/**
	 * @return Returns the userCancelled.
	 */
	public boolean hasUserCancelled() {
		return userCancelled;
	}
}
