package org.simbrain.custom_sims.simulations.creatures;

import java.util.BitSet;
import java.util.Random;

import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.Gender;
import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.LifeStage;

public class CreaturesGene {
	
	// A variable for bitset length, for easy configuration later
	static int GeneLength = 5;

    // TODO: Definitely add docs here to explain what these mean
    
    // Header
    String id;
    boolean duplicatable;
    boolean mutable;
    boolean cuttable;
    Gender gender;
    LifeStage lifeStage;
    
    // Mutable part of gene
    //Byte[] bitarray = new Byte[5];
    BitSet bitset;
    
    Random rand = new Random();

    public CreaturesGene(String id, boolean duplicatable, boolean mutable,
            boolean cuttable, Gender gender, LifeStage lifeStage, BitSet bitset) {
        super();
        this.id = id;
        this.duplicatable = duplicatable;
        this.mutable = mutable;
        this.cuttable = cuttable;
        this.gender = gender;
        this.lifeStage = lifeStage;
        this.bitset = bitset;
    }
    
    public CreaturesGene(String id, boolean duplicatable, boolean mutable, boolean cuttable, Gender gender, LifeStage lifeStage) {
    	// Create a random bitset
    	BitSet randomSet = new BitSet(GeneLength);
    	for (int i = 0; i < GeneLength; i++) {
            if(rand.nextBoolean()){
                randomSet.set(i);                
            }
        }
    	
    	// Call constructor
    	// TODO: Not sure what's wrong with this call. It looks fine to me?
    	//CreaturesGene(id, duplicatable, mutable, cuttable, gender, lifeStage, randomSet);
    	
    	// TEMP
    	this.id = id;
        this.duplicatable = duplicatable;
        this.mutable = mutable;
        this.cuttable = cuttable;
        this.gender = gender;
        this.lifeStage = lifeStage;
        this.bitset = randomSet;
    }
    
    /**
     * Creates a copy of the gene. Used in creating child genomes and in duplication mutations.
     * @return
     */
    public CreaturesGene deepCopy() {
    	CreaturesGene copy = new CreaturesGene(this.id, this.duplicatable, this.mutable, this.cuttable, this.gender, this.lifeStage, this.bitset);
    	return copy;
    }
    
    public String toString() {
        String retString = "";
        retString += "Id:" + this.id + "\n";
        retString += "gender:" + this.gender + "\n";
        retString += "bitset:";
        for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i+1)) {
            retString += Boolean.toString(bitset.get(i)) + ",";
        }
        retString+= "\n";
        return retString;
    }
}