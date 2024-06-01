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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.themoep.bukkitplugin.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ItemStorage {

	private final HikariDataSource dataSource;
	private final BukkitPlugin plugin;

	public ItemStorage(@NotNull BukkitPlugin plugin, @NotNull String host, int port, @NotNull String database, @NotNull String user, @NotNull String password) {
		this.plugin = Objects.requireNonNull(plugin);
		// Create HikariCP data source
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + Objects.requireNonNull(host) + ":" + port + "/" + Objects.requireNonNull(database));
		config.setUsername(user);
		config.setPassword(password);
		dataSource = new HikariDataSource(config);

		// Create the items table if it doesn't exist
		createItemsTable();
	}

	private void createItemsTable() {
		try (Connection connection = dataSource.getConnection();
		     PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS items (" +
				     "id BIGINT AUTO_INCREMENT NOT NULL," +
				     "player_uuid CHAR(36) NOT NULL," +
				     "item_type VARCHAR(255) NOT NULL," +
				     "data_version INT NOT NULL," +
				     "timestamp BIGINT NOT NULL," +
				     "item_data BLOB NOT NULL," +
				     "INDEX player_uuid," +
				     "PRIMARY KEY (player_uuid, item_type, data_version, timestamp))")) {
			statement.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to create items table", e);
		}
	}

	public CompletableFuture<Void> storeItems(@NotNull UUID playerUUID, Collection<ItemStack> items) {
		if (items == null || items.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		Objects.requireNonNull(playerUUID);

		CompletableFuture<Void> future = new CompletableFuture<>();

		plugin.runAsync(() -> {
			try (Connection connection = dataSource.getConnection();
			     PreparedStatement statement = connection.prepareStatement("INSERT INTO items (player_uuid, item_type, data_version, timestamp, item_data) VALUES (?, ?, ?, ?, ?)")) {
				for (ItemStack item : items) {
					statement.setString(1, playerUUID.toString());
					statement.setString(2, item.getType().name());
					statement.setInt(3, Bukkit.getServer().getUnsafe().getDataVersion());
					statement.setLong(4, System.currentTimeMillis());
					statement.setBytes(5, item.serializeAsBytes());
					statement.addBatch();
				}
				statement.executeBatch();
			} catch (SQLException e) {
				plugin.getLogger().log(Level.SEVERE, "Failed to store items", e);
				future.completeExceptionally(new RuntimeException("Failed to store items", e));
			}
			future.complete(null);
		});

		return future;
	}

	public CompletableFuture<Map<Long, StoredItem>> getStoredItems(@NotNull UUID playerUUID) {
		Objects.requireNonNull(playerUUID);

		CompletableFuture<Map<Long, StoredItem>> future = new CompletableFuture<>();

		plugin.runAsync(() -> {
			Map<Long, StoredItem> storedItems = new LinkedHashMap<>();

			try (Connection connection = dataSource.getConnection();
			     PreparedStatement statement = connection.prepareStatement("SELECT item_type, data_version, timestamp, item_data FROM items WHERE player_uuid = ?")) {
				statement.setString(1, playerUUID.toString());
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						long id = resultSet.getLong("id");
						String itemType = resultSet.getString("item_type");
						int dataVersion = resultSet.getInt("data_version");
						long timestamp = resultSet.getLong("timestamp");
						byte[] itemData = resultSet.getBytes("item_data");

						storedItems.put(id, new StoredItem(id, itemType, dataVersion, timestamp, itemData));
					}
				}
			} catch (SQLException e) {
				plugin.getLogger().log(Level.SEVERE, "Failed to retrieve stored items", e);
				future.completeExceptionally(new RuntimeException("Failed to retrieve stored items", e));
			}

			future.complete(storedItems);
		});

		return future;
	}

	public CompletableFuture<Boolean> removeStoredItem(long id) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();

		plugin.runAsync(() -> {
			try (Connection connection = dataSource.getConnection();
			     PreparedStatement statement = connection.prepareStatement("DELETE FROM items WHERE id = ?")) {
				statement.setLong(1, id);
				future.complete(statement.executeUpdate() > 0);
			} catch (SQLException e) {
				plugin.getLogger().log(Level.SEVERE, "Failed to remove stored item", e);
				future.completeExceptionally(new RuntimeException("Failed to remove stored item", e));
			}
		});

		return future;
	}

	public void close() {
		dataSource.close();
	}
}
