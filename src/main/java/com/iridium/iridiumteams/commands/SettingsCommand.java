package com.iridium.iridiumteams.commands;

import com.iridium.iridiumcore.utils.StringUtils;
import com.iridium.iridiumteams.IridiumTeams;
import com.iridium.iridiumteams.Setting;
import com.iridium.iridiumteams.database.IridiumUser;
import com.iridium.iridiumteams.database.Team;
import com.iridium.iridiumteams.database.TeamSetting;
import com.iridium.iridiumteams.gui.SettingsGUI;
import lombok.NoArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor
public class SettingsCommand<T extends Team, U extends IridiumUser<T>> extends Command<T, U> {
    public SettingsCommand(List<String> args, String description, String syntax, String permission) {
        super(args, description, syntax, permission);
    }

    @Override
    public void execute(U user, T team, String[] args, IridiumTeams<T, U> iridiumTeams) {
        Player player = user.getPlayer();
        if (args.length == 0) {
            player.openInventory(new SettingsGUI<>(team, player.getOpenInventory().getTopInventory(), iridiumTeams).getInventory());
            return;
        } else if (args.length == 2) {
            String settingKey = args[0];
            for (Map.Entry<String, Setting> setting : iridiumTeams.getSettingsList().entrySet()) {
                if (!setting.getValue().getDisplayName().equalsIgnoreCase(settingKey)) continue;
                Optional<String> value = setting.getValue().getValues().stream().filter(s -> s.equalsIgnoreCase(args[1])).findFirst();

                if (!value.isPresent()) {
                    player.sendMessage(StringUtils.color(iridiumTeams.getMessages().invalidSettingValue
                            .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
                    ));
                    return;
                }

                TeamSetting teamSetting = iridiumTeams.getTeamManager().getTeamSetting(team, setting.getKey());
                teamSetting.setValue(value.get());
                player.sendMessage(StringUtils.color(iridiumTeams.getMessages().settingSet
                        .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
                        .replace("%setting%", setting.getValue().getDisplayName())
                        .replace("%value%", value.get())
                ));
                return;
            }
            player.sendMessage(StringUtils.color(iridiumTeams.getMessages().invalidSetting
                    .replace("%prefix%", iridiumTeams.getConfiguration().prefix)
            ));
            return;
        }
        player.sendMessage(StringUtils.color(syntax.replace("%prefix%", iridiumTeams.getConfiguration().prefix)));
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] args, IridiumTeams<T, U> iridiumTeams) {
        switch (args.length) {
            case 1:
                return iridiumTeams.getSettingsList().values().stream().map(Setting::getDisplayName).collect(Collectors.toList());
            case 2:
                for (Map.Entry<String, Setting> setting : iridiumTeams.getSettingsList().entrySet()) {
                    if (!setting.getValue().getDisplayName().equalsIgnoreCase(args[0])) continue;
                    return setting.getValue().getValues();
                }
            default:
                return Collections.emptyList();
        }
    }

}