package com.blackboxpro.testcellbcbridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TestCellBcBridgeHelperPlugin extends JavaPlugin implements CommandExecutor {

    private static final String CHANNEL = "BungeeCord";

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        PluginCommand command = getCommand("bbswitch");
        if (command != null) {
            command.setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (args.length != 1 || args[0].trim().isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /bbswitch <server>");
            return true;
        }

        Player player = (Player) sender;
        String target = args[0].trim();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(target);
        player.sendPluginMessage(this, CHANNEL, out.toByteArray());
        player.sendMessage(ChatColor.GREEN + "Switching to " + target + "...");
        return true;
    }
}
