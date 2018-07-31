package com.github.galdosd.betamax;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import lombok.NonNull;
import org.lwjgl.system.MemoryUtil;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.StandardOpenOption.*;
import static org.lwjgl.opengl.GL11.glGetError;

/** miscellaneous utilities */
public final class OurTool {
    private OurTool(){}

    /** Return full contents of file at _filename_ from built in resources/
     *  We should be certain at compile time this file exists, so IllegalStateException will be thrown
     *  if there is any issue
     */
    public static String loadResource(String filename) {
        try (InputStream in = streamResource(filename)) {
            return CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static File fileResource(String filename) throws URISyntaxException {
        return Paths.get(OurTool.class.getResource("/" + filename).toURI()).toFile();

    }
    public static InputStream streamResource(String filename) {
        InputStream resourceAsStream = OurTool.class.getResourceAsStream("/" + filename);
        checkArgument(null!=resourceAsStream, "no such resource found: /" + filename);
        return resourceAsStream;
    }

    public static void checkGlError() {
        // in theory we should not need this because of our GlDebugMessages callbacks, but we still call this once per
        // frame (and at a few points during initialization) just in case
        // also GlDebugMessages is disabled in debug mode, this is not
        int err = glGetError();
        checkState(0 == err, "glGetError == " + err);
    }

    /** Sleep precisely, accounting for Thread.sleep idiosyncracies by using it conservatively and then busy looping
     *  for the last 5 or so ms
     */
    public static boolean sleepUntilPrecisely(long targetTime) {
        // if we are more than 5 ms out, Thread.sleep is good enough
        // only sleep for a third of the time we have left to conservatively account for inaccuracy
        long targetSleep;
        boolean slept = false;
        while((targetSleep = targetTime - System.currentTimeMillis()) > 5) {
            try {
                slept = true;
                Thread.sleep(targetSleep / 3);
            } catch (InterruptedException e) {/* doesn't matter, we'll keep trying */}
        }
        // now that we're pretty close to it, busy loop
        if(System.currentTimeMillis() < targetTime) slept = true;
        while(System.currentTimeMillis() < targetTime);
        return slept;
    }

    public static boolean fromProperty(String propertyName, boolean defaultValue) {
        String property = fromProperty(propertyName, null);
        return null==property ? defaultValue : Boolean.valueOf(property);
    }

    public static int fromProperty(String propertyName, int defaultValue) {
        String property = fromProperty(propertyName, null);
        return null==property ? defaultValue : Integer.valueOf(property);
    }

    public static String fromProperty(String propertyName, String defaultValue) {
        String property = System.getProperty(propertyName);
        return null==property ? defaultValue : property;
    }

    public static String fromProperty(String propertyName) {
        String propertyValue = fromProperty(propertyName, null);
        if(null==propertyValue) {
            throw new IllegalArgumentException(
                    "You forgot to define a property. Add -D"+propertyName+"=... to your java command line.");
        } else {
            return propertyValue;
        }
    }

    public static Optional<FileChannel> readCached(String... key) {
        File file = cachedFilename(key);
        if (!file.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(FileChannel.open(file.toPath(), READ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static FileChannel writeCached(boolean overwrite, String... key) {
        try {
            if(overwrite) return FileChannel.open(cachedFilename(key).toPath(), CREATE, TRUNCATE_EXISTING, WRITE);
            else return FileChannel.open(cachedFilename(key).toPath(), CREATE_NEW, WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File cachedFilename(String[] key) {
       StringBuilder strings = new StringBuilder();
       for(String str: key) {
           strings.append(str);
           strings.append("\n");
       }
       UUID uuid = UUID.nameUUIDFromBytes(strings.toString().getBytes(Charsets.UTF_8));
       return new File(Global.textureCacheDir + uuid.toString() + ".dat");
   }

    /** You must deallocate this manually! */
    public static ByteBuffer readOffHeapBuffer(String fileName) {
        try(InputStream inputStream = streamResource(fileName)){
            byte[] bytes = ByteStreams.toByteArray(inputStream);
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            return byteBuffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void yield() {
        // TODO we basically only use this in places where we should do a more proper notification :/
        try {
            Thread.sleep(4);
        } catch (InterruptedException e) { }
    }

    public static void unimplemented() {
        throw new UnsupportedOperationException("FIX"+"ME: This is not yet implemented. This should never be called "+
                "in release code! Search for all accesses of OurTool#unimplemented before a release.");
    }
}

