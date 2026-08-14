package com.mtxcore.unifiedlegacysettings;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class ContinuityCompat {

  private static final String[] CONNECTED_TEXTURE_PACK_CANDIDATES = {
      "continuity/default",
      "continuity:default",
  };
  private static Boolean connectedTexturesDesired;

  private ContinuityCompat() {}

  private static Boolean desiredConnectedTexturesState() {
    if (connectedTexturesDesired != null)
      return connectedTexturesDesired;
    return ModSettingsConfig.get().continuityConnectedTexturesEnabled;
  }

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (section != ModSettingsCompat.Section.ADVANCED_GRAPHICS)
      return;
    if (!ModSettingsConfig.get().showContinuity)
      return;
    if (!RefUtil.isModLoaded("continuity"))
      return;

    RefUtil.MethodRef getStates = RefUtil.staticMethod(
        "me.pepperbell.continuity.api.client.ContinuityFeatureStates", "get");
    if (getStates == null)
      return;
    Object states = getStates.invokeStatic();
    if (states == null)
      return;

    addFeature(entries, states, "getConnectedTexturesState",
               "Connected Textures");
    addFeature(entries, states, "getEmissiveTexturesState",
               "Emissive Textures");
  }

  private static void addFeature(List<ModSettingsCompat.Entry> entries,
                                 Object states, String getterName,
                                 String label) {
    RefUtil.MethodRef getter = RefUtil.method(states.getClass(), getterName);
    if (getter == null)
      return;
    Object state = getter.invoke(states);
    if (state == null)
      return;

    RefUtil.MethodRef isEnabled = RefUtil.method(state.getClass(), "isEnabled");
    RefUtil.MethodRef enable = RefUtil.method(state.getClass(), "enable");
    RefUtil.MethodRef disable = RefUtil.method(state.getClass(), "disable");
    if (isEnabled == null || enable == null || disable == null)
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "enhanced item translucency",
        ()
            -> Component.literal(label),
        ()
            -> {
          boolean currentlyEnabled =
              "Connected Textures".equals(label)
                  ? getConnectedTexturesEnabled(isEnabled, state)
                  : RefUtil.invokeBoolean(isEnabled, state, true);
          boolean next = !currentlyEnabled;

          if (next)
            enable.invoke(state);
          else
            disable.invoke(state);

          if ("Connected Textures".equals(label)) {
            connectedTexturesDesired = next;
            RefUtil.persistModDesired(next, RefUtil.PersistKey.CONTINUITY);
            applyConnectedTexturesConfig(next);
            applyConnectedTexturePacks(next);
          }
        },
        ()
            -> "Connected Textures".equals(label)
                   ? getConnectedTexturesEnabled(isEnabled, state)
                   : RefUtil.invokeBoolean(isEnabled, state, true),
        ()
            -> Component.literal(label.equals("Connected Textures")
                                     ? "Blend matching block textures "
                                           + "together for a smoother look."
                                     : "Allow glowing parts of textures to "
                                           + "shine in the dark.")));
  }

  static void enforceRuntimeState() {
    Boolean desired = desiredConnectedTexturesState();
    if (desired == null)
      return;
    connectedTexturesDesired = desired;
    if (!RefUtil.isModLoaded("continuity"))
      return;

    RefUtil.MethodRef getStates = RefUtil.staticMethod(
        "me.pepperbell.continuity.api.client.ContinuityFeatureStates", "get");
    Object states = getStates == null ? null : getStates.invokeStatic();
    if (states == null)
      return;

    RefUtil.MethodRef getter =
        RefUtil.method(states.getClass(), "getConnectedTexturesState");
    Object state = getter == null ? null : getter.invoke(states);
    if (state == null)
      return;

    RefUtil.MethodRef isEnabled = RefUtil.method(state.getClass(), "isEnabled");
    RefUtil.MethodRef enable = RefUtil.method(state.getClass(), "enable");
    RefUtil.MethodRef disable = RefUtil.method(state.getClass(), "disable");

    boolean currentEnabled = RefUtil.invokeBoolean(isEnabled, state, true);
    if (currentEnabled != connectedTexturesDesired) {
      applyConnectedTexturesConfig(connectedTexturesDesired);
      if (connectedTexturesDesired) {
        RefUtil.invoke(enable, state);
      } else {
        RefUtil.invoke(disable, state);
      }
    }

    if (!connectedTexturesDesired && isAnyCandidatePackEnabled()) {
      applyConnectedTexturePacks(false);
    } else if (connectedTexturesDesired &&
               !isAnyCandidatePackEnabled()) {
      applyConnectedTexturePacks(true);
    }
  }

  private static void applyConnectedTexturePacks(boolean enabled) {
    if (!enabled) {
      for (String packId : CONNECTED_TEXTURE_PACK_CANDIDATES) {
        ResourcePackCompat.applySilently(packId, false);
      }
      return;
    }

    String packId = resolveAvailableConnectedTexturePack();
    if (packId == null) {
      return;
    }

    ResourcePackCompat.applySilently(packId, true);
    for (String candidate : CONNECTED_TEXTURE_PACK_CANDIDATES) {
      if (!candidate.equals(packId)) {
        ResourcePackCompat.applySilently(candidate, false);
      }
    }
  }

  private static String resolveAvailableConnectedTexturePack() {
    for (String packId : CONNECTED_TEXTURE_PACK_CANDIDATES) {
      if (isPackEnabled(packId))
        return packId;
    }

    Set<String> availableIds = availableResourcePackIds();
    for (String packId : CONNECTED_TEXTURE_PACK_CANDIDATES) {
      if (availableIds.contains(packId))
        return packId;
    }
    return null;
  }

  private static Set<String> availableResourcePackIds() {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null)
      return Set.of();

    return new HashSet<>(mc.getResourcePackRepository().getAvailableIds());
  }

  private static boolean isAnyCandidatePackEnabled() {
    for (String packId : CONNECTED_TEXTURE_PACK_CANDIDATES) {
      if (isPackEnabled(packId))
        return true;
    }
    return false;
  }

  private static boolean isPackEnabled(String packId) {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.options == null || packId == null || packId.isBlank())
      return false;
    return (mc.options.resourcePacks.contains(packId) ||
            mc.options.incompatibleResourcePacks.contains(packId));
  }

  private static void applyConnectedTexturesConfig(boolean enabled) {
    Object config = RefUtil.staticField(
        "me.pepperbell.continuity.client.config.ContinuityConfig", "INSTANCE");
    if (config == null)
      return;

    Object connectedTexturesOption =
        RefUtil.instanceField(config, "connectedTextures");
    if (connectedTexturesOption != null) {
      RefUtil.MethodRef set = RefUtil.method(connectedTexturesOption.getClass(),
                                             "set", Object.class);
      if (set != null) {
        set.invoke(connectedTexturesOption, enabled);
      } else {
        RefUtil.MethodRef setBool = RefUtil.method(
            connectedTexturesOption.getClass(), "set", Boolean.class);
        if (setBool != null)
          setBool.invoke(connectedTexturesOption, enabled);
      }
    }

    RefUtil.MethodRef save = RefUtil.method(config.getClass(), "save");
    RefUtil.invoke(save, config);

    Minecraft mc = Minecraft.getInstance();
    RefUtil.refreshLevelRenderer(mc);
  }

  private static boolean
  getConnectedTexturesEnabled(RefUtil.MethodRef isEnabled, Object state) {
    Boolean desired = desiredConnectedTexturesState();
    if (desired != null)
      return desired;
    return RefUtil.invokeBoolean(isEnabled, state, true);
  }
}
