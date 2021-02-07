package org.simbrain.network;

import org.junit.Test;
import smile.math.matrix.Matrix;

/**
 * @return
 */
public class SmileTest {

    @Test
    public void basics() {

        var a = Matrix.eye(3);
        var b = Matrix.diag(new double[]{1, 4, 5});
        // System.out.println(a);
        var c = b.mm(b);
        System.out.println(c);

        //Todo: Just put this through the paces.
        //  Try as much matrix stuff possible

    }
}
