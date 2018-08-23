-- create db
drop database if exists geoparammodel;
create database geoparammodel;
use geoparammodel;

-- create tables
drop table if exists geomodel;
create table geomodel (
  modelid int not null primary key auto_increment,
  modeldataid varchar(255) not null,
  modeltype varchar(255),
  paramtemplateid int not null,
  paramtemplatedataid varchar(255) not null,
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

drop table if exists alerts;
create table alerts (
  _id int not null primary key auto_increment,
  `from` varchar(1024),
  logged_at timestamp not null default current_timestamp on update current_timestamp,
  originator varchar(1024),
  reason text
) engine=InnoDB default charset=utf8;

CREATE INDEX index_model_data_id ON geomodel (modeldataid);

show tables;
show index from geomodel;
show index from scripttemplate;