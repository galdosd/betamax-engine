package com.github.galdosd.betamax;

import com.codahale.metrics.MetricRegistry;
import com.github.galdosd.betamax.graphics.TextureName;

import static com.github.galdosd.betamax.OurTool.fromProperty;

/** It's not what it looks like honey!
 *  In all seriousness, this is pretty much just global constants, no real objects or mutable stuff
 *  Everything that could possibly make sense to override at runtime via system properties can be!
 */
// TODO I know I know. Use DI or something. But only once it really matters. Extra magic ain't free
// in particular, (so far!) I've avoided sticking anything here that would impede easy mocks in unit tests
// that and i've put off making unit tests lol. jeez.
public final class Global {
    public static final MetricRegistry metrics = new MetricRegistry();
    public static final String spritePackageBase = "com.github.galdosd.betamax.sprites.";
    public static final String spritePathBase = "com/github/galdosd/betamax/sprites/";
    public static final String scriptBase = "com/github/galdosd/betamax/scripts/";
    public static final String shaderBase = "com/github/galdosd/betamax/shaders/";
    public static final String helpFile = "help.txt";
    public static final TextureName pausedTextureFile = new TextureName("paused.tif");
    public static final TextureName loadingTextureFile = new TextureName("loading.tif");
    public static final TextureName crashTextureFile = new TextureName("crash.tif");

    /** we try to hew to this framerate and all other timing, eg sound timing, is based on this too, even if we
     *  are temporarily fast forwarding / rewinding
     */
    public static final int targetFps = fromProperty("betamax.targetFps", 8);
    public static final  int devConsoleUpdateIntervalMillis = fromProperty("betamax.devConsoleUpdateInterval", 500);
    public static final boolean startFullScreen = fromProperty("betamax.startFullScreen", false);
    /** Optionally, you can have multiple scripts loaded in order, comma separated.
     *  This is necessary because since we load from resources so jars will work,
     *  we end up breaking normal python import keyword
     */
    public final static String mainScript = fromProperty("betamax.mainScript", null);
    /** Inconvenient name, should be more like compiled textures, not cached textures. Anyway, TextureCompiler
     * needs this property too, and not any others
     */
    public final static String textureCacheDir = fromProperty("betamax.textureCacheDir");
    /** sound is neither loaded nor played if disabled */
    public static final boolean enableSound = fromProperty("betamax.enableSound", true);
    /** Any sprite with less than this many frames will stay in memory forever, other sprites will be evicted as
     *  quickly as we finish rendering them frame by frame
     */
    public static final int textureMaxFramesForResidentMemoryStrategy =
            fromProperty( "betamax.textureMaxFramesForResidentMemoryStrategy", 10);
    /** we try to preload frames for upcoming sprite frames as far as this many frames ahead in the background */
    public static final int texturePreloadFrameLookahead = fromProperty(
            "betamax.texturePreloadFrameLookahead", 16
    );
    /** ugh */
    public static final int texturePreloadBatchSize = fromProperty("betamax.texturePreloadBatchSize", 2);
    /** Which monitor to try to put the dev console on (zero-indexed); we won't fail if there's not enough monitors */
    public static final int devConsoleScreen = fromProperty("betamax.devConsoleScreen", 1);
    /** Wait, eg 50% of a frame length before giving up and using a loading screen if not all textures are loaded into
     * RAM needed for rendering that frame yet
     */
    public static int textureLoadGracePeriodFramePercent = fromProperty("betamax.textureLoadGracePeriodFramePercent", 50);
    /** Log opengl debug messages, show the dev console, and uh maybe other stuff */
    public static final boolean debugMode = fromProperty("betamax.debugMode", true);
    /** you probably want to disable this if you use a custom cursor sprite (Sprite#setPinnedToCursor) */
    public static final boolean showSystemCursor = fromProperty("betamax.showSystemCursor", true);
    /** How much we let audio and video fall out of sync.
     *   See https://en.wikipedia.org/wiki/Audio-to-video_synchronization for thoughts on determining this value
     *   FIXME: We should consider exactly where in the frame pipeline we set this, because ultimately the sync
     *   point should be when glfwSwapBuffers is called. In fact we should measure that with a timer (the average
     *   time between frameclock incrementing and the next glfwSwapBuffers call) and skew this value by that much,
     *   and do the drift correction at the frameclock increment point. We don't do much of that right now (well maybe
     *   just the measurement) but hopefully we'll still keep total drift under control to prevent perceptible lip flap
     *
     *   On my machine 10ms is median (20ms is 95pth percentile) for video frame drift, and the above wikipedia article
     *   shows professional film groups recommending around 45ms maximum, so 45 - 10 = 35ms seems like a reasonable
     *   maximum value for us that hopefully won't cause too much resyncing.
     *
     *   We're still not factoring in the actual double buffering / V sync / monitor refresh time though. I think it's
     *   a wash for us though
     */
    public static final int maxSoundDriftMillis = fromProperty("betamax.maxSoundDriftInMs", 35);
    /** Once we detect there's sound/video drift, how long will we wait to see if it um, goes away on its own?
     *  This is necessary because if there's ever a laggy video/logic frame which there may be here and then, it'll
     *  show as drift, but it will self correct right away. We don't want to thrash back and forth!
     */
    public static final int driftTolerancePeriodFrames = fromProperty("betamax.driftTolerancePeriodFrames", 6);
}
