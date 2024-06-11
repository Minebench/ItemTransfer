package de.minebench.itemtransfer;

/*
 * ItemTransfer
 * Copyright (c) 2024 Max Lee aka Phoenix616 (max@themoep.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class ClickBlockListener implements Listener {
	private final ItemTransfer plugin;

	public ClickBlockListener(ItemTransfer plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onBlockClick(PlayerInteractEvent event) {
		if (event.getPlayer().hasPermission("itemtransfer.click.storeitems")) {
			if (plugin.isStoreItemsBlock(event.getClickedBlock())) {
			event.setCancelled(true);
				plugin.openStoreItemsConfirmGui(event.getPlayer());
			}
		}
		if (event.getPlayer().hasPermission("itemtransfer.click.getitems")) {
			if (plugin.isGetItemsBlock(event.getClickedBlock())) {
				event.setCancelled(true);
				plugin.openGetItemsGui(event.getPlayer());
			}
		}
	}
}
