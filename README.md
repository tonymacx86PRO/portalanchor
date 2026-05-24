# Portal Anchor

<p align="center">
  <img src="docs/assets/portalanchor-icon.png" alt="Portal Anchor icon" width="256">
</p>

Portal Anchor is a small Fabric mod for Minecraft `26.1.2`.

It gives mobs a short grace window after they move between dimensions, so vanilla does not instantly despawn them just because every player in the destination dimension is far away.

The original use case is portal-based mob farms. A mob can go through a Nether portal into a loaded dimension, arrive nowhere near any player, and get deleted by distance despawn before the farm can do its thing. Portal Anchor gives that mob a little time to exist like it should.

## What It Does

- Watches for non-player mobs changing dimensions.
- Gives the new destination-side mob a configurable despawn grace window.
- Cancels vanilla distance despawn while that grace window is active.
- Leaves Peaceful difficulty cleanup alone.
- Does not make mobs permanent.

Default grace time is `30` seconds.

## Commands

All commands require game master permission.

```mcfunction
/portalanchor
/portalanchor status
/portalanchor enabled <true|false>
/portalanchor grace <1-300>
/portalanchor save
/portalanchor reload
/portalanchor reset-stats
```

Runtime changes do not touch disk until you run:

```mcfunction
/portalanchor save
```

Use this to test settings live without accidentally making them permanent.

## Config

Portal Anchor creates this file on first run:

```text
config/portalanchor.json
```

Default config:

```json
{
  "enabled": true,
  "graceSeconds": 30
}
```

`/portalanchor reload` reloads the file from disk. `/portalanchor status` shows whether runtime settings match the saved config.

## Building

This project targets Java 25.

```bash
JAVA_HOME=/usr/lib/jvm/java-25-openjdk ./gradlew build
```

The built jar lands in:

```text
build/libs/
```

## License

Licensed under `MIT`.
