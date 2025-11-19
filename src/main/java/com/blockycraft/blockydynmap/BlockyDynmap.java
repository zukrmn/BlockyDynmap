package com.blockycraft.blockydynmap;

import com.blockycraft.blockyclaim.BlockyClaim;
import com.blockycraft.blockyclaim.managers.ClaimManager;
import com.blockycraft.blockygroups.BlockyGroups;
import com.blockycraft.blockydynmap.listener.CommandListener;
import com.blockycraft.blockydynmap.manager.DynmapManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockyDynmap extends JavaPlugin {
    private DynmapManager dynmapManager;
    private BlockyClaim blockyClaim;
    private BlockyGroups blockyGroups;
    private boolean initialized = false;
    private int checkTaskId = -1;

    @Override
    public void onEnable() {
        // Verifica dependências de plugins
        this.checkTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (setupDependencies()) {
                    getServer().getScheduler().cancelTask(checkTaskId);
                    initialize();
                }
            }
        }, 0L, 20L);
    }

    private void initialize() {
        if (initialized) return;
        this.dynmapManager = new DynmapManager(this);
        syncAllClaims(); // Sincroniza todos os markers corretamente ao ligar

        getServer().getPluginManager().registerEvents(new CommandListener(this), this);

        System.out.println("[BlockyDynmap] Dependencias encontradas. Plugin ativado e integrado com sucesso!");
        initialized = true;
    }

    @Override
    public void onDisable() {
        if (dynmapManager != null) {
            dynmapManager.cleanup();
        }
        System.out.println("[BlockyDynmap] Plugin desativado.");
    }

    private boolean setupDependencies() {
        PluginManager pm = getServer().getPluginManager();
        Plugin dynmapPlugin = pm.getPlugin("dynmap");
        if (dynmapPlugin == null || !dynmapPlugin.isEnabled()) {
            System.out.println("[BlockyDynmap] Aguardando o plugin Dynmap ser ativado...");
            return false;
        }
        Plugin claimPlugin = pm.getPlugin("BlockyClaim");
        if (claimPlugin == null || !(claimPlugin instanceof BlockyClaim)) {
            System.out.println("[BlockyDynmap] ERRO CRITICO: BlockyClaim nao encontrado.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        this.blockyClaim = (BlockyClaim) claimPlugin;
        Plugin groupsPlugin = pm.getPlugin("BlockyGroups");
        if (groupsPlugin == null || !(groupsPlugin instanceof BlockyGroups)) {
            System.out.println("[BlockyDynmap] ERRO CRITICO: BlockyGroups nao encontrado.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        this.blockyGroups = (BlockyGroups) groupsPlugin;
        return true;
    }

    /**
     * Remove todos os markers existentes e desenha apenas os markers atuais das claims ainda existentes.
     * Chame este método:
     * - No boot
     * - No reload
     * - Em listeners de alteração de claims (CommandListener etc)
     */
    public void syncAllClaims() {
        ClaimManager claimManager = blockyClaim.getClaimManager();
        if (claimManager != null && dynmapManager != null) {
            System.out.println("[BlockyDynmap] Iniciando sincronizacao completa de todos os claims existentes...");
            dynmapManager.syncMarkers(claimManager.getAllClaims());
            System.out.println("[BlockyDynmap] Sincronizacao completa finalizada.");
        }
    }

    public DynmapManager getDynmapManager() {
        return dynmapManager;
    }
    public BlockyClaim getBlockyClaim() {
        return blockyClaim;
    }
    public BlockyGroups getBlockyGroups() {
        return blockyGroups;
    }
}
