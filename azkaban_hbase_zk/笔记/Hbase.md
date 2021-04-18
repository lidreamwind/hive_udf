# 概念

## 特点

1. 海海量量存储： 底层基于HDFS存储海海量量数据
2. 列列式存储：HBase表的数据是基于列列族进⾏行行存储的，⼀一个列列族包含若⼲干列列
3. 极易易扩展：底层依赖HDFS，当磁盘空间不不⾜足的时候，只需要动态增加DataNode服务节点就可以
4. ⾼高并发：⽀支持⾼高并发的读写请求
5. 稀疏：稀疏主要是针对HBase列列的灵活性，在列列族中，你可以指定任意多的列列，在列列数据为空的情况下，是不不会占⽤用存储空间的。
6. 数据的多版本：HBase表中的数据可以有多个版本值，默认情况下是根据版本号去区分，版本号就是插⼊入数据的时间戳
7. 数据类型单⼀一：所有的数据在HBase中是以字节数组进⾏行行存储

## 适用场景

1. 交通⽅方⾯面：船舶GPS信息，每天有上千万左右的数据存储。

2. ⾦金金融⽅方⾯面：消费信息、贷款信息、信⽤用卡还款信息等

3. 电商⽅方⾯面：电商⽹网站的交易易信息、物流信息、游览信息等

4. 电信⽅方⾯面：通话信息

   

   **总结：HBase适合海海量量明细数据的存储，并且后期需要有很好的查询性能（单表超千万、上亿，且并发要求⾼高）**

## 数据模型

![image-20210412210532955](.\图片\Hbase逻辑结构示意图.png)

storefile

**NameSpace**

**Table**

**Row**

**RowKey**

**Column Family**

**Column Qualifier**

**Cell**

**Region**

# 架构

![](.\图片\Hbase架构图.png)

​	Hbase集群的元数据是被存储为一张Hbase的表。

​	**每个region是由多个store组成，每个store保存一个列族**

​	**每个store由一个MemStore和多个StoreFile组成**

​	

# 安装-错误

安装hbase后，启动所有节点，发现linux121节点没有任何hbase服务。查看日志，报如下错误。

![image-20210413224625448](.\图片\hbase安装错误一.png)

​	此处错误，是由于master1节点，在寻址的时候，hbase寻址到了外网。解决办法暂时是跳过此节点。以待日后研究。

# shell操作

```shell
# 进入命令操作界面
hbase shell

# 查看帮助命令
hbase> help

# 查看当前数据库中有哪些表
hbase> list

# 创建一张表，包含base_info,extra_info两个列族
hbase> create  'lagou','base_info','extra_info'
hbase> create 'lagou',{NAME =>'base_info', VERSIONS => '3'},{NAME => 'extra_info', VERSIONS => '3'}

# 添加数据
hbase> put 'lagou','rk1','base_info:name','wang'
hbase> put 'lagou', 'rk1', 'base_info:age', 30

# 查询数据
hbase> get 'lagou','rk1'
hbase> get 'lagou', 'rk1', {COLUMN => ['base_info', 'extra_info']}
hbase> get 'lagou', 'rk1', {COLUMN => ['base_info:name', 'extra_info:address']}

# 获取表中rowkey为rk1， cell的值为wang的信息
hbase> get 'lagou','rk1',{FILTER => "ValueFilter(=, 'binary:wang')"}

# 获取表中rowkey为rk1， 列标识符中含有a的信息
hbase> get 'lagou','rk1',{FILTER => "(QualifierFilter(=, 'substring:a'))"}

# 查询所有数据
hbase> scan 'lagou'

#列族查询
hbase> scan 'lagou', {COLUMNS => 'base_info'}
   # scan时可以设置是否开启Raw模式，开启 Raw模式会返回包括已添加删除标记但是未实际删除的数据
   # VERSIONS 指定查询的最大版本数
hbase> scan 'lagou', {COLUMNS => 'base_info', RAW => true, VERSIONS => 3}

# 指定多个列族，并且标识符中含有a字符的信息
hbase> scan 'lagou',{COLUMNS => ['base_info','extra_info'], FILTER => "(QualifierFilter(=,'substring:a'))"}

# 指定rowkey的范围查询
hbase> scan 'lagou',{COLUMNS => 'base_info', STARTROW => 'rk1', ENDROW => 'rk3'}

# 指定rowkey模糊查询
hbase> scan 'lagou',{FILTER => "PrefixFilter('rk')"}

# 更新数据,更新操作和插入操作一样，有数据机会更新，没有数据添加
hbase> put 'lagou','rk1','base_info:name','liang'

# 删除数据和表
	# 删除字段
hbase> delete 'lagou','rk1','base_info:name'
	# 删除列族
hbase> alter 'lagou','delete' => 'base_info'
	# 删除表数据
hbase> truncate 'lagou'
	# 删除表
hbase> disable 'lagou'
hbase> drop 'lagou'
```



# Hbase原理深入

## 读流程

![image-20210415225239748](.\图片\Hbase读数据流程.png)

HBase读操作
1）⾸首先从zk找到meta表的region位置，然后读取meta表中的数据，meta表中存储了了⽤用户表的region信息
2）根据要查询的namespace、表名和rowkey信息。找到写⼊入数据对应的region信息
3）找到这个region对应的regionServer，然后发送请求
4）查找对应的region
5）先从memstore查找数据，如果没有，再从BlockCache上读取

HBase上Regionserver的内存分为两个部分
⼀一部分作为Memstore，主要⽤用来写；
另外⼀一部分作为BlockCache，主要⽤用于读数据；
6）如果BlockCache中也没有找到，再到StoreFile上进⾏行行读取
从storeFile中读取到数据之后，不不是直接把结果数据返回给客户端，  ⽽而是把数据先写⼊入到BlockCache中，⽬目的是为了了加快后续的查询；然后在返回结果给客户端。

## 写流程

![image-20210415225421322](.\图片\Hbase写数据流程.png)

1）⾸首先从zk找到meta表的region位置，然后读取meta表中的数据，meta表中存储了了⽤用户表的region信息
2）根据namespace、表名和rowkey信息。找到写⼊入数据对应的region信息
3）找到这个region对应的regionServer，然后发送请求
4）把数据分别写到HLog（write ahead log）和memstore各⼀一份
5）memstore达到阈值后把数据刷到磁盘，⽣生成storeFile⽂文件
6）删除HLog中的历史数据

## 合并机制

### Flush机制

1、当memstore的大小超过如下参数的值时就会将memsotre的内容Flush到磁盘，默认为128M。

```xml
<property>
    <name>hbase.hregion.memstore.flush.size</name>
    <value>134217728</value>
</property>
```

2、当memstore的数据时间超过1小时，会flush到磁盘

```xml
<property>
    <name>hbase.regionserver.optionalcacheflushinterval</name>
    <value>3600000</value>
</property>
```

3、HregionServer的全局memstore的大小，超过该大小会触发Flush到磁盘的操作，默认是堆大小的40%

​	  单个regionServer的所有memstore的累计大小，超过regionServer堆内存的40%，会进行触发

```xml
<property>
    <name>hbase.regionserver.global.memstore.size</name>
    <value>0.4</value>
</property>
```

4、手动Flush

```shell
flush tableName
```

### 阻塞机制

​		以上介绍的是Store中memstore数据刷写磁盘的标准，但是Hbase中是周期性的检查是否满⾜足以上标准满⾜足则进⾏行行刷写，但是如果在下次检查到来之前，数据疯狂写⼊Memstore中，会触发阻塞机制。，此时数据无法写入Memstore，无法写入Hbase集群。

1、Memstore中数据达到512MB

​	 计算公式： hbase.hregion.memstore.flush.size*hbse.hregion.memstore.block.multiplier

​	hbase.hregion.memstore.flush.size刷写的阈值，默认值是134217728，即128MB。

​	hbse.hregion.memstore.block.multiplier是一个倍数，默认是4.

2、RegionServer全部Memtore达到规定值

​	hbase.regionserver.global.memstore.size.low.limit是0.95

​    hbase.regionserver.global.memstore.size是0.4

​    堆内存是16GB，触发刷写的阈值是16x0.4x0.95=6.08GB，触发阻塞的阈值是6.4GB

### Compact合并机制

**minor compact小合并**

将store只用多个HFile（storeFile)合并为一个HFile。		

​		删除和更新的数据仅仅做了一个标记，并没有物理移除。

```xml
<!--待合并⽂文件数据必须⼤大于等于下面这个值-->
<property>
    <name>hbase.hstore.compaction.min</name>
    <value>3</value>
</property>
<!--待合并⽂文件数据必须⼩小于等于下⾯面这个值-->
<property>
    <name>hbase.hstore.compaction.max</name>
    <value>10</value>
</property>
<!--默认值为128m,表示⽂文件⼤大⼩小⼩小于该值的store file ⼀一定会加⼊入到minor compaction的store file中-->
<property>
    <name>hbase.hstore.compaction.min.size</name>
    <value>134217728</value>
</property>
<!--默认值为LONG.MAX_VALUE，表示⽂文件⼤大⼩小⼤大于该值的store file ⼀一定会被minor compaction排除-->
<property>
    <name>hbase.hstore.compaction.max.size</name>
    <value>9223372036854775807</value>
</property>
```

触发条件

​	1、memstore flush，在进行memstore Flush前后会判断是否触发compact

​	2、定期检查线程，周期性检查是否需要进行compaction操作

​			由参数： hbase.server.thread.wakefrequency决定，默认值是10000 millSeconds

**major compact大合并**

合并store中的所有Hfile为一个Hfile。有标记删除的数据会被真正移除，同时超过单元格maxVersion的版本记录也会被删除。合并频率比较低，默认7天执行一次。而且性能消耗严重。



触发条件

```xml
<!--默认值为7天进⾏行行⼀一次⼤大合并，-->
<property>
    <name>hbase.hregion.majorcompaction</name>
    <value>604800000</value>
</property>
```

或者手动触发：

​	major_compact tableName



## Region拆分机制

拆分策略

1、ConstantSizeRegionSplitPolicy

​		0.94版本前的默认切分策略		

```text
当region⼤大⼩小⼤大于某个阈值(hbase.hregion.max.filesize=10G)之后就会触发切分，⼀一个region等分为2个region。

但是在⽣生产线上这种切分策略略却有相当⼤大的弊端：切分策略略对于⼤大表和⼩小表没有明显的区分。阈值(hbase.hregion.max.filesize)设置较⼤大对⼤大表⽐比较友
好，但是⼩小表就有可能不不会触发分裂，极端情况下可能就1个，这对业务来说并不不是什什么好事。如果设置较⼩小则对⼩小表友好，但⼀一个⼤大表就会在整个集群产⽣生⼤大量量的region，这对于集群的管理理、资源使⽤用、failover来说都不不是⼀一件好事。
```



2、IncreasingToUpperBoundRegionSplitPolicy

​		0.94版本~2.0版本的默认切分策略

```text
切分策略略稍微有点复杂，总体看和ConstantSizeRegionSplitPolicy思路路相同，⼀一个region⼤大⼩小⼤大于设置阈值就会触发切分。但是这个阈值并不不像
ConstantSizeRegionSplitPolicy是⼀一个固定的值，⽽而是会在⼀一定条件下不不断调整，调整规则和region所属表在当前regionserver上的region个数有关系.

region split的计算公式是：
regioncount^3 * 128M * 2，当region达到该size的时候进⾏行行split
例例如：
第⼀次split：1^3 * 256 = 256MB
第⼆次split：2^3 * 256 = 2048MB
第三次split：3^3 * 256 = 6912MB
第四次split：4^3 * 256 = 16384MB > 10GB，因此取较⼩小的值10GB
后⾯面每次split的size都是10GB了了
```

3、SteppingSplitPolicy

​		2.0版本的默认切分策略。

```text
这种切分策略略的切分阈值⼜又发⽣生了了变化，相⽐比 IncreasingToUpperBoundRegionSplitPolicy 简单了了⼀一些，依然和待分裂region所属表在当前
regionserver上的region个数有关系，如果region个数等于1，

切分阈值为flush size * 2，否则为MaxRegionFileSize。这种切分策略对于大集群中的大表、小表会比IncreasingToUpperBoundRegionSplitPolicy 更加友好，小表不会再产生大量的小region，而是适可而止。
```

4、KeyPrefixRegionSplitPolicy

根据rowKey的前缀对数据进行分组，这⾥里里是指定rowKey的前多少位作为前缀，⽐比如rowKey都是16位的，指定前5位是前缀，那么前5位相同的rowKey在进行region split的时候会分到相同的region中。

5、DelimitedKeyPrefixRegionSplitPolicy

```text
保证相同前缀的数据在同一个region中，例例如rowKey的格式为：userid_eventtype_eventid，指定的delimiter为 _ ，则split的的时候会确保userid相同的数据在同一个region中。
```

5、DisabledRegionSplitPolicy	

​	不起用自动拆分，需要指定手动拆分。

### 拆分策略应用

1、全局指定	

​	在hbase-site.xml中配置

```xml
<property>
    <name>hbase.regionserver.region.split.policy</name>
    <value>org.apache.hadoop.hbase.regionserver.IncreasingToUpperBoundRegionSplitPolicy</value>
</property>
```

2、通过Api单独为表指定Region拆分策略

```java
HTableDescriptor tableDesc = new HTableDescriptor("test1");
tableDesc.setValue(HTableDescriptor.SPLIT_POLICY, IncreasingToUpperBoundRegionSplitPolicy.class.getName());
tableDesc.addFamily(new HColumnDescriptor(Bytes.toBytes("cf1")));
admin.createTable(tableDesc)
```

3、通过Hbase shell指定

```shell
hbase> create 'test2', {METADATA => {'SPLIT_POLICY' => 'org.apache.hadoop.hbase.regionserver.IncreasingToUpperBoundRegionSplitPolicy'}},{NAME => 'cf1'}
```

## 预分区

hbase> create 'person','info1','info2',SPLITS => ['1000','2000','3000']

hbase> create 'student','info',SPLITS_FILE => '/root/hbase/split.txt'



## Region合并

**冷合并**

​	需要关闭hbase集群。

```text
需求：需要把student表中的2个region数据进⾏行行合并：student,,1593244870695.10c2df60e567e73523a633f20866b4b5.
student,1000,1593244870695.0a4c3ff30a98f79ff6c1e4cc927b3d0d.

这⾥里里通过org.apache.hadoop.hbase.util.Merge类来实现，不不需要进⼊入hbase shell，直接执⾏行行（需要先关闭hbase集群）：

hbase org.apache.hadoop.hbase.util.Merge student \
student,,1595256696737.fc3eff4765709e66a8524d3c3ab42d59. \
student,aaa,1595256696737.1d53d6c1ce0c1bed269b16b6514131d0.
```

**热合并**

​	不需要关闭hbase集群，在线进行合并

	与冷合并不不同的是，online_merge的传参是Region的hash值，⽽而Region的hash值就是Region名称的最后那段在两个.之间的字符串串部分。
	
	需求：需要把lagou_s表中的2个region数据进⾏行行合并：
	student,,1587392159085.9ca8689901008946793b8d5fa5898e06. \
	student,aaa,1587392159085.601d5741608cedb677634f8f7257e000.
	
	需要进⼊入hbase shell：
	merge_region 'c8bc666507d9e45523aebaffa88ffdd6','02a9dfdf6ff42ae9f0524a3d8f4c7777'
