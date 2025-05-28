-- Drop existing tables (if any) to allow re-running
DROP TABLE IF EXISTS stop_times;
DROP TABLE IF EXISTS calendar_dates;
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS stops;

-- 1. calendar_dates
CREATE TABLE calendar_dates (
        service_id          VARCHAR(50)   NOT NULL,
        date                DATE          NOT NULL,
        exception_type      TINYINT       NOT NULL,
        PRIMARY KEY (service_id, date)
) ENGINE=InnoDB
    COMMENT='Exceptions for service schedules (GTFS calendar_dates.txt)';

-- 2. stops
CREATE TABLE stops (
       stop_id             VARCHAR(50)    NOT NULL,
       stop_code           VARCHAR(50)    NULL,
       stop_name           VARCHAR(255)   NOT NULL,
       stop_lat            DECIMAL(9,6)   NOT NULL,
       stop_lon            DECIMAL(9,6)   NOT NULL,
       location_type       TINYINT        NOT NULL DEFAULT 0,
       parent_station      VARCHAR(50)    NULL,
       stop_timezone       VARCHAR(50)    NULL,
       wheelchair_boarding TINYINT        NOT NULL DEFAULT 0,
       platform_code       VARCHAR(50)    NULL,
       zone_id             VARCHAR(50)    NULL,
       PRIMARY KEY (stop_id)
) ENGINE=InnoDB
    COMMENT='Stops and stations (GTFS stops.txt)';

-- 3. trips
CREATE TABLE trips (
       trip_id             VARCHAR(50)    NOT NULL,
       route_id            VARCHAR(50)    NOT NULL,
       service_id          VARCHAR(50)    NOT NULL,
       realtime_trip_id    VARCHAR(100)   NULL,
       trip_headsign       VARCHAR(255)   NULL,
       trip_short_name     VARCHAR(100)   NULL,
       trip_long_name      VARCHAR(255)   NULL,
       direction_id        TINYINT        NULL,
       block_id            VARCHAR(50)    NULL,
       shape_id            VARCHAR(50)    NULL,
       wheelchair_accessible TINYINT      NOT NULL DEFAULT 0,
       bikes_allowed       TINYINT        NOT NULL DEFAULT 0,
       PRIMARY KEY (trip_id)
) ENGINE=InnoDB
    COMMENT='Trips (GTFS trips.txt)';

-- 4. stop_times
CREATE TABLE stop_times (
        trip_id             VARCHAR(50)    NOT NULL,
        stop_sequence       INT            NOT NULL,
        stop_id             VARCHAR(50)    NOT NULL,
        stop_headsign       VARCHAR(255)   NULL,
        arrival_time        TIME           NOT NULL,
        departure_time      TIME           NOT NULL,
        pickup_type         TINYINT        NOT NULL DEFAULT 0,
        drop_off_type       TINYINT        NOT NULL DEFAULT 0,
        timepoint           TINYINT        NOT NULL DEFAULT 0,
        shape_dist_traveled DECIMAL(10,2)  NULL,
        fare_units_traveled DECIMAL(10,2)  NULL,
        PRIMARY KEY (trip_id, stop_sequence),
        FOREIGN KEY (trip_id) REFERENCES trips(trip_id),
        FOREIGN KEY (stop_id) REFERENCES stops(stop_id)
) ENGINE=InnoDB
    COMMENT='Stop times and sequencing for each trip (GTFS stop_times.txt)';
