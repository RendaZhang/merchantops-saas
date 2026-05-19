/*!40103 SET @merchantops_v18_old_time_zone = @@session.time_zone */;
/*!40103 SET time_zone = '+00:00' */;

ALTER TABLE auth_session
    MODIFY COLUMN created_at DATETIME NOT NULL;

ALTER TABLE auth_session
    MODIFY COLUMN expires_at DATETIME NOT NULL;

ALTER TABLE auth_session
    MODIFY COLUMN revoked_at DATETIME NULL;

/*!40103 SET time_zone = @merchantops_v18_old_time_zone */;
