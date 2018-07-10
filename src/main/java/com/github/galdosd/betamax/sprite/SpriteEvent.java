package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.sprite.SpriteName;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Wither;

import static com.google.common.base.Preconditions.checkArgument;

/** An event such as an input or timing event that pertains to a single specific sprite
 *
 * XXX: not sure if this C union style is best here as opposed to an oop hierarchy, but tbqh i think it's the right
 * way for a thing that has no polymorphic methods applicable anyway that belong here.
 */
@ToString
@EqualsAndHashCode
public final class SpriteEvent {
    public final EventType eventType;
    public final SpriteName spriteName;
    /**
     * ignored and must be 0 except for eventType==SPRITE_MOMENT
     */
    @Wither public final int moment;


    public SpriteEvent(@NonNull EventType eventType, @NonNull SpriteName spriteName, int moment) {
        checkArgument(eventType == EventType.SPRITE_MOMENT || moment == 0, "moment may only be set for EventType.SPRITE_MOMENT");
        checkArgument(spriteName != null || eventType == EventType.BEGIN, "sprite must be set for sprite events");
        checkArgument(spriteName == null || eventType != EventType.BEGIN, "sprite may not be set for BEGIN event");

        this.eventType = eventType;
        this.spriteName = spriteName;
        this.moment = moment;
    }
}
