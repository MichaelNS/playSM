#!SetUp
INSERT INTO "sm_path_move" (ID, STORE_NAME, PATH_FROM, PATH_TO) VALUES ('-1', '111', '222', '333');
INSERT INTO "sm_path_move" (ID, STORE_NAME, PATH_FROM, PATH_TO) VALUES ('-2', 'a', 'b', 'c'); -- clearJob

#!TearDown
DELETE FROM sm_path_move WHERE ID < 0;
