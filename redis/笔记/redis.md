# 基本概念

![image-20210418203136561](.\图片\大纲.png)



## 缓存基本思想

1、提高系统响应

​	 qps达到11万/s读请求，8万/s写请求



2、缓存

3、分布式锁

## 缓存失效

缓存穿透：

缓存雪崩：访问量过大

缓存击穿

## 缓存并发竞争



## 缓存的读写模式



### Cache Aside Pattern

![image-20210418215650894](.\图片\Cache Aside Pattern-缓存.png)

# 参数

```shell
# redis.conf
daemonize yes	# 启动守护进程
protected-mode no	# 关闭保护模式
# bind 127.0.0.1	# 关闭本地ip

# 不设置maxmemory ，无最大内存限制
# 设置maxmemory，需要配置maxmemory-policy策略
#maxmemory-policy的取值：allkeys-lru、volatile-lru、volatile-lfu、allkeys-lfu、volatile-random、allkeys-random、volatile-ttl、noenviction
# allkeys-lru：在不确定时，一般采用策略，冷热数据交换
# volatile-lru：比allkeys-lru性能差，存：过期时间
# allkeys-random：希望请求符合平均分布，每个元素一相同的概率被访问
# 自己控制，volatile-ttl，防止缓存穿透：缓存穿透
maxmemory-policy noeviction # 禁止驱逐，不淘汰，默认值
maxmemory-policy allkeys-lru # 主动删除过期数据，从数据集中挑选最近最少使用的数据淘汰
maxmemory-policy volatile-lru # 从已设置过期时间的数据集中挑选最近最少使用的数据淘汰，没有设置过期时间，不淘汰

#持久化，开启混合持久化
aof-use-rdb-preamble yes
#持久化,RDB方式
save "" # 为空，默认不使用RDB存储，不能主从
save 900 1 # 表示15分钟（900秒钟）内至少一个键被更改进行快照
save 300 10 # 表示5分钟（300秒钟）内至少10个键被更改进行快照
save 60  10000 # 表示1分钟内至少10000个键被更改进行快照
# 持久化，AOF方式
appendonly yes		# 是否开启AOF功能，默认不开启
dir ./				#Aof文件存放位置
appendfilename "appendonly.aof"  #文件名称
# appendfsync always  AOF_FSYNC_ALWAYS ：# 每执行一个命令保存一次。（不推荐） 以下三个小节将分别讨论这三种保存模式。
appendfsync everysec  # AOF_FSYNC_EVERYSEC ：# 每一秒钟保存一次。（默认）,推荐
# appendfsync no  AOF_FSYNC_NO ：#不保存。 
##### 重写AOF
# 表示当前aof文件大小超过上一次aof文件大小的百分之多少的时候会进行重写。如果之前没有重写过，以启动时aof文件大小为准
auto-aof-rewrite-percentage 100
# 限制允许重写最小aof文件大小，也就是文件大小小于64mb的时候，不需要进行优化
auto-aof-rewrite-min-size 64mb

# 慢查询日志
#执行时间超过多少微秒的命令请求会被记录到日志上 0 :全记录 <0 不记录
slowlog-log-slower-than  10000
#slowlog-max-len 存储慢查询日志条数
slowlog-max-len 128
# Redis使用列表存储慢查询日志，采用队列方式（FIFO）
# config set的方式可以临时设置，redis重启后就无效
# config set slowlog-log-slower-than 微秒 config set slowlog-max-len 条数



```

# 数据类型

key的类型是字符串

value的类型：字符串、列表、set 、有序set、散列



**key的设计**

1、用: 分隔

2、把表名转换为key的前缀，如user:

3、第二段方主键值

4、第三段放置列名



### String类型

String可以表达：字符串、整数、浮点数

| 命令名称 | 样例                 | 命令描述                                                     |
| -------- | -------------------- | ------------------------------------------------------------ |
| set      | set key value        | 赋值                                                         |
| get      | get key              | 取值                                                         |
| getset   | getset key value     | 取值并赋值                                                   |
| setnx    | setnx key value      | 当value不存在时采用赋值； set key value nx px 3000，原子操作，px设置毫秒数 |
| append   | append key value     | 向尾部追加值                                                 |
| strlen   | strlen key           | 获取字符的长度                                               |
| incr     | incr key             | 递增数字                                                     |
| incrby   | incrby key increment | 增加指定整数                                                 |
| decr     | decr key             | 递减数字                                                     |
| decrby   | decrby key decrement | 减少指定的整数                                               |

**应用场景**

1、key和命令是字符串

2、普通的赋值

3、incr用于乐观锁

​	 incr：递增数字，可以用于实现乐观锁watch(事务)

4、setnx用于分布式锁

​	当value不存在时采用赋值，可用于实现分布式锁

```shell
set name:01 zhangfei NX  # 没有值则添加，
set name:01 zhaoyun  # 覆盖
set name:02 zhangfei NX px 10000 # 10s内不能进行操作
```

### list列表类型

list列表类型可以存储有序、可重复的元素

获取头部或者尾部附近的记录是很快的

list的元素个数为2^32-1个，40亿个

- [ ] | 命令名称   | 命令格式                             | 描述                                                         |
  | ---------- | ------------------------------------ | ------------------------------------------------------------ |
  | lpush      | lpush key v1 v2 v3                   | 从左侧插入列表                                               |
  | lpop       | lpop key                             | 从列表左侧取出                                               |
  | rpush      | rpush key v1 v2 v3                   | 从右侧插入                                                   |
  | rpop       | rpop key                             | 从列表右侧取出                                               |
  | lpushx     | lpushx key value                     | 将值插入到列表头部                                           |
  | rpushx     | rpushx key value                     | 将值插入列表尾部                                             |
  | blpop      | blpop key timeout                    | 从列表左侧取出，当列表为空时阻塞，可以设置最大阻塞时间，单位为秒 |
  | brpop      | brpop key timeout                    | 从列表右侧取出，当列表为空时阻塞，可以设置最大阻塞时间，单位为秒 |
  | llen       | llen key                             | 获得列表中元素个数                                           |
  | lindex     | lindex key value                     | 获得列表中下表为index的元素，index从0开始                    |
  | lrange     | lrange key start end                 | 返回列表中指定区间的元素，区间通过start和end指定             |
  | lrem       | lrem key count key                   | 删除列表中与value相等的元素。当count>0时，lrem会从列表左边开始删除；当count<0时，lrem会从列表右边开始删除；当count=0时，lrem删除所有值为value的元素。 |
  | ltrim      | ltrim key start end                  | 对列表进行修剪，只保留start到end区间                         |
  | lset       | lset key index value                 | 将列表index位置的元素设置为value的值                         |
  | rpoplpush  | rpoplpush key1 key2                  | 从key1列表右侧弹出并插入到key2的列表左侧                     |
  | brpoplpush | brpoplpush key1 key2                 | 从key1列表右侧弹出并插入到key2的列表左侧，会阻塞             |
  | linsert    | linsert key BEFORE/AFTER pivot value | 将value插入列表，且位于值pivot之前或之后                     |

应用场景

​	1、作为栈或者队列使用

​		列表有序可以作为栈和队列使用

​	2、可以用于各种列表，用户列表、商品列表、评论列表等

### set集合

set无序，唯一元素

集合中最大成员数量为2^32-1

| 命令名称    | 命令格式              | 描述                                     |
| ----------- | --------------------- | ---------------------------------------- |
| sadd        | sadd key mem1 mem2    | 为集合添加新成员                         |
| srem        | srem key mem1 mem2    | 删除集合中指定成员                       |
| smembers    | smembers key          | 获得集合中所有元素                       |
| spop        | spop key              | 返回集合中的一个随机元素，并将该元素删除 |
| srandmember | srandmember key       | 返回集合中的一个随机元素，不会删除该元素 |
| scard       | scard key             | 获得集合元素的数量                       |
| sismember   | sismember key member  | 判断元素是否在集合内                     |
| sinter      | sinter key1 key2 key3 | 求多集合的交集                           |
| sdiff       | sdiff key1 key2 key3  | 求多集合的差集                           |
| sunion      | sunion key1 key2 key3 | 求多集合的并集                           |

应用场景：

​	适用于不能重复且不需要顺序的数据结构

比如：关注的用户，还可以通过spop进行随机抽奖



### sortedset有序集合

元素本身是无序不重复的。

每个元素关联一个分数score

可以按照分数排序，分数可重复。

1、zrank	： 从大到小排名

2、zrevrank：从小到大

应用场景：

​	1、各种排行榜，点击排行榜，销量排行榜，关注排行榜



### hash散列

![image-20210419213858578](.\图片\hash散列.png)

### bitmap位图类型

bitmap是进行位操作的。

通过一个bit位来表示某个元素对应的值或者状态，其中的key就是对应元素本身。

```shell
setbit 	  	setbit key offset value			设置key在offset处的bit值，只能是0或者1
getbit		getbit key offset				获得key在offset处的bit值
bitcount	bitcount key					获得key的bit位为1的个数
bitpos		bitpos key value				返回第一个被设置为bit值的索引值
bitop		bitop and[or/xor/not] destkey key [key] 	对多个key进行逻辑运算后存入destkey中
```

应用场景：

​	1、用户没有签到，用户id为key，日期作为偏移量 1表示签到

​	2、统计活跃用户，日期作为key，用户id为偏移量，1表示活跃

​	3、查询用户在线状态，日期为key，用户id为偏移量，1表示在线

### Geo地理位置

​	geo是Redis用来处理地理位置信息的。

### Stream数据流类型

# Redis缓存过期与淘汰策略

### Maxmemory

**不设置的场景**

 	1、redis的key是固定的，不会增加

​	 2、redis作为Db使用，保证数据的完整性，不能淘汰，可以做集群，横向扩展

​	 缓存淘汰策略，禁止驱逐(默认)。

**设置的场景**

​	1、redis作为缓存使用，不断增加key

​	2、maxmenmory：默认值0，不限制大小。

​	

​	Redis物理内存的3/4

​	若是有slaver，需要再多预留一些。

### ttl生存周期

```shell
 expire key 5   # 5s中后删除
 
 # 过期结构
 typedef struct redisDb {
  dict *dict;  -- key Value
  dict *expires; -- key ttl
  dict *blocking_keys;
  dict *ready_keys;
  dict *watched_keys;
  int id;
} redisDb;
```

上面的代码是Redis 中关于数据库的结构体定义，这个结构体定义中除了 id 以外都是指向字典的指针，其中我们只看 dict 和 expires。

dict 用来维护一个 Redis 数据库中包含的所有 Key-Value 键值对，expires则用于维护一个 Redis 数据库中设置了失效时间的键(即key与失效时间的映射)。

当我们使用 expire命令设置一个key的失效时间时，Redis 首先到 dict 这个字典表中查找要设置的key是否存在，如果存在就将这个key和失效时间添加到 expires 这个字典表。

当我们使用 setex命令向系统插入数据时，Redis 首先将 Key 和 Value 添加到 dict 这个字典表中，然后将 Key 和失效时间添加到 expires 这个字典表中。

简单地总结来说就是，设置了失效时间的key和具体的失效时间全部都维护在 expires 这个字典表中。

### 删除策略

定时删除、惰性删除和主动删除

Redis目前采用惰性删除+主动删除的方式。

**惰性删除**
在key被访问时如果发现它已经失效，那么就删除它。
调用expireIfNeeded函数，该函数的意义是：读取数据之前先检查一下它有没有失效，如果失效了就删除它。

**主动删除**
在redis.conf文件中可以配置主动删除策略,默认是no-enviction（不删除）

maxmemory-policy allkeys-lru

### LRU

least recenty userd.

maxmemory-policy allkeys-lru # 主动删除过期数据，从数据集中挑选最近最少使用的数据淘汰
maxmemory-policy volatile-lru   # 从已设置过期时间的数据集中挑选最近最少使用的数据淘汰，没有设置过期时间，不淘汰

### LFU

volatile-lfu

allkeys-lfu

### **随机**

volatile-random

allkeys-random

volatile-ttl

noenviction



# 持久化机制

缓存雪崩：redis的大量key失效，穿透到db层

缓存穿透：redis的key未找到，需要到db层



Redis持久化是为了快速的恢复数据，而不是粗糙农户数据。

redis的rdb默认开启，而aof默认不开启。

## RDB

RDB（Redis DataBase），是redis默认的存储方式，RDB是通过快照完成的。

**触发方式**

1、符合自定义配置的快照规则

2、执行save或者bgsave命令

3、执行flushall命令

4、执行主从复制操作（第一次）



```shell
save "" # 为空，默认不使用RDB存储，不能主从
save 900 1 # 表示15分钟（900秒钟）内至少一个键被更改进行快照
save 300 10 # 表示5分钟（300秒钟）内至少10个键被更改进行快照
save 60  10000 # 表示1分钟内至少10000个键被更改进行快照
```

![image-20210420101947709](.\图片\RDB生成方式.png)

## RDB结构

![image-20210420102259440](E:\拉勾-大数据系统资料与教程\redis_kafka\redis\图片\RDB结构.png)

## RDB优缺点

**有点**

​	1、RDB是二进制压缩文件，占用空间小，便于传输（传给slaver)

​	2、主进程fork子进程，可以最大化redis性能，主进程不能太大，Redis的数据量不能太大，复制过程中主进程阻塞

**缺点**

​	1、不能保证数据完整性，丢失最后一次快照以后修改的所有数据

## AOF

Redis默认不开启。

Redis将所有对数据库进行**写入的命令（及其参数）**记录到AOF文件，以此达到记录数据库状态的目的。

这样当Redis重启后只要按顺序回放这些命令就会恢复到原始状态了。

**AOF只会记录过程，RDB只管结果**

```shell
appendonly yes
dir ./
appendfilename "appendonly.aof"
# appendfsync always
appendfsync everysec
# appendfsync no


AOF_FSYNC_NO ：#不保存。 
AOF_FSYNC_EVERYSEC ：# 每一秒钟保存一次。（默认）,推荐
AOF_FSYNC_ALWAYS ：# 每执行一个命令保存一次。（不推荐） 以下三个小节将分别讨论这三种保存模式。
```

步骤：

​	1、命令传播

​	2、缓存追加

​	3、文件写入和保存

![image-20210420103602067](.\图片\AOF文件.png)

## AOF重写

将AOF日志的重复记录重新整合，将保留最新的数据日志过程是AOF重写。根据Redis

![image-20210420112041321](.\图片\AOF重写.png)

1、配置触发

```shell
# 重写触发
# 表示当前aof文件大小超过上一次aof文件大小的百分之多少的时候会进行重写。如果之前没有重写过，以启动时aof文件大小为准
auto-aof-rewrite-percentage 100
# 限制允许重写最小aof文件大小，也就是文件大小小于64mb的时候，不需要进行优化
auto-aof-rewrite-min-size 64mb
```

2、命令触发

​	127.0.0.1:6379>  bgrewriteaof



## 混合持久化

Redis4.0开始支持rdb和aof混合持久化

aof-use-rdb-preamble yes

RDB的头+AOF的身体

## AOF文件的载入与还原

伪客户端重写

## RDB与AOF对比

1、RDB才在某个时刻的数据快照，采用二进制压缩，AOF存储操作命令，采用文本存储

2、RDB性能高，AOF性能低

3、RDB在配置状态会丢失最后一次快照以后更改的数据，AOF每秒钟存储一次，最多丢失两秒钟的数据

4、Redis以主服务器模式运行，RDB不会保存过期键值对数据，Redis以从服务器模式运行，RDB会保存过期键值对，当主服务器向从服务器同步时，再清空过期键值对。



AOF写入文件时，对过期的key会追加一条del命令，当执行AOF重写时，会忽略过期key和del命令。

## 应用场景

内存数据库 rdb+aof 数据不容易丢失

缓存服务器 rdb 一般性能高

原始数据源：每次启动都从原始数据源中初始化，则不用开启持久化。



用作DB 不能主从 数据量小

做缓存 较高性能： 开rdb

Redis数据量存储过大，性能突然下降，

fork 时间过长 阻塞主进程

则只开启AOF



# Redis扩展特性

## 发布订阅机制

哨兵模式、高可用使用。



## 事务概念

1、事务通过multi、exec、discard和watch四个命令来完成的。

2、单个命令都是原子性的，所以这里需要确保事务性的对象是命令集合

3、redis将命令集合序列化并确保处于同一事务的命令集合连续不被打断的执行

4、redis不支持回滚



mulit：事务开启，任务放入到命令队列

exec：执行命令队列

discard：清除命令队列

watch：监视key，当key变化，执行失败

unwatch：取消监视



### 事务机制

1、事务开始

​	在RedisClient中，有属性flags，用来标示是否在事务中。

​	flags=REDIS_MULTI

2、命令入队

​	RedisClient将明了放在事务队列中

​	(EXEC, DISCARD,WATHC,MULT除外)

3、事务队列

​	multiCmd*commands 用于存放命令

4、执行事务

​	RedisClient向服务器发送exec命令，redis服务器会遍历事务队列，执行队列中的命令，最后将执行结果一次性返回给客户端。



如果命令队列中发生错误，redisClient将flags置位REDIS_DIRTY_EXEC，exec命令返回失败。



### Reids弱事务

1、redis语法错误

2、redis运行错误



## Lua脚本

​		lua一种轻量小巧的脚本语言，用标准C语言编写并以源代码形式开放。设计目的是为了嵌入应用程序中。



​	**Lua应用场景**：游戏开发、独立应用脚本、web应用脚本、扩展和数据插件。

​	nginx上使用Lua实现高并发。

​	OpenRestry：一个可伸缩的基于Nginx的web平台，是在nginx上集成了lua模块的第三方服务器。



​    redis执行lua脚本

​		evel script numkeys key



​	lua调用redis命令

​		redis.call

​		redis.pcall

​		evalsha 

​		

### 脚本复制

redis的传播方式，脚本传播和命令传播。默认是脚本传播。

脚本传播不能有时间、内部状态、随机函数等。

## 监视器



## 慢查询

slowlog get

```shell
#执行时间超过多少微秒的命令请求会被记录到日志上 0 :全记录 <0 不记录
slowlog-log-slower-than  10000
#slowlog-max-len 存储慢查询日志条数
slowlog-max-len 128
```

Redis使用列表存储慢查询日志，采用队列方式（FIFO）
config set的方式可以临时设置，redis重启后就无效
config set slowlog-log-slower-than 微秒 config set slowlog-max-len 条数

# 高可用

1、主从复制原理、同步数据集

2、配置redis主从复制

3、redis主从+哨兵模式

4、哨兵模式执行流程、故障转移和leader选举

5、掌握一致性算法

6、rediscluster的分片原理

7、掌握RedisCluster的部署方案和迁移扩容等操作



## 主从复制

1、读写分离，主从同步

2、主负责写，从负责读



## 哨兵机制



## 集群介绍

1、高可用：集群的故障转移能力

2、高性能：多台计算能力；读写分离能力

3、高扩展：横向主机扩展



## 分区

1、数字分区，雪花算法

2、hash分区，扩展后数据混乱

3、一致性hash，对大数据进行取模。2^32取模。  ip%(2^32)

​		缺点：环偏移---》虚拟节点

​					复杂度高、不易扩展



## RedisCluster分区

方案采用去中心化的方式，包括：sharding（分区）、replication（复制）、failover（故障转移）。称为RedisCluste。



# 缓存问题

## 缓存穿透

​	一般的缓存系统，都是按照key去缓存查询，如果不存在对应的value，就应该去后端系统查找，比如DB。

​	**缓存穿透是指在高并发下查询key不存在的数据，会穿过缓存查询数据库。导致数据库压力过大而宕机**

​	

​	**解决方案**

​		1、不存在的key设置默认值，缓存时间ttl设置短一点，若是不存在的key过多，，会占用太多空间

​		2、使用布隆过滤器，用于检查一个元素是否在一个集合中。

![image-20210424154753057](.\图片\布隆过滤器讲解.png)

​		![image-20210424154713440](.\图片\布隆过滤器.png)	

​			数据库更新，与布隆过滤器同步更新。

## 缓存雪崩

​	缓存服务器重启或者大量缓存集中在某一个时间段失效，这样在失效的时候，也会给后端系统带来很大压力。

​	

**解决方案**

​	1、key的失效期分开放，不同的key设置不同的有效期

​	2、设置二级缓存（数据可能不一致）

​	3、redis 设置高可用，（脏读）



## 缓存击穿

​	设置了过期时间的key，如果这些key可能会在某些点时间点被超高并发访问，是一种非常“热点”数据。

​	

​	缓存在某个时间点过期的时候，恰好在这个时间点对这个key有大量的并发请求过来，这些请求发现缓存过期一般都会从后端DB家在数据并设置回到缓存，这个时候大并发的请求可能会瞬间把后端DB压垮。



**解决方案**

​	1、分布式锁控制访问的线程

​		使用redis的setnx互斥锁先进性判断，这样其他线程处于等待状态。

​	2、不设置超时时间，volatile-lru，但会曹正一致性问题



# 数据不一致

​	缓存和DB的数据不一致问题，数据源不一致导致的。

​		**解决办法**：强一致性比较难，追求最终一致性。

​		**保证数据的最终一致性（延时双删）**

​		1、先更新数据库同时删除缓存项（key），等待读的时候在天充好

​		2、2秒后再删除一次缓存项（key）

​		3、设置缓存过期时间，Expired Time 比如10秒或者一小时

​		4、将缓存删除失败记录到日志中，利用脚本提取失败记录再次删除（缓存失效期过长7*24）），通过数据库的binlog来异步淘汰key。



​	缓存的主从之间数据不一致。



## 数据并发

​	多个客户端并发的去set value

**方案一：分布式锁+时间戳**

​	 setnx :如果没有才设置

**方案二：利用消息队列**

​	利用中间件，将队列串行起来执行



## Hotkey

​		当有大量的请求访问某个Redis某个Key时，由于流量集中达到网络上限，从而导致这个Redis的服务器宕机。造成缓存击穿，接下来对这个key的访问将直接访问数据库造成数据库崩溃，或者访问数据库回填Redis在访问Redis，继续崩溃。



**如何发现hotkey**

1、预估热key，比如秒杀的商品，火爆的新闻等

2、在客户端进行统计，实现简单

3、如果是proxy，比如Codis在Proxy端收集

4、利用redis自带的命令，如monitor、hotkeys，执行缓慢，不建议用

5、利用大数据流式计算技术进行统计，将热点数据写入zookeeper中

**如何处理hotkey**

​	1、变分布式缓存为本地缓存，Guava  Cache，Ehcache

​	2、在每个redis主节点上备份热key数据，这样在 读取时可以采用随机读取的方式，将访问压力负载到每个redis上

​	3、利用对热点数据访问的限流熔断保护措施

​			每个系统实例每秒最多请求缓存集群读操作不超过400此，一超过就可以熔断掉，不让请求缓存集群，直接返回一个空白信息。



## BigKey

​	存储的value比较大

​		1、热门话题下的讨论

​		2、大v的粉丝列表

​		3、序列化后的图片

​		4、没有及时处理的垃圾数据



**影响**

​		1、大key会占用内存，在集群中无法均衡

​		2、redis性能下降，主从复制异常

​		3、会主动删除或过期删除时操作时间过长而引起服务阻塞



**如何发现bigkey**

​	1、redis-cli --bigkeys命令，可以找到某个实例中5种数据类型（string、hash、list、set、zset）的最大key

​			如果redis的key比较多，执行该命令会比较慢

​	2、获取生产Redis的rdb文件，通过rdbtools分析生成csv文件，再倒入MySQL或其他数据库中进行统计，根据size_in_bytes统计bigkey



**如何处理bigkey**

​		优化big key的原则就是string减少字符串长度，list、hash、set、zset等减少成员数。

​		1、string类型的big key，尽量不要存入redis中，可以使用文档型数据库MongoDB或缓存到CDN上。

​		2、单个简单的key的value很大，可以尝试将对象拆分成几个key-value，使用mget获取值，这样拆分的意义在于分拆单词操作的压力，将压力平摊到多次操作中，降低redis的io影响。

​		3、hash、set、list、zset中存储过多的元素，可以将这些元素分拆。

​		4、删除大key时不要使用del，因为del是阻塞命令，删除时会影响性能。

​				使用 lazy delete（unlink命令）

​				删除指定的key，若key不存在则该key被跳过。但是相对于del会启用新的线程删除。



# 分布式锁

​		让进程互斥，串行化。

​	![image-20210424165642468](.\图片\分布式锁.png)

## 乐观锁

​		乐观锁基于CAS（Compare And Swap）思想，是不具有互斥性，不会产生所等待而消耗资源，但是需要反复的重试，但是也因为重试的机制，能比较快的响应。

​		

**redis实现乐观锁**

​	1、利用redis的watch功能，监控这个rediskey的状态值

​	2、获取rediskey的值

​	3、创建redis事务

​	4、给这个key的值+1

​	5、然后执行这个事务，如果key的值被修改过则回滚，key不加1



## 分布式锁

​		单机： 无法保证高可用

​		主从：

​		![image-20210424202137073](.\图片\分布式锁问题.png)

![image-20210424202510507](.\图片\分布式锁的应用.png)



## redisson

![image-20210424205206042](.\图片\分布式锁-官网提供.png)

![image-20210424213458460](.\图片\分布式锁-实现方式.png)



​			



