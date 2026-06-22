package net.renzoboy.inarisrestart;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InarisRestart implements ModInitializer {

	public static final String MOD_ID = "inaris-restart";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		// Register the /restart and /cancelrestart commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				RestartCommand.register(dispatcher));

		// When a player joins mid-countdown, give them the boss bar immediately
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				RestartManager.get(server).addPlayer(handler.player));

		LOGGER.info("Inaris Restart initialised.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}