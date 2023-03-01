package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;
import smile.io.Read;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public class ProjectTSNETest {

    @Test
    public void testTSNE() {

        // Creation projector
        Projector proj = new Projector(5);
        proj.setUseColorManager(false);
        proj.setProjectionMethod("TSNE");

        try {
            proj.getUpstairs().setData(Read.arff("simulations/tables/iris.arff").toArray());
            proj.getProjectionMethod().project();

        } catch (IOException | ParseException | URISyntaxException e) {
            throw new RuntimeException(e);
        }


    }

}