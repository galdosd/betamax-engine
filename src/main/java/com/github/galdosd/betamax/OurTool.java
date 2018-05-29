package com.github.galdosd.betamax;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.IllegalFormatCodePointException;

import static com.google.common.base.Preconditions.checkState;

/** miscellaneous utilities */
public final class OurTool {
    /** Return full contents of file at _filename_ from built in resources/
     *  We should be certain at compile time this file exists, so IllegalStateException will be thrown
     *  if there is any issue
     */
    public static String loadResource(String filename) {
        try {
            InputStream in = streamResource(filename);
            return CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static InputStream streamResource(String filename) {
        return OurTool.class.getResourceAsStream("/"+filename);
    }
}

