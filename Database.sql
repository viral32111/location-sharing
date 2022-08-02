CREATE TABLE `Bookmarks` (
  `Identifier` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `User` int(10) unsigned NOT NULL,
  `Name` varchar(32) NOT NULL,
  `Longitude` binary(16) NOT NULL,
  `Latitude` binary(16) NOT NULL,
  `Radius` int(10) unsigned NOT NULL DEFAULT 10,
  PRIMARY KEY (`Identifier`),
  KEY `BookmarkUser` (`User`),
  CONSTRAINT `BookmarkUser` FOREIGN KEY (`User`) REFERENCES `Users` (`Identifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `Events`;
CREATE TABLE `Events` (
  `Identifier` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `Condition` enum('Arrives','Leaves') NOT NULL,
  `Bookmark` int(10) unsigned NOT NULL,
  `Target` int(10) unsigned NOT NULL,
  PRIMARY KEY (`Identifier`),
  KEY `EventBookmark` (`Bookmark`),
  KEY `EventTarget` (`Target`),
  CONSTRAINT `EventBookmark` FOREIGN KEY (`Bookmark`) REFERENCES `Bookmarks` (`Identifier`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `EventTarget` FOREIGN KEY (`Target`) REFERENCES `Users` (`Identifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `Groups`;
CREATE TABLE `Groups` (
  `Identifier` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `Name` varchar(32) NOT NULL,
  `Creator` int(10) unsigned NOT NULL,
  PRIMARY KEY (`Identifier`),
  UNIQUE KEY `NAME` (`Name`) USING BTREE,
  KEY `GroupCreator` (`Creator`),
  CONSTRAINT `GroupCreator` FOREIGN KEY (`Creator`) REFERENCES `Users` (`Identifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `History`;
CREATE TABLE `History` (
  `Identifier` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `User` int(10) unsigned NOT NULL,
  `Latitude` binary(16) NOT NULL,
  `Longitude` binary(16) NOT NULL,
  `Recorded` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`Identifier`),
  KEY `HistoryUser` (`User`),
  CONSTRAINT `HistoryUser` FOREIGN KEY (`User`) REFERENCES `Users` (`Identifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `Invites`;
CREATE TABLE `Invites` (
  `Identifier` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `Group` int(10) unsigned NOT NULL,
  `Code` varchar(8) NOT NULL,
  `Created` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`Identifier`),
  UNIQUE KEY `CODE` (`Code`),
  KEY `InviteGroup` (`Group`),
  CONSTRAINT `InviteGroup` FOREIGN KEY (`Group`) REFERENCES `Groups` (`Identifier`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `Users`;
CREATE TABLE `Users` (
  `Identifier` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `Name` varchar(32) NOT NULL,
  `Password` binary(64) DEFAULT NULL,
  `Salt` binary(16) NOT NULL,
  `Registered` timestamp NOT NULL DEFAULT current_timestamp(),
  `Group` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`Identifier`),
  UNIQUE KEY `UNIQUE_SALT` (`Salt`),
  UNIQUE KEY `UNIQUE_NAME` (`Name`) USING BTREE,
  UNIQUE KEY `UNIQUE_PASSWORD` (`Password`) USING BTREE,
  KEY `UserGroup` (`Group`),
  CONSTRAINT `UserGroup` FOREIGN KEY (`Group`) REFERENCES `Groups` (`Identifier`) ON DELETE SET NULL ON UPDATE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;
