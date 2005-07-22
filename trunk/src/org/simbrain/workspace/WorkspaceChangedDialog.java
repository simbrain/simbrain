package org.simbrain.workspace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

import org.simbrain.gauge.GaugeFrame;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.UserPreferences;
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
	private boolean clear = false;
	private Workspace parent;

	public WorkspaceChangedDialog(ArrayList __networkChangeList, ArrayList __odorWorldChangeList, ArrayList __dataWorldChangeList,ArrayList __gaugeChangeList,Workspace parent){
		networkChangeList = __networkChangeList;
		odorWorldChangeList = __odorWorldChangeList;
		dataWorldChangeList = __dataWorldChangeList;
		gaugeChangeList = __gaugeChangeList;
		this.parent = parent;
		init();
	}

	public WorkspaceChangedDialog(ArrayList __networkChangeList, ArrayList __odorWorldChangeList, ArrayList __dataWorldChangeList,ArrayList __gaugeChangeList,Workspace parent,boolean clear){
		networkChangeList = __networkChangeList;
		odorWorldChangeList = __odorWorldChangeList;
		dataWorldChangeList = __dataWorldChangeList;
		gaugeChangeList = __gaugeChangeList;
		init();
		this.parent = parent;
		this.clear = clear;
	}
	
	public void init(){
		
		for (int i = 0; i < networkChangeList.size(); i++){
			NetworkFrame save = (NetworkFrame)networkChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			nCheckBoxList.add(i,checker);
		}
		for (int i = 0; i < odorWorldChangeList.size(); i++){
			OdorWorldFrame save = (OdorWorldFrame)odorWorldChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			oCheckBoxList.add(i,checker);
		}
		for (int i = 0; i < dataWorldChangeList.size(); i++){
			DataWorldFrame save = (DataWorldFrame)dataWorldChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			dCheckBoxList.add(i,checker);
		}
		for (int i = 0; i < gaugeChangeList.size(); i++){
			GaugeFrame save = (GaugeFrame)gaugeChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			gCheckBoxList.add(i,checker);
		}

		
		
	JButton ok = new JButton("Save Checked Frames");
	JButton cancel = new JButton("Cancel");
	panel.addItem("",ok);
	panel.addItem("",cancel);
	ok.addActionListener(this);
	ok.setActionCommand("ok");
	cancel.addActionListener(this);
	cancel.setActionCommand("cancel");

	setContentPane(panel);
	setLocationRelativeTo(getParent());
	pack();
	setVisible(true);
	setTitle("Workspace has changed");
	setModal(true);
	}	

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("ok")){
			for(int i = 0;i<nCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)nCheckBoxList.get(i);
				NetworkFrame testFrame = (NetworkFrame)networkChangeList.get(i);
				if(test.isSelected()){
					testFrame.getNetPanel().saveAs();
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
			if(!clear)
				quit();
			if(clear){
				parent.disposeAllFrames();
				parent.getCouplingList().clear();
				parent.current_file = null;
				parent.setTitle("Simbrain");
			}
		} else if (e.getActionCommand().equals("cancel")){
			dispose();
		}
	}
	
	protected void quit() {
		UserPreferences.saveAll(); // Save all user preferences
		System.exit(0);
	}
}
