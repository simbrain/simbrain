package org.simbrain.util.piccolo;

import org.simbrain.world.odorworld.entities.RotatingEntityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class RotatingSprite extends Sprite {

    private ArrayList<Animation> animations = new ArrayList<>();

    public RotatingSprite(ArrayList<Animation> animations) {
        super(animations.get(0), new HashSet<>(animations));
        this.animations = animations;
    }

    public RotatingSprite(SingleFrameAnimation animation) {
        super(animation);
        this.animations.add(animation);
    }

    public void updateHeading(double degree) {
        setCurrentAnimation(RotatingEntityManager.getAnimationByHeading(animations, degree));
    }
}
