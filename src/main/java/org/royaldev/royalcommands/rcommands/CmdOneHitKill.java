package org.royaldev.royalcommands.rcommands;

import org.royaldev.royalcommands.MessageColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalcommands.configuration.PConfManager;
import org.royaldev.royalcommands.RUtils;
import org.royaldev.royalcommands.RoyalCommands;

public class CmdOneHitKill implements CommandExecutor {

    private RoyalCommands plugin;

    public CmdOneHitKill(RoyalCommands instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("onehitkill")) {
            if (!plugin.ah.isAuthorized(cs, "rcmds.onehitkill")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (args.length > 0) {
                Player t = plugin.getServer().getPlayer(args[0]);
                if (t == null || plugin.isVanished(t, cs)) {
                    OfflinePlayer op = plugin.getServer().getOfflinePlayer(args[0]);
                    PConfManager pcm = PConfManager.getPConfManager(op);
                    if (!pcm.exists()) {
                        cs.sendMessage(MessageColor.NEGATIVE + "That player does not exist!");
                        return true;
                    }
                    Boolean ohk = pcm.getBoolean("ohk");
                    if (ohk == null || !ohk) {
                        pcm.set("ohk", true);
                        cs.sendMessage(MessageColor.POSITIVE + "You have enabled onehitkill mode for " + MessageColor.NEUTRAL + op.getName() + MessageColor.POSITIVE + ".");
                        return true;
                    }
                    pcm.set("ohk", false);
                    cs.sendMessage(MessageColor.POSITIVE + "You have disabled onehitkill mode for " + MessageColor.NEUTRAL + op.getName() + MessageColor.POSITIVE + ".");
                    return true;
                }
                Player p = plugin.getServer().getPlayer(args[0]);
                PConfManager pcm = PConfManager.getPConfManager(p);
                if (!pcm.exists()) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That player does not exist!");
                    return true;
                }
                Boolean ohk = pcm.getBoolean("ohk");
                if (ohk == null || !ohk) {
                    pcm.set("ohk", true);
                    cs.sendMessage(MessageColor.POSITIVE + "You have enabled onehitkill mode for " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + ".");
                    p.sendMessage(MessageColor.POSITIVE + "The player " + MessageColor.NEUTRAL + cs.getName() + MessageColor.POSITIVE + " has enabled onehitkill for you.");
                    return true;
                }
                pcm.set("ohk", false);
                cs.sendMessage(MessageColor.POSITIVE + "You have disabled onehitkill mode for " + MessageColor.NEUTRAL + p.getName() + MessageColor.POSITIVE + ".");
                p.sendMessage(MessageColor.NEGATIVE + "The player " + MessageColor.NEUTRAL + cs.getName() + MessageColor.NEGATIVE + " has disabled your onehitkill.");
                return true;
            }
            if (args.length < 1) {
                if (!(cs instanceof Player)) {
                    cs.sendMessage(cmd.getDescription());
                    return false;
                }
                Player p = (Player) cs;
                PConfManager pcm = PConfManager.getPConfManager(p);
                Boolean ohk = pcm.getBoolean("ohk");
                if (ohk == null || !ohk) {
                    pcm.set("ohk", true);
                    p.sendMessage(MessageColor.POSITIVE + "You have enabled onehitkill for yourself.");
                    return true;
                }
                pcm.set("ohk", false);
                p.sendMessage(MessageColor.POSITIVE + "You have disabled onehitkill for yourself.");
                return true;
            }
        }
        return false;
    }

}
