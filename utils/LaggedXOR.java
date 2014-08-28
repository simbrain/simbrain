import java.util.Arrays;


//
// Created lagged XOR file for training networks to handle XOR-in-time tasks.
//
public class LaggedXOR {
    
    public static void main(String[] args) {
        int timeLag = 0;
        int numTimes  = 20;

        // Generate inputs     
        int[] inputData = new int[numTimes + timeLag];
        for (int i = 1; i < numTimes; i++) {
            if (Math.random() > .5)  
                inputData[i] = 0;
            else 
                inputData[i] = 1;
        }        
        System.out.println(Arrays.toString(inputData));

        // Generate training data  
        int[] trainData = new int[numTimes + timeLag];
        for (int i = timeLag + 2; i < numTimes + timeLag; i++) {
            int p = inputData[i - (timeLag + 1)];
            int q = inputData[i - timeLag];
            trainData[i] = p ^ q; // ^ is xor
        }        
        System.out.println(Arrays.toString(trainData));
    }

}
