# Inaris Restart

A Fabric server mod for Minecraft 26.1.2 that adds a `/restart` command with a countdown boss bar and title screen notifications.

## Features

- `/restart <seconds> [reason]` — schedules a server restart with a countdown
- `/cancelrestart` — cancels an in-progress restart
- Red boss bar (full → empty) tied to the countdown timer
- Title screen flashes at key intervals: every minute, 30s, and every second in the last 10
- Reason displayed as subtitle on the title screen
- Chat broadcast when a restart is scheduled or cancelled
- Players who join mid-countdown automatically receive the boss bar
- On countdown end: players are kicked and the server halts — your process manager (e.g. Pterodactyl) restarts it automatically

## Usage

```
/restart 300 Weekly maintenance
/restart 60 Applying updates
/cancelrestart
```

Time is always in seconds. The mod formats the display automatically (`5 minutes`, `2m 30s`, `10 seconds`).

Both commands require operator level 2 or higher.

## Requirements

- Minecraft 26.1.2
- Fabric Loader ≥ 0.19.3
- Fabric API

## Notes

This mod calls `server.halt()` to stop the process. It does not restart the server itself — that is handled by your hosting panel or process manager. Inaris Restart was built for and tested on [UltraServers](https://ultraservers.com) running Pterodactyl, which detects the halt and brings the server back up automatically.
