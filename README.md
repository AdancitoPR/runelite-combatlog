# Combat Log

A RuneLite plugin that records the hits you land on enemies.

Each of your hitsplats becomes a row: who you hit, for how much, and when. The sidebar keeps the full fight, and a small overlay shows session DPS plus the latest hits.

The game client colors **your** damage blue (including max hits and the cyan, yellow, orange, white, and poise variants). This plugin records those. It does not record:

- damage other players deal
- damage you take
- poison, venom, disease, burn, bleed, or corruption, because those splats are not marked as yours

Blocks are recorded by default and do not count toward damage or DPS. Multi-hit weapons log one row per hitsplat.
<img width="272" height="385" alt="image" src="https://github.com/user-attachments/assets/09c4b7cd-67d1-424f-b92d-5f07ae94ecec" />
<img width="282" height="391" alt="image" src="https://github.com/user-attachments/assets/1f58b062-efe7-4ff3-91c9-ba3bba7e4b9d" />
<img width="323" height="699" alt="image" src="https://github.com/user-attachments/assets/cceb80d0-761f-4544-9472-bfb3430229d7" />




## Run it

You need JDK 11 or newer.

```sh
./gradlew run
```

That starts a development RuneLite client with Combat Log loaded and developer mode on. Log in with a Jagex account using the steps at <https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts>.

Open the blue hitsplat icon in the sidebar. Attack an NPC. Hits you land should appear in the panel and in the overlay at the top left.

## What to check in game

- A normal hit on an NPC shows the NPC's name, the damage, and the time.
- A max hit is marked with `*`.
- A block shows as "Block" and does not increase the damage total.
- Session DPS appears after the fight has lasted at least one game tick.
- Recent DPS covers the last 10 seconds (change the window in the plugin settings).
- Poison and venom ticks do not add rows.
- Another player's hit on your target does not add a row.
- Clear asks you to confirm. Copy puts the log on the clipboard, oldest hit first.
- Turning the plugin off hides it and keeps the hits. "Clear on logout" wipes the log when you return to the login screen; a world hop keeps it.

## Sideload the jar

`./gradlew run` is the reliable way to use the plugin. To load it into another developer-mode client:

```sh
./gradlew jar
mkdir -p ~/.runelite/sideloaded-plugins
cp build/libs/combat-log-1.0.0.jar ~/.runelite/sideloaded-plugins/
```

Start that client with `--developer-mode`. Copy the jar from `build/libs/combat-log-1.0.0.jar`, not the larger `combat-log-1.0.0-all.jar`.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| Record blocks | On | Keep 0-damage blocks in the log |
| Record player hits | On | Also log hits you land on other players |
| Log length | 300 | Drop the oldest rows after this |
| Recent DPS window | 10 seconds | Window used for recent DPS |
| Clear on logout | Off | Clear when you reach the login screen |
| Show overlay | On | Draw the on-screen log |
| Overlay rows | 6 | How many hits the overlay lists |

## Tests

```sh
./gradlew test
```

The tests cover which hitsplats are kept, DPS over the session and the recent window, per-target totals, and the copied log. They do not start the game.
