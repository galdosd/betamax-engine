package com.github.galdosd.betamax.imageio;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.graphics.TextureName;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public final class TextureCompiler {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    public static void main(String[] args) throws IOException, URISyntaxException {
        LOG.info("Collecting filenames");
        Reflections reflections = new Reflections(Global.spritePackageBase, new ResourcesScanner());
        List<TextureName> textureNames = reflections.getResources(SpriteTemplateManifest.TIF_PATTERN)
                .stream().sorted().map(TextureName::new).collect(toList());
        List<TextureName> necessaryTextureNames = textureNames.stream()
                .filter(name -> !isUpToDate(name)).collect(toList());
        LOG.info("Found {} textures on disk ({} need to be recompiled)",
                textureNames.size(), necessaryTextureNames.size());
        int jj = 0;
        long startTime = System.currentTimeMillis();
        for(TextureName textureName: necessaryTextureNames) {
            long now = System.currentTimeMillis();
            long eta = necessaryTextureNames.size() * (now - startTime) / (jj + 1) - now + startTime;

            LOG.info("Compiling texture {} of {} ({}%, ETA {} seconds)",
                    ++jj, necessaryTextureNames.size(), 100*jj/necessaryTextureNames.size(), eta / 1000);
            TextureImage textureImage = TextureImagesIO.fromRgbaFile(textureName.getFilename());
            TextureImagesIO.saveToCache(true, textureImage);
            textureImage.close();
        }
        LOG.info("Successfuly recompiled {} textures", necessaryTextureNames.size());
    }

    private static boolean isUpToDate(TextureName textureName) {
        File inputFile = null;
        try {
            inputFile = OurTool.fileResource(textureName.getFilename());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        File outputFile = TextureImagesIO.cachedFilename(textureName);
        if(!outputFile.exists()) return false;
        LOG.trace("Input {} modified {}", inputFile, inputFile.lastModified());
        LOG.trace("Output {} modified {}", outputFile, outputFile.lastModified());
        return inputFile.lastModified() < outputFile.lastModified();
    }
}
