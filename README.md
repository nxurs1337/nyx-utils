<div align="center">

<img src="docs/banner.png" alt="Nyx Utils" width="100%"/>

**A just-for-fun Fabric utility client for Minecraft 1.21.11.**
No cheats, just an arcade, chat toys and screen effects.

</div>

## Features

**GUI** (Right Shift) - a neon arcade hall with push-button tabs, light-up module buttons
and a CRT "service mode" settings panel, all in its own pixel font.

**Game room** - seven playable cabinets with saved hi-scores:
Dot Dash (maze chase), Neon Snake, Block Drop (falling blocks), Gate Hopper, Mine Field,
Star Defender and Brick Blaster.

**High Roller Lounge (18+)** - 15 original slot machines (tumble, paylines, 243 ways, money collect)
with free spins and bonus buys, each tuned by simulation to return about 96%.
Played with *Nyx Coins*: play money with no real-world value that can't be bought or cashed out.

**Fun**
- FancyChat - restyle your messages (small caps, fullwidth, bubble, uwu, leet...)
- ChatEmotes - `:shrug:`, `:tableflip:`, `:lenny:` and friends
- BouncingLogo - the screensaver classic, with a corner-hit party
- ScreenWeather - snow, hearts, sakura, confetti or shooting stars
- ScreenPet - a pixel cat living next to your hotbar
- RainbowCat - a caped cat flying across your screen on a rainbow
- BadgeWall / Badge - an 88x31 button wall with zoom, and a badge on your HUD

**Render**
- ModelChanger - show yourself, other players and/or mobs as any mob, or as a custom `.glb` model.
  Right click the *Model* setting for the picker. Purely client side.

## Custom models
Drop static `.glb` (binary glTF) files into `.minecraft/nyxutils/models/`, open the model picker and
choose them under *Custom models*. `CustomYaw` and `CustomSize` fix orientation and size.

## Building
Requires JDK 21 or newer.

```bash
./gradlew build
```

The mod jar ends up in `build/libs/`. `./gradlew runClient` starts a development client.


## License
[MIT](LICENSE)
