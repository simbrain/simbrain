package org.simbrain.util;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;

public class UtilsTest extends TestCase {

    @Test
    public void testGetDoubleMatrix() {
        File file = new File(getClass().getClassLoader().getResource("iris_test.csv").getFile());
        var doubles = Utils.getDoubleMatrix(file);
        assertEquals(5.1,doubles[0][0]);
    }

    @Test
    public void testGetStringMatrix() {
        File file = new File(getClass().getClassLoader().getResource("iris_test.csv").getFile());
        var strings = Utils.getStringMatrix(file);
        assertEquals("5.1",strings[0][0]);
    }

}