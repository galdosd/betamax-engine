package com.github.galdosd.betamax;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.google.common.base.Preconditions.checkState;

/** miscellaneous utilities */
public final class OurTool {
    /** Return full contents of file at _filename_ from built in resources/
     *  We should be certain at compile time this file exists, so IllegalStateException will be thrown
     *  if there is any issue
     */
    public static String loadResource(String filename) {
        try {
            URL resource = OurTool.class.getClassLoader().getResource(filename);
            checkState(null != resource);
            String absoluteFilename = resource.getFile();
            return Files.toString(new File(absoluteFilename), Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
