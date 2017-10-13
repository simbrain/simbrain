package org.simbrain.custom_sims.simulations.creatures;

public class CreaturesSVRule {

    // Just a copy of the opcodes from Creatures 1 for now. We might want to
    // trim
    // this list down later.
    // List of opcodes was found here:
    // http://double.nz/creatures/tutorial2/tutorial2.htm
    // TODO: Do we want to keep all possible opcodes in one enum, or do we want
    // to
    // split the operator opcodes from the variable opcodes?
    public static enum OpCode {
        end, Zero, One, SixtyFour, TwoHundredTwentyFive, 
        chem0, chem1, chem2, chem3, 
        state, 
        output, 
        thres, 
        type0, type1, 
        anded0, anded1, 
        input, 
        conduct, suscept, 
        STW, LTW, 
        Strength, 
        TRUE, PLUS, MINUS, TIMES, INCR, DECR
    };

}
