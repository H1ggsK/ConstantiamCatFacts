# Cat Facts Addon

Meteor Client addon that periodically sends cat facts in chat.

## Features

- Sends cat facts at configurable intervals
- Two interval modes: message count or time-based (seconds/minutes/hours)
- Fetches from [catfacts.cc](https://catfacts.cc) or [meowfacts](https://meowfacts.herokuapp.com/) or randomly picks between both
- Configurable fallback message when APIs are down
- HUD element showing countdown to next fact
- `.catfacts` command for status, `.catfacts send` to manually fire one off

## Settings

| Setting | Description | Default |
|---|---|---|
| Fact Source | API to pull facts from (catfacts.cc / meowfacts / random) | Random |
| Fallback Message | Sent when API fails | "Cats are mysterious..." |
| Interval Mode | Messages received or time elapsed | Messages |
| Message Interval | Messages to wait (only in message mode) | 10000 |
| Time Interval | Time to wait (only in time mode) | 5 |
| Time Unit | Seconds, minutes, or hours (only in time mode) | Minutes |

## Commands

| Command | Description |
|---|---|
| `.catfacts` | Shows time/messages remaining until next fact |
| `.catfacts status` | Detailed status with current mode |
| `.catfacts send` | Immediately sends a cat fact |

## HUD

Add "Cat Facts" from the HUD editor. Shows either message countdown or time countdown depending on your interval mode.

## Install

Drop the jar into `.minecraft/mods` alongside Meteor Client. Requires Fabric.

## Build

```
./gradlew clean build
```

Jar will be in `build/libs/`.

## Compatibility

Works on 1.20.x and 1.21.x. Uses only Meteor Client APIs and standard Java — no direct Minecraft imports.
