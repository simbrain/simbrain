package org.simnet.networks.actorcritic;

import java.util.*;

public class RandomExplorationPolicy implements ExplorationPolicy {
	
    /** probability of picking the acting with the highest activation */
    double maxActionProbability_ = 0.7;
    
    /** random number generator */
    Random rand_;

    public RandomExplorationPolicy(){
	this.rand_ = new Random(System.currentTimeMillis());
    }
    
    public void selectAction(double[] actions) {
	double max = actions[0];
	int index = 0;
	actions[0] = 1;
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
	double prob = this.rand_.nextDouble();
	if(prob > this.maxActionProbability_){
	    int index2;
	    index2 = this.rand_.nextInt(actions.length-2);
	    if(index2 >= index) index2++;
	    actions[index2] = 1;
	    actions[index] = 0;
	}
    }

}
