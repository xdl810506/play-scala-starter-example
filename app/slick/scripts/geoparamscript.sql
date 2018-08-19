-- create db
drop database if exists geoparammodel;
create database geoparammodel;
use geoparammodel;

-- create tables
drop table if exists geomodel;
create table geomodel (
  modelid int not null primary key auto_increment,
  modeldataid varchar(255) not null,
  paramtemplateid int not null,
  modeltype varchar(255),
  created timestamp not null default current_timestamp,
  lastmodified timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8;

drop table if exists scripttemplate;
create table scripttemplate (
  templateid int not null primary key auto_increment,
  name varchar(1024),
  templatedataid varchar(255) not null,
  created timestamp not null default current_timestamp,
  lastmodified timestamp not null default current_timestamp on update current_timestamp
) engine=InnoDB default charset=utf8;

show tables;