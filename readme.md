# BCS1600 Spring 2025 Project Skeleton

The files in this repository can be used as a basis for your implementation of the Routing
Engine portion of the project.

[Maven]: https://maven.apache.org/

## Building

Use [Maven][] to build, clean, test, and start your program. The command-line tool is called
`mvn`, and IDEs like IntelliJ IDEA and Visual Studio Code have built-in support for Maven
actions. Maven is configured with the [`pom.xml`](pom.xml) file in the root directory of your
repository.

 - `mvn clean`: removes the `target/` directory (but note that e.g. vscode will immediately put
   it back if vscode is open on the project!).

 - `mvn compile`: build the main project.

 - `mvn test`: run the test suite.

 - `mvn exec:java`: invoke the main class of the project (`exec.mainClass` property in
   `pom.xml`). Use the `-q` flag to silence Maven's own output: `mvn -q exec:java`.

## Files

 - `pom.xml`: [Maven][] configuration file.

 - `src/main/java/RoutingEngine.java`: Skeleton Routing Engine server implementation for you to
   build on.



## GTFS MySQL database setup and loader guide

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
   CREATE DATABASE gtfs_Amsterdam;
   exit;
   ```

4. **Import the provided GTFS SQL dump**:
   In your terminal:

   ```bash
   mysql -u root -p gtfs_Amsterdam < path/to/gtfs_Amsterdam.sql
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

NOTE : the data loaded in the current Amsterdam gtfs database are the preprocessed csv files meow.

---
