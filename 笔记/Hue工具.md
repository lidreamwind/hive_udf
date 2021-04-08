# 安装

**结合课件hue安装部分进行安装**

yum install ant asciidoc cyrus-sasl-devel cyrus-sasl-gssapi cyrus-sasl-plain gcc gcc-c++ krb5-devel libffi-devel libxml2-devel libxslt-devel make mysql mysql-evel openldap-devel python-devel sqlite-devel gmp-devel -y



**为python2.7.5安装pip**

 yum -y install epel-release 

yum install python-pip -y



yum install -y rsync



cd /opt/lagou/software/hue-release-4.3.0
PREFIX=/opt/lagou/servers make install
cd /opt/lagou/servers

![image-20210405223403239](.\图片\编译Hue报错.png)

yum install openssl openssl-devel -y

![image-20210405224145349](.\图片\安装hue错误MySQL的.png)

yum  -y install mlocate

yum -y install mysql-devel



# 配置

## core-site.xml

```xml
    <!-- HUE -->
    <property>
        <name>hadoop.proxyuser.hue.hosts</name>
        <value>*</value>
    </property>
    <property>
    	<name>hadoop.proxyuser.hue.groups</name>
      	<value>*</value>
    </property>
    <property>
        <name>hadoop.proxyuser.hdfs.hosts</name>
        <value>*</value>
    </property>
    <property>
        <name>hadoop.proxyuser.hdfs.groups</name>
        <value>*</value>
    </property>
```

## hdfs-site.xml

```xml
    <!-- HUE -->
    <property>
        <name>dfs.webhdfs.enabled</name>
        <value>true</value>
    </property>
    <property>
        <name>dfs.permissions.enabled</name>
        <value>false</value>
    </property>
```

## httpfs-site.xml

```xml
<configuration>
    <!-- HUE -->
    <property>
        <name>httpfs.proxyuser.hue.hosts</name>
        <value>*</value>
    </property>
    <property>
        <name>httpfs.proxyuser.hue.groups</name>
        <value>*</value>
    </property>
</configuration>
```

## Hue配置

```shell
# 进入 Hue 安装目录
cd /opt/lagou/servers/hue
# 进入配置目录
cd desktop/conf
# 复制一份HUE的配置文件，并修改复制的配置文件
cp pseudo-distributed.ini.tmpl pseudo-distributed.ini
vi pseudo-distributed.ini

# [desktop]
http_host=linux122
http_port=8000
is_hue_4=true
time_zone=Asia/Shanghai
dev=true
server_user=hue
server_group=hue
default_user=hue
# 211行左右。禁用solr，规避报错
app_blacklist=search 
# [[database]]。Hue默认使用SQLite数据库记录相关元数据，替换为mysql
engine=mysql
host=linux123
port=3306
user=hive
password=12345678
name=hue
# 1003行左右，Hadoop配置文件的路径
hadoop_conf_dir=/opt/lagou/servers/hadoop-2.9.2/etc/hadoo
```

## 创建数据库和用户

```sql
-- 创建hue用户和表
set global validate_password_policy=0;
set global validate_password_mixed_case_count=0;
set global validate_password_number_count=3;
set global validate_password_special_char_count=0;
set global validate_password_length=3;
CREATE DATABASE `hue` CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci';
create user hue IDENTIFIED by 'hue';
grant all PRIVILEGES on hue.* to 'hue'@'%' identified by 'hue' with grant option;
grant all PRIVILEGES on hue.* to 'hue'@'localhost' identified by 'hue' with grant option;
flush privileges;

-- # 初始化数据库
build/env/bin/hue syncdb
build/env/bin/hue migrate
```



```shell
# 增加 hue 用户和用户组
groupadd hue
useradd -g hue hue
# 在hue安装路径下执行
/opt/lagou/servers/hue/build/env/bin/supervisor
```

