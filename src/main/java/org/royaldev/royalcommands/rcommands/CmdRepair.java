package org.royaldev.royalcommands.rcommands;

import org.royaldev.royalcommands.MessageColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.royaldev.royalcommands.RUtils;
import org.royaldev.royalcommands.RoyalCommands;

public class CmdRepair implements CommandExecutor {

    private RoyalCommands plugin;

    public CmdRepair(RoyalCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("repair")) {
            if (!plugin.ah.isAuthorized(cs, "rcmds.repair")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(MessageColor.NEGATIVE + "This command is only available to players!");
                return true;
            }
            if (args.length < 1) {
                Player p = (Player) cs;
                ItemStack hand = p.getItemInHand();
                if (hand.getTypeId() == 0) {
                    cs.sendMessage(MessageColor.NEGATIVE + "You can't repair air!");
                    return true;
                }
                if (hand.getDurability() == (short) 0) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That doesn't need to be repaired!");
                    return true;
                }
                hand.setDurability((short) 0);
                cs.sendMessage(MessageColor.POSITIVE + "Fixed your " + MessageColor.NEUTRAL + RUtils.getItemName(hand) + MessageColor.POSITIVE + ".");
                return true;
            }
            if (args.length > 0) {
                Player p = (Player) cs;
                ItemStack[] pInv = p.getInventory().getContents();
                String items = "";
                for (ItemStack aPInv : pInv) {
                    if (aPInv != null && aPInv.getTypeId() != 0 && aPInv.getDurability() != (short) 0) {
                        aPInv.setDurability((short) 0);
                        if (items.equals("")) {
                            items = items.concat(RUtils.getItemName(aPInv));
                        } else {
                            items = items.concat(", " + RUtils.getItemName(aPInv));
                        }
                    }
                }
                if (!items.equals("")) {
                    cs.sendMessage(MessageColor.POSITIVE + "Fixed: " + MessageColor.NEUTRAL + items + MessageColor.POSITIVE + ".");
                    return true;
                }
                cs.sendMessage(MessageColor.NEGATIVE + "You have nothing to repair!");
                return true;
            }
        }
        return false;
    }

}
