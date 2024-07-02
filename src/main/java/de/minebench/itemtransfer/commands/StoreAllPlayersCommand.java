package de.minebench.itemtransfer.commands;

/*
 * ItemTransfer
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

import com.lishid.openinv.OpenInv;
import de.minebench.itemtransfer.ItemTransfer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class StoreAllPlayersCommand implements CommandExecutor {

	private final ItemTransfer plugin;
	private final OpenInv openInv;

	public StoreAllPlayersCommand(ItemTransfer plugin) {
		this.plugin = plugin;
		openInv = OpenInv.getPlugin(OpenInv.class);
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		ArrayDeque<OfflinePlayer> players = new ArrayDeque<>();
		Collections.addAll(players, plugin.getServer().getOfflinePlayers());
		sender.sendMessage(plugin.getLang(sender, "storeallplayers.started", "count", String.valueOf(players.size())));

		if (players.isEmpty()) {
			sender.sendMessage(plugin.getLang(sender, "storeallplayers.no_players"));
			return true;
		}

		CompletableFuture<Integer> future = new CompletableFuture<>();
		future.thenAccept(count -> {
			sender.sendMessage(plugin.getLang(sender, "storeallplayers.finished", "count", String.valueOf(count)));
			openInv.releaseAllPlayers(plugin);
		});
		processNextInQueue(sender, players, 0, future);
		return true;
	}

	private void processNextInQueue(CommandSender sender, ArrayDeque<OfflinePlayer> players, int count, CompletableFuture<Integer> future) {
		if (players.isEmpty()) {
			future.complete(count);
			return;
		}

		processPlayer(sender, players.poll(), count + 1, players.size())
				.thenRun(() -> processNextInQueue(sender, players, count + 1, future));
	}

	private CompletableFuture<Void> processPlayer(CommandSender sender, OfflinePlayer offlinePlayer, int count, int left) {
		Player player = getPlayer(offlinePlayer);
		if (player == null) {
			return CompletableFuture.completedFuture(null);
		}
		openInv.retainPlayer(player, plugin);

		List<ItemStack> items = new ArrayList<>();
		for (ItemStack itemStack : player.getInventory().getContents()) {
			if (itemStack != null) {
				items.add(new ItemStack(itemStack));
			}
		}
		for (ItemStack itemStack : player.getEnderChest().getContents()) {
			if (itemStack != null) {
				items.add(new ItemStack(itemStack));
			}
		}
		int itemCount = items.size();
		if (itemCount == 0) {
			plugin.getLogger().info(count + " | " + left + " | No items for " + player.getName());
			openInv.releasePlayer(player, plugin);
			return CompletableFuture.completedFuture(null);
		}
		String playerName = player.getName();
		return plugin.getItemStorage().storeItems(offlinePlayer.getUniqueId(), items).thenAccept(aVoid -> {
			plugin.getLogger().info(count + " | " + left + " | Stored " + itemCount + " items for " + playerName);
			if (count % 10 == 0) {
				sender.sendMessage(plugin.getLang(sender, "storeallplayers.progress", "count", String.valueOf(count), "left", String.valueOf(left)));
			}
			player.getInventory().clear();
			player.getInventory().setArmorContents(new ItemStack[4]);
			player.getEnderChest().clear();
			player.saveData();
			openInv.releasePlayer(player, plugin);
		}).exceptionally(throwable -> {
			plugin.getLogger().log(Level.SEVERE, count + " | " + left + " | Failed to store items for " + playerName, throwable);
			sender.sendMessage(plugin.getLang(sender, "storeallplayers.failed", "player", playerName));
			openInv.releasePlayer(player, plugin);
			return null;
		});

	}

	private Player getPlayer(OfflinePlayer offlinePlayer) {
		if (offlinePlayer.isOnline()) {
			if (offlinePlayer instanceof Player) {
				return (Player) offlinePlayer;
			}
			return offlinePlayer.getPlayer();
		}
		try {
			return openInv.loadPlayer(offlinePlayer);
		} catch (Exception e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to load player " + offlinePlayer.getName() + " for storing items", e);
			return null;
		}
	}
}
