package com.mtxcore.unifiedlegacysettings;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

final class TexturePackCommand {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().create();

  private TexturePackCommand() {}

  static void register() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess)
            -> dispatcher.register(
                literal("uls-pack")
                    .then(argument("pack_id", StringArgumentType.string())
                              .then(literal("on").executes(
                                  ctx
                                  -> apply(ctx.getSource(),
                                           StringArgumentType.getString(
                                               ctx, "pack_id"),
                                           true)))
                              .then(literal("off").executes(
                                  ctx
                                  -> apply(ctx.getSource(),
                                           StringArgumentType.getString(
                                               ctx, "pack_id"),
                                           false)))
                              .then(literal("toggle").executes(
                                  ctx
                                  -> toggle(ctx.getSource(),
                                            StringArgumentType.getString(
                                                ctx, "pack_id")))))));
  }

  private static int toggle(FabricClientCommandSource source, String packId) {
    Minecraft mc = Minecraft.getInstance();
    Options options = mc.options;
    boolean enabled = isPackEnabled(options, packId);
    return apply(source, packId, !enabled, false);
  }

  private static int apply(FabricClientCommandSource source,
                           String packId, boolean enable) {
    return apply(source, packId, enable, false);
  }

  static boolean applySilently(String packId, boolean enable) {
    return apply(null, packId, enable, true) > 0;
  }

  private static int apply(FabricClientCommandSource source,
                           String packId, boolean enable, boolean silent) {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.options == null)
      return 0;

    Options options = mc.options;
    List<String> selected = options.resourcePacks;
    List<String> incompatible = options.incompatibleResourcePacks;
    String normalized = packId == null ? "" : packId.trim();
    if (normalized.isEmpty())
      return 0;
    boolean wasEnabled = isPackEnabled(options, normalized);

    CompatDebug.log("Pack apply request: id='{}', enable={}, silent={}",
                    normalized, enable, silent);

    if (enable) {
      if (!selected.contains(normalized)) {
        selected.add(normalized);
      }
      incompatible.remove(normalized);
      if (!silent && source != null) {
        source.sendFeedback(
            Component.literal("Enabled resource pack: " + normalized));
      }
    } else {
      selected.remove(normalized);
      incompatible.remove(normalized);
      if (!silent && source != null) {
        source.sendFeedback(
            Component.literal("Disabled resource pack: " + normalized));
      }
    }

    boolean packStateChanged = wasEnabled != enable;
    boolean legacyChanged = updateLegacyPackConfigs(normalized, enable);
    if (packStateChanged || legacyChanged) {
      options.save();
      mc.reloadResourcePacks();
    }
    return 1;
  }

  private static boolean isPackEnabled(Options options, String packId) {
    return (options.resourcePacks.contains(packId) ||
            options.incompatibleResourcePacks.contains(packId));
  }

  private static boolean updateLegacyPackConfigs(String packId,
                                                 boolean enable) {
    Path gameDir = FabricLoader.getInstance().getGameDir();
    CompatDebug.log("Game dir resolved to {}", gameDir);

    Path globalPacks = gameDir.resolve("config/legacy/global_packs.json");
    boolean globalChanged = updateJsonStringArrays(globalPacks, packId, enable);
    CompatDebug.log("Legacy global packs file {} exists={}, changed={}",
                    globalPacks, Files.exists(globalPacks), globalChanged);

    boolean albumChanged = false;
    Path albumsIndex = gameDir.resolve("resource_albums.json");
    String defaultAlbum = readDefaultAlbum(albumsIndex);
    CompatDebug.log("Resource album index {} exists={}, defaultAlbum={}",
                    albumsIndex, Files.exists(albumsIndex), defaultAlbum);
    if (defaultAlbum != null && !defaultAlbum.isBlank()) {
      Path albumDir = gameDir.resolve("resource_albums");
      Path albumFileRaw = albumDir.resolve(defaultAlbum);
      Path albumFileJson = albumDir.resolve(defaultAlbum + ".json");
      boolean rawChanged = updateJsonStringArrays(albumFileRaw, packId, enable);
      boolean jsonChanged =
          updateJsonStringArrays(albumFileJson, packId, enable);
      albumChanged = rawChanged || jsonChanged;
      CompatDebug.log("Resource album files raw={} (exists={}, changed={}), "
                          + "json={} (exists={}, changed={})",
                      albumFileRaw, Files.exists(albumFileRaw), rawChanged,
                      albumFileJson, Files.exists(albumFileJson), jsonChanged);
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
    } catch (Exception e) {
      CompatDebug.log("Could not read Legacy4J resource album index", e);
      return null;
    }
  }

  private static boolean updateJsonStringArrays(Path file, String packId,
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
        if (!(value instanceof JsonArray arr))
          continue;
        if (!isStringArray(arr))
          continue;

        changed |= toggleInArray(arr, packId, enable);
      }

      if (changed) {
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(obj));
      }
      return changed;
    } catch (Exception e) {
      CompatDebug.log("Could not update Legacy4J pack file {}", file, e);
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
      if (value.equals(arr.get(i).getAsString())) {
        arr.remove(i);
      }
    }
    return true;
  }
}
