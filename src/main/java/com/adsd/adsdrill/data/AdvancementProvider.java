package com.adsd.adsdrill.data;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.adsd.adsdrill.AdsDrillAddon;
import com.google.gson.JsonElement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class AdvancementProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PackOutput.PathProvider advancementsPathProvider;
    private final Path sourceFolder;

    public AdvancementProvider(PackOutput packOutput) {
        this.advancementsPathProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");

        this.sourceFolder = packOutput.getOutputFolder().getParent().getParent().getParent().resolve("src/main/datagen_resources/data");
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cachedOutput) {
        Path sourceAdvancementsDir = this.sourceFolder.resolve(AdsDrillAddon.MOD_ID).resolve("advancement");

        if (!Files.exists(sourceAdvancementsDir)) {
            AdsDrillAddon.LOGGER.warn("Advancement source directory not found, skipping: {}", sourceAdvancementsDir);
            return CompletableFuture.completedFuture(null);
        }

        try (Stream<Path> stream = Files.walk(sourceAdvancementsDir)) {
            return CompletableFuture.allOf(stream
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(sourcePath -> {
                        Path relativePath = sourceAdvancementsDir.relativize(sourcePath);

                        String normalizedPath = relativePath.toString().replace('\\', '/');

                        // .json 확장자를 제거하고 ResourceLocation을 생성합니다.
                        Path targetPath = this.advancementsPathProvider.json(ResourceLocation.fromNamespaceAndPath(AdsDrillAddon.MOD_ID, normalizedPath.replace(".json", "")));

                        try {
                            String content = Files.readString(sourcePath);
                            JsonElement jsonElement = GSON.fromJson(content, JsonElement.class);
                            return DataProvider.saveStable(cachedOutput, jsonElement, targetPath);
                        } catch (IOException e) {
                            AdsDrillAddon.LOGGER.error("Failed to copy advancement file: {}", sourcePath, e);
                            return CompletableFuture.completedFuture(null);
                        }
                    }).toArray(CompletableFuture[]::new));
        } catch (IOException e) {
            AdsDrillAddon.LOGGER.error("Failed to walk advancements directory: {}", sourceAdvancementsDir, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull String getName() {
        return "AdsDrill JSON Advancements";
    }
}