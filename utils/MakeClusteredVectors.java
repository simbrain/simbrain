import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//
// Make a set of vectors that are clustered in an n-d space.
// Useful for unsupervised learning. Thanks to Scott Hotton for help!
//
public class MakeClusteredVectors {    

    static int numVectorsPerCluster = 3;
    static double range = .3;
    static List<double[]> prototypeVectors = new<double[]> ArrayList();

    static {
        prototypeVectors.add(new double[] { 0, 1, 0, 0, 0, 0, 0, 0, 0 });
        prototypeVectors.add(new double[] { 0, 0, 0, 0, 0, 0, 0, 1, 0 });
//        prototypeVectors.add(new double[] { 0, 1, 0});
 //       prototypeVectors.add(new double[] { 1, 0, 1});
    }

    public static void main(String[] args) {
        try {
            FileWriter writer = new FileWriter(new File("PrototypeVecs.csv"));
            for (double[] vector : prototypeVectors) {
                for (int i = 0; i < numVectorsPerCluster; i++) {
                    double[] theVec = getRandomVectorInRange(vector, range);
                    String vectorString = Arrays.toString(theVec);
                    // strip brackets
                    vectorString = vectorString.substring(1, vectorString.length()-1);
                    writer.append("" + vectorString);
                    writer.append("\n");
                    //System.out.println(vectorString);
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 
    // Returns a random vector within a given range of a given vector.  
    // The vector is drawn from a uniform distribution on a hypercube
    // around the given vector.
    // 
    public static double[] getRandomVectorInRange(final double[] baseVector,
            final double range) {

        double[] returnVector = new double[baseVector.length];

        for (int i = 0; i < baseVector.length; i++) {
            double component = baseVector[i];
            double randComponent = ((2*range) * (Math.random() - .5)) + component;
            returnVector[i] = randComponent;
        }
        return returnVector;
    }
    
}
