# --- !Ups

CREATE TABLE sm_category_fc
(
  id                  VARCHAR NOT NULL,
  f_name              VARCHAR NOT NULL,
  category_type       VARCHAR,
  sub_category_type   VARCHAR,
  description         VARCHAR,
  CONSTRAINT sm_category_fc_pkey PRIMARY KEY (id,f_name),
  CONSTRAINT unq_sm_category_fc_id UNIQUE (id)
);

CREATE TABLE sm_device
(
  id            SERIAL NOT NULL,
  name          VARCHAR NOT NULL,
  label         VARCHAR NOT NULL,
  uid           VARCHAR NOT NULL,
  sync_date     TIMESTAMP NOT NULL,
  description   VARCHAR,
  visible       BOOL DEFAULT TRUE NOT NULL,
  reliable      BOOL DEFAULT TRUE NOT NULL,
  CONSTRAINT sm_device_pkey PRIMARY KEY (id),
  CONSTRAINT idx_sm_device_device_uid UNIQUE (uid)
);

CREATE TABLE sm_exif
(
  id                      VARCHAR NOT NULL,
  date_time               TIMESTAMP,
  date_time_original      TIMESTAMP,
  date_time_digitized     TIMESTAMP,
  make                    VARCHAR,
  model                   VARCHAR,
  software                VARCHAR,
  exif_image_width        VARCHAR,
  exif_image_height       VARCHAR,
  gps_version_id          VARCHAR,
  gps_latitude_ref        VARCHAR,
  gps_latitude            VARCHAR,
  gps_longitude_ref       VARCHAR,
  gps_longitude           VARCHAR,
  gps_altitude_ref        VARCHAR,
  gps_altitude            VARCHAR,
  gps_time_stamp          VARCHAR,
  gps_processing_method   VARCHAR,
  gps_date_stamp          VARCHAR,
  gps_latitude_dec        DECIMAL,
  gps_longitude_dec       DECIMAL,
  CONSTRAINT sm_exif_pkey PRIMARY KEY (id)
);

CREATE TABLE sm_job_path_move
(
  id           SERIAL NOT NULL,
  device_uid   VARCHAR NOT NULL,
  path_from    VARCHAR NOT NULL,
  path_to      VARCHAR NOT NULL,
  done         TIMESTAMP,
  CONSTRAINT sm_job_path_move_pkey PRIMARY KEY (id),
  CONSTRAINT unq_sm_job_path_move_device_uid UNIQUE (device_uid,path_from)
);

CREATE TABLE sm_file_card
(
  id                     VARCHAR NOT NULL,
  device_uid             VARCHAR NOT NULL,
  f_parent               VARCHAR NOT NULL,
  f_name                 VARCHAR NOT NULL,
  f_extension            VARCHAR,
  f_creation_date        TIMESTAMP NOT NULL,
  f_last_modified_date   TIMESTAMP NOT NULL,
  f_size                 BIGINT,
  f_mime_type_java       VARCHAR,
  sha256                 VARCHAR,
  f_name_lc              VARCHAR NOT NULL,
  CONSTRAINT sm_file_card_pkey PRIMARY KEY (id),
  CONSTRAINT fk_sm_file_card_sm_device FOREIGN KEY (device_uid) REFERENCES sm_device (uid),
  CONSTRAINT fk_sm_file_card_sm_category_fc FOREIGN KEY (sha256,f_name) REFERENCES sm_category_fc (id,f_name)
-- ,
--   CONSTRAINT fk_sm_file_card_sm_exif FOREIGN KEY (id) REFERENCES sm_exif (id),
--   CONSTRAINT fk_sm_file_card_sm_job_path_move FOREIGN KEY (device_uid,f_parent) REFERENCES sm_job_path_move (device_uid,path_from)
);

CREATE INDEX idx_f_parent
ON sm_file_card (f_parent);

CREATE INDEX idx_fc_f_name_lc
ON sm_file_card (f_name_lc);

CREATE INDEX idx_last_modified
ON sm_file_card (f_last_modified_date);

CREATE INDEX idx_sha256
ON sm_file_card (sha256);

CREATE INDEX idx_sm_file_card_device_uid
ON sm_file_card (device_uid, f_parent);

CREATE INDEX idx_sm_file_card_sha256
ON sm_file_card (sha256, f_name);


# --- !Downs

DROP TABLE IF EXISTS sm_file_card CASCADE;

DROP TABLE IF EXISTS sm_job_path_move CASCADE;

DROP TABLE IF EXISTS sm_category_fc CASCADE;

DROP TABLE IF EXISTS sm_exif CASCADE;

DROP TABLE IF EXISTS sm_device CASCADE;
