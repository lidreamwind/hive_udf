# 概念

1、	zookeeper本质是一个分布式的小文件存储系统。提供基于类似文件目录树方式的数据存储，并且可以对树中的节点进行有效管理。

2、	提供给客户端监控在zk内部数据的功能，从而可以达到基于数据的集群管理。如：统一命名服务(dubbo)、分布式配置管理(solr的配置集中管理)、分布式消息队列(sub/pub)、分布式锁、分布式协调等功能。

## 架构

![image-20210411170534409](.\图片\zookeeper架构.png)

Follower

- Zookeeper 集群工作的核心角色
- 集群内部各个服务器的调度者。
- 事务请求（写操作） 的唯一调度和处理者，保证集群事务处理的顺序性；对于 create，setData， delete 等有写操作的请求，则需要统一转发给leader 处理， leader 需要决定编号、执行操作，这个过程称为一个事务。

leader

- 处理客户端非事务（读操作） 请求，
- 转发事务请求给 Leader；
- 参与集群 Leader 选举投票 2n-1台可以做集群投票

Observer。

- 观察者角色，观察 Zookeeper 集群的最新状态变化并将这些状态同步过来，其对于非事务请求可以进行独立处理，对于事务请求，则会转发给 Leader服务器进行处理。
- 不会参与任何形式的投票只提供非事务服务，通常用于在不影响集群事务处理能力的前提下提升集群的非事务处理能力。增加了集群增加并发的读请求

**特点：**

1. Zookeeper：一个领导者（leader:老大），多个跟随者（follower:小弟）组成的集群。
2. Leader负责进行投票的发起和决议，更新系统状态(内部原理)
3. Follower用于接收客户请求并向客户端返回结果，在选举Leader过程中参与投票
4. 集群中只要有**半数以上节点存活**，Zookeeper集群就能正常服务。
5. 全局数据一致：每个server保存一份相同的数据副本，Client无论连接到哪个server，数据都是一
致的。
6. 更新请求顺序进行(内部原理)
7. 数据更新原子性，一次数据更新要么成功，要么失败。

## 搭建

```sh
# 解压
tar -zxvf /opt/lagou/software/zookeeper-3.4.14.tar.gz -C /opt/lagou/servers/

#创建zk日志文件目录
mkdir -p /opt/lagou/servers/zookeeper-3.4.14/data/logs

#修改zk配置文件
cd /opt/lagou/servers/zookeeper-3.4.14/conf
#文件改名
mv zoo_sample.cfg zoo.cfg

vim zoo.cfg
#更新datadir
dataDir=/opt/lagou/servers/zookeeper-3.4.14/data
#增加logdir
dataLogDir=/opt/lagou/servers/zookeeper-3.4.14/data/logs
#增加集群配置
##server.服务器ID=服务器IP地址：服务器之间通信端口：服务器之间投票选举端口
server.1=linux121:2888:3888
server.2=linux122:2888:3888
server.3=linux123:2888:3888
#打开注释
#ZK提供了自动清理事务日志和快照文件的功能，这个参数指定了清理频率，单位是小时
autopurge.purgeInterval=1

#同步
rsync-script /opt/lagou/servers/zookeeper-3.4.14

#节点2执行linux122
echo 2 >/opt/lagou/servers/zookeeper-3.4.14/data/myid

# 节点3执行，linux123
echo 3 >/opt/lagou/servers/zookeeper-3.4.14/data/myid

# 编写启动脚本
vim zk.sh
#!/bin/sh
echo "start zookeeper server..."
if(($#==0));then
echo "no params";
exit;
fi
hosts="linux121 linux122 linux123"
for host in $hosts
do
ssh $host "source /etc/profile; /opt/lagou/servers/zookeeper-3.4.14/bin/zkServer.sh $1"
done

# 配置环境变量
vi  /etc/profile
export ZK_HOME=/opt/lagou/servers/zookeeper-3.4.14
export PATH=$PATH:$ZK_HOME/bin

# 更改权限
chmod 755 /opt/lagou/servers/zookeeper-3.4.14/bin/zk.sh
```

# 原理

## 数据结构

​	在zookeeper中，数据信息保存在一个个数据节点上，这些个节点被称为Znode。存储数据的最小单位。

### Znode类型

​	持久性节点

​	临时性节点

​	顺序性节点，创建节点时创建数值保证顺序，数值自动递增。

​	**类型**

​	1、持久性节点，一直保存，一直到删除操作主动删除。

​	2、临时节点，生命周期和会话Session绑定在一起，会话结束，节点被删除。临时节点不能创建子节点。

​	3、持久顺序节点，

​	4、临时顺序节点 ，

​	**事务**

​		事务是指能够改变zookeeper服务器状态的操作。创建、删除、更新。为其分布一个全局唯一的事务id，通常是一个64位的数字。

​		为了解决数据一致性问题。

	### Znode状态信息

```shell
#使用bin/zkCli.sh 连接到zk集群
[zk: localhost:2181(CONNECTED) 2] get /zookeeper
cZxid = 0x0
ctime = Wed Dec 31 19:00:00 EST 1969
mZxid = 0x0
mtime = Wed Dec 31 19:00:00 EST 1969
pZxid = 0x0
cversion = -1
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 0
numChildren = 1

# cZxid 就是 Create ZXID，表示节点被创建时的事务ID。
# ctime 就是 Create Time，表示节点创建时间。
# mZxid 就是 Modified ZXID，表示节点最后一次被修改时的事务ID。
# mtime 就是 Modified Time，表示节点最后一次被修改的时间。
# pZxid 表示该节点的子节点列表最后一次被修改时的事务 ID。只有子节点列表变更才会更新 pZxid，子
# 节点内容变更不会更新。
# cversion 表示子节点的版本号。
# dataVersion 表示内容版本号。
# aclVersion 标识acl版本
# ephemeralOwner 表示创建该临时节点时的会话 sessionID，如果是持久性节点那么值为 0
# dataLength 表示数据长度。
# numChildren 表示直系子节点数

```



## 监听机制

​	watcher发布订阅机制。

​	![image-20210411180622408](.\图片\zookeeper-watcher机制.png)

​	**Zookeeper的监听机制**主要包括**客户端线程**、**客户端WatcherManager**、zookeeper服务器三部分。

## Leader选举

**选举机制**

​	1、集群中板书以上机器存活，集群可用。所以zookeeper适合安装奇数台服务器。

​	2、zookeeper没有指定Master和Slave，但是通过选举产生。

### 集群首次启动

![image-20210411193546221](.\图片\zookeeper-选举-第一次启动.png)

### 集群非首次启动

​	每个节点选举时都会参考自身的zxid(事务id)，优先选择zxid值大的节点为leader。

## ZAB一致性协议

​	zk就是分布式一致性工业解决方案。

​	zab协议。

​	![image-20210411201316055](.\图片\zookeeper-ZAB协议.png)

**广播消息**

​	zk提供的应该是最终一致性的标准。zk所有节点接收请求之后可以在一定时间内保证所有节点能看到该条数据。

### Leader崩溃问题

1. ZAB 协议确保那些已经在 Leader 提交的事务最终会被所有服务器提交。、
2. ZAB 协议确保丢弃那些只在 Leader 提出/复制，但没有提交的事务。





# 基本使用

## 命令

```shell
zkcli.sh  # 连接本地zk
zkcli.sh -server ip:port # 连接远程服务器

# 创建节点,-s顺序节点。 -e临时节点 。 不指定，则创建持久节点。
create [-s] [-e] path data
create -s /zk-test 123

#更新节点
set path data

# 删除节点
delete path

```

## 代码

zkClient是Github上一个开源的zookeeper客户端，在Zookeeper原生API接口之上进行了包装，是一个更易用的Zookeeper客户端，同时zkClient在内部还实现了诸如**Session超时重连**、**Watcher反复注册**等功能。

### Pom

```xml-dtd
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.example</groupId>
    <artifactId>zk_hbase_azkaban</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <zookeeper.version>3.4.14</zookeeper.version>
        <zkClient.version>0.10</zkClient.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.zookeeper</groupId>
            <artifactId>zookeeper</artifactId>
            <version>${zookeeper.version}</version>
        </dependency>
        <dependency>
            <groupId>com.101tec</groupId>
            <artifactId>zkclient</artifactId>
            <version>${zkClient.version}</version>
        </dependency>
    </dependencies>

</project>
```



### 基本操作，增删

```java
package com.lagou.zkExample;

import org.I0Itec.zkclient.ZkClient;

public class ZNodeAddDelete {
    public static void main(String[] args) {
        // 先获取到zkClient对象
        ZkClient zkClient = new ZkClient("linux121:2181");
        System.out.println("zkClient is ready");

        // 1、创建节点,第一个参数是节点路径，dirge参数是是否创建父Znode，级联创建
//        zkClient.createPersistent("/lagou/lg-test",true);
//        System.out.println("/lagou/lg-test is created!");

        // 2、删除节点
//        boolean b = zkClient.deleteRecursive("/lagou");  // 递归删除
//        boolean delete = zkClient.delete("/lagou");  //删除本级目录
//        System.out.println(b?"删除成功":"删除失败");

        // 3、

    }
}

```

### 监听子节点变化

1、可以对不存在的目录进行监听

2、监听目录下子节点发生变化，可以接收到通知，携带数据有子节点列表

3、监听目录创建和删除本身也会被监听

4、**子节点的子节点目录**不会被监听

```java
package com.lagou.zkExample;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.List;

/**
 * zkClent使用监听器
            1 监听器可以对不存在的目录进行监听
            2 监听目录下子节点发生改变，可以接收到通知，携带数据有子节点列表
            3 监听目录创建和删除本身也会被监听到
            4 子节点的子节点不会被监听
 */
public class ZkWatcherChild {
    public static void main(String[] args) throws InterruptedException {
        // 获取zkClient
        ZkClient zkClient = new ZkClient("linux121:2181");

        // zkClient对指定目录进行监听（不存在目录/lagou），指定接受通知之后的逻辑
        zkClient.subscribeChildChanges("/lg-client", new IZkChildListener() {
            // 接收子节点变化而产生的操作
            @Override
            public void handleChildChange(String s, List<String> list) throws Exception {
                //打印信息
                System.out.println(s + " childs changes, current childs " +list);
            }
        });

        // 只用zkClient创建节点，删除节点，验证监听是否运行
        zkClient.createPersistent("/lg-client");
        Thread.sleep(1000);
        zkClient.createPersistent("/lg-client/lph");
        Thread.sleep(2000);
        zkClient.createPersistent("/lg-client/lph/t1");  //监听不到
        Thread.sleep(1000);
        zkClient.deleteRecursive("/lg-client");
        Thread.sleep(Integer.MAX_VALUE);
    }
}

```

### 监听节点数据情况

​	zkClient.readData("/lg-client") 需要自定义序列化器，不定义会报错。

```java
package com.lagou.zkExample;

import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

public class ZkStrSerializer implements ZkSerializer {
    // 数据转成 byte
    @Override
    public byte[] serialize(Object o) throws ZkMarshallingError {
        return String.valueOf(o).getBytes();
    }
    // 反序列化 byte-数据
    @Override
    public Object deserialize(byte[] bytes) throws ZkMarshallingError {
        return new String(bytes);
    }
}



package com.lagou.zkExample;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

/**
 * 监听节点数据的变化
 */
public class ZkWatcherData {
    public static void main(String[] args) throws InterruptedException {
        // 获取zkClient对象
        ZkClient zkClient = new ZkClient("linxu121:2181");
        // 判断节点是否存在，不存在则创建节点
        if(!zkClient.exists("/lg-client")){
            zkClient.createPersistent("/lg-client");
        }
        // 注册监听器，节点数据改变的类型，接收通知后的逻辑处理定义
        zkClient.subscribeDataChanges("/lg-client", new IZkDataListener() {
            // 处理数据的改变
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
                System.out.println(s + " data is changed, new data is " + o);
            }
            // 节点删除
            @Override
            public void handleDataDeleted(String s) throws Exception {
                System.out.println(s + " is deleted!");
            }
        });

        // 设置序列化器，不自定义会发生错误。
        zkClient.setZkSerializer(new ZkStrSerializer());

        // 更新节点，删除节点，监听器是否正常运行
        Object o = zkClient.readData("/lg-client");  // 需要进行序列化
        System.out.println(o);
        // 更新数据
        zkClient.writeData("/lg-client","new data");

        Thread.sleep(3000);

        // 删除
        zkClient.deleteRecursive("/lg-client");

        Thread.sleep(Integer.MAX_VALUE);
    }
}

```

# zookeeper应用实践

## 服务器动态上下线监听



## 分布式锁

1、单机程序中，当存在多个线程可以同事改变某个变量（可共享变量）时，为了保证线程安全（数据不能出现脏数据）就需要对变量或代码块做同步，使其在修改这种变量时能够串行执行消除并发修改变量

2、对变量或者堆代码做同步本质就是加锁。



![image-20210411213548580](.\图片\zookeeper-分布式锁.png)





# HDFS-HA工作机制





# Yarn-HA共组机制

