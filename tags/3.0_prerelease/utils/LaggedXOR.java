import java.util.Arrays;
import java.io.*;


//
// Created lagged XOR file for training networks to handle XOR-in-time tasks.
//
// To print data to console simply uncoment the System.out.println's below.
//
public class LaggedXOR {

    public static void main(String[] args) {
        int timeLag = 0;
        int numTimes  = 20;

        // Generate inputs     
        try {
            FileWriter writer = new FileWriter(new File("LaggedXOR_In.csv"));
            int[] inputData = new int[numTimes + timeLag];
            for (int i = 1; i < numTimes; i++) {
                if (Math.random() > .5)  
                    inputData[i] = 0;
                else 
                    inputData[i] = 1;
            }        
            for(int i = 0; i < inputData.length; i++) {
                writer.append("" + inputData[i]);
                writer.append("\n");                
            }
            writer.flush();
            writer.close();
            //System.out.println(Arrays.toString(inputData));

            // Generate training data  
            FileWriter writer2 = new FileWriter(new File("LaggedXOR_Train.csv"));
            int[] trainData = new int[numTimes + timeLag];
            for (int i = timeLag + 2; i < numTimes + timeLag; i++) {
                int p = inputData[i - (timeLag + 1)];
                int q = inputData[i - timeLag];
                trainData[i] = p ^ q; // ^ is xor
            }
            for(int i = 0; i < trainData.length; i++) {
                writer2.append("" + trainData[i]);
                writer2.append("\n");                
            }
            writer2.flush();
            writer2.close();            
            //System.out.println(Arrays.toString(trainData));            
    }
    catch(IOException e) {
        e.printStackTrace();
    }     
}

}
