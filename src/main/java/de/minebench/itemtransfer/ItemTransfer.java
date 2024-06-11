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

import de.minebench.itemtransfer.commands.GetItemsCommand;
import de.minebench.itemtransfer.commands.StoreItemsCommand;
import de.themoep.bukkitplugin.BukkitPlugin;
import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.GuiStorageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.utils.lang.bukkit.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;

public final class ItemTransfer extends BukkitPlugin {

	private static final String[] GUI_CONFIRM = {
			"         ",
			"  y   n  ",
			"         "
	};
	private static final String[] GUI_STORE = {
			"sssssssss",
			"sssssssss",
			"sssssssss"
	};
	private static final String[] GUI_GET = {
			"sssssssss",
			"sssssssss",
			"sssssssss",
			"p       n"
	};
	private static final Component BASE_COMPONENT = Component.empty().decoration(TextDecoration.ITALIC, false);
	private static final BiConsumer<ItemMeta, String> ITEM_NAME_SETTER
			= (itemMeta, name) -> itemMeta.displayName(BASE_COMPONENT.append(MineDown.parse(name)));
	private static final BiConsumer<ItemMeta, List<String>> ITEM_LORE_SETTER
			= (itemMeta, lore) -> itemMeta.lore(lore.stream().map(s -> BASE_COMPONENT.append(MineDown.parse(s))).toList());
	private static final ItemStack ITEM_CONFIRM = createItem(Material.LIME_WOOL, 1001);
	private static final ItemStack ITEM_CANCEL = createItem(Material.RED_WOOL, 1001);
	private static final ItemStack ITEM_FILLER = createItem(Material.BLACK_STAINED_GLASS_PANE, 1001);
	private BlockInfoSet clickStore = new BlockInfoSet();
	private BlockInfoSet clickGet = new BlockInfoSet();

	private static ItemStack createItem(@NotNull Material material, int customModelData) {
		ItemStack item = new ItemStack(material);
		item.editMeta(meta -> meta.setCustomModelData(customModelData));
		return item;
	}

	private final InventoryGui.InventoryCreator inventoryCreator = new InventoryGui.InventoryCreator(
			(gui, who, type) -> getServer().createInventory(new InventoryGui.Holder(gui), type, getLang(who, gui.getTitle())),
			(gui, who, size) -> getServer().createInventory(new InventoryGui.Holder(gui), size, getLang(who, gui.getTitle())));

	private ItemStorage itemStorage;
	private LanguageManager lang;

	@Override
	public void onEnable() {
		super.onEnable();

		getCommand("storeitems").setExecutor(new StoreItemsCommand(this));
		getCommand("getitems").setExecutor(new GetItemsCommand(this));

		getServer().getPluginManager().registerEvents(new ClickBlockListener(this), this);
	}

	@Override
	public boolean loadConfig() {
		boolean success = true;
		// Load configuration from config.yml
		String host = getConfig().getString("mysql.host");
		int port = getConfig().getInt("mysql.port");
		String database = getConfig().getString("mysql.database");
		String tablePrefix = getConfig().getString("mysql.table-prefix");
		String user = getConfig().getString("mysql.user");
		String password = getConfig().getString("mysql.pass");

		if (itemStorage != null) {
			itemStorage.close();
		}
		try {
			itemStorage = new ItemStorage(this, host, port, database, tablePrefix, user, password);
		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Failed to initialize item storage", e);
			itemStorage = null;
			success = false;
		}

		lang = new LanguageManager(this, getConfig().getString("default-locale"));

		clickStore = loadBlocks("click-blocks.store");
		clickGet = loadBlocks("click-blocks.get");
		return success;
	}

	private BlockInfoSet loadBlocks(String key) {
		BlockInfoSet blocks = new BlockInfoSet();
		for (String blockString : getConfig().getStringList(key)) {
			String[] parts = blockString.split(";");
			if (parts.length != 4) {
				getLogger().warning("Invalid block string in " + key + ": " + blockString);
				continue;
			}
			try {
				World world = Bukkit.getWorld(parts[0]);
				if (world == null) {
					getLogger().warning("Invalid world: " + parts[0] + " in " + key + " config " + blockString);
				}
				blocks.add(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
			} catch (NumberFormatException e) {
				getLogger().warning("Invalid " + key + " block string: " + blockString);
			}
		}
		return blocks;
	}

	@Override
	public void onDisable() {
		// Close the item storage
		itemStorage.close();
	}

	public @NotNull String getRawLang(CommandSender sender, @NotNull String key, @NotNull String... replacements) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(replacements);
		String rawString = lang.getConfig(sender).get(key, replacements);
		if (rawString == null) {
			return "Missing translation for key: " + key;
		}
		return rawString;
	}

	public @NotNull Component getLang(CommandSender sender, @NotNull String key, @NotNull String... replacements) {
		Objects.requireNonNull(key);
		Objects.requireNonNull(replacements);
		return MineDown.parse(getRawLang(sender, key), replacements);
	}

	public boolean isStoreItemsBlock(Block block) {
		return clickStore.contains(block);
	}

	public boolean isGetItemsBlock(Block block) {
		return clickGet.contains(block);
	}

	private InventoryGui createGui(String title, String[] rows, GuiElement... elements) {
		return new InventoryGui(this, inventoryCreator, ITEM_NAME_SETTER, ITEM_LORE_SETTER,
				null, title, rows, elements);
	}

	public void openStoreItemsConfirmGui(@NotNull Player player) {
		Objects.requireNonNull(player);
		if (itemStorage == null) {
			player.sendMessage(getLang(player, "storage_not_initialized"));
			return;
		}
		createGui("gui.confirm.title", GUI_CONFIRM,
				new StaticGuiElement('n', ITEM_CANCEL, click -> {
					click.getGui().close();
					return true;
				}, getRawLang(player, "gui.confirm.cancel")),
				new StaticGuiElement('y', ITEM_CONFIRM, click -> {
					openStoreItemsGui(player);
					return true;
				}, getRawLang(player, "gui.confirm.confirm"))
		).show(player);
	}

	private void openStoreItemsGui(@NotNull Player player) {
		Objects.requireNonNull(player);
		if (itemStorage == null) {
			player.sendMessage(getLang(player, "storage_not_initialized"));
			return;
		}
		Inventory inventory = getServer().createInventory(null, InventoryType.CHEST);
		InventoryGui gui = createGui("gui.store.title", GUI_STORE, new GuiStorageElement('s', inventory, -1, () -> {
		}, placeInfo -> true, takeInfo -> false));
		gui.setCloseAction(close -> {
			Collection<ItemStack> items = Arrays.stream(inventory.getContents()).filter(Objects::nonNull).toList();
			final int itemAmount = items.stream().map(ItemStack::getAmount).reduce(0, Integer::sum);
			itemStorage.storeItems(player.getUniqueId(), items)
					.whenComplete((v, ex) -> {
						if (ex == null) {
							player.sendMessage(getLang(player, "gui.store.done", "amount", String.valueOf(itemAmount)));
						} else {
							player.sendMessage(getLang(player, "gui.store.error"));
							getLogger().log(Level.SEVERE, "Failed to store items for " + player.getName(), ex);
							for (ItemStack item : items) {
								getLogger().info(player.getName() + ": " + Base64.getEncoder().encodeToString(item.serializeAsBytes()));
							}
						}
					});
			return false;
		});
		gui.show(player);
	}

	public void openGetItemsGui(@NotNull Player player) {
		Objects.requireNonNull(player);
		if (itemStorage == null) {
			player.sendMessage(getLang(player, "storage_not_initialized"));
			return;
		}
		itemStorage.getStoredItems(player.getUniqueId())
				.whenComplete((storedItems, ex1) -> {
					if (ex1 != null) {
						player.sendMessage(getLang(player, "gui.get.error"));
						getLogger().log(Level.SEVERE, "Failed to get items for " + player.getName(), ex1);
						return;
					}
					if (storedItems.isEmpty()) {
						player.sendMessage(getLang(player, "gui.get.no_items"));
						return;
					}

					createGui("gui.get.title", GUI_GET,
							new GuiPageElement('p', new ItemStack(Material.PAPER), GuiPageElement.PageAction.PREVIOUS, getRawLang(player, "gui.get.previous")),
							new GuiPageElement('n', new ItemStack(Material.PAPER), GuiPageElement.PageAction.NEXT, getRawLang(player, "gui.get.next")),
							new DynamicGuiElement('s', who -> {
								int serverDataVersion = Bukkit.getServer().getUnsafe().getDataVersion();

								GuiElementGroup group = new GuiElementGroup('s');
								for (StoredItem storedItem : storedItems.values()) {
									if (storedItem.dataVersion() > serverDataVersion) {
										group.addElement(new StaticGuiElement('s', new ItemStack(Material.BARRIER), click -> true,
												getRawLang(player, "gui.get.data_version_mismatch",
														"item", storedItem.itemType(),
														"data_version", String.valueOf(storedItem.dataVersion()),
														"server_data_version", String.valueOf(serverDataVersion))
										));
										break;
									}

									ItemStack item = ItemStack.deserializeBytes(storedItem.itemData());
									group.addElement(new StaticGuiElement('s', item, click -> {
										if (player.getInventory().firstEmpty() == -1) {
											player.sendMessage(getLang(player, "gui.get.full_inventory"));
											return true;
										}
										itemStorage.removeStoredItem(storedItem.id()).whenComplete((removed, ex2) -> {
											if (ex2 != null) {
												player.sendMessage(getLang(player, "gui.get.error"));
												getLogger().log(Level.SEVERE, "Failed to remove item " + storedItem.id() + " for " + player.getName(), ex2);
												return;
											}
											if (removed) {
												storedItems.remove(storedItem.id());
												click.getGui().draw(click.getWhoClicked());
												getLogger().info(player.getName() + " took item " + storedItem.id() + " (" + storedItem.itemType() + ")");
												for (ItemStack rest : player.getInventory().addItem(item).values()) {
													player.getWorld().dropItem(player.getLocation(), rest).setOwner(player.getUniqueId());
												}
											} else {
												player.sendMessage(getLang(player, "gui.get.error"));
											}
										});
										return true;
									}));
								}
								return group;
							})).filler(ITEM_FILLER).show(player);
				});
	}
}
