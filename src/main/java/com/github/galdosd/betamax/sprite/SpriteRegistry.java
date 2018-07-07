package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
import com.github.galdosd.betamax.scripting.EventType;
import com.github.galdosd.betamax.scripting.LogicHandler;
import lombok.Getter;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * FIXME: Document this class
 */
public class SpriteRegistry {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final FrameClock frameClock;
    private final Map<String,SpriteTemplate> registeredTemplates = new HashMap<>();

    // TODO sprite destruction will be O(number of sprites on screen)
    // this will probably never significantly impact performance tho
    // could just store the index anyway
    private final Map<SpriteName,Sprite> registeredSprites = new HashMap<>();
    private final Deque<Sprite> orderedSprites = new LinkedList<>();
    private final Queue<SpriteEvent> enqueuedSpriteEvents = new LinkedList<>();

    public SpriteRegistry(FrameClock frameClock) {
        this.frameClock = frameClock;
    }

    // TODO it might be nice to have some mechanism by which templates not used for a while are unloaded
    // that said it probably makes sense to have that partially manually controlled (to group templates into
    // dayparts for example) rather than entirely automagical, especially since the performance implications are
    // quite serious if the magic gets it wrong
    // conversely, automatic background loading of things that will be needed in the future might be worthwhile
    public SpriteTemplate getTemplate(String name) {
        SpriteTemplate template = registeredTemplates.get(name);
        if(null==template) {
            template = new SpriteTemplate(Global.spriteBase+name, frameClock);
            registeredTemplates.put(name,template);
        }
        return template;
    }

    // this should only be called from script handlers, or else a duplicate moment#0 event may be dispatched on the
    // first loop of a sprite created by the outer program
    // this should also not be called before the BEGIN event is processed or moment events would happen early
    // again, leave that to scripts
    public void createSprite(String templateName, SpriteName spriteName) {
        addSprite(getTemplate(templateName).create(spriteName));
        enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_CREATE, spriteName, 0));
        // dispatchSpriteMomentEvents will only catch sprites that already existed before this frame and the frame
        // will then increment, so without this we'd miss the first sprite moment#0 event
        enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_MOMENT, spriteName, 0));
    }

    private void addSprite(Sprite sprite) {
        SpriteName name = sprite.getName();
        checkArgument(!registeredSprites.containsKey(name), "duplicate sprite name: " + name);
        registeredSprites.put(name, sprite);
        orderedSprites.add(sprite);
    }

    public Sprite getSpriteByName(SpriteName spriteName) {
        Sprite sprite = registeredSprites.get(spriteName);
        checkArgument(null!=sprite, "No such sprite: " + spriteName);
        return sprite;
    }

    public void destroySprite(SpriteName spriteName) {
        Sprite sprite = getSpriteByName(spriteName);
        registeredSprites.remove(spriteName);
        boolean wasRemovedFromList = orderedSprites.remove(sprite);
        checkState(wasRemovedFromList);
        enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_DESTROY, spriteName, 0));
    }

    public void renderAll() {
        for(Sprite sprite: orderedSprites) {
            sprite.render();
        }
    }

    public void dispatchEvents(LogicHandler logicHandler) {
        // TODO I'm not sure the choreography is consistent yet of making sure you get events
        // in a well defined order, which I care about because of rewing/replay, particularly the first moment#0 event
        dispatchSpriteMomentEvents(logicHandler);
        dispatchBeginEvent(logicHandler);
        for(int jj = 0; !enqueuedSpriteEvents.isEmpty(); jj++) {
            dispatchEnqueuedSpriteEvents(logicHandler);
            // a stupid jython script could cause us to recurse forever, eg, by creating a sprite from within its
            // own destruction handler and then destroying it from within its creation handler, so we try to detect it
            if(jj > 1000) { // if 1000 is good enough for java's recursion limit I guess it's good enough for us
                throw new IllegalStateException("Event handling recursion limit exceeded; your jython script is buggy");
            }
        }
    }

    @Getter boolean alreadyBegun = false;
    private void dispatchBeginEvent(LogicHandler logicHandler) {
        if(!alreadyBegun) {
            alreadyBegun = true;
            logicHandler.onBegin();
        }
    }

    private void dispatchSpriteMomentEvents(LogicHandler logicHandler) {
        // TODO negative moments should be implemented, count backwards from end python style
        for(Sprite sprite: orderedSprites) {
            SpriteEvent momentEvent = new SpriteEvent(
                    EventType.SPRITE_MOMENT,
                    sprite.getName(),
                    sprite.getRenderedFrame()
            );
            logicHandler.onSpriteEvent(momentEvent);
        }
    }
    private void dispatchEnqueuedSpriteEvents(LogicHandler logicHandler) {
        SpriteEvent event;
        while(null != (event = enqueuedSpriteEvents.poll())) {
            logicHandler.onSpriteEvent(event);
        }
    }

    public Optional<SpriteName> getSpriteAtCoordinate(TextureCoordinate coordinate) {
        // look through sprites in reverse draw order, ie from front to back
        Iterator<Sprite> spriteIterator = orderedSprites.descendingIterator();
        while(spriteIterator.hasNext()) {
            Sprite sprite = spriteIterator.next();
            if (sprite.isClickableAtCoordinate(coordinate)) {
                return Optional.of(sprite.getName());
            }
        }
        return Optional.empty();

    }

    public void enqueueSpriteEvent(SpriteEvent spriteEvent) {
        enqueuedSpriteEvents.add(spriteEvent);
    }
}
