package com.y4mato.portalanchor;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.minecraft.world.entity.Mob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.y4mato.portalanchor.access.PortalAnchorTracked;

public class PortalAnchor implements ModInitializer {
	public static final String MOD_ID = "portalanchor";
	public static final int PORTAL_DESPAWN_GRACE_TICKS = 20 * 30;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static int anchoredMobCount;

	@Override
	public void onInitialize() {
		PortalAnchorCommands.register();

		ServerEntityLevelChangeEvents.AFTER_ENTITY_CHANGE_LEVEL.register((originalEntity, newEntity, origin, destination) -> {
			if (newEntity instanceof Mob mob) {
				anchorMob(mob);
			}
		});

		LOGGER.info("Portal Anchor is ready.");
	}

	public static void anchorMob(Mob mob) {
		((PortalAnchorTracked) mob).portalanchor$setDespawnGraceTicks(PORTAL_DESPAWN_GRACE_TICKS);
		anchoredMobCount++;
		LOGGER.info("Anchored {} for {} ticks after dimension travel.", mob.getName().getString(), PORTAL_DESPAWN_GRACE_TICKS);
	}

	public static int getAnchoredMobCount() {
		return anchoredMobCount;
	}
}
