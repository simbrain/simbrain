package org.simbrain.custom_sims.simulations.creatures;

import java.util.List;
import java.util.Random;

/**
 * The genes of a creature defines the physiological of a Creature, from how
 * chemicals affect them to how their brain are shaped and wired.
 */
public class CreaturesGene {

    /**
     * Gender defines whether a gene will be expressed in creatures of male, female,
     * or either sex.
     */
    public static enum Gender {
        Both, Male, Female
    }

    /**
     * LifeStage defines the life stage at which a gene will start to be expressed.
     * Note that switching to another lifestage will not un-express the gene -
     * nothing can do that as far as I know.
     */
    public static enum LifeStage {
        Embryo, Child, Adolescent, Youth, Adult, Senile
    }

    /**
     * GeneType defines the way the gene will be expressed. A BrainLobe gene will
     * create a brain lobe, a ChemReceptor gene a chemical receptor, etc.
     * ChemHalfLife and ChemConcentration genes are a bit special, in that only one
     * of each is needed to define the half lives and initial concentrations of ALL
     * chemicals.
     * <p>
     * @see <a href=http://double.nz/creatures/genetics.htm></a>
     */
    public static enum GeneType {
        BrainLobe, ChemReceptor, ChemEmitter, ChemReaction, ChemHalfLife, ChemConcentration, Stimulus, Instinct
    }

    /**
     * These variables are collectively known as the header. Every gene contains a
     * header, and these variables are unmutable. The "duplicatable" variable
     * defines whether the gene can be duplicated or not. The "mutable" variable
     * defines whether the gene could have point mutations or not. The "cuttable"
     * variable defines whether the gene could be deleted or not. "gender",
     * "geneType", and "lifeStage" have descriptive javadocs under their enum's
     * definition.
     */
    boolean duplicatable;
    boolean mutable;
    boolean cuttable;
    Gender gender;
    GeneType geneType;
    LifeStage lifeStage;

    /**
     * An optional string that can be used to describe what the individual gene
     * does.
     */
    String desc;

    // Below replaces the bitset.
    // It should specify what a given "allele" of a gene does
    // This may not work long term for all gene types but it's a start
    // Mutable part of gene
    String allele;

    static Random rand = new Random();

    public CreaturesGene(String desc, boolean duplicatable, boolean mutable, boolean cuttable, Gender gender, GeneType geneType, LifeStage lifeStage, String value) {
        super();
        this.desc = desc;
        this.duplicatable = duplicatable;
        this.mutable = mutable;
        this.cuttable = cuttable;
        this.gender = gender;
        this.geneType = geneType;
        this.lifeStage = lifeStage;
        this.allele = value;
    }

    public CreaturesGene(boolean duplicatable, boolean mutable, boolean cuttable, Gender gender, GeneType geneType, LifeStage lifeStage, String value) {
        this(null, duplicatable, mutable, cuttable, gender, geneType, lifeStage, value);
    }

    public CreaturesGene(String desc, boolean duplicatable, boolean mutable, boolean cuttable, Gender gender, GeneType geneType, LifeStage lifeStage, List<String> valueChoices) {
        this(desc, duplicatable, mutable, cuttable, gender, geneType, lifeStage, valueChoices.get(rand.nextInt(valueChoices.size())));
    }

    public CreaturesGene(boolean duplicatable, boolean mutable, boolean cuttable, Gender gender, GeneType geneType, LifeStage lifeStage, List<String> valueChoices) {
        this(null, duplicatable, mutable, cuttable, gender, geneType, lifeStage, valueChoices.get(rand.nextInt(valueChoices.size())));
    }

    /**
     * Creates a copy of the gene. Used in creating child genomes and in duplication
     * mutations.
     *
     * @return
     */
    public CreaturesGene deepCopy() {
        CreaturesGene copy = new CreaturesGene(this.desc, this.duplicatable, this.mutable, this.cuttable, this.gender, this.geneType, this.lifeStage, this.allele);
        return copy;
    }

    public String toString() {
        String retString = "CREATURE GENE \n";
        if (desc != null) {
            retString += "Description: " + this.desc + "\n";
        } else {
            retString += "No gene description available. \n";
        }
        retString += "Header \n";
        retString += "gene type: " + this.geneType + "\n";
        retString += "dup, mut, cut: " + this.duplicatable + ", " + this.mutable + ", " + this.cuttable + "\n";
        retString += "gender: " + this.gender + "\n";
        retString += "life stage: " + this.lifeStage + "\n";
        retString += "value: " + this.allele;
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