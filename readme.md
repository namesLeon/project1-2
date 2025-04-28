## GTFS MySQL database Setup and Loader Guide

### set up the MySQL Database

1. **install MySQL Server** (if you don't have it yet):
   - Download from [https://dev.mysql.com/downloads/mysql/](https://dev.mysql.com/downloads/mysql/)

2. **Start your MySQL Server**:
   - Windows: start mySQL service meow
   - Mac/Linux: `sudo systemctl start mysql`

3. **Create a new database**:
   Open a terminal and run:

   ```bash
   mysql -u root -p
   ```

   Inside the MySQL shell:

   ```sql
   CREATE DATABASE gtfs_berlin;
   exit;
   ```

4. **Import the provided GTFS SQL dump**:
   In your terminal:

   ```bash
   mysql -u root -p gtfs_berlin < path/to/gtfs_berlin.sql
   ```
and that s it !!! should work now ^-^
---

###  GTFSMySQLLoader . java

The `GTFSMySQLLoader` class **connects to the MySQL database** and **loads GTFS data** into Java objects.

It supports:
- `loadStops()` → Load stops from the `stops` table into a `Map<String, Stop>`
- `loadRoutes()` → Load routes from the `routes` table into a `Map<String, Route>`
- `loadTrips()` → Load trips from the `trips` table into a `Map<String, Trip>`
- `loadStopTimes()` → Load stop times from the `stop_times` table into a `List<StopTime>`
- `loadCalendarDates()` → Load service dates from the `calendar_dates` table into a `List<CalendarDate>`

The loader uses:
- Standard JDBC (Java Database Connectivity)
- Works similarly to the original ZIP GTFSLoader, but reads data from MySQL instead of a `.zip` file.

NOTE : the data loaded in the current BERLIN gtfs database are the preprocessed csv files meow.

---