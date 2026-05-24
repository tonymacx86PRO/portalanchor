package com.y4mato.portalanchor.mixin;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.y4mato.portalanchor.PortalAnchor;
import com.y4mato.portalanchor.access.PortalAnchorTracked;

@Mixin(Mob.class)
public class MobMixin implements PortalAnchorTracked {
	@Unique
	private int portalanchor$despawnGraceTicks;

	@Override
	public void portalanchor$setDespawnGraceTicks(int ticks) {
		this.portalanchor$despawnGraceTicks = Math.max(this.portalanchor$despawnGraceTicks, ticks);
	}

	@Override
	public int portalanchor$getDespawnGraceTicks() {
		return this.portalanchor$despawnGraceTicks;
	}

	@Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
	private void portalanchor$skipFarAwayDespawnDuringPortalGrace(CallbackInfo info) {
		if (!PortalAnchor.isEnabled()) {
			return;
		}

		if (this.portalanchor$despawnGraceTicks <= 0) {
			return;
		}

		Mob mob = (Mob) (Object) this;

		// Peaceful difficulty is a different vanilla cleanup path. Portal Anchor is only about
		// distance despawn after dimension travel, so don't keep hostile mobs alive on Peaceful.
		if (mob.level().getDifficulty() == Difficulty.PEACEFUL && !mob.getType().isAllowedInPeaceful()) {
			return;
		}

		this.portalanchor$despawnGraceTicks--;
		PortalAnchor.recordProtectedDespawnCheck();
		info.cancel();
	}
}
