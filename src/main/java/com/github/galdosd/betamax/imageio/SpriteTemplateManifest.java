package com.github.galdosd.betamax.imageio;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.sound.SoundName;
import lombok.Value;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/** The indirection between SpriteTemplateManifest/SpriteTemplate is here so we can find out all the constituent
 * files early before doing the expensive work of loading the textures, so we can use the data for other purposes,
 * like resolving named moments
 */
@Value public class SpriteTemplateManifest {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    public static final Pattern TIF_PATTERN = Pattern.compile("^.*\\.tif$");
    private static final Pattern OGG_PATTERN = Pattern.compile("^.*\\.ogg$");
    private static final Pattern MOMENT_PATTERN = Pattern.compile("^.*/([^/]+)\\.tif$");
    private static final Pattern MOMENT_TAG_PATTERN = Pattern.compile("^.*\\[([^\\[\\]]+)\\]$");
    String templateName;
    List<TextureName> spriteFilenames;
    Optional<SoundName> soundName;


    public static Map<String,SpriteTemplateManifest> preloadEverything() {
        Reflections reflections = new Reflections(Global.spriteBase, new ResourcesScanner());
        Set<String> templateNames = reflections.getResources(TIF_PATTERN).stream()
                .map(tifFilename -> {
                    String directory = tifFilename.substring(0, tifFilename.lastIndexOf("/"));
                    return directory.substring(1+directory.lastIndexOf("/"));
                }).collect(toSet());
        LOG.info("Preloading {} sprite template manifests", templateNames.size());
        Map<String,SpriteTemplateManifest> allTemplates = new HashMap<>();
        for(String templateName: templateNames) {
            allTemplates.put(templateName, load(templateName));
        }
        return allTemplates;
    }

    public static SpriteTemplateManifest load (String templateName) {
        String pkgName = Global.spriteBase + templateName;

        Reflections reflections = new Reflections(pkgName + ".", new ResourcesScanner());
        List<TextureName> spriteFilenames = reflections.getResources(TIF_PATTERN)
                .stream().sorted().map(TextureName::new).collect(toList());
        List<String> soundFilenames = reflections.getResources(OGG_PATTERN)
                .stream().sorted().collect(toList());
        checkArgument(soundFilenames.size() <= 1, "Too many OGG files for sprite template %s", templateName);
        checkArgument(0 != spriteFilenames.size(), "no sprite frame files found for " + pkgName);
        Optional<SoundName> soundName;
        if (soundFilenames.size() > 0 && Global.enableSound) {
            soundName = Optional.of(new SoundName(soundFilenames.get(0)));
        } else {
            soundName = Optional.empty();
        }
        return new SpriteTemplateManifest(templateName, spriteFilenames, soundName);
    }

    public List<String> getMomentNames() {
        return spriteFilenames.stream().map(textureName -> {
            Matcher frameNameMatcher = MOMENT_PATTERN.matcher(textureName.getFilename());
            checkState(frameNameMatcher.matches(), "Malformed sprite frame filemane %s", textureName);
            String frameName = frameNameMatcher.group(1);
            checkState(frameName != null && frameName.length() > 0, "Malformed sprite frame filename %s", textureName);
            Matcher matcher = MOMENT_TAG_PATTERN.matcher(frameName);
            if(matcher.matches()) {
                return matcher.group(1);
            } else {
                return frameName;
            }
        }).collect(toList());
    }

    public int getMomentByName(String soughtMomentName) {
        int momentId = 0;
        for(String momentName: getMomentNames()) {
            if(momentName.equals(soughtMomentName)) {
                return momentId;
            }
            momentId++;
        }
        throw new IllegalArgumentException("No moment name " +soughtMomentName+ " in sprite template " +templateName);
    }
}
