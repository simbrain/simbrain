import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

//
// Read a bitmap file and convert images to binary vectors.    Can be adapted fairly easily
// to bitmaps containing different letters in different sizes.
//
public class PatternsFromBitmap {
    
    public static void main(String[] args) {
        BufferedImage img = null;
        ArrayList<int[][]> letters = new ArrayList<int[][]>();
        try {
            img = ImageIO.read(new File("chars.bmp"));
            for(int charnum = 0; charnum < 26; charnum++) {
                int[][] letter = new int[10][8];
                for (int row = 0; row < 10; row++) {
                    for (int col = 0; col < 8; col++) {
                        letter[row][col] = (img.getRGB(col+(charnum*8),row) == -1) ? 0 : 1;
                        //System.out.print(letter[row][col]);
                    }                
                    //System.out.println();
                }
                //System.out.println("--------");
                letters.add(letter);
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        
        for (int[][] letter : letters) {
            for (int row = 0; row < 10; row++) {
                for (int col = 0; col < 8; col++) {
                    System.out.print(letter[row][col]);
                    if (!((row == 9) && (col == 7))) {
                        System.out.print(",");
                    }
                }                
            }
            System.out.println();            
        }
        
    }

}
