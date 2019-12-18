# --- !Ups

CREATE TABLE sm_category_fc
(
  id                varchar NOT NULL,
  f_name            varchar NOT NULL,
  category_type     varchar,
  sub_category_type varchar,
  description       varchar,
  CONSTRAINT sm_category_fc_pkey PRIMARY KEY (id, f_name)
);

CREATE TABLE sm_device
(
  id          serial            NOT NULL,
  name        varchar           NOT NULL,
  label       varchar           NOT NULL,
  device_uid  varchar           NOT NULL,
  sync_date   timestamp         NOT NULL,
  description varchar,
  visible     bool DEFAULT true NOT NULL,
  reliable    bool DEFAULT true NOT NULL,
  CONSTRAINT sm_device_pkey PRIMARY KEY (id),
  CONSTRAINT idx_sm_device_device_uid UNIQUE (device_uid)
);

CREATE TABLE sm_exif
(
  id                    varchar NOT NULL,
  date_time             timestamp,
  date_time_original    timestamp,
  date_time_digitized   timestamp,
  make                  varchar,
  model                 varchar,
  software              varchar,
  exif_image_width      varchar,
  exif_image_height     varchar,
  gps_version_id        varchar,
  gps_latitude_ref      varchar,
  gps_latitude          varchar,
  gps_longitude_ref     varchar,
  gps_longitude         varchar,
  gps_altitude_ref      varchar,
  gps_altitude          varchar,
  gps_time_stamp        varchar,
  gps_processing_method varchar,
  gps_date_stamp        varchar,
  gps_latitude_dec      decimal,
  gps_longitude_dec     decimal,
  CONSTRAINT sm_exif_pkey PRIMARY KEY (id)
);

CREATE TABLE sm_file_card
(
  id                   varchar   NOT NULL,
  device_uid           varchar   NOT NULL,
  f_parent             varchar   NOT NULL,
  f_name               varchar   NOT NULL,
  f_extension          varchar,
  f_creation_date      timestamp NOT NULL,
  f_last_modified_date timestamp NOT NULL,
  f_size               bigint,
  f_mime_type_java     varchar,
  sha256               varchar,
  f_name_lc            varchar   NOT NULL,
  CONSTRAINT sm_file_card_pkey PRIMARY KEY (id),
  CONSTRAINT fk_sm_file_card_sm_device FOREIGN KEY (device_uid) REFERENCES sm_device (device_uid)
);

CREATE INDEX idx_f_parent ON sm_file_card ( f_parent ASC NULLS LAST);

CREATE INDEX idx_fc_f_name_lc ON sm_file_card(f_name_lc ASC NULLS LAST);

CREATE INDEX idx_last_modified ON sm_file_card ( f_last_modified_date DESC NULLS LAST);

CREATE INDEX idx_sha256 ON sm_file_card ( sha256 ASC NULLS LAST);

CREATE TABLE sm_job_path_move
(
  id         serial  NOT NULL,
  device_uid varchar NOT NULL,
  path_from  varchar NOT NULL,
  path_to    varchar NOT NULL,
  done       bool DEFAULT false,
  CONSTRAINT sm_job_path_move_pkey PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE IF EXISTS sm_file_card CASCADE;

DROP TABLE IF EXISTS sm_job_path_move CASCADE;

DROP TABLE IF EXISTS sm_category_fc CASCADE;

DROP TABLE IF EXISTS sm_exif CASCADE;

DROP TABLE IF EXISTS sm_device CASCADE;
