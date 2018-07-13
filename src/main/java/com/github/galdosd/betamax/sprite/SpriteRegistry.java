package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.FrameClock;
import com.github.galdosd.betamax.graphics.TextureCoordinate;
import com.github.galdosd.betamax.scripting.EventType;
import lombok.Getter;
import lombok.Setter;
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

    // TODO sprite destruction will be O(number of sprites on screen)
    // this will probably never significantly impact performance tho
    // could just store the index anyway
    private final Map<SpriteName,Sprite> registeredSprites = new HashMap<>();
    private final Deque<Sprite> orderedSprites = new LinkedList<>();
    private final Queue<SpriteEvent> enqueuedSpriteEvents = new LinkedList<>();
    public final SpriteTemplateRegistry spriteTemplateRegistry;

    // we track the last dispatched moment so that if logic is paused, the same frame can be processed many times
    // but moment events get dispatched just once
    private int lastDispatchedMoment;
    private boolean alreadyBegun = false;
    @Getter @Setter private boolean acceptingCallbacks = false;

    public SpriteRegistry(SpriteTemplateRegistry spriteTemplateRegistry, FrameClock frameClock) {
        this.spriteTemplateRegistry = spriteTemplateRegistry;
        this.frameClock = frameClock;
    }

    // this should only be called from script handlers, or else a duplicate moment#0 event may be dispatched on the
    // first loop of a sprite created by the outer program
    // this should also not be called before the BEGIN event is processed or moment events would happen early
    // again, leave that to scripts
    public Sprite createSprite(String templateName, SpriteName spriteName) {
        LOG.debug("Creating {} (from template {})", spriteName, templateName);
        Sprite sprite = spriteTemplateRegistry.getTemplate(templateName).create(spriteName, frameClock);
        addSprite(sprite);
        enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_CREATE, spriteName, 0));
        // dispatchSpriteMomentEvents will only catch sprites that already existed before this frame and the frame
        // will then increment, so without this we'd miss the first sprite moment#0 event
        enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_MOMENT, spriteName, 0));
        return sprite;
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
        LOG.debug("Destroying {}", spriteName);
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

    private void dispatchBeginEvent(LogicHandler logicHandler) {
        if(!alreadyBegun) {
            alreadyBegun = true;
            frameClock.setPaused(true);
            logicHandler.onBegin();
            frameClock.setPaused(false);
        }
    }

    private void dispatchSpriteMomentEvents(LogicHandler logicHandler) {
        if(lastDispatchedMoment == frameClock.getCurrentFrame()) {
            return;
        }
        // we first generate the events then process them, because otherwise
        // if a script creates or destroys a sprite, orderedSprites will be modified
        // while we are iterating over orderedSprites, resulting in a ConcurrentModificationException
        // ...ask me how i know
        List<SpriteEvent> generatedEvents = new ArrayList<>();
        for(Sprite sprite: orderedSprites) {
            SpriteEvent momentEvent = new SpriteEvent(
                    EventType.SPRITE_MOMENT,
                    sprite.getName(),
                    sprite.getRenderedFrame()
            );
            generatedEvents.add(momentEvent);
        }
        for(SpriteEvent event: generatedEvents) {
            logicHandler.onSpriteEvent(event);
        }
        lastDispatchedMoment = frameClock.getCurrentFrame();
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

    public boolean spriteExists(SpriteName spriteName) {
        return registeredSprites.containsKey(spriteName);
    }

    public void loadTemplate(String templateName) {
        spriteTemplateRegistry.getTemplate(templateName);
    }
}
