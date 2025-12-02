package com.bocktom.worldeditentityscanner;

import com.bocktom.worldeditentityscanner.util.Config;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldEditEntityScannerPlugin extends JavaPlugin {

	public static WorldEditEntityScannerPlugin plugin;

	@Override
	public void onEnable() {
		plugin = this;

		new Config(this);

		getCommand("/scanentities").setExecutor(new CountEntitiesCommand());
		getCommand("/scantileentities").setExecutor(new CountEntitiesCommand());
		getCommand("/scanall").setExecutor(new CountEntitiesCommand());
		getCommand("/scanstop").setExecutor(new CountEntitiesCommand());
	}

}
