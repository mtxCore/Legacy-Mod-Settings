package com.mtxcore.legacymodsettings;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

final class LmsDebugCommand {

  private LmsDebugCommand() {}

  static void register() {
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess)
            -> dispatcher.register(literal("lms-debug")
                                       .executes(ctx -> status(ctx.getSource()))
                                       .then(literal("on").executes(
                                           ctx -> set(ctx.getSource(), true)))
                                       .then(literal("off").executes(
                                           ctx -> set(ctx.getSource(), false)))
                                       .then(literal("status").executes(
                                           ctx -> status(ctx.getSource())))));
  }

  private static int set(FabricClientCommandSource source, boolean enabled) {
    CompatDebug.setEnabled(enabled);
    source.sendFeedback(Component.literal("[LMS] Debug logging: " +
                                          (enabled ? "§aON§r" : "§cOFF§r")));
    CompatDebug.log("Debug logging toggled {}", enabled ? "ON" : "OFF");
    return 1;
  }

  private static int status(FabricClientCommandSource source) {
    boolean enabled = CompatDebug.enabled();
    source.sendFeedback(Component.literal("[LMS] Debug logging is " +
                                          (enabled ? "§aON§r" : "§cOFF§r")));
    return 1;
  }
}
