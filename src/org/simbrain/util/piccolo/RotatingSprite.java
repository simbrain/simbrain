package org.simbrain.util.piccolo;

import org.simbrain.world.odorworld.entities.RotatingEntityManager;

import java.util.ArrayList;
import java.util.HashSet;

public class RotatingSprite extends Sprite {

    private ArrayList<Animation> animations;

    public RotatingSprite(ArrayList<Animation> animations) {
        super(animations.get(0), new HashSet<>(animations));
        this.animations = animations;
    }

    public void updateHeading(double degree) {
        setCurrentAnimation(RotatingEntityManager.getAnimationByHeading(animations, degree));
    }
}
