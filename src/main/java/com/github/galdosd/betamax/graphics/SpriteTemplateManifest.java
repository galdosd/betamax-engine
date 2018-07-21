package com.github.galdosd.betamax.graphics;

import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.sound.SoundName;
import lombok.Value;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

/** The indirection between SpriteTemplateManifest/SpriteTemplate is here so we can find out all the constituent
 * files early before doing the expensive work of loading the textures, so we can use the data for other purposes,
 * like resolving named moments
 */
@Value public class SpriteTemplateManifest {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    private static final Pattern TIF_PATTERN = Pattern.compile("^.*\\.tif$");
    private static final Pattern OGG_PATTERN = Pattern.compile("^.*\\.ogg$");
    private static final Pattern MOMENT_PATTERN = Pattern.compile("^.*/([^/]+)\\.tif$");
    private static final Pattern MOMENT_TAG_PATTERN = Pattern.compile("^.*\\[([^\\[\\]]+)\\]$");
    String templateName;
    List<String> spriteFilenames;
    Optional<SoundName> soundName;

    public static SpriteTemplateManifest load (String templateName) {
        String pkgName = Global.spriteBase + templateName;

        Reflections reflections = new Reflections(pkgName + ".", new ResourcesScanner());
        List<String> spriteFilenames = reflections.getResources(TIF_PATTERN)
                .stream().sorted().collect(toList());
        List<String> soundFilenames = reflections.getResources(OGG_PATTERN)
                .stream().sorted().collect(toList());
        checkArgument(soundFilenames.size() <= 1, "Too many OGG files for sprite template %s", templateName);
        checkArgument(0 != spriteFilenames.size(), "no sprite frame files found for " + pkgName);
        Optional<SoundName> soundName;
        if (soundFilenames.size() > 0) {
            soundName = Optional.of(new SoundName(soundFilenames.get(0)));
        } else {
            soundName = Optional.empty();
        }
        return new SpriteTemplateManifest(templateName, spriteFilenames, soundName);

    }

    public List<String> getMomentNames() {
        return spriteFilenames.stream().map(filename -> {
            Matcher frameNameMatcher = MOMENT_PATTERN.matcher(filename);
            checkState(frameNameMatcher.matches(), "Malformed sprite frame filemane %s", filename);
            String frameName = frameNameMatcher.group(1);
            checkState(frameName != null && frameName.length() > 0, "Malformed sprite frame filename %s", filename);
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
