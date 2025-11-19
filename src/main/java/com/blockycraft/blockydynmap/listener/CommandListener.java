package com.blockycraft.blockydynmap.listener;

import com.blockycraft.blockydynmap.BlockyDynmap;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listener para comandos relevantes do BlockyClaim e BlockyGroups,
 * realizando ressincronização completa do Dynmap sempre que possível alteração.
 */
public class CommandListener implements Listener {
    private final BlockyDynmap plugin;

    public CommandListener(BlockyDynmap plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }
        String[] args = event.getMessage().toLowerCase().split(" ");
        String command = args[0].replace("/", "");

        // Sincronização nos comandos de claim
        if (command.equals("claim") || command.equals("grp")) {
            // Aguarda 5 ticks para garantir que o BlockyClaim já alterou/remoeu/adicionou claims
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    plugin.syncAllClaims();
                }
            }, 5L);
        }
    }
}
