package com.y4mato.portalanchor;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import com.y4mato.portalanchor.access.PortalAnchorTracked;

public final class PortalAnchorCommands {
	private PortalAnchorCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("portalanchor")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("selftest").executes(context -> runSelfTest(context.getSource())))
						.then(Commands.literal("eventtest").executes(context -> runEventTest(context.getSource())))
						.then(Commands.literal("status").executes(context -> showStatus(context.getSource())))
		));
	}

	private static int runSelfTest(CommandSourceStack source) {
		ServerLevel level = source.getLevel();

		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			source.sendFailure(Component.literal("Portal Anchor self-test needs Easy, Normal, or Hard. Zombies are deleted on Peaceful before distance despawn logic runs."));
			return 0;
		}

		Vec3 sourcePosition = source.getPosition();
		double testDistance = EntityType.ZOMBIE.getCategory().getDespawnDistance() + 32.0D;
		Vec3 testPosition = sourcePosition.add(testDistance, 0.0D, 0.0D);

		Zombie vanillaZombie = createTestZombie(level, testPosition);
		Zombie anchoredZombie = createTestZombie(level, testPosition.add(0.0D, 0.0D, 2.0D));

		if (vanillaZombie == null || anchoredZombie == null) {
			source.sendFailure(Component.literal("Portal Anchor self-test could not create test zombies."));
			return 0;
		}

		vanillaZombie.checkDespawn();

		((PortalAnchorTracked) anchoredZombie).portalanchor$setDespawnGraceTicks(PortalAnchor.PORTAL_DESPAWN_GRACE_TICKS);
		anchoredZombie.checkDespawn();

		boolean vanillaDespawned = vanillaZombie.isRemoved();
		boolean anchoredSurvived = !anchoredZombie.isRemoved();

		if (!vanillaZombie.isRemoved()) {
			vanillaZombie.discard();
		}

		if (!anchoredZombie.isRemoved()) {
			anchoredZombie.discard();
		}

		if (vanillaDespawned && anchoredSurvived) {
			source.sendSuccess(() -> Component.literal("Portal Anchor self-test passed: vanilla far-away zombie despawned, anchored zombie survived the despawn check."), false);
			return 1;
		}

		source.sendFailure(Component.literal("Portal Anchor self-test failed: vanillaDespawned=" + vanillaDespawned + ", anchoredSurvived=" + anchoredSurvived + "."));
		return 0;
	}

	private static Zombie createTestZombie(ServerLevel level, Vec3 position) {
		Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);

		if (zombie != null) {
			zombie.setPos(position);
		}

		return zombie;
	}

	private static int runEventTest(CommandSourceStack source) {
		ServerLevel destination = source.getLevel();
		ServerLevel origin = source.getServer().getLevel(destination.dimension() == Level.NETHER ? Level.OVERWORLD : Level.NETHER);

		if (origin == null) {
			source.sendFailure(Component.literal("Portal Anchor event-test could not find another dimension to teleport from."));
			return 0;
		}

		if (destination.getDifficulty() == Difficulty.PEACEFUL) {
			source.sendFailure(Component.literal("Portal Anchor event-test needs Easy, Normal, or Hard."));
			return 0;
		}

		Zombie zombie = createTestZombie(origin, new Vec3(0.5D, 80.0D, 0.5D));

		if (zombie == null || !origin.addFreshEntity(zombie)) {
			source.sendFailure(Component.literal("Portal Anchor event-test could not create the source zombie."));
			return 0;
		}

		double testDistance = EntityType.ZOMBIE.getCategory().getDespawnDistance() + 32.0D;
		Vec3 destinationPosition = source.getPosition().add(testDistance, 0.0D, 0.0D);
		Entity teleported = zombie.teleport(new TeleportTransition(destination, destinationPosition, Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));

		if (!(teleported instanceof Mob teleportedMob)) {
			source.sendFailure(Component.literal("Portal Anchor event-test failed: teleport did not return a mob."));
			return 0;
		}

		int graceTicks = ((PortalAnchorTracked) teleportedMob).portalanchor$getDespawnGraceTicks();
		teleportedMob.checkDespawn();
		boolean survived = !teleportedMob.isRemoved();

		if (survived) {
			teleportedMob.discard();
		}

		if (graceTicks > 0 && survived) {
			source.sendSuccess(() -> Component.literal("Portal Anchor event-test passed: dimension-change mob got " + graceTicks + " grace ticks and survived despawn."), false);
			return 1;
		}

		source.sendFailure(Component.literal("Portal Anchor event-test failed: graceTicks=" + graceTicks + ", survived=" + survived + "."));
		return 0;
	}

	private static int showStatus(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("Portal Anchor has anchored " + PortalAnchor.getAnchoredMobCount() + " mob(s) since this world/server started."), false);
		return PortalAnchor.getAnchoredMobCount();
	}
}
