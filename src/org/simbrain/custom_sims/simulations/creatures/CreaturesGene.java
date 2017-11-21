package org.simbrain.custom_sims.simulations.creatures;

import java.util.List;
import java.util.Random;

import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.Gender;
import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.GeneType;
import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.LifeStage;

/**
 * Add javadocs.
 */
public class CreaturesGene {

    // TODO: Definitely add docs here to explain what these mean

    // Header
    String id;
    boolean duplicatable;
    boolean mutable;
    boolean cuttable;
    Gender gender;
    GeneType geneType;
    LifeStage lifeStage;

    // Below replaces the bitset.
    // It shoudl specify what a given "allele" of a gene does
    // This may not work long term for all gene types but it's a start
    // Mutable part of gene
    String allele;

    static Random rand = new Random();

    public CreaturesGene(String id, boolean duplicatable, boolean mutable,
            boolean cuttable, Gender gender, GeneType geneType,
            LifeStage lifeStage, String value) {
        super();
        this.id = id;
        this.duplicatable = duplicatable;
        this.mutable = mutable;
        this.cuttable = cuttable;
        this.gender = gender;
        this.geneType = geneType;
        this.lifeStage = lifeStage;
        this.allele = value;
    }

    public CreaturesGene(String id, boolean duplicatable, boolean mutable,
            boolean cuttable, Gender gender, GeneType geneType,
            LifeStage lifeStage, List<String> valueChoices) {
        this(id, duplicatable, mutable, cuttable, gender, geneType, lifeStage,
                valueChoices.get(rand.nextInt(valueChoices.size())));

    }

    /**
     * Creates a copy of the gene. Used in creating child genomes and in
     * duplication mutations.
     * 
     * @return
     */
    public CreaturesGene deepCopy() {
        CreaturesGene copy = new CreaturesGene(this.id, this.duplicatable,
                this.mutable, this.cuttable, this.gender, this.geneType,
                this.lifeStage, this.allele);
        return copy;
    }

    public String toString() {
        String retString = "";
        retString += "Id:" + this.id + "\n";
        retString += "gender:" + this.gender + "\n";
        retString += "value:" + this.allele;
        retString += "\n";
        return retString;
    }

    /**
     * @return the allele
     */
    public String getAllele() {
        return allele;
    }
}