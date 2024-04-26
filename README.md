# 多模态文件管理系统后端工程 filemanage-backend

## 创建数据库表
运行sql/fileManage.sql（基于MySQL，版本不低于5.5，推荐8）

## 自定义项目配置
修改application.yml下的配置信息（或部署后创建生产环境配置文件）

## 开发环境下启动
启动src/main/java/cn/czyx007/filemanage/Application.java

## 打包工程到生产环境部署
1.mvn clean

2.mvn package