# BloodJail

Simple Paper plugin for jailing players with timer, reason, and restrictions.

## Features

- `/prison <player> <time> <reason>` to jail player
- `/unprison <player>` to release player
- `/checkprison <player>` for moderators to see remaining time and who jailed
- `/setprison` to set the fixed jail location from your current position
- Time format: `1m`, `10m`, `1h`, `2d`, `1h30m`
- Jail time decreases only while the player is online
- Broadcast in chat when a player is jailed (with reason)
- Jailed player gets action bar countdown
- Jailed players cannot chat, fight, or drop items
- Released players are returned to where they were jailed
- Jail records persist in `plugins/BloodJail/prisoners.yml`
- Old `releaseAt` data is migrated automatically to online-only remaining time

## Permissions

- `bloodjail.*` (all commands + bypass)
- `bloodjail.command.prison`
- `bloodjail.command.unprison`
- `bloodjail.command.checkprison`
- `bloodjail.command.setprison`
- `bloodjail.bypass`

## Config

Edit `plugins/BloodJail/config.yml` after first start:

```yml
jail:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0
```

Or set this point in-game with `/setprison`.

## Messages and Style

- All player/admin messages are in `plugins/BloodJail/messages.yml`
- Messages use Adventure MiniMessage format
- Default theme is orange-white and can be edited in `prefix` and message templates
- Broadcasts now use Adventure Components instead of legacy string broadcasts

## Build

```powershell
mvn clean package
```

JAR output:

- `target/bloodjail-1.0.jar`

