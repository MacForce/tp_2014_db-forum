DROP TABLE IF EXISTS user;
CREATE TABLE user (
  id          INT AUTO_INCREMENT  NOT NULL,
  email       CHAR(25) UNIQUE     NOT NULL,
  username    CHAR(25),
  name        CHAR(25),
  about       TEXT,
  isAnonymous BOOL DEFAULT FALSE  NOT NULL,
  PRIMARY KEY (id),
  KEY id_email (id, email)
);

DROP TABLE IF EXISTS friendship;
CREATE TABLE friendship (
  user CHAR(25) NOT NULL,
  follower CHAR(25) NOT NULL,
  PRIMARY KEY (user, follower),
  KEY f_u (follower, user)
);

DROP TABLE IF EXISTS forum;
CREATE TABLE forum (
  id         INT AUTO_INCREMENT NOT NULL,
  name       CHAR(35)       NOT NULL UNIQUE,
  short_name CHAR(35)       NOT NULL UNIQUE,
  user       CHAR(25)       NOT NULL,
  PRIMARY KEY (id),
  KEY (short_name)
);

DROP TABLE IF EXISTS thread;
CREATE TABLE thread (
  id        INT AUTO_INCREMENT NOT NULL,
  title     CHAR(50)       NOT NULL,
  slug      CHAR(50)       NOT NULL,
  forum     CHAR(35)       NOT NULL,
  user      CHAR(25)       NOT NULL,
  posts     INT DEFAULT 0      NOT NULL,
  likes     INT DEFAULT 0      NOT NULL,
  dislikes  INT DEFAULT 0      NOT NULL,
  points    INT DEFAULT 0      NOT NULL,
  isDeleted BOOL DEFAULT FALSE NOT NULL,
  isClosed  BOOL DEFAULT FALSE NOT NULL,
  date      DATETIME           NOT NULL,
  message   TEXT               NOT NULL,
  PRIMARY KEY (id),
  KEY forum_date (forum, date),
  KEY user_date (user, date)
);

DROP TABLE IF EXISTS post;
CREATE TABLE post (
  id            INT AUTO_INCREMENT NOT NULL,
  message       TEXT               NOT NULL,
  forum         CHAR(35)           NOT NULL,
  user          CHAR(25)           NOT NULL,
  parent        INT DEFAULT NULL,
  thread        INT                NOT NULL,
  likes         INT DEFAULT 0      NOT NULL,
  dislikes      INT DEFAULT 0      NOT NULL,
  points        INT DEFAULT 0      NOT NULL,
  isDeleted     BOOL DEFAULT FALSE NOT NULL,
  isSpam        BOOL DEFAULT FALSE NOT NULL,
  isEdited      BOOL DEFAULT FALSE NOT NULL,
  isApproved    BOOL DEFAULT FALSE NOT NULL,
  isHighlighted BOOL DEFAULT FALSE NOT NULL,
  date          DATETIME           NOT NULL,
  PRIMARY KEY (id),
  KEY user_date (user, date),
  KEY forum_date (forum, date),
  KEY thread_date (thread, date)
);

DROP TABLE IF EXISTS subscribe;
CREATE TABLE subscribe (
  user   CHAR(25) NOT NULL,
  thread INT          NOT NULL,
  PRIMARY KEY (user, thread)
);