create database if not exists fileManage charset=utf8mb4;

use fileManage;

/**
  * 文件表
 */
create table if not exists files(
      id bigint primary key auto_increment comment '唯一ID',
      name varchar(255) comment '文件名',
      store_name varchar(64) comment '文件存储名-uuid',
      path varchar(255) comment '文件存储路径',
      size bigint comment '文件大小',
      type tinyint unsigned comment '文件类型-0:图片 1:视频 2:音频 3:文档 4:其他',
      create_time datetime comment '文件创建时间',
      update_time datetime comment '文件更新时间',
      is_delete boolean default false comment '是否逻辑删除',
      user_id bigint comment '所属用户ID',
      oss_id bigint comment '存储策略ID'
) DEFAULT CHARSET=utf8mb4;

/**
  * 用户表
 */
create table if not exists user(
    id bigint primary key auto_increment comment '唯一ID',
    email varchar(255) comment '邮箱',
    username varchar(255) comment '用户名',
    password char(40) comment '密码',
    create_time datetime comment '注册时间',
    update_time datetime comment '个人信息更新时间',
    is_admin boolean default false comment '是否为管理员',
    storage_used bigint default 0 comment '已使用存储空间(B)',
    # 默认1024GB
    storage_total bigint default (1024*1024*1024*1024) comment '总存储空间(B)'
)DEFAULT CHARSET=utf8mb4;

/**
  * 存储策略表
 */
create table if not exists oss(
    id bigint primary key auto_increment comment '唯一ID',
    name varchar(64) comment '存储策略名',
    type tinyint unsigned comment '存储策略类型-0:阿里云 1:腾讯云 2:七牛云 3:本地',
    config json comment '存储策略配置信息',
    create_time datetime comment '存储策略创建时间',
    update_time datetime comment '存储策略更新时间',
    creator bigint comment '存储策略创建者ID',
    updater bigint comment '存储策略更新者ID',
    is_active boolean default false comment '是否启用'
)DEFAULT CHARSET=utf8mb4;

insert into oss values (1, 'default', 3, null, now(), now(), -1, -1, true);