package com.github.galdosd.betamax.junk;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardOpenOption.*;

/**
 * FIXME: Document this class
 */
public class FileChannelExperiment {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    public static void main(String[] args) throws Exception {
        experiment(256, "/tmp/experiment.dat");
    }

    private static void experiment(int mb, String filename) throws IOException {
        Path path = new File(filename).toPath();
        int length = mb * 1024 * 1024;
/*        ByteBuffer randomData = ByteBuffer.allocate(length); // not actually random, zeros, will it get optimized
        try(FileChannel writeChannel = FileChannel.open(path, TRUNCATE_EXISTING, WRITE, CREATE)) {
            randomData.position(0);
            long before = System.currentTimeMillis();
            writeChannel.write(randomData);
            long after = System.currentTimeMillis();
            LOG.info("Took {} ms to write {} mb", after - before, mb);
        }*/
        ByteBuffer readBack = ByteBuffer.allocate(length);
        try(FileChannel readChannel = FileChannel.open(path, READ)) {
            readBack.position(0);
            long before = System.currentTimeMillis();
            readChannel.read(readBack);
            long after = System.currentTimeMillis();
            LOG.info("Took {} ms to read {} mb", after - before, mb);
        }
    }
}
