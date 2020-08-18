-- 2 diff dir
SELECT (SELECT COUNT(1)
        FROM sm_file_card x2
        WHERE x2.f_name = x1.f_name
        AND   x2.sha256 = x1.sha256
        AND   x2.device_uid = x1.device_uid),(SELECT ARRAY_AGG(x2.f_parent)
                                              FROM sm_file_card x2
                                              WHERE x2.f_name = x1.f_name
                                              AND   x2.sha256 = x1.sha256
                                              AND   x2.device_uid = x1.device_uid),
       x1.f_name,
       x1.sha256,
       x1.f_parent,
       x2.f_parent
FROM sm_file_card x1
  INNER JOIN sm_file_card x2
          ON x2.f_name = x1.f_name
         AND x2.sha256 = x1.sha256
         AND x1.id != x2.id
         AND x2.device_uid != x1.device_uid
         AND x1.f_parent != x2.f_parent
WHERE x1.device_uid = 'WINDOWS'
ORDER BY x1.f_parent,
         x2.f_parent,
         x1.f_name;

SELECT x1.device_uid,
       x1.f_parent,
       x2.device_uid,
       x2.f_parent
FROM sm_file_card x1
  INNER JOIN sm_file_card x2
          ON x2.f_name = x1.f_name
         AND x2.sha256 = x1.sha256
         AND x1.id != x2.id
         AND x2.device_uid != x1.device_uid
         AND x1.f_parent != x2.f_parent
WHERE x1.device_uid = 'WINDOWS'
GROUP BY x1.device_uid,
         x1.f_parent,
         x2.device_uid,
         x2.f_parent
ORDER BY x1.f_parent,
         x2.f_parent LIMIT 500;

