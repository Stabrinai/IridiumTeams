package com.iridium.iridiumteams.listeners;

import com.iridium.iridiumcore.utils.StringUtils;
import com.iridium.iridiumteams.IridiumTeams;
import com.iridium.iridiumteams.database.IridiumUser;
import com.iridium.iridiumteams.database.Team;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@AllArgsConstructor
public class PlayerJoinListener<T extends Team, U extends IridiumUser<T>> implements Listener {
    private final IridiumTeams<T, U> iridiumTeams;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        U user = iridiumTeams.getUserManager().getUser(player);
        user.setBypassing(false);
        user.initBukkitTask(iridiumTeams);

        // Update the internal username in case of name change
        user.setName(event.getPlayer().getName());


        if (player.isOp() && iridiumTeams.getConfiguration().patreonMessage) {
            player.getScheduler().runDelayed(iridiumTeams, (task) ->
                            player.sendMessage(StringUtils.color(iridiumTeams.getConfiguration().prefix + " &7Thanks for using " + iridiumTeams.getDescription().getName() + ", if you like the plugin, consider donating at " + iridiumTeams.getCommandManager().getColor() + "www.patreon.com/Peaches_MLG"))
                    , null, 5);
        }

        // This isnt great, but as this requires database operations, we can pre-run it async, otherwise it will have to be loaded sync. I need to recode/rethink this eventually but this should fix some lag caused by missions for now
        iridiumTeams.getTeamManager().getTeamViaID(user.getTeamID()).ifPresent(team -> Bukkit.getAsyncScheduler().runNow(iridiumTeams, task -> iridiumTeams.getMissionManager().generateMissionData(team)));
    }

}
