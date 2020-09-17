# --- !Ups

CREATE INDEX idx_fc_dev_sha_name
    ON sm_file_card (device_uid, sha256, f_name);

CREATE INDEX idx_fc_parent_name
    ON sm_file_card (f_parent, f_name);

# --- !Downs

DROP INDEX idx_fc_dev_sha_name;
DROP INDEX idx_fc_parent_name;
