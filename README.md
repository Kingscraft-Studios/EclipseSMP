# Eclipse SMP

A day/night SMP plugin for Paper (1.21.4+, `api-version: 26.2`) built around two opposing
allegiances, dynamic Sol/Luna powers, and periodic **Blood Eclipse** events where PvP drops
Eclipse Shards.

## Features

- **Allegiances** — choose between *Sol* (day) and *Luna* (night); powers follow your allegiance.
- **Sol / Luna powers** — strength in sunlight vs. the dark, with a double-sneak dash (Luna)
  and AoE knockback + ignite flare (Sol).
- **Blood Eclipse** — scheduled (with jitter) or player-triggered via the Eclipse Totem.
  Worlds freeze at midnight under a storm, combined powers kick in, and a random *surge*
  (damage / speed / regen) is assigned per player.
- **Eclipse Shards** — drop on deaths during an eclipse, banked for safekeeping, and used to
  craft and upgrade Eclipse gear.
- **Eclipse gear** — tiered diamond **or netherite** weapons and armor crafted with shards
  (the result keeps the material you put in, locked at craft time), upgraded in an
  anvil (1 shard = 1 tier), reforged automatically to your allegiance.
- **Elimination** — bank dropping too low bans a player until the penalty expires.
- **Admin debug commands** and an asynchronous **Discord webhook** notifier.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/eclipse` | Open the Eclipse SMP menu | `eclipse.smp.use` |
| `/eclipse choose` | Pick or switch allegiance | `eclipse.smp.use` |
| `/eclipse status` | Your state + the eclipse phase | `eclipse.smp.use` |
| `/eclipse shards deposit [all\|<n>]` | Bank carried shards | `eclipse.smp.use` |
| `/eclipse shards withdraw <n>` | Withdraw shards from your bank | `eclipse.smp.use` |
| `/eclipse top` | Shard leaderboard | `eclipse.smp.use` |
| `/eclipse admin trigger\|cancel\|reload` | Control eclipses, reload config | `eclipse.smp.admin` |
| `/eclipse admin debug ...` | Debug commands (needs `debug.enabled: true`) | `eclipse.smp.admin` |
| `/top` | Shard leaderboard (alias) | `eclipse.smp.use` |
| `/deposit [all\|<n>]` | Deposit carried shards | `eclipse.smp.use` |
| `/withdraw [n]` | Withdraw shards from your bank | `eclipse.smp.use` |
| `/recipe` | View Eclipse crafting and anvil recipes | `eclipse.smp.use` |

**Permissions**

- `eclipse.smp.use` — base commands (default: true).
- `eclipse.smp.admin` — admin controls (default: op).

## Configuration

Everything is tuned in `config.yml` (`/eclipse admin reload` after editing):

- `eclipse.*` — scheduling, warning/duration, freeze/storm, titles, boss bar.
- `sol.*` / `luna.*` — side powers and abilities (dash / flare), including
  `luna.dash.hide-equipment` (hide gear from others during the dash) and
  `luna.dash.hide-equipment-self` (also hide your own gear from your client).
- `eclipse-powers.*` — combined ultra-state and surges.
- `shards.*` / `gear.*` — drop rules, bank, tier scaling, set bonuses, netherite toggle.
- `elimination.*` — death ban and grace.
- `choose.*` — allegiance switch costs.
- `discord-webhook.*` — Discord alerts (async, optional).
- `debug.enabled` — unlocks `/eclipse admin debug ...`.
- `messages.*` — override any plugin text.

## Dependencies

- **Paper** (API 26.2, Java 25) — required.
- **PacketEvents** (v2.13.0, `packetevents`) — required. Used to hide a Luna player's
  armor and held items from other clients during the vanish dash (packet-level masking,
  gear stays equipped and functional server-side). Loaded before EclipseSMP via `depend`.
- **AuthMe** (optional) — allegiance prompts wait for login, detected via reflection
  (no hard dependency).

## Building

```bash
mvn clean package
```

The shaded jar is produced under `target/`. Install by copying it into `plugins/` and
restarting the server.
