# Eclipse SMP

> A domain-fight day/night SMP plugin for **Paper** (1.21.4+, `api-version: 26.2`).
> Two opposing allegiances — **Sol** (day) and **Luna** (night) — clash under a periodic
> **Blood Eclipse** where PvP drops Eclipse Shards.

Eclipse SMP is the plugin that powers the Eclipse SMP server. Want to join the fight?
Come play on the official servers — **[join the Discord](https://discord.gg/DF5deBd3yc)** to find out how!

- **Browse & download:** [Modrinth](https://modrinth.com/mod/eclipse-smp-s2)
- **Source code:** this repository
- **Report a bug / request a feature:** [GitHub Issues](https://github.com/Kingscraft-Studios/EclipseSMP/issues)

## Features

- **Allegiances** — choose between *Sol* (day) and *Luna* (night); powers follow your allegiance
  and your Eclipse gear is reforged to match it automatically. Switching is free at first, then
  costs Eclipse Shards.
- **Sol / Luna powers** — strength in sunlight vs. the dark:
  - *Sol* in sunlight: damage boost, Regen, Speed, and fire; in darkness: Slowness + Weakness.
  - *Luna* in darkness: Speed, damage boost, and a **backstab crit** from behind; in direct
    sunlight they take extra damage.
  - **Vanish Dash (Luna)** — double-sneak in the dark to dash forward, turn invisible, and
    hide your armor/held items from other players (packet-level via PacketEvents; your gear
    stays equipped and functional server-side).
  - **Solar Flare (Sol)** — double-sneak in sunlight for an AoE knockback + ignite burst that
    also deals instant damage to nearby enemies.
- **Blood Eclipse** — scheduled (with jitter) or player-triggered by shattering the **Eclipse
  Totem**. Worlds freeze at midnight under a storm, everyone gets an ultra-state (Speed, Regen,
  Night Vision) plus a random *surge* (extra damage / speed / regen), and PvP drops Eclipse Shards.
- **Eclipse Shards** — drop on PvP kills during an eclipse (bonus for killing the opposite
  allegiance), banked for safekeeping, and used to craft and upgrade Eclipse gear. Carried shards
  drop on **any** death, and natural deaths / non-eclipse PvP losses cost banked shards too. A
  **combat tag** (10s) ensures only meaningful recent hits can claim kill credit. Low on shards?
  Forge new ones with a catch-up crafting recipe (sun glowstone + moon lapis over amethyst),
  gated by a wealth cap, per-player cooldown, and a lifetime limit.
- **Allegiance Guide** — a `/eclipse` menu book that explains every power, gear passive, and
  eclipse effect your chosen side grants.
- **Season control** — admins can run a PvP **grace period** (`/grace`) and schedule the opening
  of the sealed **End portals** (`/end`), both persisted across restarts.
- **Mace control** — vanilla Mace crafting is gated behind a server-wide lifetime budget
  (auto-Crafters are always blocked from producing Maces).
- **Vanilla recipe tweaks** — optional rebalances: cheaper golden apple, craftable cobweb,
  cheaper anvil, and a craftable Totem of Undying (all toggleable).
- **Eclipse gear** — tiered diamond **or netherite** weapons and armor crafted with shards
  (the result keeps the material you put in, locked at craft time), upgraded in an
  anvil (1 shard = 1 tier), reforged automatically to your allegiance. Gear has side-specific
  passives: Luna lifesteal and Sol blade/axe boosts in daylight, bow effects (Sol ignites,
  Luna slows) on eclipse hits, and full-set armor bonuses (Regen/Speed/Strength scaling with
  total tier) plus damage reduction in darkness/eclipses.
- **Elimination** — bank dropping too low bans a player until the penalty expires.
- **Shard leaderboard** — `/eclipse top` (or `/top`) tracks total shards and kills.
- **Admin debug commands** and an asynchronous **Discord webhook** notifier (optional role ping).

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
| `/eclipse admin unban <player>` | End an elimination ban | `eclipse.smp.admin` |
| `/eclipse admin mace <show\|n>` | Inspect / adjust the Mace crafting budget | `eclipse.smp.admin` |
| `/eclipse admin debug ...` | Debug commands (needs `debug.enabled: true`) | `eclipse.smp.admin` |
| `/top` | Shard leaderboard (alias) | `eclipse.smp.use` |
| `/deposit [all\|<n>]` | Deposit carried shards | `eclipse.smp.use` |
| `/withdraw [n]` | Withdraw shards from your bank | `eclipse.smp.use` |
| `/recipes` | View Eclipse crafting and anvil recipes | `eclipse.smp.use` |
| `/grace [minutes\|cancel]` | Start / cancel a PvP grace period | `eclipse.smp.admin` |
| `/end [minutes]` | Schedule the opening of the End portals | `eclipse.smp.admin` |

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
- `shards.*` / `gear.*` — drop rules, combat tag window, bank, tier scaling, set bonuses,
  netherite toggle, and the catch-up shard forging recipe (`shards.recipe.*`).
- `mace-control.*` — the server-wide Mace crafting budget.
- `vanilla-tweaks.*` — golden apple / cobweb / anvil / totem recipe rebalances.
- `season.*` — default `/grace` length and `/end` timer, and whether the End starts open.
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

## License

This project is licensed under the **GNU General Public License v3.0** — see
[LICENSE](License.md) for details.

## Support

- **Discord** — for the official Eclipse SMP servers and community: https://discord.gg/DF5deBd3yc
- **Issues & feature requests** — [GitHub Issues](https://github.com/Kingscraft-Studios/EclipseSMP/issues)
- **Download** — [Modrinth](https://modrinth.com/mod/eclipse-smp-s2)
