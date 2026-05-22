package com.mtxcore.unifiedlegacysettings;

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
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class OptionScreenEntryInjector {

  private static final Map<Object, EnumSet<ModSettingsCompat.Section>>
      INJECTED_BY_LIST = Collections.synchronizedMap(new WeakHashMap<>());

  private OptionScreenEntryInjector() {}

  public static void inject(Object screen) {
    inject(screen, null);
  }

  private static void inject(Object screen,
                             ModSettingsCompat.Section forcedSection) {
    if (screen == null)
      return;

    String className = screen.getClass().getName();
    if (!className.startsWith("wily.legacy.client.screen.OptionsScreen"))
      return;

    try {
      Field rvlField =
          Class.forName("wily.legacy.client.screen.PanelVListScreen")
              .getDeclaredField("renderableVList");
      rvlField.setAccessible(true);
      Object renderableVList = rvlField.get(screen);
      if (renderableVList == null)
        return;

      Field renderablesField =
          renderableVList.getClass().getField("renderables");
      @SuppressWarnings("unchecked")
      List<Object> renderables =
          (List<Object>)renderablesField.get(renderableVList);

      String title = readScreenTitle(screen);
      ModSettingsCompat.Section section =
          forcedSection == null
              ? ModSettingsCompat.detectSection(title, className, renderables)
              : forcedSection;
      if (section == ModSettingsCompat.Section.OTHER) {
        return;
      }

      if (forcedSection == null)
        injectAdvancedOptionsScreen(screen, section);

      if (alreadyInjected(renderableVList, section)) {
        return;
      }

      List<ModSettingsCompat.Entry> entries =
          ModSettingsCompat.collectEntries(section);
      if (entries.isEmpty()) {
        return;
      }

      for (ModSettingsCompat.Entry entry : entries) {
        if (containsEntry(renderables, entry)) {
          continue;
        }

        Object widget;
        if (entry.kind() == ModSettingsCompat.WidgetKind.TOGGLE) {
          widget = createTickBox(entry.label(), entry.onActivate(),
                                 entry.selected(), entry.tooltip());
        } else if (entry.kind() == ModSettingsCompat.WidgetKind.CYCLE) {
          widget = createCycleButton(entry.label(), entry.onActivate());
        } else {
          widget =
              entry.customWidget() == null ? null : entry.customWidget().get();
        }
        if (widget == null)
          continue;

        applyTooltip(widget, entry.tooltip());

        Component widgetMessage = getWidgetMessage(widget);
        if (widgetMessage != null &&
            containsMessage(renderables, widgetMessage.getString())) {
          continue;
        }

        int insertIndex =
            findInsertIndex(renderables, entry.insertBeforeText());
        renderables.add(insertIndex, widget);
      }

    } catch (Exception e) {
      UnifiedLegacySettings.LOGGER.error("[ULS] Failed to inject options", e);
    }
  }

  private static boolean alreadyInjected(Object renderableVList,
                                         ModSettingsCompat.Section section) {
    synchronized (INJECTED_BY_LIST) {
      EnumSet<ModSettingsCompat.Section> injected =
          INJECTED_BY_LIST.computeIfAbsent(
              renderableVList,
              ignored -> EnumSet.noneOf(ModSettingsCompat.Section.class));
      if (injected.contains(section)) {
        return true;
      }
      injected.add(section);
      return false;
    }
  }

  private static void injectAdvancedOptionsScreen(
      Object screen, ModSettingsCompat.Section section) {
    ModSettingsCompat.Section advanced = advancedSectionFor(section);
    if (advanced == null)
      return;

    Object advancedScreen = readAdvancedOptionsScreen(screen);
    if (advancedScreen == null || advancedScreen == screen)
      return;

    inject(advancedScreen, advanced);
  }

  private static ModSettingsCompat.Section advancedSectionFor(
      ModSettingsCompat.Section section) {
    return switch (section) {
      case GRAPHICS -> ModSettingsCompat.Section.ADVANCED_GRAPHICS;
      case GAME_OPTIONS -> ModSettingsCompat.Section.ADVANCED_GAME_OPTIONS;
      case AUDIO -> ModSettingsCompat.Section.ADVANCED_AUDIO;
      case USER_INTERFACE -> ModSettingsCompat.Section.ADVANCED_USER_INTERFACE;
      default -> null;
    };
  }

  private static Object readAdvancedOptionsScreen(Object screen) {
    try {
      Field advancedOptionsField =
          findFieldInHierarchy(screen.getClass(), "advancedOptionsScreen");
      if (advancedOptionsField == null)
        return null;
      advancedOptionsField.setAccessible(true);
      return advancedOptionsField.get(screen);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static int findInsertIndex(List<Object> renderables,
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
        Component msg = getWidgetMessage(renderables.get(index));
        if (msg == null)
          continue;
        if (componentMatches(msg, needle)) {
          return insertAfter ? Math.min(index + 1, renderables.size()) : index;
        }
      }
    }
    return renderables.size();
  }

  private static boolean containsMessage(List<Object> renderables,
                                         String text) {
    if (text == null || text.isBlank())
      return false;
    String needle = text.toLowerCase(Locale.ROOT);
    for (Object renderable : renderables) {
      Component msg = getWidgetMessage(renderable);
      if (msg != null && componentMatches(msg, needle)) {
        return true;
      }
    }
    return false;
  }

  private static boolean componentMatches(Component message,
                                          String loweredNeedle) {
    String resolved = message.getString();
    if (resolved != null &&
        resolved.toLowerCase(Locale.ROOT).contains(loweredNeedle)) {
      return true;
    }

    String raw = message.toString();
    return raw != null && raw.toLowerCase(Locale.ROOT).contains(loweredNeedle);
  }

  private static boolean containsEntry(List<Object> renderables,
                                       ModSettingsCompat.Entry entry) {
    if (entry.dedupeText() != null && !entry.dedupeText().isBlank() &&
        containsMessage(renderables, entry.dedupeText())) {
      return true;
    }

    if (entry.kind() == ModSettingsCompat.WidgetKind.TOGGLE ||
        entry.kind() == ModSettingsCompat.WidgetKind.CYCLE) {
      Supplier<Component> labelSupplier = entry.label();
      if (labelSupplier != null) {
        Component label = labelSupplier.get();
        if (label != null && containsMessage(renderables, label.getString())) {
          return true;
        }
      }
    }

    return false;
  }

  private static Object createTickBox(Supplier<Component> label,
                                      Runnable onActivate,
                                      BooleanSupplier selected,
                                      Supplier<Component> tooltipText) {
    try {
      Class<?> tickBoxClass =
          Class.forName("wily.legacy.client.screen.TickBox");
      Constructor<?> ctor = findTickBoxConstructor(tickBoxClass);
      if (ctor == null) {
        UnifiedLegacySettings.LOGGER.error(
            "[ULS] TickBox constructor not found");
        return null;
      }

      Function<Boolean, Tooltip> tooltip =
          ignored -> createTooltip(tooltipText);
      Consumer<Object> press = ignored -> onActivate.run();
      Function<Boolean, Component> labelFn = ignored -> label.get();

      if (ctor.getParameterTypes().length == 9) {
        return ctor.newInstance(0, 0, 200, 16, selected.getAsBoolean(),
                                labelFn, tooltip, press, selected);
      }
      return ctor.newInstance(0, 0, 200, selected.getAsBoolean(), labelFn,
                              tooltip, press, selected);
    } catch (Exception e) {
      UnifiedLegacySettings.LOGGER.error("[ULS] Could not create TickBox", e);
      return null;
    }
  }

  private static AbstractWidget createCycleButton(Supplier<Component> label,
                                                  Runnable onActivate) {
    return LegacyWidgetFactory.button(
        0, 0, 200, 20, label.get(),
        btn -> {
          onActivate.run();
          btn.setMessage(label.get());
        });
  }

  private static Tooltip createTooltip(Supplier<Component> tooltipText) {
    if (tooltipText == null)
      return null;
    Component component = tooltipText.get();
    return component == null ? null : Tooltip.create(component);
  }

  private static void applyTooltip(Object widget,
                                   Supplier<Component> tooltipText) {
    Tooltip tooltip = createTooltip(tooltipText);
    if (tooltip == null)
      return;

    try {
      widget.getClass()
          .getMethod("setTooltip", Tooltip.class)
          .invoke(widget, tooltip);
    } catch (Exception ignored) {
    }
  }

  private static Component getWidgetMessage(Object widget) {
    try {
      return (Component)widget.getClass()
          .getMethod("getMessage")
          .invoke(widget);
    } catch (Exception ignored) {
    }
    try {
      Field f = findFieldInHierarchy(widget.getClass(), "message");
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

  private static String readScreenTitle(Object screen) {
    try {
      Object title = screen.getClass().getMethod("getTitle").invoke(screen);
      if (title instanceof Component c) {
        return c.getString() + " " + c;
      }
    } catch (Exception ignored) {
    }
    return "";
  }

  private static Field findFieldInHierarchy(Class<?> cls, String name) {
    while (cls != null) {
      try {
        return cls.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        cls = cls.getSuperclass();
      }
    }
    return null;
  }

  private static Constructor<?> findTickBoxConstructor(Class<?> cls) {
    Constructor<?> fallback = null;
    for (Constructor<?> c : cls.getConstructors()) {
      if (isTickBoxConstructor(c)) {
        if (c.getParameterTypes().length == 9)
          return c;
        fallback = c;
      }
    }
    for (Constructor<?> c : cls.getDeclaredConstructors()) {
      if (isTickBoxConstructor(c)) {
        c.setAccessible(true);
        if (c.getParameterTypes().length == 9)
          return c;
        fallback = c;
      }
    }
    return fallback;
  }

  private static boolean isTickBoxConstructor(Constructor<?> ctor) {
    Class<?>[] params = ctor.getParameterTypes();
    if (params.length != 8 && params.length != 9)
      return false;

    int selectedIndex = params.length == 8 ? 3 : 4;
    return params[0] == int.class &&
           params[1] == int.class &&
           params[2] == int.class &&
           (params.length == 8 || params[3] == int.class) &&
           params[selectedIndex] == boolean.class &&
           Function.class.isAssignableFrom(params[selectedIndex + 1]) &&
           Function.class.isAssignableFrom(params[selectedIndex + 2]) &&
           Consumer.class.isAssignableFrom(params[selectedIndex + 3]) &&
           BooleanSupplier.class.isAssignableFrom(params[selectedIndex + 4]);
  }
}
