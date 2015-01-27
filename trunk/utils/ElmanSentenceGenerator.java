import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Generate sentences as described in Elman 1990.
 *
 * @author Sam Spevack
 * @author Jeff Yoshimi
 *
 */
public class ElmanSentenceGenerator {

    public static void main(String[] args) {

        List<String> noun_hum = Arrays.asList("man", "woman");
        List<String> noun_anim = Arrays.asList("cat", "mouse");
        List<String> noun_inanim = Arrays.asList("book", "rock");
        List<String> noun_agress = Arrays.asList("dragon", "monster");
        List<String> noun_frag = Arrays.asList("glass", "plate");
        List<String> noun_food = Arrays.asList("cookie", "bread");
        List<String> verb_intran = Arrays.asList("think", "sleep");
        List<String> verb_tran = Arrays.asList("see", "chase");
        List<String> verb_agpat = Arrays.asList("move", "break");
        List<String> verb_percept = Arrays.asList("see", "smell");
        List<String> verb_destroy = Arrays.asList("break", "smash");
        List<String> verb_eat = Arrays.asList("eat");

        int strlength = 1000;
        StringBuilder list = new StringBuilder();

        for (int i = 0; i < strlength; i++) {
            Random generator = new Random();
            switch (generator.nextInt(14)) {
            case 0:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_eat.get(generator.nextInt(1)));
                list.append(" ");
                list.append(noun_food.get(generator.nextInt(2)));
                list.append("\n");
            case 1:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_percept.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_frag.get(generator.nextInt(2)));
                list.append("\n");
            case 2:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_destroy.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_frag.get(generator.nextInt(2)));
                list.append("\n");
            case 3:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_intran.get(generator.nextInt(2)));
                list.append("\n");
            case 4:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_tran.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append("\n");
            case 5:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_agpat.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_inanim.get(generator.nextInt(2)));
                list.append("\n");
            case 6:
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_agpat.get(generator.nextInt(2)));
                list.append("\n");
            case 7:
                list.append(noun_anim.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_eat.get(generator.nextInt(1)));
                list.append(" ");
                list.append(noun_food.get(generator.nextInt(2)));
                list.append("\n");
            case 8:
                list.append(noun_anim.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_tran.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_anim.get(generator.nextInt(2)));
                list.append("\n");
            case 9:
                list.append(noun_anim.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_agpat.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_inanim.get(generator.nextInt(2)));
                list.append("\n");
            case 10:
                list.append(noun_anim.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_agpat.get(generator.nextInt(2)));
                list.append("\n");
            case 11:
                list.append(noun_inanim.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_agpat.get(generator.nextInt(2)));
                list.append("\n");
            case 12:
                list.append(noun_agress.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_destroy.get(generator.nextInt(2)));
                list.append(" ");
                list.append(noun_frag.get(generator.nextInt(2)));
                list.append("\n");
            case 13:
                list.append(noun_agress.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_eat.get(generator.nextInt(1)));
                list.append(" ");
                list.append(noun_hum.get(generator.nextInt(2)));
                list.append("\n");
            case 14:
                list.append(noun_agress.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_eat.get(generator.nextInt(1)));
                list.append(" ");
                list.append(noun_anim.get(generator.nextInt(2)));
                list.append("\n");
            case 15:
                list.append(noun_agress.get(generator.nextInt(2)));
                list.append(" ");
                list.append(verb_eat.get(generator.nextInt(1)));
                list.append(" ");
                list.append(noun_food.get(generator.nextInt(2)));
                list.append("\n");
            default:
                break;
            }

        }
        System.out.println(list);
    }

}
