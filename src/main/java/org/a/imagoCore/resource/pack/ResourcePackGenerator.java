package org.a.imagoCore.resource.pack;

import org.a.imagoCore.ImagoCore;
import org.a.imagoCore.config.CharEntry;
import org.a.imagoCore.config.CharRegistry;
import org.a.imagoCore.config.GuiEntry;
import org.a.imagoCore.config.GuiRegistry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates a Minecraft resource pack ({@code build.zip}) from the
 * embedded template, {@link GuiRegistry}-driven GUI entries, and
 * {@link CharRegistry}-driven character images.
 *
 * <p>Each registered entry gets:
 * <ol>
 *   <li>A font provider entry in {@code font/default.json}</li>
 *   <li>Its texture copied into the zip</li>
 * </ol>
 */
public class ResourcePackGenerator {

    private static final String TEMPLATE_ROOT = "/resource-pack-template/";

    private final ImagoCore plugin;
    private final File outputFile;
    private final GuiRegistry guiRegistry;
    private final CharRegistry charRegistry;

    /**
     * @param plugin      plugin instance
     * @param outputFile  the target zip file
     * @param guiRegistry GUI registry with all entries to include
     */
    public ResourcePackGenerator(ImagoCore plugin, File outputFile,
                                 GuiRegistry guiRegistry) {
        this(plugin, outputFile, guiRegistry, null);
    }

    /**
     * @param plugin       plugin instance
     * @param outputFile   the target zip file
     * @param guiRegistry  GUI registry with all entries to include
     * @param charRegistry character-image registry (nullable)
     */
    public ResourcePackGenerator(ImagoCore plugin, File outputFile,
                                 GuiRegistry guiRegistry,
                                 CharRegistry charRegistry) {
        this.plugin = plugin;
        this.outputFile = outputFile;
        this.guiRegistry = guiRegistry;
        this.charRegistry = charRegistry;
    }

    /**
     * Builds the resource pack zip.
     *
     * @throws IOException if file I/O fails
     */
    public void build() throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            Files.createDirectories(parent.toPath());
        }

        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(outputFile))) {

            // 1. Static template files
            copyEntry(zos, "pack.mcmeta");
            copyEntry(zos, "pack.png");

            // 2. Font definition (shift chars + all GUIs + all chars)
            GuiFontDefinition fontDef = GuiFontDefinition.createFrom(guiRegistry, charRegistry);
            zos.putNextEntry(new ZipEntry("assets/minecraft/font/default.json"));
            zos.write(fontDef.toJson().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. GUI background textures from the registry
            for (GuiEntry entry : guiRegistry.getEntries()) {
                File texFile = entry.getTextureFile();
                if (!texFile.exists()) {
                    plugin.getLogger().warning("Texture not found: " + texFile);
                    continue;
                }
                zos.putNextEntry(new ZipEntry(entry.getZipEntryPath()));
                Files.copy(texFile.toPath(), zos);
                zos.closeEntry();
            }

            // 4. Character-image textures (base + variants, deduplicated)
            if (charRegistry != null) {
                Set<String> writtenPaths = new java.util.HashSet<>();
                for (CharEntry entry : charRegistry.getAllEntries()) {
                    File texFile = entry.getTextureFile();
                    if (!texFile.exists()) {
                        plugin.getLogger().warning("Char texture not found: " + texFile);
                        continue;
                    }
                    String zipPath = entry.getZipEntryPath();
                    if (!writtenPaths.add(zipPath)) continue; // variant shares texture
                    zos.putNextEntry(new ZipEntry(zipPath));
                    Files.copy(texFile.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }

        plugin.getLogger().info("Resource pack built: " + outputFile.getAbsolutePath());
    }

    private void copyEntry(ZipOutputStream zos, String path) throws IOException {
        String resourcePath = TEMPLATE_ROOT + path.replace('\\', '/');
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warning("Template resource not found: " + resourcePath);
                return;
            }
            zos.putNextEntry(new ZipEntry(path));
            in.transferTo(zos);
            zos.closeEntry();
        }
    }
}
