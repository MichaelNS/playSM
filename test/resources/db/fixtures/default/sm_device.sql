#!SetUp
INSERT INTO sm_device (id, name, label, uid, sync_date)
VALUES (-1, 'sda3', '', '444', '2017-11-23 05:12:36.426000');

#!TearDown
DELETE
FROM sm_device;
