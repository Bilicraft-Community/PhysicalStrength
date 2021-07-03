package com.bilicraft.physicalstrength;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class CheckRunnable extends BukkitRunnable implements Listener {
    private final PhysicalStrength plugin;
    private final int tickElytra;
    private final int tickSprint;
    private final int tickSwim;
    private final int cooldown;
    private final int recover;
    private final int strength;
    private final Component bossBarTitle = Component.text("体力").decoration(TextDecoration.BOLD, true).color(TextColor.color(239, 140, 27));
    private final Map<Player, BossBar> bossBarMap = new HashMap<>();
    private final int yellow;
    private final int red;

    public CheckRunnable(PhysicalStrength plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.tickSprint = plugin.getConfig().getInt("consume.sprinting");
        this.tickElytra = plugin.getConfig().getInt("consume.elytra");
        this.tickSwim = plugin.getConfig().getInt("consume.swim");
        this.cooldown = plugin.getConfig().getInt("cooldown");
        this.recover = plugin.getConfig().getInt("recover");
        this.strength = plugin.getConfig().getInt("strength");
        this.yellow = plugin.getConfig().getInt("color.yellow");
        this.red = plugin.getConfig().getInt("color.red");
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            StrengthData data = plugin.getStrengthData(player.getUniqueId());
            if (player.hasPermission("physicalstrength.bypass")) {
                return;
            }
            if (player.isSprinting()) {
                if (!data.tick(tickSprint)) {
                    player.setSprinting(false);
                    PotionEffect slowEffect = new PotionEffect(PotionEffectType.SLOW, 60, 2);
                    if (player.getActivePotionEffects().stream().noneMatch(effect -> effect.getType() == PotionEffectType.SLOW && effect.getAmplifier() >= slowEffect.getAmplifier() && effect.getDuration() >= slowEffect.getDuration())) {
                        player.addPotionEffect(slowEffect);
                    }
                }
            }
            if (player.isGliding()) {
                if (!data.tick(tickElytra)) {
                    player.setGliding(false);
                }
            }
            if (player.isSwimming()) {
                if (!data.tick(tickSwim)) {
                    player.setSwimming(false);
                    player.setRemainingAir(0);
                }
            }
            if (System.currentTimeMillis() - data.getLastConsume() > cooldown && player.isOnGround()) {
                if (data.getStrength() + recover > strength) {
                    data.setStrength(strength);
                } else {
                    data.setStrength(data.getStrength() + recover);
                }
            }
            updateStatus(player, data);
        }

    }

    public void unload() {
        bossBarMap.forEach(Audience::hideBossBar);
    }

    private void updateStatus(Player player, StrengthData data) {
        if (player.hasPermission("physicalstrength.bypass")) {
            return;
        }
        BossBar bossBar = bossBarMap.get(player);
        if (bossBar == null && data.getStrength() != this.strength) {
            bossBar = BossBar.bossBar(bossBarTitle, 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
            bossBarMap.put(player, bossBar);
            player.showBossBar(bossBar);
            return;
        }
        if (bossBar != null) {
            if (!bakeBossBar(bossBar, data)) {
                player.hideBossBar(bossBar);
                bossBarMap.remove(player);
            }else{
                player.showBossBar(bossBar);
            }
        }
    }

    private boolean bakeBossBar(BossBar bossBar, StrengthData data) {
        if (data.getStrength() == this.strength) {
            return false;
        }
        BossBar.Color color = BossBar.Color.GREEN;
        if (data.getStrength() <= yellow) {
            color = BossBar.Color.YELLOW;
        }
        if (data.getStrength() <= red) {
            color = BossBar.Color.RED;
        }
        bossBar.color(color);

        float percent = (float) data.getStrength() / (float) strength;

        bossBar.progress(percent);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerOpenElytra(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.hasPermission("physicalstrength.bypass")) {
                return;
            }
            if (event.isGliding()) {
                StrengthData data = plugin.getStrengthData(player.getUniqueId());
                if (!data.tick(tickElytra)) {
                    player.setGliding(false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerSprinting(PlayerToggleSprintEvent event) {
        if (event.isSprinting()) {
            StrengthData data = plugin.getStrengthData(event.getPlayer().getUniqueId());
            if (event.getPlayer().hasPermission("physicalstrength.bypass")) {
                return;
            }
            if (!data.tick(tickSprint)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void playerFallCauseNoStrength(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                Player player = (Player) event.getEntity();
                if (player.hasPermission("physicalstrength.bypass")) {
                    return;
                }
                StrengthData data = plugin.getStrengthData(player.getUniqueId());
                if (data.getStrength() <= 0) {
                    player.damage(100);
                }
            }
        }
    }
}
