**Storage manager** is a web application based on:

* [Scala 2.13.3 as programming language](http://www.scala-lang.org/)
* [Play Framework 2.8.2 as web application framework](https://www.playframework.com/)
* [SBT 1.3.13 as build tool](http://www.scala-sbt.org/)
* [PostgreSQL 9.5 as database](http://www.postgresql.org)
* [Slick 3.3.3 as database access layer](http://slick.lightbend.com)
* [Slick-pg 0.19.3 extending slick for support PostgreSQL data types](https://github.com/tminglei/slick-pg)
* [Foundation 6.4.3 as front-end framework](http://foundation.zurb.com)

## Database

### Database setup
Check the [PostgreSQL website](http://www.postgresql.org/download/) to instalation instructions.

Create a cluster, a user and a database:

####CREATE MAIN DB
    sudo -u postgres pg_createcluster -p 55554 --start 9.5 play_sm_cluster
    sudo -u postgres psql -p 55554 -c "CREATE USER play_sm_user PASSWORD '123';"
    sudo -u postgres psql -p 55554 -c "CREATE DATABASE play_sm_db WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8';"
    sudo -u postgres psql -p 55554 -c "ALTER DATABASE play_sm_db OWNER TO play_sm_user;"
    -- run evolutions
    sudo -u postgres psql -p 55554 play_sm_db -c "CREATE EXTENSION pg_trgm with schema public;"
    -- run in SQL or evolutions
    CREATE INDEX idx_gin_trgm_sm_file_card_f_name_lc ON sm_file_card USING gin(f_name_lc gin_trgm_ops);


    sudo -u postgres psql -p 55554 play_sm_db -c "VACUUM FREEZE ANALYZE;"


#####Change DB settings
    ALTER SYSTEM SET max_connections = '300';
    ALTER SYSTEM SET shared_buffers = '512MB';
    ALTER SYSTEM SET effective_cache_size = '1536MB';
    ALTER SYSTEM SET maintenance_work_mem = '128MB';
    ALTER SYSTEM SET checkpoint_completion_target = '0.9';
    ALTER SYSTEM SET wal_buffers = '16MB';
    ALTER SYSTEM SET default_statistics_target = '100';
    ALTER SYSTEM SET random_page_cost = '1.1';
    ALTER SYSTEM SET effective_io_concurrency = '200';
    ALTER SYSTEM SET work_mem = '1747kB';
    ALTER SYSTEM SET min_wal_size = '2GB';
    ALTER SYSTEM SET max_wal_size = '4GB';
    ALTER SYSTEM SET max_worker_processes = '8';


SELECT *
FROM pg_settings
WHERE name in ('max_connections',
               'shared_buffers',
               'effective_cache_size',
               'maintenance_work_mem',
               'checkpoint_completion_target',
               'wal_buffers',
               'default_statistics_target',
               'random_page_cost',
               'effective_io_concurrency',
               'work_mem',
               'min_wal_size',
               'max_wal_size',
               'max_worker_processes'
    )
;

-------------------------------------------------------------------------------------------------------------------



####CREATE TEST DB
    sudo -u postgres pg_createcluster -p 55553 --start 9.5 play_sm_cluster_test
    sudo -u postgres psql -p 55553 -c "CREATE USER play_sm_user PASSWORD '123';"
    sudo -u postgres psql -p 55553 -c "CREATE DATABASE play_sm_db_test WITH TEMPLATE = template0 ENCODING = 'UTF8' LC_COLLATE = 'en_US.utf8' LC_CTYPE = 'en_US.utf8';"
    sudo -u postgres psql -p 55553 -c "ALTER DATABASE play_sm_db_test OWNER TO play_sm_user;"

### Database mapping code
The file `models.db.Tables.scala` contains the database mapping code. It has been generated running the main class
`utils.db.SourceCodeGenerator`. If you want to regenerate the database mapping code for any reason, check the
config file `conf/application.conf` and run:

    sbt tables

## SBT

To run the project execute:

    sbt run

And open a browser with the url [http://localhost:9000](http://localhost:9000)

The plugin [sbt-updates](https://github.com/rtimush/sbt-updates) is installed (see `plugins.sbt`). To check
if all the dependencies are up to date, it is necessary to execute:

    sbt dependencyUpdates

##First steps
after create DB and run, go to http://localhost:9000/deviceImport and press "import devices" button.
Edit config file `conf/scanImport.conf` section "paths2Scan.volumes" add device ID (from sm_device.uid table) and path for scan, for example:
        "device_id" = [
          "home/user"
          "home/user/Documents"
          "home/user/Downloads"
        ]

Then run "sync device" button

