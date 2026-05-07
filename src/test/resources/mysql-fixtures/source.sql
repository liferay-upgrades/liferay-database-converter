-- MySQL dump
-- ------------------------------------------------------

DROP TABLE IF EXISTS `myaccount`;
CREATE TABLE `myaccount` (
  `accountid` bigint NOT NULL,
  `name` varchar(75) DEFAULT NULL,
  `description` text,
  PRIMARY KEY (`accountid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
LOCK TABLES `myaccount` WRITE;
UNLOCK TABLES;
