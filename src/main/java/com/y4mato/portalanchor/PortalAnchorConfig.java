package com.y4mato.portalanchor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class PortalAnchorConfig {
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(PortalAnchor.MOD_ID + ".json");

	private boolean enabled = true;
	private int graceSeconds = PortalAnchor.DEFAULT_PORTAL_DESPAWN_GRACE_TICKS / 20;

	private PortalAnchorConfig() {
	}

	public static PortalAnchorConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			PortalAnchorConfig config = new PortalAnchorConfig();
			config.save();
			return config;
		}

		try {
			String json = Files.readString(CONFIG_PATH);
			PortalAnchorConfig config = GSON.fromJson(json, PortalAnchorConfig.class);

			if (config == null) {
				config = new PortalAnchorConfig();
			}

			config.sanitize();
			return config;
		} catch (Exception exception) {
			PortalAnchor.LOGGER.warn("Could not read {}, using defaults for this run.", CONFIG_PATH, exception);
			return new PortalAnchorConfig();
		}
	}

	public void save() {
		sanitize();

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(this));
		} catch (IOException exception) {
			// Config saves are nice to have, not a reason to crash a running server.
			PortalAnchor.LOGGER.warn("Could not save {}.", CONFIG_PATH, exception);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getGraceSeconds() {
		return graceSeconds;
	}

	public void setGraceSeconds(int graceSeconds) {
		this.graceSeconds = graceSeconds;
		sanitize();
	}

	public Path getPath() {
		return CONFIG_PATH;
	}

	private void sanitize() {
		int minSeconds = PortalAnchor.MIN_PORTAL_DESPAWN_GRACE_TICKS / 20;
		int maxSeconds = PortalAnchor.MAX_PORTAL_DESPAWN_GRACE_TICKS / 20;
		graceSeconds = Math.clamp(graceSeconds, minSeconds, maxSeconds);
	}
}
