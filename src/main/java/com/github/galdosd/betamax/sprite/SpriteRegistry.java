package com.github.galdosd.betamax.sprite;

import com.github.galdosd.betamax.engine.FrameClock;
import com.github.galdosd.betamax.graphics.SpriteTemplateRegistry;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.scripting.EventType;
import com.google.common.collect.Ordering;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public class SpriteRegistry implements AutoCloseable {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    private final FrameClock frameClock;

    private final Map<SpriteName,Sprite> registeredSprites = new HashMap<>();
    private final Queue<SpriteEvent> enqueuedSpriteEvents = new LinkedList<>();

    private static final Ordering<Sprite> CREATION_ORDERING = Ordering.natural().onResultOf(Sprite::getCreationSerial);
    private static final Ordering<Sprite> LAYER_ORDERING = Ordering.natural().onResultOf(Sprite::getLayer);
    private static final Ordering<Sprite> RENDER_ORDERING = LAYER_ORDERING.compound(CREATION_ORDERING);
    private final Ordering<SpriteName> NAME_RENDER_ORDERING = RENDER_ORDERING.onResultOf(this::getSpriteByName);

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
    }

    public Sprite getSpriteByName(SpriteName spriteName) {
        Sprite sprite = registeredSprites.get(spriteName);
        checkArgument(null!=sprite, "No such sprite: " + spriteName);
        return sprite;
    }

    public void destroySprite(SpriteName spriteName) {
        LOG.debug("Destroying {}", spriteName);
        Sprite sprite = registeredSprites.get(spriteName);
        checkState(sprite!=null, "no such sprite: " + spriteName);
        sprite.close();
        registeredSprites.remove(spriteName);
        enqueueSpriteEvent(new SpriteEvent(EventType.SPRITE_DESTROY, spriteName, 0));
    }

    public List<Sprite> getSpritesInRenderOrder() {
        return registeredSprites.values().stream().sorted(RENDER_ORDERING).collect(toList());
    }

    public List<Sprite> getSpritesInReverseRenderOrder() {
        return registeredSprites.values().stream().sorted(RENDER_ORDERING.reverse()).collect(toList());
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
            logicHandler.onBegin();
            frameClock.resetLogicFrames();
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
        for(Sprite sprite: getSpritesInRenderOrder()) {
            SpriteEvent momentEvent = new SpriteEvent(
                    EventType.SPRITE_MOMENT,
                    sprite.getName(),
                    sprite.getCurrentFrame()
            );
            generatedEvents.add(momentEvent);
        }
        for(SpriteEvent event: generatedEvents) {
            dispatchSingleSpriteEvent(logicHandler, event);
        }
        lastDispatchedMoment = frameClock.getCurrentFrame();
    }

    private void dispatchEnqueuedSpriteEvents(LogicHandler logicHandler) {
        SpriteEvent event;
        while(null != (event = enqueuedSpriteEvents.poll())) {
            dispatchSingleSpriteEvent(logicHandler, event);
        }
    }

    private void dispatchSingleSpriteEvent(LogicHandler logicHandler, SpriteEvent event) {
        if(spriteExists(event.spriteName) || event.eventType == EventType.SPRITE_DESTROY) {
            logicHandler.onSpriteEvent(event);
        }
    }

    public Optional<SpriteName> getSpriteAtCoordinate(TextureCoordinate coordinate) {
        // look through sprites in reverse draw order, ie from front to back
        for(Sprite sprite: getSpritesInReverseRenderOrder()) {
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

    public void close() {
        registeredSprites.values().stream().forEach(Sprite::close);
    }
}
