package org.chapzlock.core.component;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.Channel;
import org.chapzlock.core.animation.Interpolation;
import org.chapzlock.core.animation.TargetPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimationStateBlendTest {

    private Animation makeAnim(String name) {
        Channel ch = new Channel(1, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 2, 3});
        return new Animation(name, 1f, List.of(ch));
    }

    @Test
    void playWithBlendDurationStartsCrossfade() {
        Animation walk = makeAnim("Walk");
        Animation run = makeAnim("Run");

        AnimationState state = new AnimationState(walk);
        state.setTime(0.5f);

        state.play(run, 0.3f);

        assertTrue(state.isBlending());
        assertSame(walk, state.getPreviousAnimation());
        assertEquals(0.5f, state.getPreviousTime());
        assertEquals(0f, state.getBlendFactor());
        assertEquals(0.3f, state.getBlendDuration());
        assertSame(run, state.getCurrentAnimation());
        assertEquals(0f, state.getTime());
        assertTrue(state.isPlaying());
    }

    @Test
    void playDefaultBlendDurationIs200ms() {
        Animation walk = makeAnim("Walk");
        Animation run = makeAnim("Run");

        AnimationState state = new AnimationState(walk);
        state.play(run);

        assertTrue(state.isBlending());
        assertEquals(0.2f, state.getBlendDuration(), 0.001f);
    }

    @Test
    void playWithNoPreviousAnimationDoesNotBlend() {
        Animation walk = makeAnim("Walk");

        AnimationState state = new AnimationState(null);
        state.setPlaying(false);

        state.play(walk, 0.2f);

        assertFalse(state.isBlending());
        assertSame(walk, state.getCurrentAnimation());
        assertTrue(state.isPlaying());
    }
}
