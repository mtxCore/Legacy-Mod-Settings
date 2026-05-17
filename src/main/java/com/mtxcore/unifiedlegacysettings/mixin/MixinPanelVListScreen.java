package com.mtxcore.unifiedlegacysettings.mixin;

import com.mtxcore.unifiedlegacysettings.UnifiedLegacySettings;
import com.mtxcore.unifiedlegacysettings.ModSettingsCompat;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "wily.legacy.client.screen.PanelVListScreen", remap = false)
public abstract class MixinPanelVListScreen {

  @Unique
  private static final Map<Object, EnumSet<ModSettingsCompat.Section>>
      unifiedLegacySettings$injectedByList =
          Collections.synchronizedMap(new WeakHashMap<>());

  @Inject(method = "renderableVListInit", at = @At("HEAD"), remap = false)
  private void unifiedLegacySettings$injectNativeSettings(CallbackInfo ci) {
    unifiedLegacySettings$injectNativeSettingsImpl();
  }

  @Unique
  private void unifiedLegacySettings$injectNativeSettingsImpl() {
    String className = this.getClass().getName();
    if (!className.startsWith("wily.legacy.client.screen.OptionsScreen"))
      return;

    try {
      Field rvlField =
          Class.forName("wily.legacy.client.screen.PanelVListScreen")
              .getDeclaredField("renderableVList");
      rvlField.setAccessible(true);
      Object renderableVList = rvlField.get(this);

      Field renderablesField =
          renderableVList.getClass().getField("renderables");
      @SuppressWarnings("unchecked")
      List<Object> renderables =
          (List<Object>)renderablesField.get(renderableVList);

      String title = unifiedLegacySettings$readScreenTitle(this);
      ModSettingsCompat.Section section =
          ModSettingsCompat.detectSection(title, className, renderables);
      if (section == ModSettingsCompat.Section.OTHER) {
        return;
      }

      if (unifiedLegacySettings$alreadyInjected(renderableVList, section)) {
        return;
      }

      List<ModSettingsCompat.Entry> entries =
          ModSettingsCompat.collectEntries(section);
      if (entries.isEmpty()) {
        return;
      }

      for (ModSettingsCompat.Entry entry : entries) {
        if (unifiedLegacySettings$containsEntry(renderables, entry)) {
          continue;
        }

        Object widget;
        if (entry.kind() == ModSettingsCompat.WidgetKind.TOGGLE) {
          widget = unifiedLegacySettings$createTickBox(
              entry.label(), entry.onActivate(), entry.selected(),
              entry.tooltip());
        } else if (entry.kind() == ModSettingsCompat.WidgetKind.CYCLE) {
          widget = unifiedLegacySettings$createCycleButton(entry.label(),
                                                       entry.onActivate());
        } else {
          widget =
              entry.customWidget() == null ? null : entry.customWidget().get();
        }
        if (widget == null)
          continue;

        unifiedLegacySettings$applyTooltip(widget, entry.tooltip());

        Component widgetMessage = unifiedLegacySettings$getWidgetMessage(widget);
        if (widgetMessage != null &&
            unifiedLegacySettings$containsMessage(renderables,
                                              widgetMessage.getString())) {
          continue;
        }

        int insertIndex = unifiedLegacySettings$findInsertIndex(
            renderables, entry.insertBeforeText());
        renderables.add(insertIndex, widget);
      }

    } catch (Exception e) {
      UnifiedLegacySettings.LOGGER.error(
          "[ULS] Failed to inject options", e);
    }
  }

  @Unique
  private static boolean
  unifiedLegacySettings$alreadyInjected(Object renderableVList,
                                    ModSettingsCompat.Section section) {
    synchronized (unifiedLegacySettings$injectedByList) {
      EnumSet<ModSettingsCompat.Section> injected =
          unifiedLegacySettings$injectedByList.computeIfAbsent(
              renderableVList,
              ignored -> EnumSet.noneOf(ModSettingsCompat.Section.class));
      if (injected.contains(section)) {
        return true;
      }
      injected.add(section);
      return false;
    }
  }

  @Unique
  private static int unifiedLegacySettings$findInsertIndex(List<Object> renderables,
                                                       String anchorText) {
    if (anchorText == null || anchorText.isBlank()) {
      return renderables.size();
    }

    String[] loweredAnchors = anchorText.toLowerCase(Locale.ROOT).split("\\|");
    for (String loweredAnchor : loweredAnchors) {
      String needle = loweredAnchor.trim();
      if (needle.isEmpty())
        continue;
      boolean insertAfter = needle.startsWith("after:");
      if (insertAfter) {
        needle = needle.substring("after:".length()).trim();
        if (needle.isEmpty())
          continue;
      }
      for (int index = 0; index < renderables.size(); index++) {
        Component msg =
            unifiedLegacySettings$getWidgetMessage(renderables.get(index));
        if (msg == null)
          continue;
        if (unifiedLegacySettings$componentMatches(msg, needle)) {
          return insertAfter ? Math.min(index + 1, renderables.size()) : index;
        }
      }
    }
    return renderables.size();
  }

  @Unique
  private static boolean
  unifiedLegacySettings$containsMessage(List<Object> renderables, String text) {
    if (text == null || text.isBlank())
      return false;
    String needle = text.toLowerCase(Locale.ROOT);
    for (Object renderable : renderables) {
      Component msg = unifiedLegacySettings$getWidgetMessage(renderable);
      if (msg != null && unifiedLegacySettings$componentMatches(msg, needle)) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private static boolean
  unifiedLegacySettings$componentMatches(Component message, String loweredNeedle) {
    String resolved = message.getString();
    if (resolved != null &&
        resolved.toLowerCase(Locale.ROOT).contains(loweredNeedle)) {
      return true;
    }

    String raw = message.toString();
    return raw != null && raw.toLowerCase(Locale.ROOT).contains(loweredNeedle);
  }

  @Unique
  private static boolean
  unifiedLegacySettings$containsEntry(List<Object> renderables,
                                  ModSettingsCompat.Entry entry) {
    if (entry.dedupeText() != null && !entry.dedupeText().isBlank() &&
        unifiedLegacySettings$containsMessage(renderables, entry.dedupeText())) {
      return true;
    }

    if (entry.kind() == ModSettingsCompat.WidgetKind.TOGGLE ||
        entry.kind() == ModSettingsCompat.WidgetKind.CYCLE) {
      Supplier<Component> labelSupplier = entry.label();
      if (labelSupplier != null) {
        Component label = labelSupplier.get();
        if (label != null &&
            unifiedLegacySettings$containsMessage(renderables, label.getString())) {
          return true;
        }
      }
    }

    return false;
  }

  @Unique
  private static Object
  unifiedLegacySettings$createTickBox(Supplier<Component> label,
                                  Runnable onActivate, BooleanSupplier selected,
                                  Supplier<Component> tooltipText) {
    try {
      Class<?> tickBoxClass =
          Class.forName("wily.legacy.client.screen.TickBox");
      Constructor<?> ctor =
          unifiedLegacySettings$findTickBoxConstructor(tickBoxClass);
      if (ctor == null) {
        UnifiedLegacySettings.LOGGER.error(
            "[ULS] TickBox constructor not found");
        return null;
      }

      Function<Boolean, Tooltip> tooltip =
          ignored -> unifiedLegacySettings$createTooltip(tooltipText);
      Consumer<Object> press = ignored -> onActivate.run();
      Function<Boolean, Component> labelFn = ignored -> label.get();

      return ctor.newInstance(0, 0, 200, selected.getAsBoolean(), labelFn,
                              tooltip, press, selected);
    } catch (Exception e) {
      UnifiedLegacySettings.LOGGER.error(
          "[ULS] Could not create TickBox", e);
      return null;
    }
  }

  @Unique
  private static Button
  unifiedLegacySettings$createCycleButton(Supplier<Component> label,
                                      Runnable onActivate) {
    return Button
        .builder(label.get(),
                 btn -> {
                   onActivate.run();
                   btn.setMessage(label.get());
                 })
        .bounds(0, 0, 200, 20)
        .build();
  }

  @Unique
  private static Tooltip
  unifiedLegacySettings$createTooltip(Supplier<Component> tooltipText) {
    if (tooltipText == null)
      return null;
    Component component = tooltipText.get();
    return component == null ? null : Tooltip.create(component);
  }

  @Unique
  private static void
  unifiedLegacySettings$applyTooltip(Object widget,
                                 Supplier<Component> tooltipText) {
    Tooltip tooltip = unifiedLegacySettings$createTooltip(tooltipText);
    if (tooltip == null)
      return;

    try {
      widget.getClass()
          .getMethod("setTooltip", Tooltip.class)
          .invoke(widget, tooltip);
    } catch (Exception ignored) {
    }
  }

  @Unique
  private static Component unifiedLegacySettings$getWidgetMessage(Object widget) {
    try {
      return (Component)widget.getClass()
          .getMethod("getMessage")
          .invoke(widget);
    } catch (Exception ignored) {
    }
    try {
      Field f =
          unifiedLegacySettings$findFieldInHierarchy(widget.getClass(), "message");
      if (f != null) {
        f.setAccessible(true);
        Object val = f.get(widget);
        if (val instanceof Component c)
          return c;
        if (val instanceof Function<?, ?>) {
          @SuppressWarnings("unchecked")
          Function<Boolean, Component> fn = (Function<Boolean, Component>)val;
          try {
            return fn.apply(Boolean.TRUE);
          } catch (Exception ignored2) {
          }
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  @Unique
  private static String unifiedLegacySettings$readScreenTitle(Object screen) {
    try {
      Object title = screen.getClass().getMethod("getTitle").invoke(screen);
      if (title instanceof Component c) {
        return c.getString() + " " + c;
      }
    } catch (Exception ignored) {
    }
    return "";
  }

  @Unique
  private static Field unifiedLegacySettings$findFieldInHierarchy(Class<?> cls,
                                                              String name) {
    while (cls != null) {
      try {
        return cls.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        cls = cls.getSuperclass();
      }
    }
    return null;
  }

  @Unique
  private static Constructor<?>
  unifiedLegacySettings$findTickBoxConstructor(Class<?> cls) {
    for (Constructor<?> c : cls.getConstructors()) {
      if (unifiedLegacySettings$isTickBoxConstructor(c))
        return c;
    }
    for (Constructor<?> c : cls.getDeclaredConstructors()) {
      if (unifiedLegacySettings$isTickBoxConstructor(c)) {
        c.setAccessible(true);
        return c;
      }
    }
    return null;
  }

  @Unique
  private static boolean
  unifiedLegacySettings$isTickBoxConstructor(Constructor<?> ctor) {
    Class<?>[] params = ctor.getParameterTypes();
    return params.length == 8 &&
           params[0] == int.class &&
           params[1] == int.class &&
           params[2] == int.class &&
           params[3] == boolean.class &&
           Function.class.isAssignableFrom(params[4]) &&
           Function.class.isAssignableFrom(params[5]) &&
           Consumer.class.isAssignableFrom(params[6]) &&
           BooleanSupplier.class.isAssignableFrom(params[7]);
  }
}
