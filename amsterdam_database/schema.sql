-- Drop existing tables (if any) to allow re-running
DROP TABLE IF EXISTS stop_times;
DROP TABLE IF EXISTS calendar_dates;
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS stops;
DROP TABLE IF EXISTS agency;
DROP TABLE IF EXISTS calendar;
DROP TABLE IF EXISTS routes;


-- 1. stops
CREATE TABLE stops (
       stop_id             VARCHAR(50)    NOT NULL,
       stop_name           VARCHAR(255),
       stop_lat            DECIMAL(9,6)   NOT NULL,
       stop_lon            DECIMAL(9,6)   NOT NULL,
       PRIMARY KEY (stop_id)
) ENGINE=InnoDB
    COMMENT='Stops and stations (GTFS stops.txt)';

-- 2. routes
CREATE TABLE routes (
    route_id            VARCHAR(100) PRIMARY KEY,
    agency_id           VARCHAR(100) NOT NULL,
    route_short_name    VARCHAR(100),
    route_long_name     VARCHAR(255)
) ENGINE=InnoDB
  COMMENT='Routes table (GTFS routes.txt)';

-- 3. trips
CREATE TABLE trips (
       trip_id             VARCHAR(50)    NOT NULL,
       route_id            VARCHAR(100)    NOT NULL,
       trip_headsign       VARCHAR(100)    NOT NULL,
       service_id          VARCHAR(50)    NOT NULL,
       PRIMARY KEY (trip_id),
       FOREIGN KEY (route_id) REFERENCES routes(route_id)
) ENGINE=InnoDB
    COMMENT='Trips (GTFS trips.txt)';

-- 4. stop_times
CREATE TABLE stop_times (
        trip_id             VARCHAR(50)    NOT NULL,
        stop_sequence       INT            NOT NULL,
        stop_id             VARCHAR(50)    NOT NULL,
        arrival_time        TIME           NOT NULL,
        departure_time      TIME           NOT NULL,
        PRIMARY KEY (trip_id, stop_sequence),
        FOREIGN KEY (trip_id) REFERENCES trips(trip_id),
        FOREIGN KEY (stop_id) REFERENCES stops(stop_id)
) ENGINE=InnoDB
    COMMENT='Stop times and sequencing for each trip (GTFS stop_times.txt)';

-- 5. agency
CREATE TABLE agency (
        agency_id           VARCHAR(100) PRIMARY KEY,
        agency_name         VARCHAR(100) NOT NULL,
        agency_url          VARCHAR(100) NOT NULL
) ENGINE=InnoDB
      COMMENT='Agencies table (GTFS agency.txt)';

-- 6. add indexing
 CREATE INDEX stop_times_stop_id_ix ON stop_times(stop_id);
 CREATE INDEX stop_times_stop_sequence_ix ON stop_times(stop_sequence);
