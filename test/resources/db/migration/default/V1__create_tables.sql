DROP TABLE IF EXISTS sm_device CASCADE;
CREATE TABLE sm_device
(
    id        SERIAL PRIMARY KEY,
    name      VARCHAR              NOT NULL,
    label     VARCHAR              NOT NULL,
    uid       VARCHAR              NOT NULL,
    sync_date TIMESTAMP            NOT NULL,
    describe  VARCHAR              NULL,
    visible   BOOLEAN DEFAULT TRUE NOT NULL,
    reliable  BOOLEAN DEFAULT TRUE NOT NULL
);

DROP TABLE IF EXISTS sm_file_card CASCADE;
CREATE TABLE sm_file_card
(
    id                   VARCHAR PRIMARY KEY NOT NULL,
    device_uid           VARCHAR             NOT NULL,
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

DROP TABLE IF EXISTS sm_job_path_move CASCADE;
CREATE TABLE sm_job_path_move
(
    id         SERIAL PRIMARY KEY,
    device_uid VARCHAR NOT NULL,
    path_from  VARCHAR NOT NULL,
    path_to    VARCHAR NOT NULL
);

DROP TABLE IF EXISTS sm_category_fc CASCADE;
CREATE TABLE sm_category_fc
(
    id                INTEGER,
    f_name            VARCHAR,
    category_type     VARCHAR,
    sub_category_type VARCHAR,
    description       VARCHAR,
    PRIMARY KEY (id, f_name)
);
