/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.simbrain.world.threedee.environment;

import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Quaternion;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.shape.Dome;
import com.jme.scene.state.LightState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;

/**
* A Dome used to create a sky effect.  Parts taken from jME project source.
*
* @author Matt Watson
*/
public class SkyDome extends Node {
    /** The default serial version ID. */
    private static final long serialVersionUID = 1L;
    
    /** One hundred-eighty degrees in radians. */
    private static final float HALF_CIRCLE = (float) Math.toRadians(180);

    /** The dome that will represent the sky. */
    private Dome skyboxDome;
    /** The renderer for the SkyDome. */
    private Renderer renderer;
    
    /**
     * Creates a new skyDome. The size of the skyDome and name is specified here.
     * By default, no textures are set.
     *
     * @param name The name of the SkyBox.
     * @param planes The planes of the Dome.
     * @param radialSamples The radial samples of the Dome.
     * @param radius The radius of the Dome.
     */
    public SkyDome(final String name, final int planes, final int radialSamples,
            final float radius) {
        super(name);

        skyboxDome = new Dome("topSkyDome", planes, radialSamples, radius);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getType() {
        return (Spatial.NODE | Spatial.SKY_BOX);
    }

    /**
     * Set the texture to be displayed on the given side of the skybox. Replaces
     * any existing texture on that side.
     *
     * @param texture The texture for that side to assume.
     */
    public void setTexture(final Texture texture) {
        skyboxDome.clearRenderState(RenderState.RS_TEXTURE);
        setTexture(texture, 0);
    }

    /**
     * Set the texture to be displayed on the given side of the skybox. Only
     * replaces the texture at the index specified by textureUnit.
     *
     * @param texture The texture for that side to assume.
     * @param textureUnit The texture unite of the given side's TextureState the texture
     *        will assume.
     */
    public void setTexture(final Texture texture, final int textureUnit) {
        TextureState ts = (TextureState) skyboxDome.getRenderState(RenderState.RS_TEXTURE);
        if (ts == null) {
            ts = renderer.createTextureState();
        }

        /* Initialize the texture state */
        ts.setTexture(texture, textureUnit);
        ts.setEnabled(true);

        /* Set the texture to the quad */
        skyboxDome.setRenderState(ts);

        return;
    }

    /**
     * Initializes the SkyDome.
     */
    public void initialize() {
        /* Create Dome */
        skyboxDome.setLocalRotation(new Quaternion(new float[] {0, HALF_CIRCLE, 0}));

        /* We don't want the light to effect our skybox */
        LightState lightState = renderer.createLightState();
        lightState.setEnabled(false);
        setRenderState(lightState);
        setLightCombineMode(LightState.REPLACE);
        setTextureCombineMode(TextureState.REPLACE);

        ZBufferState zbuff = renderer.createZBufferState();
        zbuff.setWritable(false);
        zbuff.setEnabled(true);
        zbuff.setFunction(ZBufferState.CF_LEQUAL);
        setRenderState(zbuff);

        /* We don't want it making our skybox disapear, so force view */
        setCullMode(Spatial.CULL_NEVER);

        /* Make sure texture is only what is set. */
        skyboxDome.setTextureCombineMode(TextureState.REPLACE);

        /* Make sure no lighting on the skybox */
        skyboxDome.setLightCombineMode(LightState.REPLACE);

        /* Make sure the quad is viewable */
        skyboxDome.setCullMode(Spatial.CULL_NEVER);

        /* Set a bounding volume */
        skyboxDome.setModelBound(new BoundingBox());
        skyboxDome.updateModelBound();

        skyboxDome.setRenderQueueMode(Renderer.QUEUE_SKIP);
        skyboxDome.setVBOInfo(null);

        /* And attach the SkyDome as a child */
        attachChild(skyboxDome);
    }

    /**
     * Force all of the textures to load. This prevents pauses later during the
     * application as you pan around the world.
     */
    public void preloadTextures() {
        TextureState ts = (TextureState) skyboxDome.getRenderState(RenderState.RS_TEXTURE);
        if (ts != null) { ts.apply(); }
    }

    /**
     * Sets the renderer for this instance.
     * 
     * @param renderer The renderer to set.
     */
    void setRenderer(final Renderer renderer) {
        this.renderer = renderer;
    }
}
