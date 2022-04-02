// package org.simbrain.util.math.ProbDistributions;
//
// import org.simbrain.util.UserParameter;
// import org.simbrain.util.math.ProbabilityDistribution;
//
// /**
//  * Returns one of two values.
//  * TODO: Do this in terms of proper theory
//  * TODO: Interact with polarity
//  */
// public class TwoValued extends ProbabilityDistribution {
//
//     @UserParameter(
//             label = "Upper value",
//             order = 1)
//     private double upper = -1;
//
//     @UserParameter(
//             label = "Lower value",
//             order = 2)
//     private double lower = 1;
//
//     @UserParameter(
//             label = "Threshold",
//             description = "If >= this threshold choose upper, else lower",
//             order = 3)
//     private double p = .5;
//
//     /**
//      * Public constructor for reflection-based creation. You are encourage to use
//      * the builder pattern provided for ProbabilityDistributions.
//      */
//     public TwoValued() {
//     }
//
//     public double nextDouble() {
//         return Math.random() > p ? upper : lower;
//     }
//
//     public int nextInt() {
//         return (int) nextDouble();
//     }
//
//
//     public String getName() {
//         return "Two valued";
//     }
//
//     public String toString() { return "Two valued"; }
//
//     @Override
//     public TwoValued deepCopy() {
//         TwoValued cpy = new TwoValued();
//         cpy.upper = this.upper;
//         cpy.lower = this.lower;
//         cpy.p = this.p;
//         return cpy;
//     }
//
//     @Override
//     public void setClipping(boolean clipping) {
//     }
//
//     @Override
//     public void setCeiling(double ceiling) {
//         this.upper = ceiling;
//     }
//
//     @Override
//     public void setFloor(double floor) {
//         this.lower = floor;
//     }
//
// }