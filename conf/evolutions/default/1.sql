# --- !Ups

CREATE TABLE sm_category_rule
(
  id              SERIAL NOT NULL,
  category_type   VARCHAR NOT NULL,
  category        VARCHAR NOT NULL,
  sub_category    VARCHAR NOT NULL,
  f_path          TEXT[] NOT NULL,
  is_begins       BOOL NOT NULL,
  description     VARCHAR,
  CONSTRAINT sm_category_rule_pkey UNIQUE (category_type,category,sub_category),
  CONSTRAINT unq_sm_category_rule_id UNIQUE (id)
);

CREATE TABLE sm_device
(
  id               SERIAL NOT NULL,
  uid              VARCHAR NOT NULL,
  name             VARCHAR NOT NULL,
  label_v          VARCHAR NOT NULL,
  name_v           VARCHAR,
  description      VARCHAR,
  visible          BOOL DEFAULT TRUE NOT NULL,
  reliable         BOOL DEFAULT TRUE NOT NULL,
  path_scan_date   TIMESTAMP NOT NULL,
  crc_date         TIMESTAMP,
  exif_date        TIMESTAMP,
  job_path_scan    BOOL DEFAULT FALSE NOT NULL,
  job_calc_crc     BOOL DEFAULT FALSE NOT NULL,
  job_calc_exif    BOOL DEFAULT FALSE NOT NULL,
  job_resize       BOOL DEFAULT FALSE NOT NULL,
  CONSTRAINT sm_device_pkey PRIMARY KEY (id),
  CONSTRAINT idx_sm_device_device_uid UNIQUE (uid)
);

CREATE TABLE sm_device_scan
(
  device_uid   VARCHAR NOT NULL,
  f_path       VARCHAR NOT NULL,
  CONSTRAINT idx_sm_device_scan_device_uid UNIQUE (device_uid,f_path),
  CONSTRAINT fk_sm_device_scan_sm_device FOREIGN KEY (device_uid) REFERENCES sm_device (uid)
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
  CONSTRAINT fk_sm_file_card_sm_device FOREIGN KEY (device_uid) REFERENCES sm_device (uid) ON DELETE RESTRICT
);

CREATE INDEX idx_f_parent
ON sm_file_card (f_parent);

CREATE INDEX idx_fc_f_name_lc
ON sm_file_card (f_name_lc);

CREATE INDEX idx_last_modified
ON sm_file_card (f_last_modified_date);

CREATE INDEX idx_sm_file_card_device_uid
ON sm_file_card (device_uid, f_parent);

CREATE INDEX idx_sha256
ON sm_file_card (sha256);

CREATE INDEX idx_fc_sha_name
ON sm_file_card (sha256, f_name);

CREATE TABLE sm_image_resize
(
  file_id   VARCHAR NOT NULL,
  sha256    VARCHAR NOT NULL,
  f_name    VARCHAR NOT NULL,
  CONSTRAINT sm_image_resize_uniq UNIQUE (sha256,f_name),
  CONSTRAINT pk_sm_image_resize_file_id PRIMARY KEY (file_id)
);

CREATE TABLE sm_job_path_move
(
  id           SERIAL NOT NULL,
  device_uid   VARCHAR NOT NULL,
  path_from    VARCHAR NOT NULL,
  path_to      VARCHAR NOT NULL,
  done         TIMESTAMP,
  CONSTRAINT idx_sm_job_path_move_device_uid PRIMARY KEY (device_uid,path_from),
  CONSTRAINT unq_sm_job_path_move UNIQUE (id),
  CONSTRAINT fk_sm_job_path_move_sm_device FOREIGN KEY (device_uid) REFERENCES sm_device (uid)
);

CREATE TABLE sm_log
(
  create_date   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  device_uid    VARCHAR NOT NULL,
  level         VARCHAR NOT NULL,
  step          VARCHAR NOT NULL,
  error         VARCHAR NOT NULL,
  stack_trace   VARCHAR,
  CONSTRAINT fk_sm_log_sm_device FOREIGN KEY (device_uid) REFERENCES sm_device (uid)
);

CREATE INDEX idx_sm_log_device_uid
ON sm_log (device_uid);

CREATE TABLE sm_category_fc
(
  id       INTEGER NOT NULL,
  sha256   VARCHAR NOT NULL,
  f_name   VARCHAR NOT NULL,
  CONSTRAINT sm_category_fc_pkey PRIMARY KEY (sha256,f_name),
  CONSTRAINT fk_sm_category_fc_sm_category_rule FOREIGN KEY (id) REFERENCES sm_category_rule (id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_sm_category_fc_id
ON sm_category_fc (id);

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
  CONSTRAINT sm_exif_pkey PRIMARY KEY (id),
  CONSTRAINT fk_sm_exif_sm_file_card FOREIGN KEY (id) REFERENCES sm_file_card (id) ON DELETE CASCADE
);

# --- !Downs

DROP TABLE IF EXISTS sm_device_scan CASCADE;

DROP TABLE IF EXISTS sm_log CASCADE;

DROP TABLE IF EXISTS sm_image_resize CASCADE;

DROP TABLE IF EXISTS sm_file_card CASCADE;

DROP TABLE IF EXISTS sm_category_rule CASCADE;

DROP TABLE IF EXISTS sm_job_path_move CASCADE;

DROP TABLE IF EXISTS sm_category_fc CASCADE;

DROP TABLE IF EXISTS sm_exif CASCADE;

DROP TABLE IF EXISTS sm_device CASCADE;
