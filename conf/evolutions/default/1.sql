# --- !Ups

CREATE TABLE "public".sm_category_fc
(
  id                varchar NOT NULL,
  f_name            varchar NOT NULL,
  category_type     varchar,
  sub_category_type varchar,
  description       varchar,
  CONSTRAINT sm_category_fc_pkey PRIMARY KEY (id, f_name)
);

CREATE TABLE "public".sm_device
(
  id          serial PRIMARY KEY,
  name        varchar           NOT NULL,
  label       varchar           NOT NULL,
  device_uid  varchar           NOT NULL,
  sync_date   timestamp         NOT NULL,
  description varchar,
  visible     bool DEFAULT true NOT NULL,
  reliable    bool DEFAULT true NOT NULL,
  CONSTRAINT sm_device_pkey PRIMARY KEY (id)
);

CREATE TABLE "public".sm_exif
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
  gps_latitude_dec      numeric(-9999999,0),
  gps_longitude_dec     numeric(-9999999,0),
  CONSTRAINT sm_exif_pkey PRIMARY KEY (id)
);

CREATE TABLE "public".sm_file_card
(
  id                   VARCHAR PRIMARY KEY NOT NULL,
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
  CONSTRAINT sm_file_card_pkey PRIMARY KEY (id)
);

CREATE INDEX f_parent_idx ON "public".sm_file_card ( f_parent ASC NULLS LAST);

CREATE INDEX last_modified_idx ON "public".sm_file_card ( f_last_modified_date DESC NULLS LAST);

CREATE INDEX sha256_idx ON "public".sm_file_card ( sha256 ASC NULLS LAST);


CREATE TABLE "public".sm_job_path_move
(
  id         serial PRIMARY KEY,
  device_uid varchar NOT NULL,
  path_from  varchar NOT NULL,
  path_to    varchar NOT NULL,
  done       bool DEFAULT false,
  CONSTRAINT sm_path_move_pkey PRIMARY KEY (id),
);

# --- !Downs

DROP TABLE IF EXISTS sm_device CASCADE;

DROP TABLE IF EXISTS sm_file_card CASCADE;

DROP TABLE IF EXISTS sm_path_move CASCADE;

DROP TABLE IF EXISTS sm_category_fc CASCADE;

DROP TABLE IF EXISTS sm_exif CASCADE;
