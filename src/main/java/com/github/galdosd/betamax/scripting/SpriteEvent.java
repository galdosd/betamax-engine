package com.github.galdosd.betamax.scripting;

import com.github.galdosd.betamax.sprite.SpriteName;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * FIXME: Document this class
 */
@ToString
@EqualsAndHashCode
public final class SpriteEvent {
    public final EventType eventType;
    public final SpriteName spriteName;
    /**
     * ignored and must be 0 except for eventType==SPRITE_MOMENT
     */
    public final int moment;

    public SpriteEvent(@NonNull EventType eventType, @NonNull SpriteName spriteName, int moment) {
        checkArgument(eventType == EventType.SPRITE_MOMENT || moment == 0, "moment may only be set for EventType.SPRITE_MOMENT");
        checkArgument(spriteName != null || eventType == EventType.BEGIN, "sprite must be set for sprite events");
        checkArgument(spriteName == null || eventType != EventType.BEGIN, "sprite may not be set for BEGIN event");

        this.eventType = eventType;
        this.spriteName = spriteName;
        this.moment = moment;
    }
}
