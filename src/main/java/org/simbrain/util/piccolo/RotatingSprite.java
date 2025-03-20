package org.simbrain.util.piccolo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RotatingSprite extends Sprite {

    private List<Animation> animations = new ArrayList<>();

    public RotatingSprite(List<Animation> animations) {
        super(animations.get(0), new HashSet<>(animations));
        this.animations = animations;
    }

    public RotatingSprite(SingleFrameAnimation animation) {
        super(animation);
        this.animations.add(animation);
    }

    public void updateHeading(double degree) {
        int degreeApart = 360 / animations.size();
        degree = Math.floor(degree + degreeApart / 2.0) % 360;
        int index = (int)(degree / degreeApart);
        setCurrentAnimation(animations.get(index));
    }
}
