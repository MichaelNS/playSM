#!SetUp
INSERT INTO sm_file_card (id, store_name, f_parent, f_name, f_extension, f_creation_date, f_last_modified_date, f_size,
                          f_mime_type_java, sha256, f_name_lc)
VALUES ('mnb', '333-444', 'Downloads/html', 'rta.js', 'js',
        '2017-05-25 12:34:47.000000', '2015-08-23 13:42:04.000000', '140',
        '', 'ADD5487EFD4FD4186CC350B66EF35AAE89FF6752', 'rta.js');

#!TearDown
DELETE
FROM sm_file_card
WHERE id = 'mnb';
