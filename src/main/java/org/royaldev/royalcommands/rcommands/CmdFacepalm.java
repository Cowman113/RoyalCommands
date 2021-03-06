package org.royaldev.royalcommands.rcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalcommands.MessageColor;
import org.royaldev.royalcommands.RUtils;
import org.royaldev.royalcommands.RoyalCommands;

public class CmdFacepalm implements CommandExecutor {

    private RoyalCommands plugin;

    public CmdFacepalm(RoyalCommands instance) {
        plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("facepalm")) {
            if (!plugin.ah.isAuthorized(cs, "rcmds.facepalm")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (args.length < 1) {
                plugin.getServer().broadcastMessage(ChatColor.YELLOW + cs.getName() + ChatColor.AQUA + " has facepalmed.");
                return true;
            }
            Player victim = plugin.getServer().getPlayer(args[0]);
            if (victim == null || plugin.isVanished(victim, cs)) {
                cs.sendMessage(MessageColor.NEGATIVE + "That player is not online!");
                return true;
            }
            if (plugin.ah.isAuthorized(victim, "rcmds.exempt.facepalm")) {
                cs.sendMessage(MessageColor.NEGATIVE + "You cannot facepalm at that player!");
                return true;
            }
            plugin.getServer().broadcastMessage(ChatColor.YELLOW + cs.getName() + ChatColor.AQUA + " has facepalmed at " + ChatColor.YELLOW + victim.getName() + ChatColor.AQUA + ".");
            return true;
        }
        return false;
    }

}
