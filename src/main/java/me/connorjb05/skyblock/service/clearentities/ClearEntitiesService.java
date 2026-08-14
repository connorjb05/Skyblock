package me.connorjb05.skyblock.service.clearentities;

import me.connorjb05.skyblock.Skyblock;
import me.connorjb05.skyblock.service.Service;
import me.connorjb05.skyblock.util.Broadcast;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;

public class ClearEntitiesService implements Service {

    private static final int TASK_INTERVAL_SECONDS = 10;
    private int CLEAR_INTERVAL_SECONDS;

    private BukkitTask clearTask = null;

    private int secondsRemaining;

    @Override
    public void onEnable(Skyblock plugin) {

        if (!plugin.getConfigService()
                .getClearEntitiesSettings()
                .enabled()) {
            return;
        }

        CLEAR_INTERVAL_SECONDS = plugin.getConfigService()
                .getClearEntitiesSettings()
                .interval();

        secondsRemaining = CLEAR_INTERVAL_SECONDS;

        clearTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> tick(plugin),
                0L,
                TASK_INTERVAL_SECONDS * 20L
        );

    }

    @Override
    public void onDisable(Skyblock plugin) {
        if (clearTask != null) {
            clearTask.cancel();
            clearTask = null;
        }
    }

    private void tick(Skyblock plugin) {

        secondsRemaining -= TASK_INTERVAL_SECONDS;

        if (secondsRemaining == 30 || secondsRemaining == 10) {
            Broadcast.send(
                    ClearEntitiesStatus.WARNING.getMessage(),
                    Placeholder.parsed(
                            "seconds",
                            String.valueOf(secondsRemaining)
                    )
            );
            return;
        }

        if (secondsRemaining > 0) {
            return;
        }

        clearEntities(plugin);

        secondsRemaining = CLEAR_INTERVAL_SECONDS;
    }

    private void clearEntities(Skyblock plugin) {

        int removedEntities = 0;

        for (World world : Bukkit.getWorlds()) {

            for (Item item : world.getEntitiesByClass(Item.class)) {

                if (!item.isValid()) {
                    continue;
                }

                item.remove();
                removedEntities++;
            }

        }

        Broadcast.send(
                ClearEntitiesStatus.COMPLETED.getMessage(),
                Placeholder.parsed(
                        "entities",
                        String.valueOf(removedEntities)
                )
        );
    }
}
