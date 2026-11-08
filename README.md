# BRanks

**Ranks and tags** plugin for Minecraft (Spigot, Paper, Purpur, Bukkit), compiled against the 1.8.8 API and built to run from **1.8 up to the newest versions**.
It is a single jar with **no dependencies**: LuckPerms is not required (but it works alongside it, see below).

## Installation
1. Put `BRanks-1.1.0.jar` in your server's `plugins/` folder.
2. Start the server. `config.yml`, `ranks.yml`, `tags.yml` and `players.yml` are created in `plugins/BRanks/`.
3. On the first run, `ranks.yml` is filled with a starter ladder: **Membro (1), VIP (100), Fire (300), HELPER (800), MOD (1200), MOD+ (1600), ADMIN (2000), OWNER (2556)**. Edit, rename or delete any of them, or add your own. Tags start empty: create yours with `/tag create`.

## Concepts
- **Rank**: a player's group (prefix, suffix, colors, permissions). Each player has one rank.
- **Tag**: an extra decoration the player picks (`/tag vip`). It can require a rank, a permission, or both.
- **Levels**: every rank has a level from **1 (lowest) to 2556 (highest)**. The higher the number, the higher the rank. Change it with `/rank level`.
- **Higher ranks unlock lower tags**: a tag with `rank: vip` can be used by VIP **and every rank with a higher level** (e.g. Fire, MOD, OWNER). So a Fire player can also pick the VIP tag.
- **Colors**: `&a`, `&l`, names (`red`, `blue`, `gold`...) or hex `#FF8800` (1.16+; older versions use the closest color).

## Commands
Type `/rank` or `/tag help` for an organized in-game menu.

### Ranks
| Command | What it does |
|---|---|
| `/rank help` | Command menu (admin sections only show for admins) |
| `/rank list` | List the ranks with their levels, highest first |
| `/rank info [player]` | Show a player's rank and tag |
| `/rank set <player> <rank\|default>` (or `/setrank`) | Set a rank. `default` resets the player |
| `/rank create <name> [level]` / `delete <rank>` | Create (level 1-2556, default 1) / delete a rank |
| `/rank name <rank> <text>` | Display name |
| `/rank prefix <rank> <text\|none>` | Prefix |
| `/rank suffix <rank> <text\|none>` | Suffix |
| `/rank color <rank> <color>` | Player name color |
| `/rank chatcolor <rank> <color>` | Chat message color |
| `/rank permission <rank> <add\|remove\|list> [perm]` | Rank permissions |
| `/rank inherit <rank> <add\|remove> <other>` | Inherit permissions from another rank |
| `/rank default <rank>` | Rank of new players |
| `/rank level <rank> <1-2556>` | Change the rank level (hierarchy) |
| `/rank reload` | Reload the files |

### Tags
| Command | What it does |
|---|---|
| `/tag` | Menu: your current tag, available tags and locked tags (with what they require) |
| `/tag <name>` | Use a tag |
| `/tag clear` | Remove your tag |
| `/tag help` | Command menu |
| `/tag set <player> <tag\|none>` (or `/settag`) | Force someone's tag |
| `/tag create <name> <text>` / `delete <tag>` | Create / delete a tag |
| `/tag text <tag> <text>` | Change the tag text |
| `/tag rank <tag> <rank\|none>` | Lowest rank that unlocks the tag (higher ranks unlock it too) |
| `/tag permission <tag> <perm\|none>` | Permission that unlocks the tag |

A tag is unlocked by its **rank OR its permission**. With neither set, everyone can use it.

## Permissions
- `branks.admin` (op): manage ranks and tags.
- `branks.tag.use` (everyone): choose tags.
- `branks.chat.colors` (op): use `&` color codes in chat.
- In `ranks.yml`: `"*"` grants every permission and `-permission` removes one.

## File configuration
`ranks.yml` (listed from the highest level to the lowest):
```yaml
fire:
  level: 300
  name: Fire
  prefix: "&6[Fire]"
  suffix: ""
  color: gold
  chat-color: ""
  default: false
  inherits: [vip]
  permissions:
    - some.permission
vip:
  level: 100
  name: VIP
  prefix: "&a[VIP]"
  color: green
  inherits: [membro]
```
`tags.yml`:
```yaml
vip:
  text: "&6[VIP]"
  rank: vip                    # VIP and every higher rank can use it
  permission: branks.tag.vip   # optional: or anyone with this permission
```
`config.yml` controls the chat, tab list and nametag formats and every message (all editable).
Placeholders: `{player} {tag} {prefix} {suffix} {rank} {color} {chatcolor} {message}`. Spaces between tag, prefix and suffix are automatic.

## Using it with LuckPerms
If you prefer LuckPerms to handle permissions, set `permissions.enabled: false` in `config.yml`.
BRanks keeps handling prefix, tag, chat and tab. Rank-based tag access still works, and a permission-based tag just needs that permission (e.g. `branks.tag.vip`) granted through LuckPerms.
If another plugin already formats the chat, use `chat.enabled: false`.

## Upgrading
If you ran an earlier 1.1.0 build, delete `ranks.yml` (or add a `level:` to each rank) so the new levels apply: ranks without a level are treated as level 1.

## Upgrading from 1.0.0
Version 1.1.0 translated everything to English, including the file names and keys:
`jogadores.yml` → `players.yml`, and in `ranks.yml`/`tags.yml` the keys `nome, prefixo, sufixo, cor, cor-chat, padrao, herda, permissoes, texto, permissao` became `name, prefix, suffix, color, chat-color, default, inherits, permissions, text, permission`. Rename them (or delete the old files to regenerate the defaults) before updating.

## Building
With Maven: `mvn package` (the jar ends up in `target/`).
Without Maven: download `spigot-api-1.8.8` and run
`javac -encoding UTF-8 --release 8 -cp spigot-api.jar -d classes src/main/java/com/branks/*.java`, copy `src/main/resources/*.yml` into `classes/` and run `jar cf BRanks-1.1.0.jar -C classes .`.
