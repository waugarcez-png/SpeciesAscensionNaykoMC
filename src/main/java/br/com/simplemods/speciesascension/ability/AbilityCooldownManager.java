package br.com.simplemods.speciesascension.ability;

import br.com.simplemods.speciesascension.race.Race;
import br.com.simplemods.speciesascension.upgrade.UpgradeEffects;
import br.com.simplemods.speciesascension.util.ScoreboardHelper;
import br.com.simplemods.speciesascension.util.ServerCommand;
import net.minecraft.server.level.ServerPlayer;

/**
 * Controla recargas imediatas e recargas que só começam quando o efeito termina.
 */
public final class AbilityCooldownManager {
    public static final String[] SLOTS = {"z", "x", "v"};

    private AbilityCooldownManager() {
    }

    public static void startImmediate(ServerPlayer player, Race race, String slot) {
        String normalized = AbilityDefinition.normalize(slot);
        int cooldown = UpgradeEffects.cooldown(player, race.id(), race.abilities().cooldownFor(normalized));
        ServerCommand.setScore(player, race.abilities().objectiveFor(normalized), cooldown);
        ServerCommand.setScore(player, race.abilities().activeObjectiveFor(normalized), 0);
        ServerCommand.setScore(player, race.abilities().queuedObjectiveFor(normalized), 0);
        ServerCommand.setScore(player, "sa_ability_result", 1);
    }

    public static void startAfterEffect(ServerPlayer player, Race race, String slot, int activeTicks) {
        String normalized = AbilityDefinition.normalize(slot);
        ServerCommand.setScore(player, race.abilities().objectiveFor(normalized), 0);
        ServerCommand.setScore(player, race.abilities().activeObjectiveFor(normalized), Math.max(1, activeTicks));
        ServerCommand.setScore(player, race.abilities().queuedObjectiveFor(normalized), 1);
        ServerCommand.setScore(player, "sa_ability_result", 1);
    }

    public static boolean isBusy(ServerPlayer player, Race race, String slot) {
        String normalized = AbilityDefinition.normalize(slot);
        return ScoreboardHelper.getScore(player, race.abilities().objectiveFor(normalized), 0) > 0
                || ScoreboardHelper.getScore(player, race.abilities().activeObjectiveFor(normalized), 0) > 0
                || ScoreboardHelper.getScore(player, race.abilities().queuedObjectiveFor(normalized), 0) > 0;
    }

    public static int activeTicks(ServerPlayer player, Race race, String slot) {
        return Math.max(0, ScoreboardHelper.getScore(
                player,
                race.abilities().activeObjectiveFor(AbilityDefinition.normalize(slot)),
                0
        ));
    }

    public static void setActiveTicks(ServerPlayer player, Race race, String slot, int ticks) {
        ServerCommand.setScore(
                player,
                race.abilities().activeObjectiveFor(AbilityDefinition.normalize(slot)),
                Math.max(0, ticks)
        );
    }

    public static void tick(ServerPlayer player, Race race) {
        for (String slot : SLOTS) {
            String cooldownObjective = race.abilities().objectiveFor(slot);
            String activeObjective = race.abilities().activeObjectiveFor(slot);
            String queuedObjective = race.abilities().queuedObjectiveFor(slot);

            int active = ScoreboardHelper.getScore(player, activeObjective, 0);
            int queued = ScoreboardHelper.getScore(player, queuedObjective, 0);
            if (active > 0) {
                active--;
                ServerCommand.setScore(player, activeObjective, active);
                if (active == 0 && queued > 0) {
                    int cooldown = UpgradeEffects.cooldown(player, race.id(), race.abilities().cooldownFor(slot));
                    ServerCommand.setScore(player, cooldownObjective, cooldown);
                    ServerCommand.setScore(player, queuedObjective, 0);
                    continue;
                }
                // Janelas de ativação sem efeito persistente, como Emboscada, mantêm a recarga imediata correndo.
                if (queued > 0) {
                    continue;
                }
            }

            int cooldown = ScoreboardHelper.getScore(player, cooldownObjective, 0);
            if (cooldown > 0) {
                ServerCommand.setScore(player, cooldownObjective, cooldown - 1);
            }
        }
    }
}
