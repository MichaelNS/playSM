#!SetUp
INSERT INTO sm_file_card (ID, STORE_NAME, F_PARENT, F_NAME, F_EXTENSION, F_CREATION_DATE, F_LAST_MODIFIED_DATE, F_SIZE,
                          F_MIME_TYPE_JAVA, SHA256, F_NAME_LC)
VALUES ('mnb', '333-444', 'Downloads/html', 'rta.js', 'js',
        '2017-05-25 12:34:47.000000', '2015-08-23 13:42:04.000000', '140',
        '', 'ADD5487EFD4FD4186CC350B66EF35AAE89FF6752', 'rta.js');

#!TearDown
DELETE
FROM sm_file_card
WHERE ID = 'mnb';
