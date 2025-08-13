> [!CAUTION]
> ## Repository is archived as I'm leaving GitHub!
>
> GitHub wants me to "[embrace AI or get out](https://www.businessinsider.com/github-ceo-developers-embrace-ai-or-get-out-2025-8)". So I'm leaving.
> 
> Continued development will happen on Codeberg: https://codeberg.org/Minebench/ItemTransfer

# ItemTransfer

This is a simple plugin that allows you to transfer items between locations or servers
 by storing items in a database and allowing to retrieve them via a box.

## Commands and Permissions

| Command                | Permission                               | Description                        |
|------------------------|------------------------------------------|------------------------------------|
| `/itemtransfer`        | `itemtransfer.command`                   | Main plugin command                |
| `/itemtransfer info`   | `itemtransfer.command.info`              | View info about the plugin         |
| `/itemtransfer reload` | `itemtransfer.command.reload`            | Reload the plugin config           |
| `/storeitems`          | `itemtransfer.command.storeitems`        | Store items                        |
| `/storeitems <player>` | `itemtransfer.command.storeitems.others` | Store items for other players      |
| `/getitems`            | `itemtransfer.command.getitems`          | Get stored items                   |
| `/getitems <player>`   | `itemtransfer.command.getitems.others`   | Get stored items for other players |
| `/storeallplayers`     | `itemtransfer.command.storeallplayers`   | Store items for all players        |

**Permissions:**

- `itemtransfer.command`: Plugin command permission (default: true)
- `itemtransfer.command.reload`: Permission to the reload command (default: op)
- `itemtransfer.command.storeitems`: Permission to store items (default: op)
- `itemtransfer.command.storeitems.others`: Permission to store items for others (default: op)
- `itemtransfer.command.getitems`: Permission to get items (default: op)
- `itemtransfer.command.getitems.others`: Permission to get items for others (default: op)
- `itemtransfer.click.storeitems`: Permission to click a block to open a gui to store items (default: op)
- `itemtransfer.click.getitems`: Permission to click a block to open a gui to get the items (default: op)
- `itemtransfer.click.storeallplayers`: Permission to store items for all players (default: op)

## Downloads

You can download the latest builds from the [Minebench Jenkins ci server](https://ci.minebench.de/job/ItemTransfer/).

## License

This plugin is licensed under the AGPL-3.0 license. See the [LICENSE](LICENSE) file for more information.

```
ItemTransfer
Copyright (c) 2024 Max Lee aka Phoenix616 (max@themoep.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```
