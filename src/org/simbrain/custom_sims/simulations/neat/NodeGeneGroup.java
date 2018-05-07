package org.simbrain.custom_sims.simulations.neat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NodeGeneGroup {
    private String groupName;
    private List<NodeGene> nodeGenes;

    private NodeGeneGroup() {}

    public static NodeGeneGroup of(String groupName, NodeGene ...nodeGenes) {
        NodeGeneGroup newGroup = new NodeGeneGroup();
        newGroup.nodeGenes = Arrays.asList(nodeGenes);
        // TODO: find a better way
        for (NodeGene ng : newGroup.nodeGenes) {
            ng.setGroupName(groupName);
        }
        newGroup.groupName = groupName;
        return newGroup;
    }

    public static NodeGeneGroup of(String groupName, NodeGene nodeGene, int count) {
        nodeGene.setGroupName(groupName);
        NodeGeneGroup newGroup = new NodeGeneGroup();
        newGroup.nodeGenes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            newGroup.nodeGenes.add(nodeGene.deepCopy());
        }
        newGroup.groupName = groupName;
        return newGroup;
    }

    public NodeGene getNodeGene(int index) {
        return nodeGenes.get(index);
    }

    public List<NodeGene> getNodeGenes() {
        return nodeGenes;
    }

    public String getName() {
        return groupName;
    }
}
