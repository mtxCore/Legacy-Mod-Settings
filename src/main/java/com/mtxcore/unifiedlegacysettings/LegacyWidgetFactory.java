package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Constructor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public final class LegacyWidgetFactory {

  private LegacyWidgetFactory() {}

  public static AbstractWidget tickBox(int x, int y, int width,
                                       SupplierLabel label,
                                       BooleanSupplier selected,
                                       Consumer<Boolean> save,
                                       SupplierLabel tooltipText) {
    try {
      Class<?> tickBoxClass =
          Class.forName("wily.legacy.client.screen.TickBox");
      Constructor<?> ctor = findTickBoxConstructor(tickBoxClass);
      if (ctor != null) {
        Function<Boolean, Component> labelFn = ignored -> label.get();
        Function<Boolean, Tooltip> tooltip =
            ignored -> createTooltip(tooltipText);
        Consumer<Object> press =
            ignored -> save.accept(!selected.getAsBoolean());

        if (ctor.getParameterTypes().length == 9) {
          return (AbstractWidget)ctor.newInstance(
              x, y, width, 16, selected.getAsBoolean(), labelFn, tooltip,
              press, selected);
        }
        return (AbstractWidget)ctor.newInstance(
            x, y, width, selected.getAsBoolean(), labelFn, tooltip, press,
            selected);
      }
    } catch (Exception e) {
      UnifiedLegacySettings.LOGGER.debug(
          "[ULS] Falling back from Legacy4J TickBox", e);
    }

    AbstractWidget button =
        button(x, y, width, 20, toggleLabel(label.get(), selected),
               b -> {
                 save.accept(!selected.getAsBoolean());
                 b.setMessage(toggleLabel(label.get(), selected));
               });
    button.setTooltip(createTooltip(tooltipText));
    return button;
  }

  public static AbstractWidget button(int x, int y, int width, int height,
                                      Component label,
                                      Button.OnPress onPress) {
    try {
      Class<?> buttonClass =
          Class.forName("wily.legacy.client.screen.LegacyButton");
      for (Constructor<?> ctor : buttonClass.getConstructors()) {
        Class<?>[] params = ctor.getParameterTypes();
        if (params.length == 2 && params[0] == Component.class &&
            Button.OnPress.class.isAssignableFrom(params[1])) {
          AbstractWidget widget =
              (AbstractWidget)ctor.newInstance(label, onPress);
          widget.setX(x);
          widget.setY(y);
          widget.setWidth(width);
          widget.setHeight(height);
          return widget;
        }
      }
    } catch (Exception e) {
      UnifiedLegacySettings.LOGGER.debug(
          "[ULS] Falling back from Legacy4J button", e);
    }

    return Button.builder(label, onPress).bounds(x, y, width, height).build();
  }

  private static Component toggleLabel(Component label,
                                       BooleanSupplier selected) {
    return Component.literal(label.getString() + ": " +
                             (selected.getAsBoolean() ? "On" : "Off"));
  }

  private static Tooltip createTooltip(SupplierLabel tooltipText) {
    if (tooltipText == null)
      return null;
    Component component = tooltipText.get();
    return component == null ? null : Tooltip.create(component);
  }

  private static Constructor<?> findTickBoxConstructor(Class<?> cls) {
    Constructor<?> fallback = null;
    for (Constructor<?> ctor : cls.getConstructors()) {
      Class<?>[] params = ctor.getParameterTypes();
      if ((params.length == 8 || params.length == 9) &&
          params[0] == int.class &&
          params[1] == int.class &&
          params[2] == int.class &&
          (params.length == 8 || params[3] == int.class) &&
          params[params.length == 8 ? 3 : 4] == boolean.class &&
          Function.class.isAssignableFrom(
              params[params.length == 8 ? 4 : 5]) &&
          Function.class.isAssignableFrom(
              params[params.length == 8 ? 5 : 6]) &&
          Consumer.class.isAssignableFrom(
              params[params.length == 8 ? 6 : 7]) &&
          BooleanSupplier.class.isAssignableFrom(
              params[params.length == 8 ? 7 : 8])) {
        if (params.length == 9)
          return ctor;
        fallback = ctor;
      }
    }
    return fallback;
  }

  @FunctionalInterface
  public interface SupplierLabel {
    Component get();
  }
}
