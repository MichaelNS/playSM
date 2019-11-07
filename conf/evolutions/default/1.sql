# --- !Ups

CREATE TABLE IF NOT EXISTS sm_device
(
  id        serial PRIMARY KEY,
  name      VARCHAR              NOT NULL,
  label     VARCHAR              NOT NULL,
  uid       VARCHAR              NOT NULL,
  sync_date TIMESTAMP            NOT NULL,
  describe  VARCHAR              NULL,
  visible   BOOLEAN DEFAULT TRUE NOT NULL,
  reliable  BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE IF NOT EXISTS sm_file_card
(
  id                   VARCHAR PRIMARY KEY NOT NULL,
  store_name           VARCHAR             NOT NULL,
  f_parent             VARCHAR             NOT NULL,
  f_name               VARCHAR             NOT NULL,
  f_extension          VARCHAR,
  f_creation_date      TIMESTAMP           NOT NULL,
  f_last_modified_date TIMESTAMP           NOT NULL,
  f_size               BIGINT,
  f_mime_type_java     VARCHAR,
  sha256               VARCHAR,
  f_name_lc            VARCHAR             NOT NULL
);

-- CREATE INDEX sha256_idx ON sm_file_card (sha256 ASC NULLS LAST);

CREATE INDEX sha256_idx
  ON sm_file_card
    (sha256 ASC NULLS LAST);

CREATE INDEX f_parent_idx
  ON sm_file_card
    (f_parent ASC NULLS LAST);

CREATE INDEX last_modified_idx
  ON sm_file_card
    (f_last_modified_date DESC NULLS LAST);


CREATE TABLE IF NOT EXISTS sm_path_move
(
  id         serial PRIMARY KEY,
  store_name VARCHAR NOT NULL,
  path_from  VARCHAR NOT NULL,
  path_to    VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS sm_category_fc
(
  id                VARCHAR,
  f_name            VARCHAR,
  category_type     VARCHAR,
  sub_category_type VARCHAR,
  description       VARCHAR,
  PRIMARY KEY (id, f_name)
);


# --- !Downs

DROP TABLE IF EXISTS sm_device CASCADE;

DROP TABLE IF EXISTS sm_file_card CASCADE;

DROP TABLE IF EXISTS sm_path_move CASCADE;

DROP TABLE IF EXISTS sm_category_fc CASCADE;
