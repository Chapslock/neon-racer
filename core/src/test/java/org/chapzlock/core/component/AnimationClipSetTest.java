package org.chapzlock.core.component;

import org.chapzlock.core.animation.Animation;
import org.chapzlock.core.animation.Channel;
import org.chapzlock.core.animation.Interpolation;
import org.chapzlock.core.animation.TargetPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnimationClipSetTest {

    @Test
    void getReturnsClipByName() {
        Channel ch = new Channel(1, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 2, 3});
        Animation walk = new Animation("Walk", 1f, List.of(ch));
        Animation idle = new Animation("Idle", 2f, List.of(ch));

        AnimationClipSet clipSet = new AnimationClipSet(Map.of("Walk", walk, "Idle", idle));

        assertSame(walk, clipSet.get("Walk"));
        assertSame(idle, clipSet.get("Idle"));
    }

    @Test
    void getReturnsNullForUnknownName() {
        AnimationClipSet clipSet = new AnimationClipSet(Map.of());
        assertNull(clipSet.get("Missing"));
    }

    @Test
    void clipNamesReturnsAllKeys() {
        Channel ch = new Channel(1, TargetPath.TRANSLATION, Interpolation.LINEAR,
            new float[]{0f, 1f}, new float[]{0, 0, 0, 1, 2, 3});
        Animation walk = new Animation("Walk", 1f, List.of(ch));

        AnimationClipSet clipSet = new AnimationClipSet(Map.of("Walk", walk));
        assertTrue(clipSet.clipNames().contains("Walk"));
        assertEquals(1, clipSet.size());
    }
}
