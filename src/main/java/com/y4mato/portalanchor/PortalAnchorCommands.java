package com.y4mato.portalanchor;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class PortalAnchorCommands {
	private PortalAnchorCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("portalanchor")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(context -> showStatus(context.getSource()))
						.then(Commands.literal("status")
								.executes(context -> showStatus(context.getSource())))
						.then(Commands.literal("enabled")
								.then(Commands.argument("value", BoolArgumentType.bool())
										.executes(context -> setEnabled(
												context.getSource(),
												BoolArgumentType.getBool(context, "value")
										))))
						.then(Commands.literal("grace")
								.then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
										.executes(context -> setGraceSeconds(
												context.getSource(),
												IntegerArgumentType.getInteger(context, "seconds")
										))))
						.then(Commands.literal("reload")
								.executes(context -> reloadConfig(context.getSource())))
						.then(Commands.literal("save")
								.executes(context -> saveConfig(context.getSource())))
						.then(Commands.literal("reset-stats")
								.executes(context -> resetStats(context.getSource())))
		));
	}

	private static int showStatus(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("==== Portal Anchor ====").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		sendStatusLine(source, "State", PortalAnchor.isEnabled() ? "enabled" : "disabled", PortalAnchor.isEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED);
		sendStatusLine(source, "Grace window", PortalAnchor.getPortalDespawnGraceTicks() / 20 + "s (" + PortalAnchor.getPortalDespawnGraceTicks() + " ticks)", ChatFormatting.AQUA);
		sendStatusLine(source, "Mobs anchored", String.valueOf(PortalAnchor.getAnchoredMobCount()), ChatFormatting.YELLOW);
		sendStatusLine(source, "Despawn checks blocked", String.valueOf(PortalAnchor.getProtectedDespawnChecks()), ChatFormatting.LIGHT_PURPLE);
		sendStatusLine(source, "Last mob", PortalAnchor.getLastAnchoredMob(), ChatFormatting.WHITE);
		sendStatusLine(source, "Last route", PortalAnchor.getLastAnchorRoute(), ChatFormatting.WHITE);
		sendConfigStatus(source);
		sendCommandList(source);
		return PortalAnchor.getAnchoredMobCount();
	}

	private static int setEnabled(CommandSourceStack source, boolean enabled) {
		PortalAnchor.setEnabled(enabled);
		source.sendSuccess(() -> Component.literal("Portal Anchor is now ")
				.withStyle(ChatFormatting.GOLD)
				.append(Component.literal(enabled ? "enabled" : "disabled").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED))
				.append(Component.literal(" for this runtime. Use /portalanchor save to persist it.").withStyle(ChatFormatting.GRAY)), true);
		return enabled ? 1 : 0;
	}

	private static int setGraceSeconds(CommandSourceStack source, int seconds) {
		PortalAnchor.setPortalDespawnGraceSeconds(seconds);
		int appliedSeconds = PortalAnchor.getPortalDespawnGraceTicks() / 20;

		source.sendSuccess(() -> Component.literal("Portal Anchor grace window set to ")
				.withStyle(ChatFormatting.GOLD)
				.append(Component.literal(appliedSeconds + "s").withStyle(ChatFormatting.AQUA))
				.append(Component.literal(" for this runtime. Use /portalanchor save to persist it.").withStyle(ChatFormatting.GRAY)), true);
		return appliedSeconds;
	}

	private static int resetStats(CommandSourceStack source) {
		PortalAnchor.resetStats();
		source.sendSuccess(() -> Component.literal("Portal Anchor stats reset. Fresh slate.").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int reloadConfig(CommandSourceStack source) {
		PortalAnchor.reloadConfig();
		source.sendSuccess(() -> Component.literal("Portal Anchor config reloaded from disk.").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static int saveConfig(CommandSourceStack source) {
		PortalAnchor.saveConfig();
		source.sendSuccess(() -> Component.literal("Portal Anchor config saved from current runtime settings.").withStyle(ChatFormatting.GREEN), true);
		return 1;
	}

	private static void sendStatusLine(CommandSourceStack source, String label, String value, ChatFormatting valueColor) {
		source.sendSuccess(() -> Component.literal(" - ")
				.withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal(label + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(value).withStyle(valueColor)), false);
	}

	private static void sendConfigStatus(CommandSourceStack source) {
		boolean dirty = PortalAnchor.hasUnsavedConfigChanges();

		source.sendSuccess(() -> Component.literal(" - Config sync: ")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.literal(dirty ? "runtime differs from saved config" : "runtime matches saved config")
						.withStyle(dirty ? ChatFormatting.YELLOW : ChatFormatting.GREEN)), false);

		sendConfigDiffLine(
				source,
				"enabled",
				String.valueOf(PortalAnchor.isEnabled()),
				String.valueOf(PortalAnchor.getSavedEnabled()),
				PortalAnchor.isEnabled() == PortalAnchor.getSavedEnabled()
		);
		sendConfigDiffLine(
				source,
				"graceSeconds",
				String.valueOf(PortalAnchor.getPortalDespawnGraceTicks() / 20),
				String.valueOf(PortalAnchor.getSavedGraceSeconds()),
				PortalAnchor.getPortalDespawnGraceTicks() / 20 == PortalAnchor.getSavedGraceSeconds()
		);
	}

	private static void sendConfigDiffLine(CommandSourceStack source, String key, String runtimeValue, String savedValue, boolean matches) {
		source.sendSuccess(() -> Component.literal("   ")
				.withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal(key + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("runtime " + runtimeValue).withStyle(matches ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
				.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.literal("saved " + savedValue).withStyle(matches ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
	}

	private static void sendCommandList(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("Commands: ")
				.withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal("/portalanchor status").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("  "))
				.append(Component.literal("/portalanchor enabled <true|false>").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("  "))
				.append(Component.literal("/portalanchor grace <1-300>").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("  "))
				.append(Component.literal("/portalanchor save").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("  "))
				.append(Component.literal("/portalanchor reload").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("  "))
				.append(Component.literal("/portalanchor reset-stats").withStyle(ChatFormatting.GRAY)), false);
	}
}
