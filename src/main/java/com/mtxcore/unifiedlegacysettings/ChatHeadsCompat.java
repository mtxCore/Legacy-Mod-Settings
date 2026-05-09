package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;

final class ChatHeadsCompat {

  private ChatHeadsCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (section != ModSettingsCompat.Section.ADVANCED_USER_INTERFACE)
      return;
    if (!ModSettingsConfig.get().showChatHeads)
      return;
    if (!RefUtil.isModLoaded("chat_heads", "chatheads"))
      return;

    Object cfgRoot =
        RefUtil.staticField("dzwdz.chat_heads.ChatHeads", "CONFIG");
    if (cfgRoot == null ||
        !cfgRoot.getClass().getName().endsWith("ChatHeadsConfigData"))
      return;

    Field renderPositionField =
        RefUtil.field(cfgRoot.getClass(), "renderPosition");
    RefUtil.MethodRef save = RefUtil.staticMethod(
        "dzwdz.chat_heads.config.ClothConfigCommonImpl", "saveConfig");
    if (renderPositionField == null)
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "display chat indicators",
        ()
            -> Component.literal("Chat Portraits"),
        ()
            -> {
          Object current = RefUtil.readField(cfgRoot, renderPositionField);
          Object next = flipRenderPosition(current);
          if (next != null) {
            RefUtil.writeField(cfgRoot, renderPositionField, next);
            RefUtil.invoke(save, null);
          }
        },
        ()
            -> isOn(RefUtil.readField(cfgRoot, renderPositionField)),
        ()
            -> Component.literal(
                "Show player portraits next to chat messages.")));
  }

  private static Object flipRenderPosition(Object current) {
    if (!(current instanceof Enum<?> e))
      return null;
    Object beforeName = RefUtil.enumConstant(e.getClass(), "BEFORE_NAME");
    Object beforeLine = RefUtil.enumConstant(e.getClass(), "BEFORE_LINE");
    if (beforeName == null || beforeLine == null)
      return null;
    return e.name().equals("BEFORE_NAME") ? beforeLine : beforeName;
  }

  private static boolean isOn(Object renderPosition) {
    return (renderPosition instanceof Enum<?> e &&
            e.name().equals("BEFORE_NAME"));
  }
}
