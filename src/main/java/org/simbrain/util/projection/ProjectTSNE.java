/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.projection;

import smile.manifold.TSNE;
import smile.projection.PCA;

public class ProjectTSNE extends ProjectionMethod {

    public ProjectTSNE(Projector projector) {
        super(projector);
    }

    @Override
    public void project() {
        // TODO: Move this to a util?
        if (projector.getUpstairs() == null) {
            return;
        }
        if (projector.getUpstairs().getNumPoints() < 1) {
            return;
        }

        var pca = PCA.fit(projector.getUpstairs().getDoubleArray());
        pca.setProjection(15);
        TSNE tsne = new TSNE(pca.project(projector.getUpstairs().getDoubleArray()), 2);
        // System.out.println(tsne);
        System.out.println("---");
        projector.getDownstairs().setData(tsne.coordinates);
        projector.getEvents().getDataChanged().fireAndBlock();
    }

    @Override
    public void init() {

    }

}
