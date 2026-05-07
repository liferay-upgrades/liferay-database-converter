-- MySQL dump
-- ------------------------------------------------------

DROP TABLE IF EXISTS `MYACCOUNT`;
CREATE TABLE `MYACCOUNT` (
  `accountid` bigint,
  `name` varchar(75) DEFAULT NULL,
  `description` blob,
  PRIMARY KEY (`accountid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
LOCK TABLES `MYACCOUNT` WRITE;
UNLOCK TABLES;
