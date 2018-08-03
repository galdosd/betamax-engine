package com.github.galdosd.betamax.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.galdosd.betamax.Global;
import com.github.galdosd.betamax.opengl.TextureCoordinate;
import com.github.galdosd.betamax.sprite.Sprite;
import com.github.galdosd.betamax.sprite.SpriteName;
import org.kohsuke.randname.RandomNameGenerator;
import lombok.Value;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * FIXME: Document this class
 */
@Value public class GameplaySnapshot {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final RandomNameGenerator NAME_GENERATOR = new RandomNameGenerator();

    String globalShader;
    int currentFrame;
    List<SpriteSnapshot> sprites;
    Map<String,String> scriptVariables;
    Date creationDate = new Date();
    String mnemonicName = NAME_GENERATOR.next();

    @Value public static class SpriteSnapshot {
        String templateName;
        SpriteName name;
        int creationSerial;
        int initialFrame;
        Sprite.Clickability clickability;
        int layer;
        int repetitions;
        boolean pinnedToCursor;
        boolean paused;
        boolean hidden;
        int pausedFrame;
        TextureCoordinate location;
    }

    File writeToFile() throws IOException {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        File mappedFile = getMappedFile(mnemonicName);
        OBJECT_MAPPER.writeValue(mappedFile, this);
        return mappedFile;
    }

    static File getMappedFile(String mnemonicName) {
        return new File(Global.snapshotDir + mnemonicName + ".json");
    }

    static GameplaySnapshot readFromFile(String filename) throws IOException {
        return OBJECT_MAPPER.readValue(new File(filename), GameplaySnapshot.class );
    }
}
