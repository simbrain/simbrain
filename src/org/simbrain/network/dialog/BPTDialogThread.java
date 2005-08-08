package org.simbrain.network.dialog;

import javax.swing.SwingUtilities;

public class BPTDialogThread extends Thread {

	private BackpropTrainingDialog dialog = null;
	private volatile boolean isRunning = false;
	
	Runnable iterate = new Runnable() {
		public void run(){
			dialog.iterate();
		}
	};
	
	public BPTDialogThread(BackpropTrainingDialog dialog){
		this.dialog = dialog;
	}

	
	public void run(){
		try {
			while(isRunning == true){
				dialog.setUpdateCompleted(false);
				SwingUtilities.invokeLater(iterate);
				while(!dialog.isUpdateCompleted()){
					sleep(10);
				}
			}	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

}
