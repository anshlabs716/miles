# Architecture

High-level structure of MILES.

# Architecture

This page describes the high-level structure of MILES. It is a starting
point and is updated as the implementation evolves.

## Overview

MILES

## Main components

- `**Kotlin**`
- `**Jetpack Compose / Material 3**`
- `**Room**`
- `**AndroidX**`
- `**Health Connect**`

## Data flow

Inputs (user actions, sensors, files or network requests) are processed by the
application logic and persisted or rendered as appropriate. Data handling is
described in [configuration.md](configuration.md) and, where relevant, in the
privacy documentation.

## Repository layout

See the project [README](../README.md) for the directory layout and build entry
points.

## Decisions

Significant design decisions are recorded under [adr/README.md](adr/README.md).
