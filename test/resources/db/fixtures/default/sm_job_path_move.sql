#!SetUp
INSERT INTO "sm_job_path_move" (ID, device_uid, PATH_FROM, PATH_TO)
VALUES ('-1', '111', '222', '333');
INSERT INTO "sm_job_path_move" (ID, device_uid, PATH_FROM, PATH_TO)
VALUES ('-2', 'a', 'b', 'c'); -- clearJob

#!TearDown
DELETE
FROM sm_job_path_move
WHERE ID < 0;
