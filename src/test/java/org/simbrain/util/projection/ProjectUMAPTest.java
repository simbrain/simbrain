package org.simbrain.util.projection;

import org.junit.jupiter.api.Test;
import smile.io.Read;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public class ProjectUMAPTest {

    @Test
    public void testUMAP() {

        // Creation projector
        Projector proj = new Projector(5);
        proj.setUseColorManager(false);
        proj.setProjectionMethod("UMAP");

        try {
            proj.getUpstairs().setData(Read.arff("simulations/tables/iris.arff").toArray());
            proj.getProjectionMethod().project();

        } catch (IOException | ParseException | URISyntaxException e) {
            throw new RuntimeException(e);
        }


    }

}