package com.bilicraft.physicalstrength;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PhysicalStrength extends JavaPlugin implements Listener {
    private final Map<UUID, StrengthData> strength = new HashMap<>();
    private CheckRunnable runnable;

    public StrengthData getStrengthData(UUID uuid){
        if(!strength.containsKey(uuid)){
            strength.put(uuid, new StrengthData(getConfig().getInt("strength"),System.currentTimeMillis()));
        }
        return strength.get(uuid);
    }


    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        this.runnable = new CheckRunnable(this);
        Bukkit.getPluginManager().registerEvents(this,this);
        runnable.runTaskTimer(this,0,1);
        getLogger().info("已启动");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        HandlerList.unregisterAll((Plugin)this);
        Bukkit.getScheduler().cancelTasks(this);
        runnable.unload();
    }
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event){
        strength.get(event.getPlayer().getUniqueId()).setStrength(getConfig().getInt("strength"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        strength.remove(event.getPlayer().getUniqueId());
    }

}
