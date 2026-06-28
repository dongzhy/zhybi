# 数据库初始化
# @author <a href="https://github.com/dongahaiyong">程序员zhy</a>

-- 创建库
create database if not exists my_db;

-- 切换库
use my_db;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_unionId (unionId)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 1. 数据看板主表
CREATE TABLE `data_board` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                              `board_name` varchar(100) NOT NULL COMMENT '看板名称',
                              `user_id` bigint NOT NULL COMMENT '所属用户ID',
                              `is_shared` tinyint(1) DEFAULT 0 COMMENT '是否公开分享 0-否 1-是',
                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `is_delete` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
                              PRIMARY KEY (`id`),
                              KEY idx_user_id (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据看板主表';

-- 2. 看板图表布局表（一个看板对应多个图表）
CREATE TABLE `board_chart_layout` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `board_id` bigint NOT NULL COMMENT '关联看板ID',
                                      `chart_id` bigint DEFAULT NULL COMMENT '关联AI分析图表ID(你原有chart表id)',
                                      `chart_title` varchar(100) NOT NULL COMMENT '图表标题',
                                      `chart_type` varchar(20) NOT NULL COMMENT '图表类型 pie/bar/line/radar',
                                      `layout_x` int NOT NULL COMMENT '布局X坐标',
                                      `layout_y` int NOT NULL COMMENT '布局Y坐标',
                                      `layout_w` int NOT NULL COMMENT '布局宽度',
                                      `layout_h` int NOT NULL COMMENT '布局高度',
                                      `min_w` int DEFAULT 4 COMMENT '最小宽度',
                                      `min_h` int DEFAULT 6 COMMENT '最小高度',
                                      `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                      `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      `is_delete` tinyint DEFAULT 0,
                                      PRIMARY KEY (`id`),
                                      KEY idx_board_id (`board_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板-图表布局表';


-- 看板分享表
CREATE TABLE board_share
(
    id            BIGINT AUTO_INCREMENT COMMENT '主键ID' PRIMARY KEY,
    board_id      BIGINT    NOT NULL COMMENT '关联看板ID',
    share_code    VARCHAR(64) NOT NULL COMMENT '分享唯一标识',
    share_password VARCHAR(128) NULL COMMENT '加密后的访问密码，null无密码',
    expire_time   DATETIME NULL COMMENT '过期时间，null永久有效',
    create_user   BIGINT    NOT NULL COMMENT '创建人用户ID',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete     TINYINT  DEFAULT 0,
    UNIQUE KEY uk_share_code (share_code),
    INDEX idx_board_id (board_id)
) COMMENT '看板分享记录表';
