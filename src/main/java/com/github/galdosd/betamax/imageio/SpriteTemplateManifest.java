package com.github.galdosd.betamax.imageio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.OurTool;
import com.github.galdosd.betamax.graphics.TextureName;
import com.github.galdosd.betamax.sound.SoundName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import lombok.Value;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/** The indirection between SpriteTemplateManifest/SpriteTemplate is here so we can find out all the constituent
 * files early before doing the expensive work of loading the textures, so we can use the data for other purposes,
 * like resolving named moments
 *
 * Also of course isolate all the noise of dealing with file system paths
 */
@Value public class SpriteTemplateManifest {
    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(new Object(){}.getClass().getEnclosingClass());
    public static final Pattern TIF_PATTERN = Pattern.compile("^.*\\.tif$");
    public static final Pattern TIF_OR_OGG_PATTERN = Pattern.compile("^.*\\.(ogg|tif)$");
    private static final Pattern OGG_PATTERN = Pattern.compile("^.*\\.ogg$");
    private static final Pattern MOMENT_PATTERN = Pattern.compile("^.*/([^/]+)\\.tif$");
    private static final Pattern MOMENT_TAG_PATTERN = Pattern.compile("^.*\\[([^\\[\\]]+)\\]$");
    String templateName;
    List<TextureName> spriteFilenames;
    Optional<SoundName> soundName;

    public static Map<String,SpriteTemplateManifest> preloadEverythingPrecompiled() throws IOException {
        return ManifestsPackage.readFromFile().getManifestsMap();
    }

    private static final class DraftManifest {
        private final List<TextureName> textureNames = new ArrayList<>();
        private final List<SoundName> soundNames = new ArrayList<>();

        public void addAsset(String assetPath) {
            String extension = assetPath.substring(assetPath.length() - 4, assetPath.length()).toLowerCase();
            if(extension.equals(".tif")) {
                textureNames.add(new TextureName(assetPath));
            } else if(extension.equals(".ogg")) {
                soundNames.add(new SoundName(assetPath));
            } else {
                throw new IllegalArgumentException("Unrecognized asset file extension: " + assetPath);
            }
        }
    }

    public static Map<String,SpriteTemplateManifest> preloadEverything() {
        Reflections reflections = new Reflections(Global.spritePackageBase, new ResourcesScanner());
        Map<String,DraftManifest> drafts = new HashMap<>();
        for(String assetPath: reflections.getResources(TIF_OR_OGG_PATTERN)) {
            String templateName = findTemplateName(assetPath);

            DraftManifest draft = drafts.get(templateName);
            if(null==draft) {
                drafts.put(templateName, draft = new DraftManifest());
            }
            draft.addAsset(assetPath);

        }
        Set<String> templateNames = drafts.keySet();
        LOG.info("Preloading {} sprite template manifests", templateNames.size());
        return drafts.entrySet().stream().collect(toMap(
                entry -> entry.getKey(),
                entry -> new SpriteTemplateManifest(
                        entry.getKey(), entry.getValue().textureNames, entry.getValue().soundNames)
        ));
    }

    private static String findTemplateName(String assetPath) {
        checkArgument(assetPath.startsWith(Global.spritePathBase), "bad TIF/OGG path %s", assetPath);
        String relativeTifPath = assetPath.substring(Global.spritePathBase.length());
        int pathSeperatorIndex = relativeTifPath.indexOf("/");
        checkArgument(pathSeperatorIndex>0, "bad TIF/OGG path %s", assetPath);
        String templateName = relativeTifPath.substring(0,pathSeperatorIndex);
        String fileName = relativeTifPath.substring(pathSeperatorIndex+1);
        checkArgument(fileName.length()>0 && !fileName.contains("/"), "bad TIF/OGG path %s", assetPath);
        return templateName;
    }

    public static SpriteTemplateManifest load (String templateName) {
        String pkgName = Global.spritePackageBase + templateName;
        //using a new Reflections instance for every template is slow as hell on windows
        // however this load method will only be called when new sprites are added due to hotloading
        // so never will happen in prod, and even in development it's just one here one there. no perf issue
        Reflections reflections = new Reflections(pkgName + ".", new ResourcesScanner());
        List<TextureName> spriteFilenames = reflections.getResources(TIF_PATTERN)
                .stream().sorted().map(TextureName::new).collect(toList());
        List<SoundName> soundFilenames = reflections.getResources(OGG_PATTERN)
                .stream().sorted().map(SoundName::new).collect(toList());
        return new SpriteTemplateManifest(templateName, spriteFilenames, soundFilenames);
    }

    /** just for fucking jackson don't use it yourself */
    @JsonCreator
    private SpriteTemplateManifest(
            @JsonProperty("templateName") String templateName,
            @JsonProperty("spriteFilenames") List<TextureName> spriteFilenames,
            @JsonProperty("soundName") Optional<SoundName> soundName) {
        this.templateName = templateName;
        this.spriteFilenames = spriteFilenames;
        this.soundName = soundName;
    }

    private SpriteTemplateManifest(
            String templateName, List<TextureName> spriteFilenames, List<SoundName> soundFilenames) {
        checkArgument(soundFilenames.size() <= 1, "Too many OGG files for sprite template %s", templateName);
        checkArgument(0 != spriteFilenames.size(), "no sprite frame files found for %s", templateName);
        if (soundFilenames.size() > 0 && Global.enableSound) {
            soundName = Optional.of(soundFilenames.get(0));
        } else {
            soundName = Optional.empty();
        }
        this.templateName = templateName;
        this.spriteFilenames = ImmutableList.copyOf(
                spriteFilenames.stream()
                .sorted(Ordering.natural().onResultOf(TextureName::getFilename))
                .collect(toList())
        );
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
