# BloodJail

Simple Paper plugin for jailing players with timer, reason, and restrictions.

## Features

- `/prison <player> <time> <reason>` to jail player
- `/unprison <player>` to release player
- `/checkprison [player]` to see remaining time and who jailed
- Time format: `1m`, `10m`, `1h`, `2d`, `1h30m`
- Broadcast in chat when a player is jailed (with reason)
- Jailed player gets action bar countdown
- Jailed players cannot chat, fight, or drop items
- Released players are returned to where they were jailed
- Jail records persist in `plugins/BloodJail/prisoners.yml`

## Permissions

- `bloodjail.command.prison`
- `bloodjail.command.unprison`
- `bloodjail.command.checkprison`
- `bloodjail.command.checkprison.others`
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

## Build

```powershell
mvn clean package
```

JAR output:

- `target/bloodjail-1.0.jar`

