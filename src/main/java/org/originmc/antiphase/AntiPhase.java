package org.originmc.antiphase;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AntiPhase extends JavaPlugin implements Listener {

    private static final String PERMISSION_PHASE = "antiphase.bypass";

    private List<Material> translucentMaterials = new ArrayList<>();

    @Override
    public void onEnable() {
        // Load settings
        saveDefaultConfig();

        for (String material : getConfig().getStringList("translucent-materials")) {
            translucentMaterials.add(Material.valueOf(material));
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info(getName() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info(getName() + " has been disabled!");
    }

    @EventHandler(ignoreCancelled = true)
    public void denyPhase(PlayerMoveEvent event) {
        // Do nothing if player is flying
        Player player = event.getPlayer();
        if (player.isFlying()) return;

        // Do nothing if player has permission
        if (player.hasPermission(PERMISSION_PHASE)) return;

        // Do nothing if player is going above sky limit
        Location t = event.getTo();
        if (t.getY() > 254) return;

        // Do nothing if player has not moved
        Location f = event.getFrom();
        double distance = f.distance(t);
        if (distance == 0.0D) return;

        // Deny movement if player has moved too far to prevent excessive lookups
        if (distance > 8.0D) {
            event.setTo(f.setDirection(t.getDirection()));
            return;
        }

        // Calculate all possible blocks the player has moved through
        int topBlockX = f.getBlockX() < t.getBlockX() ? t.getBlockX() : f.getBlockX();
        int bottomBlockX = f.getBlockX() > t.getBlockX() ? t.getBlockX() : f.getBlockX();

        int topBlockY = (f.getBlockY() < t.getBlockY() ? t.getBlockY() : f.getBlockY()) + 1;
        int bottomBlockY = f.getBlockY() > t.getBlockY() ? t.getBlockY() : f.getBlockY();
        if (player.isInsideVehicle()) bottomBlockY++;

        int topBlockZ = f.getBlockZ() < t.getBlockZ() ? t.getBlockZ() : f.getBlockZ();
        int bottomBlockZ = f.getBlockZ() > t.getBlockZ() ? t.getBlockZ() : f.getBlockZ();

        // Iterate through the outermost coordinates
        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                for (int y = bottomBlockY; y <= topBlockY; y++) {
                    // Do nothing if material is able to be moved through
                    if (translucentMaterials.contains(f.getWorld().getBlockAt(x, y, z).getType())) continue;

                    // Do nothing if player has walked over stairs
                    if (y == bottomBlockY && f.getBlockY() != t.getBlockY()) continue;

                    // Deny movement
                    event.setTo(f.setDirection(t.getDirection()));
                }
            }
        }
    }

}