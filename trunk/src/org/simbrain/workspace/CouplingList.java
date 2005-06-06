/*
 * Created on Jun 5, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.workspace;

import java.util.ArrayList;
import org.simbrain.coupling.*;
import org.simbrain.network.*;
import org.simbrain.network.pnodes.*;
import org.simbrain.world.Agent;
import org.simbrain.world.World;

/**
 * @author yoshimi
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class CouplingList extends ArrayList {
	
	public Coupling getCoupling(int i) {
		return (Coupling)this.get(i);
	}
	
	public PNodeNeuron getPNodeNeuron(int i) {
		return getCoupling(i).getNeuron();
	}

	public void initCastor() {
		for (int i = 0; i < this.size(); i++ ) {
			getCoupling(i).initCastor();
		}
	}
	
	public ArrayList getNeurons(NetworkPanel n) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == (PNodeNeuron)n.getPNodeNeurons().get(i)) {
					ret.add(getPNodeNeuron(j));
				}
					
			}
		}
		return ret;
	}
	
	public ArrayList getMotorCouplingNeurons(NetworkPanel n) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == (PNodeNeuron)n.getPNodeNeurons().get(i)) {
					if (getCoupling(j) instanceof MotorCoupling) {
						ret.add(getPNodeNeuron(j));						
					}
				}
			}
		}
		return ret;
	}

	public ArrayList getSensoryCouplingNeurons(NetworkPanel n) {
		ArrayList ret = new ArrayList();
		
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == (PNodeNeuron)n.getPNodeNeurons().get(i)) {
					if (getCoupling(j) instanceof SensoryCoupling) {
						ret.add(getPNodeNeuron(j));						
					}
				}
			}
		}
		return ret;
	}

	public void removeCouplings(NetworkPanel n) {
		for (int i = 0; i < n.getPNodeNeurons().size(); i++ ) {
			PNodeNeuron pn = (PNodeNeuron)n.getPNodeNeurons().get(i);
			for (int j = 0; j < this.size(); j++ ) {
				if(getPNodeNeuron(j) == pn) {
					remove(getCoupling(j));
				}
					
			}
		}
	}
		
	public void removeAgents(World w) {
		ArrayList agents = w.getAgentList();
		for (int i = 0; i < this.size(); i++ ) {
			for (int j = 0; j < agents.size(); j++ ) {
				if(getCoupling(i).getAgent() ==  agents.get(j)) {
					getCoupling(i).setAgent(null);
				}
					
			}
		}
	}
	
	public CouplingList getNullAgentCouplings() {
	
		CouplingList ret = new CouplingList();
		
		for(int i = 0; i < size(); i++) {
			if (getCoupling(i).getAgent() == null) {
				ret.add(getCoupling(i));
			}
		}
		
		return ret;
	}
	
	public void debug() {
		for (int i = 0; i < size(); i++) {
			System.out.println("------- Coupling [" + i + "] -------");
			getCoupling(i).debug();
		}
	}

}
