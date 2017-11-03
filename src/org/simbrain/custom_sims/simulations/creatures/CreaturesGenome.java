package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Add javadocs :) 
 */
public class CreaturesGenome {

    List<CreaturesGene> geneList = new ArrayList();

    public static enum Gender {
        Both, Male, Female
    };

    public static enum LifeStage {
        Embryo, Child, Adolescent, Youth, Adult, Senile
    };

    public CreaturesGenome() {
        super();
    }

    
    public void addGene(CreaturesGene gene) {
        geneList.add(gene);
    }
    
    // TODO: Add method stubs or methods.  mutate.  cut.  createrandomgenome
    // where possible document logic of method stubs

    // temp testing main
    public static void main(String[] args) {
        CreaturesGenome genome = new CreaturesGenome();
        genome.addGene(new CreaturesGene("gene1", true, true, true, Gender.Both,
                LifeStage.Adolescent));
        genome.addGene(new CreaturesGene("gene2", true, true, true, Gender.Both,
                LifeStage.Adolescent));
        System.out.println(genome);
    }


    @Override
    public String toString() {
        String retString = "Geneome\n------\n";
        for(CreaturesGene gene : geneList) {
            retString += gene.toString() + "\n";
        }
        return retString;
    }

}
