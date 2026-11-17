# Amsterdam Routing Engine

This project is a Java 17 public-transport routing engine for Amsterdam. It loads a
GTFS feed, builds an in-memory transit graph, and reads routing commands as JSON from
standard input. Routes can combine walking and public transport and are returned as
JSON on standard output.

The project is built with [Maven](https://maven.apache.org/) and uses Apache Commons
CSV for GTFS parsing, MySQL Connector/J for the optional database utilities, and
JXMapViewer for map visualisation components.

## Requirements

- Java Development Kit (JDK) 17 or newer
- Maven 3.8 or newer
- The provided GTFS archive, or another compatible GTFS ZIP feed

The feed used by this repository is
[`data/amsterdam_gtfs_final.zip`](data/amsterdam_gtfs_final.zip). A compatible feed
must contain `stops.txt`, `agency.txt`, `routes.txt`, one or more `trips*.txt` files,
and one or more `stop_times*.txt` files.

## Build and run

Run these commands from the repository root:

```text
mvn clean
mvn compile
mvn test
```

The application entry point is `clinterface.RoutingEngine`, configured in
[`pom.xml`](pom.xml). Start it with:

```text
mvn -q exec:java
```

The application is a streaming process: send one JSON request per line (or multiple
JSON values through standard input) and read one JSON response per request.

## JSON protocol

### Load a GTFS feed

Load the feed before requesting a route:

```json
{"load":"data/amsterdam_gtfs_final.zip"}
```

Successful loading returns:

```json
{"ok":"loaded"}
```

The path is resolved relative to the process working directory. Loading replaces the
currently loaded graph and computes walking connections between stops.

### Request a route

`startingAt` uses `HH:mm` notation. Coordinates are decimal latitude and longitude.

```json
{
  "routeFrom": {"lat": 52.3702, "lon": 4.8952},
  "to": {"lat": 52.3791, "lon": 4.9003},
  "startingAt": "08:30"
}
```

The response has an `ok` array containing journey steps. Each step includes:

- `mode`: `walk` or `ride`
- `to`: destination coordinates for that step
- `duration`: duration in minutes
- `startTime`: step start time

Ride steps additionally include the stop name and route metadata:

```json
{
  "ok": [
    {
      "mode": "ride",
      "to": {"lat": 52.36, "lon": 4.91},
      "duration": 12,
      "startTime": "08:42",
      "stop": "Central Station",
      "route": {
        "operator": "Operator",
        "shortName": "2",
        "longName": "Example line",
        "headSign": "Example destination"
      }
    }
  ]
}
```

If no transit route improves on walking, the engine returns a walking journey. Errors
are returned as JSON, for example `{"error":"No GTFS data loaded"}`.

### Temporarily remove stops

Stops can be removed for a single route request:

```json
{
  "routeFrom": {"lat": 52.3702, "lon": 4.8952},
  "to": {"lat": 52.3791, "lon": 4.9003},
  "startingAt": "08:30",
  "deletedStopIds": ["stop-id-1", "stop-id-2"]
}
```

They can also be removed when loading a feed:

```json
{
  "load": "data/amsterdam_gtfs_final.zip",
  "deletedStopIds": ["stop-id-1"]
}
```

The load-time option remains active until the graph is reloaded.

### Batch routes

After loading a feed, process newline-delimited route requests in parallel:

```json
{"batchRoutes":{"input":"data/routes-input.jsonl","output":"data/routes-output.jsonl"}}
```

Each input line must be a route request. The output contains one JSON result per
input line in the original order.

### Run a request file

The `runFile` command reads a file containing newline-delimited JSON commands:

```json
{"runFile":"data/commands.jsonl"}
```

This is useful for replaying a load-and-route sequence without typing requests
interactively.

## Optional MySQL utilities

The main routing process uses the in-memory `GTFSLoader`; it does not require MySQL.
The `DBManager` and `DBloader` classes provide an optional MySQL workflow for loading
the GTFS data and generating database edges.

1. Install and start MySQL.
2. Create the database:

   ```sql
   CREATE DATABASE gtfs_Amsterdam;
   ```

3. Create a user, or update the credentials in
   [`DBManager.java`](src/main/java/DB/DBManager.java) to match an existing account.
4. From the repository root, run:

   ```text
   mvn -q exec:java -Dexec.mainClass=DB.DBManager
   ```

The database schema is in [`amsterdam_database/schema.sql`](amsterdam_database/schema.sql).
The database loader imports the GTFS ZIP into MySQL and creates an `edges` table used
by the database-specific stop-deletion helpers.

## Project layout

- [`src/main/java/clinterface/`](src/main/java/clinterface/): application entry point,
  JSON protocol, and GTFS ZIP loader
- [`src/main/java/planner/`](src/main/java/planner/): route search and journey
  reconstruction
- [`src/main/java/storage/`](src/main/java/storage/): in-memory transit graph
- [`src/main/java/structs/`](src/main/java/structs/): stops, trips, departures, and
  journey result types
- [`src/main/java/gui/`](src/main/java/gui/): map and route/heat-map visualisation
- [`src/main/java/DB/`](src/main/java/DB/): optional MySQL loading and edge utilities
- [`src/main/resources/tiles.zip`](src/main/resources/tiles.zip): bundled map tiles
- [`map.HTML`](map.HTML): map-related project resource

## Troubleshooting

- **`No GTFS data loaded`**: send a `load` request before a route or batch request.
- **`File not found`**: check that the ZIP or input file path is relative to the
  directory where Maven was started.
- **Bad request errors**: verify that coordinates are numbers, `startingAt` is in
  `HH:mm` format, and every request is a JSON object.
- **MySQL connection errors**: confirm that MySQL is running, the database exists, and
  the credentials and JDBC URL in `DBManager.java` are correct.
