package pt.flawless.ftab;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pt.flawless.fapi.enums.EMessageTypeEnum;
import pt.flawless.fapi.logs.FConsoleLogger;
import pt.flawless.fapi.permissions.Permissions;
import pt.flawless.fapi.ranks.Ranks;

public class FTab extends JavaPlugin {
    private final FConsoleLogger consoleLogger = new FConsoleLogger(this.getName());
    private TabService service;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            TabConfig config = TabConfig.from(getConfig()); // lança IllegalArgumentException com mais de 99 regras
            config.plan().clippedWarnings().forEach(tag -> consoleLogger.sendMessage(
                    "aviso: tag cortada a 16 caracteres (limite do 1.8): " + tag, EMessageTypeEnum.INFO));
            service = new TabService(this, config.plan(), Permissions.get(), consoleLogger,
                    getServer().getPluginManager().getPlugin("fRankup") != null ? Ranks.get() : null);
            service.createTeams();
            TabHeaderFooter headerFooter = new TabHeaderFooter(config.header(), config.footer());
            getServer().getPluginManager().registerEvents(new TabListener(service, headerFooter), this);
            long period = config.updateSeconds() * 20L;
            getServer().getScheduler().runTaskTimer(this, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) service.apply(p);
            }, period, period);
            for (Player p : Bukkit.getOnlinePlayers()) { // /reload
                service.apply(p);
                headerFooter.send(p);
            }
        } catch (RuntimeException e) {
            consoleLogger.sendMessage("não foi possível iniciar o tab: " + e.getMessage(), EMessageTypeEnum.ERROR);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        consoleLogger.sendEnablePluginMessage();
    }

    @Override
    public void onDisable() {
        if (service != null) {
            service.shutdown();
            consoleLogger.sendDisablePluginMessage();
        }
    }
}
