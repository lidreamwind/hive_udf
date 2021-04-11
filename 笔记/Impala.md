# Impala适用场景

## 和Hive对比

Impala与Hive对⽐比分析
**查询过程**
Hive：在Hive中，每个查询都有⼀一个“冷启动”的常⻅见问题。（map,reduce每次都要启动关闭，申请资源，释放资源。。。）
Impala：Impala避免了了任何可能的启动开销，这是⼀一种本地查询语⾔言。 因为要始终处理理查询，则Impala守护程序进程总是在集群启动之后就准备就绪。守护进程在集群启动之后可以接收查询任务并执⾏行行查询任务。

**中间结果**

Hive：Hive通过MR引擎实现所有中间结果，中间结果需要落盘，这对降低数据处理理速度有不不利利影响。
Impala：在执⾏行行程序之间使⽤用流的⽅方式传输中间结果，避免数据落盘。尽可能使⽤用内存避免磁盘开销

**交互查询**

Hive：对于交互式计算，Hive不不是理理想的选择。
Impala：对于交互式计算，Impala⾮非常适合。(数据量量级PB级)

**计算引擎**
Hive：是基于批处理理的Hadoop MapReduce
Impala：更更像是MPP数据库

**容错**
Hive：Hive是容错的（通过MR&Yarn实现）
Impala：Impala没有容错，由于良好的查询性能，Impala遇到错误会重新执⾏行行⼀一次查询

**查询速度**
Impala：Impala⽐比Hive快3-90倍。

**Impala优势总结**

1. Impala最⼤大优点就是查询速度快，在⼀一定数据量量下；

2. 速度快的原因：避免了了MR引擎的弊端，采⽤用了了MPP数据库技术，

  

  **1.3 Impala的缺点**

3. Impala属于MPP架构，只能做到百节点级，⼀一般并发查询个数达到20左右时，整个系统的吞吐已
    经达到满负荷状态，在扩容节点也提升不不了了吞吐量量，处理理数据量量在PB级别最佳。

4. 资源不不能通过YARN统⼀一资源管理理调度，所以Hadoop集群⽆无法实现Impala、Spark、Hive等组件
    的动态资源共享。



## 使用场景

Hive: 复杂的批处理理查询任务，数据转换任务，对实时性要求不不⾼高同时数据量量⼜又很⼤大的场景。

Impala：实时数据分析，与Hive配合使⽤用,对Hive的结果数据集进⾏行行实时分析。impala不不能完全取代hive，impala可以直接处理理hive表中的数据。



# 安装

linux121 节点。

yum install -y httpd

yum enable httpd

yum start httpd

tar -zxvf cdh5.7.6-centos7.tar.gz -C /var/www/html/

ln -s /opt/lagou/software/cdh/5.7.6 /var/www/html/cdh57

**CDH下载路径**：http://archive.cloudera.com/cdh5/repo-as-tarball/5.7.6/cdh5.7.6-centos7.tar.gz



配置yum源：

​	cd /etc/yum.repos.d

​	vi local.repo

name=local
baseurl=http://linux121/cdh57/
gpgcheck=0
enabled=1

## 角色

1、impala-server，impala工作的进程。官方建议吧impala-server安装在datanode节点，更靠近数据。进程名称：短路读取。

2、impala-statored，健康监控角色，主要控制impala-server，impala-server异常时告知其他impala-server。进程名：statestored。

3、impala-catalogd，管理和维护元数据(hive元数据)。



## 安装

linux123：

yum install impala -y
yum install impala-server -y
yum install impala-state-store -y
yum install impala-catalog -y
yum install impala-shell -y



linux121，linux122：

yum install impala-server -y
yum install impala-shell -y



```xml
vim hive-site.xml
<!--指定metastore地址，之前添加过可以不不⽤用添加 -->
<property>
    <name>hive.metastore.uris</name>
    <value>thrift://linux121:9083,thrift://linux123:9083</value>
</property>
<property>
    <name>hive.metastore.client.socket.timeout</name>
    <value>3600</value>
</property
```



## 短路读取

需要hadoop安装libhadoop.so

ls $HADOOP_HOME/lib/native



**配置hdfs**

mkdir -p /var/lib/hadoop-hdfs

```xml
修改hdfs-site.xml
<!--添加如下内容 -->
<!--打开短路路读取开关 -->
<!-- 打开短路路读取配置-->
<property>
    <name>dfs.client.read.shortcircuit</name>
    <value>true</value>
</property>
<!--这是⼀一个UNIX域套接字的路路径，将⽤用于DataNode和本地HDFS客户机之间的通信 -->
<property>
    <name>dfs.domain.socket.path</name>
    <value>/var/lib/hadoop-hdfs/dn_socket</value>
</property>
<!--block存储元数据信息开发开关 -->
<property>
    <name>dfs.datanode.hdfs-blocks-metadata.enabled</name>
    <value>true</value>
</property>
<property>
    <name>dfs.client.file-block-storage-locations.timeout</name>
    <value>30000</value>
</property>
```

## Impala配置修改

**指定core-site.xml 和hdfs-site.xml到 Impala的了配置文件中**

```shell
# 有点问题，这一步
ln -s /opt/lagou/servers/hadoop-2.9.2/etc/hadoop/core-site.xml /etc/impala/conf/core-site.xml
ln -s /opt/lagou/servers/hadoop-2.9.2/etc/hadoop/hdfs-site.xml /etc/impala/conf/hdfs-site.xml
ln -s /opt/lagou/servers/hive-2.3.7/conf/hive-site.xml /etc/impala/conf/hive-site.xm
# 此步骤对
# 配置路径，而非通过连接操作。
# 参考安装impala遇到问题部分

vim /etc/default/impala
#<!--更更新如下内容 -->
IMPALA_CATALOG_SERVICE_HOST=linux123
IMPALA_STATE_STORE_HOST=linux123

# mysql-jdbc驱动包
mkdir -p /usr/share/java
cp /opt/lagou/servers/hive-2.3.7/lib/mysql-connector-java-5.1.46.jar /usr/share/java/
mv /usr/share/java/mysql-connector-java-5.1.46.jar /usr/share/java/mysql-connector-java.jar

#修改java的home路径
vim /etc/default/bigtop-utils
export JAVA_HOME=/opt/lagou/servers/jdk1.8.0_231
```

## 启动-停止

```shell
# linux123 启动如下角色，必须是如下命令，systemctl start 命令失效
service impala-state-store start
service impala-catalog start
service impala-server start
#linx121、linux122启动如下角色
service impala-server start

#停止
service impala-server stop
service impala-catalog stop
service impala-state-store stop
```



## 消除yum安装impala的影响

```shell
[root@linux122 conf]# which hadoop
/usr/bin/hadoop
[root@linux122 conf]# which hive
/usr/bin/hive
#使⽤用which命令 查找hadoop,hive等会发现，命令⽂文件是/usr/bin/hadoop ⽽而⾮非我们⾃自⼰己安装的路路
径，需要把这些删除掉,所有节点都要执⾏行行
rm -rf /usr/bin/hadoop
rm -rf /usr/bin/hdfs
rm -rf /usr/bin/hive
rm -rf /usr/bin/beeline
rm -rf /usr/bin/hiveserver2
#重新⽣生效环境变量量
source /etc/profile
```

## 安装impla遇到问题

1、/etc/hosts文件的名称和机器名称不一致，引发impalad和statestore不能通信问题。

​	查看日志： /var/log/impala/statestored.INFO解决

2、impla的元数据没能正确读取到Hive元数据

​	 查看日志：/var/log/impala/catalogd.INFO找到原因是 **读取的Hive内嵌的derby数据库**

 	解决办法：

​		指定HADOOP_HOME HIVE_HOME  HIVE_CONF_DIR

![image-20210411161652520](.\图片\impala配置.png)





# 架构



# 查询

1、explain

2、explain_level=1,2,3

3、profile



## Imapala语法

1、TextFile中不支持复杂数据格式，**parquet格式**支持复杂数据格式

2、limit offset，数据是排序的



## 插入

1、load到hive表中，使用insert插入，不推荐使用insert into ... as select 





