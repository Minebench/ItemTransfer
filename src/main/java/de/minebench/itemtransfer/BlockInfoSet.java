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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BlockInfoSet {

	private final Map<String, Map<Integer, Table<Integer, Integer, Boolean>>> blockInfoMap = new HashMap<>();

	public void add(String worldName, int x, int y, int z) {
		blockInfoMap.computeIfAbsent(worldName.toLowerCase(Locale.ROOT), k -> new HashMap<>())
				.computeIfAbsent(x, k -> HashBasedTable.create())
				.put(y, z, true);
	}

	public boolean contains(Block block) {
		if (block == null) {
			return false;
		}
		Map<Integer, Table<Integer, Integer, Boolean>> table = blockInfoMap.get(block.getWorld().getName().toLowerCase(Locale.ROOT));
		if (table == null) {
			return false;
		}
		Table<Integer, Integer, Boolean> yzTable = table.get(block.getX());
		if (yzTable == null) {
			return false;
		}
		return yzTable.contains(block.getY(), block.getZ());
	}
}
