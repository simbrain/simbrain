package org.simnet.networks.actorcritic;

public interface ExplorationPolicy {
    public void selectAction(double [] actions);
}
