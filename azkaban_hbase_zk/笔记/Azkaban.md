Azkaban使用相对简单，故而只需要记录安装记录。



# 编译

```shell
cd /opt/lagou/software/
wget https://github.com/azkaban/azkaban/archive/3.51.0.tar.gz
tar -zxvf 3.51.0.tar.gz -C ../servers/
cd /opt/lagou/servers/azkaban-3.51.0/
yum -y install git
yum -y install gcc-c++
./gradlew build installDist -x test
```



# 单机模式

```shell
tar -zxvf azkaban-solo-server-0.1.0-SNAPSHOT.tar.gz -C ../../servers/azkaban

cd /opt/lagou/servers/azkaban-solo-server-0.1.0-SNAPSHOT/conf
vim azkaban.properties
default.timezone.id=Asia/Shanghai

# 修改commonprivate.properties配置⽂文件
cd /opt/lagou/servers/azkaban-solo-server-0.1.0-SNAPSHOT/plugins/jobtypes
vim commonprivate.properties
execute.as.user=false
memCheck.enabled=false

# 启动
cd /opt/lagou/servers/azkaban-solo-server-0.1.0-SNAPSHOT
bin/start-solo.sh

# 浏览界面
http://linux122:8081/index

⽤用户名：azkaban
密码： azkaban
```



# 集群模式

```shell
# 创建数据库
mysql -uroot -p

SET GLOBAL validate_password_length=5;
SET GLOBAL validate_password_policy=0;
CREATE USER 'azkaban'@'%' IDENTIFIED BY 'azkaban';
GRANT all privileges ON azkaban.* to 'azkaban'@'%' identified by 'azkaban'
WITH GRANT OPTION;
CREATE DATABASE azkaban;
use azkaban;

#解压数据库脚本
tar -zxvf azkaban-db-0.1.0-SNAPSHOT.tar.gz -C /opt/lagou/servers/azkaban
#加载初始化sql创建表
source /opt/lagou/servers/azkaban/azkaban-db-0.1.0-SNAPSHOT/create-all-sql-0.1.0-SNAPSHOT.sql;

# 解压azkaban-web-server
mkdir /opt/lagou/servers/azkaban
tar -zxvf azkaban-web-server-0.1.0-SNAPSHOT.tar.gz –C /opt/lagou/servers/azkaban/

# 进⼊入azkaban-web-server根⽬目录下
cd /opt/lagou/servers/azkaban/azkaban-web-server-0.1.0-SNAPSHOT
#⽣生成ssl证书：
keytool -keystore keystore -alias jetty -genkey -keyalg RSA

# 修改 azkaban-web-server的配置⽂文件
cd /opt/lagou/servers/azkaban-web-server-3.51.0/conf
vim azkaban.properties
# Azkaban Personalization Settings
azkaban.name=Test
azkaban.label=My Local Azkaban
azkaban.color=#FF3601
azkaban.default.servlet.path=/index
web.resource.dir=web/
default.timezone.id=Asia/Shanghai # 时区注意后⾯面不不要有空格
# Azkaban UserManager class
user.manager.class=azkaban.user.XmlUserManager
user.manager.xml.file=conf/azkaban-users.xml
# Azkaban Jetty server properties. 开启使⽤用ssl 并且知道端⼝口
jetty.use.ssl=true
jetty.port=8443
jetty.maxThreads=25
# KeyStore for SSL ssl相关配置 注意密码和证书路路径
jetty.keystore=keystore
jetty.password=azkaban
jetty.keypassword=azkaban
jetty.truststore=keystore
jetty.trustpassword=azkaban
# Azkaban mysql settings by default. Users should configure their own username
and password.
database.type=mysql
mysql.port=3306
mysql.host=linux123
mysql.database=azkaban
mysql.user=root
mysql.password=12345678
mysql.numconnections=100
#Multiple Executor 设置为false
azkaban.use.multiple.executors=true
#azkaban.executorselector.filters=StaticRemainingFlowSize,MinimumFreeMemory,Cp
uStatus
azkaban.executorselector.comparator.NumberOfAssignedFlowComparator=1
azkaban.executorselector.comparator.Memory=1
azkaban.executorselector.comparator.LastDispatched=1
azkaban.executorselector.comparator.CpuUsage=1

# 添加属性
mkdir -p plugins/jobtypes
cd plugins/jobtypes/
vim commonprivate.properties

azkaban.native.lib=false
execute.as.user=false
memCheck.enabled=false

#配置Azkaban-exec-server
tar -zxvf azkaban-exec-server-0.1.0-SNAPSHOT.tar.gz –C /opt/lagou/servers/azkaban/
cd /opt/lagou/servers/azkaban-exec-server-3.51.0/conf
vim azkaban.properties

# Azkaban Personalization Settings
azkaban.name=Test
azkaban.label=My Local Azkaban
azkaban.color=#FF3601
azkaban.default.servlet.path=/index
web.resource.dir=web/
default.timezone.id=Asia/Shanghai
# Azkaban UserManager class
user.manager.class=azkaban.user.XmlUserManager
user.manager.xml.file=conf/azkaban-users.xml
# Loader for projects
executor.global.properties=conf/global.properties
azkaban.project.dir=projects
# Where the Azkaban web server is located
azkaban.webserver.url=https://linux122:8443
# Azkaban mysql settings by default. Users should configure their own username
and password.
database.type=mysql
mysql.port=3306
mysql.host=linux123
mysql.database=azkaban
mysql.user=root
mysql.password=12345678
mysql.numconnections=100
# Azkaban Executor settings
executor.maxThreads=50
executor.port=12321
executor.flow.threads=30


cd /opt/lagou/servers
scp -r azkaban linux121:$PWD

# 启用
#启动exec-server
bin/start-exec.sh
#启动web-server
bin/start-web.sh


cd /opt/lagou/servers/azkaban/azkaban-exec-server-0.1.0-SNAPSHOT
curl -G "linux122:$(<./executor.port)/executor?action=activate" && echo

# 访问网址
https://linux122:8443
```

