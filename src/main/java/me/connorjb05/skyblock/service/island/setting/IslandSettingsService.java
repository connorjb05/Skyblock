package me.connorjb05.skyblock.service.island.setting;

import me.connorjb05.skyblock.database.repository.IslandSettingsRepository;
import me.connorjb05.skyblock.model.island.Island;
import me.connorjb05.skyblock.model.island.settings.IslandSetting;

import java.util.concurrent.CompletableFuture;

public class IslandSettingsService {

    private final IslandSettingsRepository repository;

    public IslandSettingsService(IslandSettingsRepository repository) {
        this.repository = repository;
    }

    public boolean getValue(
            Island island,
            IslandSetting setting
    ) {
        return island.getSettingsData()
                .getValue(setting);
    }

    public CompletableFuture<Void> setValue(
            Island island,
            IslandSetting setting,
            boolean value
    ) {

        if (island == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Island cannot be null.")
            );
        }

        boolean currentValue = island
                .getSettingsData()
                .getValue(setting);

        if (currentValue == value) {
            return CompletableFuture.completedFuture(null);
        }

        return repository.setValue(
                        island.getIslandId(),
                        setting,
                        value
                )
                .thenRun(() -> {
                    island.getSettingsData()
                            .setValue(
                                    setting,
                                    value
                            );
                });
    }

    public CompletableFuture<Void> toggle(
            Island island,
            IslandSetting setting
    ) {

        boolean newValue = !getValue(
                island,
                setting
        );

        return setValue(
                island,
                setting,
                newValue
        );
    }

    public CompletableFuture<Void> reset(
            Island island,
            IslandSetting setting
    ) {
        return repository.delete(
                        island.getIslandId(),
                        setting
                )
                .thenRun(() -> island.getSettingsData().reset(setting));
    }

    public CompletableFuture<Void> resetAll(
            Island island
    ) {
        return repository.deleteAll(island.getIslandId())
                .thenRun(() -> island.getSettingsData().reset());
    }

}
