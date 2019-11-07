# --- !Ups

CREATE TABLE IF NOT EXISTS sm_exif
(
    id                    VARCHAR PRIMARY KEY NOT NULL,
    date_time             TIMESTAMP,
    date_time_original    TIMESTAMP,
    date_time_digitized   TIMESTAMP,
    make                  VARCHAR,
    model                 VARCHAR,
    software              VARCHAR,
    exif_image_width      VARCHAR,
    exif_image_height     VARCHAR,
    gps_version_id        VARCHAR,
    gps_latitude_ref      VARCHAR,
    gps_latitude          VARCHAR,
    gps_longitude_ref     VARCHAR,
    gps_longitude         VARCHAR,
    gps_altitude_ref      VARCHAR,
    gps_altitude          VARCHAR,
    gps_time_stamp        VARCHAR,
    gps_processing_method VARCHAR,
    gps_date_stamp        VARCHAR,
    gps_latitude_dec      DECIMAL,
    gps_longitude_dec     DECIMAL
);



# --- !Downs

DROP TABLE IF EXISTS sm_exif CASCADE;

