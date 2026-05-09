package com.mtxcore.legacymodsettings.mixin;

import com.mtxcore.legacymodsettings.LegacyModSettings;
import com.mtxcore.legacymodsettings.ModSettingsCompat;
import com.mtxcore.legacymodsettings.ModSettingsConfig;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
public abstract class MixinHelpAndOptionsScreen {

  @Unique
  private static final AtomicBoolean legacyModSettings$constructorProbeLogged =
      new AtomicBoolean(false);

  @Unique
  private static final Map<Object, EnumSet<ModSettingsCompat.Section>>
      legacyModSettings$injectedByList =
          Collections.synchronizedMap(new WeakHashMap<>());

  @Inject(method = "renderableVListInit", at = @At("RETURN"), remap = false)
  private void legacyModSettings$injectNativeSettings(CallbackInfo ci) {
    legacyModSettings$injectNativeSettingsImpl();
  }

  @Inject(method = "init", at = @At("RETURN"), remap = false, require = 0)
  private void legacyModSettings$injectNativeSettingsFallback(CallbackInfo ci) {
    legacyModSettings$injectNativeSettingsImpl();
  }

  @Unique
  private void legacyModSettings$injectNativeSettingsImpl() {
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

      String title = legacyModSettings$readScreenTitle(this);
      ModSettingsCompat.Section section =
          ModSettingsCompat.detectSection(title, className, renderables);
      if (ModSettingsConfig.get().debugCompatLogs) {
        LegacyModSettings.LOGGER.info(
            "[Legacy Mod Settings] Mixin hook fired for class={} title='{}' "
                + "section={} renderables={}",
            className, title, section, renderables.size());
      }

      if (section == ModSettingsCompat.Section.OTHER) {
        return;
      }

      if (legacyModSettings$alreadyInjected(renderableVList, section)) {
        return;
      }

      if (ModSettingsCompat.isLegacySettingsMenusEnabled() &&
          !ModSettingsConfig.get().showInLegacySettings) {
        if (section != ModSettingsCompat.Section.ADVANCED_GRAPHICS) {
          if (ModSettingsConfig.get().debugCompatLogs) {
            LegacyModSettings.LOGGER.info(
                "[Legacy Mod Settings] Skipping section={} because "
                    + "legacySettingsMenus=true, showInLegacySettings=false, "
                    + "and it's not Advanced Graphics",
                section);
          }
          return;
        }
      }

      List<ModSettingsCompat.Entry> entries =
          ModSettingsCompat.collectEntries(section);
      if (entries.isEmpty()) {
        return;
      }

      int added = 0;

      for (ModSettingsCompat.Entry entry : entries) {
        if (legacyModSettings$containsEntry(renderables, entry)) {
          continue;
        }

        Object widget;
        if (entry.kind() == ModSettingsCompat.WidgetKind.TOGGLE) {
          widget = legacyModSettings$createTickBox(
              entry.label(), entry.onActivate(), entry.selected(),
              entry.tooltip());
        } else if (entry.kind() == ModSettingsCompat.WidgetKind.CYCLE) {
          widget = legacyModSettings$createCycleButton(entry.label(),
                                                       entry.onActivate());
        } else {
          widget =
              entry.customWidget() == null ? null : entry.customWidget().get();
        }
        if (widget == null)
          continue;

        legacyModSettings$applyTooltip(widget, entry.tooltip());

        Component widgetMessage = legacyModSettings$getWidgetMessage(widget);
        if (widgetMessage != null &&
            legacyModSettings$containsMessage(renderables,
                                              widgetMessage.getString())) {
          continue;
        }

        int insertIndex = legacyModSettings$findInsertIndex(
            renderables, entry.insertBeforeText());
        renderables.add(insertIndex, widget);
        added++;
      }

      if (added > 0) {
        legacyModSettings$reloadUI(renderableVList);
      }

      LegacyModSettings.LOGGER.info(
          "[Legacy Mod Settings] Injected compat entries in {} as {}",
          className, section);
    } catch (Exception e) {
      LegacyModSettings.LOGGER.error(
          "[Legacy Mod Settings] Failed to inject options", e);
    }
  }

  @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
  private void legacyModSettings$constructorProbe(CallbackInfo ci) {
    if (legacyModSettings$constructorProbeLogged.compareAndSet(false, true)) {
      LegacyModSettings.LOGGER.info(
          "[Legacy Mod Settings] PanelVListScreen mixin constructor probe "
          + "fired (mixin is active)");
    }
  }

  @Unique
  private static boolean
  legacyModSettings$alreadyInjected(Object renderableVList,
                                    ModSettingsCompat.Section section) {
    synchronized (legacyModSettings$injectedByList) {
      EnumSet<ModSettingsCompat.Section> injected =
          legacyModSettings$injectedByList.computeIfAbsent(
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
  private static int legacyModSettings$findInsertIndex(List<Object> renderables,
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
            legacyModSettings$getWidgetMessage(renderables.get(index));
        if (msg == null)
          continue;
        if (legacyModSettings$componentMatches(msg, needle)) {
          return insertAfter ? Math.min(index + 1, renderables.size()) : index;
        }
      }
    }
    return renderables.size();
  }

  @Unique
  private static boolean
  legacyModSettings$containsMessage(List<Object> renderables, String text) {
    if (text == null || text.isBlank())
      return false;
    String needle = text.toLowerCase(Locale.ROOT);
    for (Object renderable : renderables) {
      Component msg = legacyModSettings$getWidgetMessage(renderable);
      if (msg != null && legacyModSettings$componentMatches(msg, needle)) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private static boolean
  legacyModSettings$componentMatches(Component message, String loweredNeedle) {
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
  legacyModSettings$containsEntry(List<Object> renderables,
                                  ModSettingsCompat.Entry entry) {
    if (entry.dedupeText() != null && !entry.dedupeText().isBlank() &&
        legacyModSettings$containsMessage(renderables, entry.dedupeText())) {
      return true;
    }

    if (entry.kind() == ModSettingsCompat.WidgetKind.TOGGLE ||
        entry.kind() == ModSettingsCompat.WidgetKind.CYCLE) {
      Supplier<Component> labelSupplier = entry.label();
      if (labelSupplier != null) {
        Component label = labelSupplier.get();
        if (label != null &&
            legacyModSettings$containsMessage(renderables, label.getString())) {
          return true;
        }
      }
    }

    return false;
  }

  @Unique
  private static Object
  legacyModSettings$createTickBox(Supplier<Component> label,
                                  Runnable onActivate, BooleanSupplier selected,
                                  Supplier<Component> tooltipText) {
    try {
      Class<?> tickBoxClass =
          Class.forName("wily.legacy.client.screen.TickBox");
      Constructor<?> ctor =
          legacyModSettings$findCtorByParamCount(tickBoxClass, 8);
      if (ctor == null) {
        LegacyModSettings.LOGGER.error(
            "[Legacy Mod Settings] TickBox constructor not found");
        return null;
      }

      Function<Boolean, Tooltip> tooltip =
          ignored -> legacyModSettings$createTooltip(tooltipText);
      Consumer<Object> press = ignored -> onActivate.run();
      Function<Boolean, Component> labelFn = ignored -> label.get();

      return ctor.newInstance(0, 0, 200, selected.getAsBoolean(), labelFn,
                              tooltip, press, selected);
    } catch (Exception e) {
      LegacyModSettings.LOGGER.error(
          "[Legacy Mod Settings] Could not create TickBox", e);
      return null;
    }
  }

  @Unique
  private static Button
  legacyModSettings$createCycleButton(Supplier<Component> label,
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
  legacyModSettings$createTooltip(Supplier<Component> tooltipText) {
    if (tooltipText == null)
      return null;
    Component component = tooltipText.get();
    return component == null ? null : Tooltip.create(component);
  }

  @Unique
  private static void
  legacyModSettings$applyTooltip(Object widget,
                                 Supplier<Component> tooltipText) {
    Tooltip tooltip = legacyModSettings$createTooltip(tooltipText);
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
  private static Component legacyModSettings$getWidgetMessage(Object widget) {
    try {
      return (Component)widget.getClass()
          .getMethod("getMessage")
          .invoke(widget);
    } catch (Exception ignored) {
    }
    try {
      Field f =
          legacyModSettings$findFieldInHierarchy(widget.getClass(), "message");
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
  private static String legacyModSettings$readScreenTitle(Object screen) {
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
  private static Field legacyModSettings$findFieldInHierarchy(Class<?> cls,
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
  private static void legacyModSettings$reloadUI(Object renderableVList) {
    try {
      Field accessorField = legacyModSettings$findFieldInHierarchy(
          renderableVList.getClass(), "accessor");
      if (accessorField == null)
        return;
      accessorField.setAccessible(true);

      Object accessor = accessorField.get(renderableVList);
      if (accessor == null)
        return;

      accessor.getClass().getMethod("reloadUI").invoke(accessor);
    } catch (Exception e) {
      LegacyModSettings.LOGGER.debug(
          "[Legacy Mod Settings] Could not reload UI after injection", e);
    }
  }

  @Unique
  private static Constructor<?>
  legacyModSettings$findCtorByParamCount(Class<?> cls, int count) {
    for (Constructor<?> c : cls.getConstructors()) {
      if (c.getParameterCount() == count)
        return c;
    }
    for (Constructor<?> c : cls.getDeclaredConstructors()) {
      if (c.getParameterCount() == count) {
        c.setAccessible(true);
        return c;
      }
    }
    return null;
  }
}
