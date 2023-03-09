package rip.diamond.practice.match.task;

import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.EdenItems;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.MatchTaskTicker;
import rip.diamond.practice.profile.PlayerProfile;

import java.util.Objects;

public class MatchResetTask extends MatchTaskTicker {
    private final Eden plugin = Eden.INSTANCE;
    private final Match match;

    public MatchResetTask(Match match) {
        super(1, Eden.INSTANCE.getConfigFile().getInt("match.end-duration"), false, match);
        this.match = match;
    }

    @Override
    public void onRun() {
        if (getTicks() <= 0) {
            cancel();

            match.clearEntities(true);
            match.getMatchPlayers().stream().filter(player -> Objects.nonNull(player) && player.isOnline())
                    .filter(player -> PlayerProfile.get(player).getMatch() == match) //This is to prevent player is in another match because of the requeue item
                    .forEach(player -> plugin.getLobbyManager().sendToSpawnAndReset(player));
            match.getSpectators().forEach(match::leaveSpectate);
            match.getTasks().forEach(BukkitRunnable::cancel);
            match.getArenaDetail().restoreChunk();
            match.getArenaDetail().setUsing(false);
            Match.getMatches().remove(match.getUuid());
        }
    }

    @Override
    public void preRun() {
        //Cancel MatchClearBlockTask first, to save performance
        match.getTasks().stream().filter(taskTicker -> taskTicker instanceof MatchClearBlockTask).forEach(BukkitRunnable::cancel);

        //Give 'Play Again' item like Minemen Club
        if (plugin.getConfigFile().getBoolean("match.allow-requeue")) {
            match.getMatchPlayers().forEach(player ->  EdenItems.giveItem(player, EdenItems.MATCH_REQUEUE));
        }
    }

    @Override
    public TickType getTickType() {
        return TickType.COUNT_DOWN;
    }

    @Override
    public int getStartTick() {
        return 1;
    }
}
