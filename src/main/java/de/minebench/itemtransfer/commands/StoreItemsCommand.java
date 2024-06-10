package de.minebench.itemtransfer.commands;


/*
 * itemtransfer
 * Copyright (c) 2024 Max Lee aka Phoenix616 (max@themoep.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import de.minebench.itemtransfer.ItemTransfer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StoreItemsCommand implements CommandExecutor {

	private final ItemTransfer plugin;

	public StoreItemsCommand(ItemTransfer plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		List<Player> targets = new ArrayList<>();
		if (args.length > 0 && sender.hasPermission("itemtransfer.command.storeitems.others")) {
			plugin.getServer().selectEntities(sender, args[0]).forEach(entity -> {
				if (entity instanceof Player) {
					targets.add((Player) entity);
				}
			});
			if (targets.isEmpty()) {
				sender.sendMessage(plugin.getLang(sender, "player_not_found", "player", args[0]));
				return false;
			}
		} else if (sender instanceof Player player) {
			targets.add(player);
		} else {
			return false;
		}

		for (Player target : targets) {
			plugin.openStoreItemsConfirmGui(target);
		}
		sender.sendMessage(plugin.getLang(sender, "storeitems_gui_opened"));
		return true;
	}
}
