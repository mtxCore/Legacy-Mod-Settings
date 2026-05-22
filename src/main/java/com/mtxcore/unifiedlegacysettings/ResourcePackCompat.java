package com.mtxcore.unifiedlegacysettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

final class ResourcePackCompat {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().create();

  private ResourcePackCompat() {}

  static boolean applySilently(String packId, boolean enable) {
    return apply(packId, enable);
  }

  private static boolean apply(String packId, boolean enable) {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.options == null)
      return false;

    String normalized = packId == null ? "" : packId.trim();
    if (normalized.isEmpty())
      return false;

    Options options = mc.options;
    List<String> selected = options.resourcePacks;
    List<String> incompatible = options.incompatibleResourcePacks;
    boolean wasEnabled = isPackEnabled(options, normalized);

    if (enable) {
      if (!selected.contains(normalized))
        selected.add(normalized);
      incompatible.remove(normalized);
    } else {
      selected.remove(normalized);
      incompatible.remove(normalized);
    }

    boolean packStateChanged = wasEnabled != enable;
    boolean legacyChanged = updateLegacyPackConfigs(normalized, enable);
    if (packStateChanged || legacyChanged) {
      options.save();
      mc.reloadResourcePacks();
    }
    return packStateChanged || legacyChanged;
  }

  private static boolean isPackEnabled(Options options, String packId) {
    return options.resourcePacks.contains(packId) ||
        options.incompatibleResourcePacks.contains(packId);
  }

  private static boolean updateLegacyPackConfigs(String packId,
                                                 boolean enable) {
    Path gameDir = FabricLoader.getInstance().getGameDir();

    Path globalPacks = gameDir.resolve("config/legacy/global_packs.json");
    boolean globalChanged = updateJsonPackArrays(globalPacks, packId, enable);

    boolean albumChanged = false;
    Path albumsIndex = gameDir.resolve("resource_albums.json");
    String defaultAlbum = readDefaultAlbum(albumsIndex);
    if (defaultAlbum != null && !defaultAlbum.isBlank()) {
      Path albumDir = gameDir.resolve("resource_albums");
      Path albumFileRaw = albumDir.resolve(defaultAlbum);
      Path albumFileJson = albumDir.resolve(defaultAlbum + ".json");
      albumChanged = updateJsonPackArrays(albumFileRaw, packId, enable) |
                     updateJsonPackArrays(albumFileJson, packId, enable);
    }

    return globalChanged || albumChanged;
  }

  private static String readDefaultAlbum(Path albumsIndex) {
    if (!Files.exists(albumsIndex))
      return null;
    try {
      JsonElement root = JsonParser.parseString(Files.readString(albumsIndex));
      if (!(root instanceof JsonObject obj))
        return null;
      JsonElement def = obj.get("default");
      return def != null && def.isJsonPrimitive() ? def.getAsString() : null;
    } catch (Exception ignored) {
      // Albums are optional Legacy4J state; malformed JSON should not block the
      // global resource-pack toggle.
      return null;
    }
  }

  private static boolean updateJsonPackArrays(Path file, String packId,
                                              boolean enable) {
    if (!Files.exists(file))
      return false;
    try {
      JsonElement root = JsonParser.parseString(Files.readString(file));
      if (!(root instanceof JsonObject obj))
        return false;

      boolean changed = false;
      for (var entry : obj.entrySet()) {
        String key = entry.getKey().toLowerCase(Locale.ROOT);
        if ("order".equals(key))
          continue;

        JsonElement value = entry.getValue();
        if (!(value instanceof JsonArray arr) || !isStringArray(arr))
          continue;

        changed |= toggleInArray(arr, packId, enable);
      }

      if (changed) {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(obj));
      }
      return changed;
    } catch (Exception ignored) {
      // Leave hand-edited or future album files alone if they do not match the
      // current simple array format.
      return false;
    }
  }

  private static boolean isStringArray(JsonArray arr) {
    for (JsonElement e : arr) {
      if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
        return false;
    }
    return true;
  }

  private static boolean toggleInArray(JsonArray arr, String value,
                                       boolean add) {
    boolean contains = false;
    for (JsonElement e : arr) {
      if (value.equals(e.getAsString())) {
        contains = true;
        break;
      }
    }

    if (add) {
      if (contains)
        return false;
      arr.add(value);
      return true;
    }

    if (!contains)
      return false;
    for (int i = arr.size() - 1; i >= 0; i--) {
      if (value.equals(arr.get(i).getAsString()))
        arr.remove(i);
    }
    return true;
  }
}
