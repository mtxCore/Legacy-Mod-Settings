package com.mtxcore.legacymodsettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

final class RefUtil {

  private RefUtil() {}

  static boolean isModLoaded(String... ids) {
    FabricLoader loader = FabricLoader.getInstance();
    for (String id : ids) {
      if (loader.isModLoaded(id))
        return true;
    }
    return false;
  }

  static Field field(Class<?> cls, String name) {
    for (Class<?> cur = cls; cur != null; cur = cur.getSuperclass()) {
      try {
        Field f = cur.getDeclaredField(name);
        f.setAccessible(true);
        return f;
      } catch (NoSuchFieldException ignored) {
      }
    }
    return null;
  }

  static Object readField(Object target, Field field) {
    if (field == null)
      return null;
    try {
      return field.get(target);
    } catch (Exception ignored) {
      return null;
    }
  }

  static void writeField(Object target, Field field, Object value) {
    if (field == null)
      return;
    try {
      field.set(target, value);
    } catch (Exception e) {
      CompatDebug.log("Failed writing field {}: {}", field.getName(),
                      e.getMessage());
    }
  }

  static Object instanceField(Object target, String fieldName) {
    if (target == null)
      return null;
    Field f = field(target.getClass(), fieldName);
    return readField(target, f);
  }

  static Object staticField(String className, String fieldName) {
    Class<?> cls = classForName(className);
    if (cls == null)
      return null;
    Field f = field(cls, fieldName);
    return readField(null, f);
  }

  static MethodRef method(Class<?> cls, String name, Class<?>... params) {
    if (cls == null)
      return null;
    try {
      // Prefer public lookup first so inherited/interface APIs keep working
      // across mod updates.
      Method m = cls.getMethod(name, params);
      m.setAccessible(true);
      return new MethodRef(m, false);
    } catch (NoSuchMethodException ignored) {
    }
    try {
      // Fall back to declared lookup for private/protected internals used by
      // older builds.
      Method m = cls.getDeclaredMethod(name, params);
      m.setAccessible(true);
      return new MethodRef(m, false);
    } catch (NoSuchMethodException ignored) {
    }
    return null;
  }

  static MethodRef method(String className, String name, Class<?>... params) {
    return method(classForName(className), name, params);
  }

  static MethodRef staticMethod(String className, String name,
                                Class<?>... params) {
    Class<?> cls = classForName(className);
    if (cls == null)
      return null;
    MethodRef ref = method(cls, name, params);
    return ref == null ? null : new MethodRef(ref.method(), true);
  }

  static Class<?> classForName(String name) {
    if (name == null || name.isBlank())
      return null;
    try {
      return Class.forName(name);
    } catch (Throwable ignored) {
      return null;
    }
  }

  static Object enumConstant(Class<?> cls, String name) {
    if (cls == null || !cls.isEnum() || name == null)
      return null;
    return Arrays.stream(cls.getEnumConstants())
        .filter(c -> c instanceof Enum<?> e && e.name().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  static String prettyEnumName(Object value) {
    if (!(value instanceof Enum<?> e))
      return "Unknown";
    String[] parts = e.name().toLowerCase(Locale.ROOT).split("_");
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
      if (part.isEmpty())
        continue;
      if (!sb.isEmpty())
        sb.append(' ');
      sb.append(Character.toUpperCase(part.charAt(0)))
          .append(part.substring(1));
    }
    return sb.toString();
  }

  static boolean asBool(Object value, boolean fallback) {
    return value instanceof Boolean b ? b : fallback;
  }

  static boolean readBooleanField(Object target, Field field,
                                  boolean fallback) {
    return asBool(readField(target, field), fallback);
  }

  static boolean invokeBoolean(MethodRef methodRef, Object target,
                               boolean fallback, Object... args) {
    return asBool(invoke(methodRef, target, args), fallback);
  }

  static Object invoke(MethodRef methodRef, Object target, Object... args) {
    if (methodRef == null)
      return null;
    return methodRef.isStatic() ? methodRef.invokeStatic(args)
                                : methodRef.invoke(target, args);
  }

  static boolean legacySettingsMenusEnabled() {
    Object option =
        staticField("wily.legacy.client.LegacyOptions", "legacySettingsMenus");
    if (option == null)
      return false;
    MethodRef getter = method(option.getClass(), "get");
    return getter != null && asBool(getter.invoke(option), false);
  }

  static Component widgetMessage(Object widget) {
    if (widget == null)
      return null;

    try {
      Object result = widget.getClass().getMethod("getMessage").invoke(widget);
      if (result instanceof Component c)
        return c;
    } catch (Exception ignored) {
    }

    Field f = field(widget.getClass(), "message");
    if (f == null)
      return null;
    Object val = readField(widget, f);

    if (val instanceof Component c)
      return c;

    if (val instanceof Function<?, ?> fn) {
      try {
        // Some Legacy4J widgets expose label suppliers instead of a concrete
        // message field.
        @SuppressWarnings("unchecked")
        Object res = ((Function<Boolean, Component>)fn).apply(Boolean.TRUE);
        if (res instanceof Component c)
          return c;
      } catch (Exception ignored) {
      }
    }

    return null;
  }

  static boolean hasMessageText(List<Object> renderables, String needle) {
    if (needle == null || needle.isBlank() || renderables == null)
      return false;
    String lo = needle.toLowerCase(Locale.ROOT);
    for (Object widget : renderables) {
      Component msg = widgetMessage(widget);
      if (msg != null) {
        String text = msg.getString();
        if (text != null && text.toLowerCase(Locale.ROOT).contains(lo))
          return true;
      }
    }
    return false;
  }

  static boolean hasAnyMessageText(List<Object> renderables,
                                   String... needles) {
    if (renderables == null || renderables.isEmpty() || needles == null)
      return false;

    // Normalize once to keep per-widget matching cheap while scanning full
    // option lists.
    String[] normalized = Arrays.stream(needles)
                              .filter(s -> s != null && !s.isBlank())
                              .map(s -> s.toLowerCase(Locale.ROOT))
                              .toArray(String[] ::new);
    if (normalized.length == 0)
      return false;

    for (Object widget : renderables) {
      Component msg = widgetMessage(widget);
      if (msg == null)
        continue;
      String text = msg.getString();
      if (text == null)
        continue;
      String lower = text.toLowerCase(Locale.ROOT);
      for (String needle : normalized) {
        if (lower.contains(needle))
          return true;
      }
    }
    return false;
  }

  record MethodRef(Method method, boolean isStatic) {
    Object invoke(Object target, Object... args) {
      try {
        return method.invoke(target, args);
      } catch (Exception ignored) {
        return null;
      }
    }

    Object invokeStatic(Object... args) {
      return isStatic ? invoke(null, args) : null;
    }
  }
}
