package org.a.imagoCore.resource.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.a.imagoCore.config.CharEntry;
import org.a.imagoCore.config.CharRegistry;
import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.config.GuiRegistry;
import org.a.imagoCore.config.ShiftRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the {@code assets/minecraft/font/default.json} content for the
 * ImagoCore resource pack.
 *
 * <p>The generated JSON includes:
 * <ul>
 *   <li>Shift characters (coarse -16px, fine -8px)</li>
 *   <li>All registered GUI background textures from {@link GuiRegistry}</li>
 *   <li>All registered character images from {@link CharRegistry}</li>
 * </ul>
 */
public class GuiFontDefinition {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<FontProvider> providers = new ArrayList<>();

    private GuiFontDefinition() {
    }

    /**
     * Creates a definition with shift characters and all registered GUI
     * backgrounds from the given registry.
     */
    public static GuiFontDefinition createFrom(GuiRegistry registry) {
        return createFrom(registry, null);
    }

    /**
     * Creates a definition with shift characters, all registered GUI
     * backgrounds, and all registered character images.
     *
     * @param guiRegistry  GUI background registry (required)
     * @param charRegistry character-image registry (nullable)
     */
    public static GuiFontDefinition createFrom(GuiRegistry guiRegistry,
                                                CharRegistry charRegistry) {
        GuiFontDefinition def = new GuiFontDefinition();

        // Power-of-2 shift characters (8 negative + 8 positive)
        for (Map.Entry<String, Integer> shift : ShiftRegistry.getAllShiftAdvances().entrySet()) {
            def.addProvider(FontProvider.space(shift.getKey(), shift.getValue()).build());
        }

        // All registered GUI backgrounds
        for (GuiEntry entry : guiRegistry.getEntries()) {
            def.addProvider(FontProvider.bitmap(
                    entry.getTexturePackPath(),
                    entry.getAscent(),
                    entry.getHeight(),
                    entry.getBackgroundChar()
            ).build());
        }

        // All registered character images
        if (charRegistry != null) {
            for (CharEntry entry : charRegistry.getEntries()) {
                def.addProvider(FontProvider.bitmap(
                        entry.getTexturePackPath(),
                        entry.getAscent(),
                        entry.getHeight(),
                        entry.getCharacter()
                ).build());
            }
        }

        return def;
    }

    /** Add an extra font provider entry. */
    public GuiFontDefinition addProvider(FontProvider provider) {
        providers.add(provider);
        return this;
    }

    /** Serialise to pretty-printed JSON. */
    public String toJson() {
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        for (FontProvider p : providers) {
            arr.add(p.toJson());
        }
        root.add("providers", arr);
        return GSON.toJson(root);
    }
}
