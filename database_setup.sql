-- Create the database.
create database if not exists csx370_mb_platform;

-- Use the created database.
use csx370_mb_platform;

-- Create the user table.
create table if not exists user (
    userId int auto_increment,
    username varchar(255) not null,
    password varchar(255) not null,
    firstName varchar(255) not null,
    lastName varchar(255) not null,
    primary key (userId),
    unique (username),
    constraint userName_min_length check (char_length(trim(userName)) >= 2),
    constraint firstName_min_length check (char_length(trim(firstName)) >= 2),
    constraint lastName_min_length check (char_length(trim(lastName)) >= 2)
);

-- Create the posts table
create table if not exists post (
    postId int auto_increment,
    userId int not null,
    content text not null,
    createdAt timestamp default current_timestamp,
    primary key (postId),
    foreign key (userId) references `user`(userId) on delete cascade
);

-- Create the comments table
create table if not exists comment (
    commentId int auto_increment,
    postId int not null,
    userId int not null,
    content text not null,
    createdAt timestamp default current_timestamp,
    primary key (commentId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (userId) references `user`(userId) on delete cascade
);

-- Create the likes table
create table if not exists postLike (
    userId int not null,
    postId int not null,
    createdAt timestamp default current_timestamp,
    primary key (userId, postId),
    foreign key (userId) references `user`(userId) on delete cascade,
    foreign key (postId) references post(postId) on delete cascade
);

-- Create the bookmarks table
create table if not exists bookmark (
    userId int not null,
    postId int not null,
    createdAt timestamp default current_timestamp,
    primary key (userId, postId),
    foreign key (userId) references `user`(userId) on delete cascade,
    foreign key (postId) references post(postId) on delete cascade
);

-- Create the follows table
create table if not exists follow (
    followerId int not null,
    followeeId int not null,
    createdAt timestamp default current_timestamp,
    primary key (followerId, followeeId),
    foreign key (followerId) references `user`(userId) on delete cascade,
    foreign key (followeeId) references `user`(userId) on delete cascade
);

-- Create the hashtags table
create table if not exists hashtag (
    hashtagId int auto_increment,
    tag varchar(100) not null,
    primary key (hashtagId),
    unique (tag)
);

-- Create the postHashtag table (many-to-many relation)
create table if not exists postHashtag (
    postId int not null,
    hashtagId int not null,
    primary key (postId, hashtagId),
    foreign key (postId) references post(postId) on delete cascade,
    foreign key (hashtagId) references hashtag(hashtagId) on delete cascade
);
