package org.simbrain.custom_sims.simulations.creatures;

import java.util.BitSet;
import java.util.Random;

import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.Gender;
import org.simbrain.custom_sims.simulations.creatures.CreaturesGenome.LifeStage;

public class CreaturesGene {

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
    BitSet bitset = new BitSet(5);
    
    Random rand = new Random();

    public CreaturesGene(String id, boolean duplicatable, boolean mutable,
            boolean cuttable, Gender gender, LifeStage lifeStage) {
        super();
        this.id = id;
        this.duplicatable = duplicatable;
        this.mutable = mutable;
        this.cuttable = cuttable;
        this.gender = gender;
        this.lifeStage = lifeStage;
        
        for (int i = 0; i < 5; i++) {
            if(rand.nextBoolean()){
                bitset.set(i);                
            }
        }
        
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