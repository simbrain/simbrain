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

package org.simbrain.world.threedee;

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
* A Box made of textured quads that simulate having a sky, horizon and so forth
* around your scene. Either attach to a camera node or update on each frame to
* set this skybox at the camera's position.
*
* @author David Bitkowski
* @author Jack Lindamood (javadoc only)
* @version $Id: SkyDome.java,v 1.1 2006/02/27 12:01:25 achilleterzo Exp $
*/
public class SkyDome extends Node {
    private static final long serialVersionUID = 1L;

//    private int planes;
//    private int radialSamples;
//    private float radius;

    private Dome skyboxDome;

    static int counter = 0;
    
    final int count = counter++;
    
    Renderer renderer;
    
    /**
     * Creates a new skyDome. The size of the skyDome and name is specified here.
     * By default, no textures are set.
     *
     * @param name
     *            The name of the skybox.
     * @param planes
     *            The planes of the Dome.
     * @param radialSamples
     *            The radial samples of the Dome.
     * @param radius
     *            The radius of the Dome.
     */
    public SkyDome(String name, int planes, int radialSamples, float radius) {
        super(name);

//        this.planes = planes;
//        this.radialSamples = radialSamples;
//        this.radius = radius;

        skyboxDome = new Dome("topSkyDome", planes, radialSamples, radius);
        
//        initialize();
    }

    public SkyDome(String name, int planes, int radialSamples, float radius, boolean reverse) {
        super(name);

//        this.planes = planes;
//        this.radialSamples = radialSamples;
//        this.radius = radius;
        skyboxDome = new Dome("topSkyDome", planes, radialSamples, radius);
//        initialize();
    }   
   
//    public void draw(Renderer r)
//    {
//        System.out.println("SkyDome" + count + ": " + r);
//        System.out.println("SkyDome" + count + ": " + (this.renderer == r));
//        super.draw(r);
//    }
    
    public int getType() {
    return (Spatial.NODE | Spatial.SKY_BOX);
    }

    /**
     * Set the texture to be displayed on the given side of the skybox. Replaces
     * any existing texture on that side.
     *
     * @param direction
     *            One of Skybox.NORTH, Skybox.SOUTH, and so on...
     * @param texture
     *            The texture for that side to assume.
     */
    public void setTexture(Texture texture) {
        skyboxDome.clearRenderState(RenderState.RS_TEXTURE);
        setTexture(texture, 0);
    }

    /**
     * Set the texture to be displayed on the given side of the skybox. Only
     * replaces the texture at the index specified by textureUnit.
     *
     * @param direction
     *            One of Skybox.NORTH, Skybox.SOUTH, and so on...
     * @param texture
     *            The texture for that side to assume.
     * @param textureUnit
     *            The texture unite of the given side's TextureState the texture
     *            will assume.
     */
    public void setTexture(Texture texture, int textureUnit) {
        TextureState ts = (TextureState) skyboxDome.getRenderState(RenderState.RS_TEXTURE);
        if (ts == null) {
            ts = renderer.createTextureState();
            
            System.out.println("skydome " + count + ": " + ts);
        }

        // Initialize the texture state
        ts.setTexture(texture, textureUnit);
        ts.setEnabled(true);

        // Set the texture to the quad
        skyboxDome.setRenderState(ts);

        return;
    }

    public void initialize() {
        // Create Dome
        skyboxDome.setLocalRotation(new Quaternion(new float[] { 0,
                (float) Math.toRadians(180), 0 }));

        // We don't want the light to effect our skybox
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

        // We don't want it making our skybox disapear, so force view
        setCullMode(Spatial.CULL_NEVER);

        // Make sure texture is only what is set.
        skyboxDome.setTextureCombineMode(TextureState.REPLACE);

        // Make sure no lighting on the skybox
        skyboxDome.setLightCombineMode(LightState.REPLACE);

        // Make sure the quad is viewable
        skyboxDome.setCullMode(Spatial.CULL_NEVER);

        // Set a bounding volume
        skyboxDome.setModelBound(new BoundingBox());
        skyboxDome.updateModelBound();

        skyboxDome.setRenderQueueMode(Renderer.QUEUE_SKIP);
        skyboxDome.setVBOInfo(null);

        // And attach the skybox as a child
        attachChild(skyboxDome);
    }

    /**
     * Force all of the textures to load. This prevents pauses later during the
     * application as you pan around the world.
     */
    public void preloadTextures()
    {
        TextureState ts = (TextureState) skyboxDome
                .getRenderState(RenderState.RS_TEXTURE);
        if (ts != null)
            System.out.println("apply");
            ts.apply();
    }
}
