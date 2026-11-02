# Elytra Buff

A Minecraft mod for **Fabric** and **NeoForge** (Minecraft 1.21.1 – 26.3) that makes flying with an elytra easier
to start, lets a flight go on forever and lets you flap the wings for extra speed.

- **Cold start** – take off from the ground without fireworks or a cliff. With an elytra on, crouch while standing
  on the ground:
  - your wings slowly rise, gathering strength for one mighty wing beat,
  - after 1 second the air starts to stir around your feet, more and more,
  - after 3 seconds (configurable) feathers drift off the wings and they rustle: the cold start is charged.
  - Release crouch to beat the wings down and be thrown into the air, 10 blocks high by default, in a gust of wind
    and feathers. Press jump in the air to open the elytra and fly off like after jumping from a cliff.
  - Releasing crouch early, leaving the ground, taking off the elytra or getting into water cancels the charge.
    Elytras that are about to break cannot cold start, just like they cannot glide.
- **Endless flight** – in vanilla, diving turns height into speed, but drag always takes a bit more than climbing back
  up returns, so every flight slowly runs out of energy. The mod adds a small bonus to the speed gained while losing
  height (20 % more by default). Diving and pulling up again now recovers all the speed and height of the flight, and
  with a good rhythm (dive at about 30–40°, climb at about 40–50°) you slowly climb higher. Climbing works like in
  vanilla, and very steep dives still lose more to drag than they gain, so flying still needs a bit of skill.
- **Wing flaps** – crouch while flying to flap the wings: like a weak firework rocket, flapping pushes you along the
  direction you look, with a flapping sound at every wing beat. The push gets weaker the faster you already
  fly and never gets you as fast as a firework.
  - The wings tire out after 3 seconds of flapping (configurable) and need 5 seconds (configurable) to recover.
  - Releasing crouch earlier lets them recover from the time flapped so far, without waiting for the full cooldown.

The wings move along with the cold start and the wing flaps, for yourself in third person and for other players.
This needs the mod on the client of the player watching.

No items, blocks or textures are added.

## Installation

Install the mod on the **server** (or in singleplayer, which runs both). The cold start runs entirely on the server,
so it works for every player, even without the mod on their client.

The endless flight and the wing flaps need the mod on the **client** as well, since the client simulates the player's
own flight. The server sends its settings to clients with the mod. Players without it fly like in vanilla, and a
client with the mod on a server without it flies like in vanilla too. On Fabric, the mod requires
[Fabric API](https://modrinth.com/mod/fabric-api).

## Configuration

`config/elytrabuff.properties` is created on first start. Changes are applied automatically while the game is running.
It can also be edited in game: on NeoForge in *Mods → Elytra Buff → Config*, on Fabric through
[Mod Menu](https://modrinth.com/mod/modmenu).
All settings are decided by the server: in multiplayer the server's values apply.

| Key            | Default | Description                                                                                                                                                 |
|----------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `coldStart`    | `true`  | Crouch on the ground with an elytra on to charge a cold start, release crouch to launch. `false` = crouching does nothing special.                         |
| `chargeTime`   | `3.0`   | How long to crouch until the cold start is charged, in seconds. The air starts to stir after 1 second, feathers and rustling show that it is charged. `0.5` to `30`. |
| `launchHeight` | `10.0`  | How high a cold start throws the player, in blocks. `1` to `64`.                                                                                            |
| `energyBonus`  | `0.2`   | Extra speed gained from losing height while flying, as a fraction of the energy the descent gives in vanilla. `0.2` = 20 % more, `0` = vanilla. `0` to `2`. |
| `wingFlaps`    | `true`  | Crouch while flying to flap the wings and speed up, like with a weak firework rocket. `false` = crouching in flight does nothing special.                   |
| `flapStrength` | `0.04`  | Speed each tick of flapping adds, in blocks per tick. Less the faster you fly, flapping never gets faster than 1.5 blocks per tick. `0` to `0.5`.            |
| `flapTime`     | `3.0`   | How long the wings can flap at once, in seconds. `0.5` to `30`.                                                                                             |
| `flapCooldown` | `5.0`   | How long tired out wings take to recover before they can flap again, in seconds. `0` to `60`.                                                               |

## Building

The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build every Minecraft version and loader from one
source tree. Each node in `settings.gradle.kts` targets one Minecraft version and declares the compatible range in
`stonecutter.properties.toml`.

```sh
./gradlew build                          # build all versions and loaders
./gradlew buildAndCollect                # copy all jars to build/libs/<mod version>/
./gradlew :1.21.1-fabric:runClient       # run a specific version (Fabric)
./gradlew :26.3-neoforge:runClient       # run a specific version (NeoForge)
```

Every push to `main` builds all versions on GitHub Actions; the jars can be downloaded from the run's
artifacts.

On **NixOS**, run the game inside the provided Nix shell so Minecraft finds its native libraries
(otherwise it crashes with `GLFW error during init: Failed to detect any supported platform`):

```sh
nix-shell --run './gradlew :1.21.1-fabric:runClient'
```

Version-specific code uses Stonecutter comments (`//? if >=1.21.2 {`). The source tree is checked in for the
`1.21.1-fabric` node; switch the active version with the `Set active project to …` Gradle tasks before editing code
for another version, and switch back to `1.21.1-fabric` (`Reset active project`) before committing.

## License

[MIT](LICENSE)
