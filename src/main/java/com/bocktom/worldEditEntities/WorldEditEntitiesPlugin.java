package com.bocktom.worldEditEntities;

import com.bocktom.worldEditEntities.util.Config;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldEditEntitiesPlugin extends JavaPlugin {

	public static WorldEditEntitiesPlugin plugin;

	@Override
	public void onEnable() {
		plugin = this;

		new Config(this);

		getCommand("/countentities").setExecutor(new CountEntitiesCommand());
		getCommand("/counttileentities").setExecutor(new CountEntitiesCommand());
		getCommand("/countall").setExecutor(new CountEntitiesCommand());
		getCommand("/countstop").setExecutor(new CountEntitiesCommand());
	}

}
