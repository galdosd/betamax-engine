package com.github.galdosd.betamax;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import lombok.NonNull;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
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

    public static void sleepUntilPrecisely(long targetTime) {
        // if we are more than 5 ms out, Thread.sleep is good enough
        // only sleep for a third of the time we have left to conservatively account for inaccuracy
        while(targetTime - System.currentTimeMillis() > 5) {
            try {
                Thread.sleep((targetTime - System.currentTimeMillis()) / 3);
            } catch (InterruptedException e) {/* doesn't matter, we'll keep trying */}
        }
        // now that we're pretty close to it, busy loop
        while(System.currentTimeMillis() < targetTime);
    }

    public static String fromProperty(String propertyName, String defaultValue) {
        String property = System.getProperty(propertyName);
        if(null!=property) {
            return property;
        } else {
          return defaultValue;
        }
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


    public static FileChannel writeCached(String... key) {
        try {
            return FileChannel.open(cachedFilename(key).toPath(), CREATE_NEW, WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static File cachedFilename(String[] key) {
       StringBuilder strings = new StringBuilder();
       for(String str: key) {
           strings.append(str);
           strings.append("\n");
       }
       UUID uuid = UUID.nameUUIDFromBytes(strings.toString().getBytes(Charsets.UTF_8));
       return new File(Global.textureCacheDir + uuid.toString() + ".dat");
   }

}

