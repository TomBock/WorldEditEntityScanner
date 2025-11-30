package com.bocktom.worldEditEntities;

import org.bukkit.plugin.java.JavaPlugin;

public final class WorldEditEntitiesPlugin extends JavaPlugin {

	@Override
	public void onEnable() {

		getCommand("/countentities").setExecutor(new CountEntitiesCommand());
	}

}
