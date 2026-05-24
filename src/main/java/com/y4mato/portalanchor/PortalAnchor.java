package com.y4mato.portalanchor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.y4mato.portalanchor.access.PortalAnchorTracked;

public class PortalAnchor implements ModInitializer {
	public static final String MOD_ID = "portalanchor";
	public static final int DEFAULT_PORTAL_DESPAWN_GRACE_TICKS = 20 * 30;
	public static final int MIN_PORTAL_DESPAWN_GRACE_TICKS = 20;
	public static final int MAX_PORTAL_DESPAWN_GRACE_TICKS = 20 * 300;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static PortalAnchorConfig config;
	private static boolean enabled = true;
	private static int portalDespawnGraceTicks = DEFAULT_PORTAL_DESPAWN_GRACE_TICKS;
	private static int anchoredMobCount;
	private static int protectedDespawnChecks;
	private static String lastAnchoredMob = "none yet";
	private static String lastAnchorRoute = "none yet";

	@Override
	public void onInitialize() {
		config = PortalAnchorConfig.load();
		applyConfig(config);
		PortalAnchorCommands.register();

		ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL.register((originalEntity, newEntity, origin, destination) -> {
			if (newEntity instanceof Mob mob) {
				// Non-player entities are recreated when they change dimensions. The new copy is the one
				// vanilla may instantly despawn if every player in the destination is too far away.
				anchorMob(mob, origin, destination);
			}
		});

		LOGGER.info("Portal Anchor is ready. Config: {}", config.getPath());
	}

	public static void anchorMob(Mob mob, ServerLevel origin, ServerLevel destination) {
		if (!enabled) {
			return;
		}

		((PortalAnchorTracked) mob).portalanchor$setDespawnGraceTicks(portalDespawnGraceTicks);
		anchoredMobCount++;
		lastAnchoredMob = mob.getName().getString();
		lastAnchorRoute = origin.dimension().identifier() + " -> " + destination.dimension().identifier();
		LOGGER.debug("Anchored {} for {} ticks after dimension travel.", lastAnchoredMob, portalDespawnGraceTicks);
	}

	public static int getAnchoredMobCount() {
		return anchoredMobCount;
	}

	public static void recordProtectedDespawnCheck() {
		protectedDespawnChecks++;
	}

	public static int getProtectedDespawnChecks() {
		return protectedDespawnChecks;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		PortalAnchor.enabled = enabled;
	}

	public static int getPortalDespawnGraceTicks() {
		return portalDespawnGraceTicks;
	}

	public static void setPortalDespawnGraceSeconds(int seconds) {
		int ticks = seconds * 20;
		portalDespawnGraceTicks = Math.clamp(ticks, MIN_PORTAL_DESPAWN_GRACE_TICKS, MAX_PORTAL_DESPAWN_GRACE_TICKS);
	}

	public static String getLastAnchoredMob() {
		return lastAnchoredMob;
	}

	public static String getLastAnchorRoute() {
		return lastAnchorRoute;
	}

	public static void resetStats() {
		anchoredMobCount = 0;
		protectedDespawnChecks = 0;
		lastAnchoredMob = "none yet";
		lastAnchorRoute = "none yet";
	}

	public static void reloadConfig() {
		config = PortalAnchorConfig.load();
		applyConfig(config);
	}

	public static void saveConfig() {
		config.setEnabled(enabled);
		config.setGraceSeconds(portalDespawnGraceTicks / 20);
		config.save();
	}

	private static void applyConfig(PortalAnchorConfig config) {
		enabled = config.isEnabled();
		portalDespawnGraceTicks = Math.clamp(config.getGraceSeconds() * 20, MIN_PORTAL_DESPAWN_GRACE_TICKS, MAX_PORTAL_DESPAWN_GRACE_TICKS);
	}

	public static boolean getSavedEnabled() {
		return config.isEnabled();
	}

	public static int getSavedGraceSeconds() {
		return config.getGraceSeconds();
	}

	public static boolean hasUnsavedConfigChanges() {
		return enabled != config.isEnabled() || portalDespawnGraceTicks / 20 != config.getGraceSeconds();
	}
}
