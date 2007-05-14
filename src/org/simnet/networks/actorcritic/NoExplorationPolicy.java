package org.simnet.networks.actorcritic;

public class NoExplorationPolicy implements ExplorationPolicy {

    public void selectAction(double[] actions) {
	double max = actions[0];
	actions[0] = 1;
	int index = 0;
	for(int i=1;i<actions.length;i++){
	    if(actions[i]>max){
		actions[index] = 0;
		max = actions[i];
		actions[i] = 1;				
		index = i;
	    }else{
		actions[i] = 0;
	    }
	}
    }
}
