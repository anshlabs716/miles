# Configuration

How to configure MILES.

Reference for configuring MILES.

## Where configuration lives

Configuration is provided through the project's own configuration files and
build settings. Do not commit secrets, keys, tokens or machine-specific paths.

## Options

Document each user-facing option here as it is added, including its default and
effect. Options that are not implemented should not be listed.

### Target zone alerts

Settings → Workout → Target Zone Alerts. During a recorded workout MILES compares
live, measured values against these zones and cues you when you drift outside
them. Alerts use only real sensor data: heart rate from a connected sensor, and
speed and pace from GPS. Without a heart-rate sensor the heart-rate alert stays
silent. At most one alert fires every 15 seconds.

| Option | Default | Effect |
| --- | --- | --- |
| Heart Rate Zone Alert | On, 120–175 BPM | Alerts when heart rate goes above or below the zone. Zone adjustable from 80 to 210 BPM in 5 BPM steps. |
| Pace Alert | On, ±25 s per km | Alerts when pace is slower or faster than the target pace by more than the tolerance (5–60 s). Only checked while moving faster than 3 km/h. The target pace itself is chosen on the workout HUD's ghost-pacer page. |
| Speed Alert | Off, 8–16 km/h | Alerts when speed goes below or above the range (2–60 km/h). Useful for cycling. |
| Vibrate on Alert | On | Vibrates on each alert: a double buzz for warnings (heart rate too high, speed too high), a short rhythm otherwise. |
| Speak Alert | On | Speaks the alert message using the device's text-to-speech. |

The workout HUD's ghost-pacer page also has a **Pace alerts** switch next to the
target-pace picker. It is the same setting as Pace Alert above; turning it off
silences pace alerts but leaves the ghost pacer running.

## Environment

Where environment variables are supported, an `.env.example` file lists the
required names with placeholder values only.
