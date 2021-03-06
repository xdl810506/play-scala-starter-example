-- create db
drop database if exists application;
create database application;
use application;

-- create tables
drop table if exists scope;
create table scope (
  _id int not null primary key auto_increment,
  scope char(36) not null,
  family varchar(1024)
) engine=InnoDB default charset=utf8;

show tables;