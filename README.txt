
* Custom Query Based Data Source Class: 'org.wso2.dss.connectors.rethinkdb.RethinkDBDataSource'

* Data Source Properties :-

  - 'rethinkDB_server': the host to connect to (default localhost).
     e.g.:-
      - "localhost"
      - "10.100.7.3"
  - 'rethinkDB_port': the port to connect on (default 28015).
     e.g.:-
      - "28015"
  - 'rethinkDB_database': database name (default test).
     e.g.:-
      - "test"
  - 'rethinkDB_timeout': timeout period in seconds for the connection to be opened (default 20).
     e.g.:-
      - "20"

* Required dependencies (to be copied to "<dss_home>/repository/components/lib"):-

  - rethinkdb-driver (rethinkdb-driver-2.3.0.jar)
  - json-simple (json-simple-1.1.1.jar)
  - ds-connector-rethinkdb (ds-connector-rethinkdb-1.0.0-SNAPSHOT.jar)



