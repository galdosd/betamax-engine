package com.github.galdosd.betamax.imageio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.galdosd.betamax.Global;
import lombok.Value;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * FIXME: Document this class
 */
@Value public class ManifestsPackage {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static { OBJECT_MAPPER.registerModule(new Jdk8Module()); }

    Map<String, SpriteTemplateManifest> manifestsMap;

    static ManifestsPackage compile() {
        return new ManifestsPackage(SpriteTemplateManifest.preloadEverything());
    }

    File writeToFile() throws IOException {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        File mappedFile = Global.manifestsPackageFilename;
        OBJECT_MAPPER.writeValue(mappedFile, this);
        return mappedFile;
    }

    static ManifestsPackage readFromFile() throws IOException {
        return OBJECT_MAPPER.readValue(Global.manifestsPackageFilename, ManifestsPackage.class );
    }
}
