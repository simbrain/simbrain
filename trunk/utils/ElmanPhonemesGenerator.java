import java.util.Random;

/**
 * Generate phoneme strings as described in Elman 1990.
 *
 * @author Sam Spevack
 * @author Jeff Yoshimi
 * 
 */
public class ElmanPhonemesGenerator {

    private static int strlength = 1000;

    public static void main(String[] args) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < strlength; i++) {
            Random generator = new Random();
            int number = generator.nextInt(3);
            switch (number) {
            case 0:
                list.append("ba");
                break;
            case 1:
                list.append("dii");
            default:
                list.append("guuu");
                break;
            }
        }
        System.out.println(list);
    }

}
