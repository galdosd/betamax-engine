package com.github.galdosd.betamax.imageio;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.graphics.TextureName;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * FIXME: Document this class
 */
public final class TextureCompiler {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());

    public static void main(String[] args) throws IOException {
        LOG.info("Collecting filenames");
        Reflections reflections = new Reflections(Global.spriteBase, new ResourcesScanner());
        List<TextureName> textureNames = reflections.getResources(SpriteTemplateManifest.TIF_PATTERN)
                .stream().sorted().map(TextureName::new).collect(toList());
        LOG.info("Found {} textures on disk", textureNames.size());
        int jj = 0;
        for(TextureName textureName: textureNames) {
            LOG.info("Compiling texture {} of {}", ++jj, textureNames.size());
            TextureImage textureImage = TextureImagesIO.fromRgbaFile(textureName.getFilename());
            TextureImagesIO.saveToCache(true, textureImage);
        }
    }
}
