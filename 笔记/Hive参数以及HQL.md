# Hive参数配置

```shell
#查看全部参数
hive> set;
#查看某一个参数
hive> set hive.exec.mode.local.auto;

# 执行SQL的语句
hive -e ""

# 执行SQL脚本
hive -f hqlfile.sql

# 查看hdfs文件
hive> dfs -ls /;
```

配置参数的方式

​	1、用户自定义配置文件  hive-set.xml

​	2、启动Hive时指定， -hiveconf

​	3、hive命令行指定参数

​	优先级： 3>2>1>默认值  set > -hiveconf > hive-site.xml >hive-default.xml



# 数据类型

![image-20210404162157222](.\图片\Hive基本数据类型和Java对应.png)

自动类型转换。

![image-20210404162453323](.\图片\Hive自动类型转换.png)

**强制类型转换**

​	转换失败返回空值。

​	select cast('1111' as int);

## 集合数据类型

ARRAY：有序的相同数据类型的集合，集合字段访问： select arr[1] from table

MAP：key-value对。key必须是基本数据类型，value不限。访问map类型  select mymap["x"] from (select map("a",10,"b",100) mymap) t1

STRUCT：不同类型字段的集合。类似于C语言的结构体，访问 select struct("lisi",18,176.3) as userinfo

​					select userinfo.name from (select name_struct("name",lisi","age",18,"height",176.3) as userinfo) t1

UNION：不同类型的元素存储在同一字段的不同行中



# 文件编码

ctrl+A ：区分字段

ctrl+B：分隔Array、Map、struct中的元素

ctrl+C：分隔map中的key、value分隔符

\n ：换行符

# 读时模式

在传统数据库中，在加载时发现数据不符合表的定义，则拒绝加载数据。数据在写入数据库时对照表模式进行检查，这种模式称为"写时模式"（schema on write）。
	写时模式 -> 写数据检查 -> RDBMS；

Hive中数据加载过程采用"读时模式" (schema on read)，加载数据时不进行数据格式的校验，读取数据时如果不合法则显示NULL。这种模式的优点是加载数据迅速。
	读时模式 -> 读时检查数据 -> Hive；好处：加载数据快；问题：数据显示NULL



# DDL

参考：https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL
DDL（data definition language）： 主要的命令有CREATE、ALTER、DROP等。

![image-20210404165101732](.\图片\DDL表的层次结构.png)

desc database external mydb1;

desc formatted t;

alter table t1 set tblproperties("EXTERNAL"="TRUE")   # 修改为外部表

load data local inpath "/home/haodop/t1.dat" into table t3  partition(dt="202001")

show partitios t3;

**分区修改**

alter table t3 add partition(dt="202001") location '/usr/hive/warehouse/dt=202001'

alter table t3 partition(dt="2020-05") set location '/usr/home/tmp/dt='202005'

atler table t3 drop partition(dt='20200603')

**重命名**

alter table t3 rename to t4;

alter table t3 change column name   subject string;

alter table t3 change column id id string;

**增加**

alter table t3 add columns(col1 string)

**删除**

alter table t3 replace columns( id string,subjecty string,scorez string)

<font color=red>生产多使用外部表。</font>

## 分桶表

​		使用分统计数将数据划分成更细的粒度。

​		clustered by (id) into 3 buckets --分成3个桶

​		分桶key.hashcode()%分桶数据

​		insert ... select ... 给桶表加载数据必须为普通表。



# 数据操作

## 数据加载

1、load data [local] inpath '/home/t1.data' [overwrite] into table tableName [partition (dt='partion value')]

2、insert into table tableName partition(month='202001') values(1,"name");

3、insert into table tableName partition(month='202002')  select * from tabe

4、from tabc  insert overwrite table tabc partition(month='202003') select * from tt  

​					insert overwrite table tabc partition(month='202004') select * from tt

5、create table tt as select * from t1;

## 数据导出

1、-- 导出到本地

​	insert overwrite local directory '/home/hadoop/aa.txt'  select * from tt;  

2、查询结果格式化导出

​	insert overwrite local directory '/home/hadoop/data/tabc' row format delimited fields terminated by ' ' select * from tabc;

3、将结果导出到HDFS

​	insert overwrite directory '/home/hadoop/data/tabc' row format delimited fields terminated by ' ' select * from tabc;

4、dfs命令拷贝数据，执行的是hdfs dfs -get 

5、Hive命令导出数据到本地。执行查询重定向到文件 。

​		hive -e "select * from tabC" > a.log

6、export导出数据到HDFS，**反之，可以使用import命令导入**

​	hive>export table tablec  '/user/hadoop/data/tabC4'

7、截断表

​	外部表不能进行truncate操作。

​	truncate table t3; 



# DQL语言

1、**仅支持等值连接** 不支持非等值连接

2、多表连接，会按照从左到右的顺序执行。

3、排序

​		order by 执行全局排序，只有一个reduce

​		sort by 每个MR内部排序

​		**nvl(col,0)**

​		distributed by 将特定的行发送到特定的reducer中，便于后续的聚合与排序操作。

​		distributed by + sort by = cluster by  ---distributed by + sort by 与cluster by 是相同一个字段时。

# 函数

show functions;

desc function [extended] func_name;

## 日期函数

1、 当前日期：unix_timestamp()  =current_timestamp  unix_timestamp 毫秒，， current_timestamp()当前时间戳

2、毫秒数转成格式：from_unixtime(192930231,'yyyy/MM/dd HH:mm:ss') 

​	  时间转成毫秒数：unix_timestamp('2020-01-01 01:01:01')  

3、日期时间差：datediff('2020-06-01','2020-10-10')

4、当前月份的第几天：dayofmonth(current_date)

5、月份的最后一天：last_day(current_date)

6、当月第一天：date_sub(current_date,dayofmonth(current_date)-1)

7、下一个月第一天：add_months(date_sub(current_date,dayofmonth(current_date)-1))

8、字符串转为时间类型：to_date('2020-01-01 01:01:01')

9、时间格式化：date_format(current_timestamp,'yyyy-MM-dd HH:mm:ss')

## 字符串函数和数字函数

1、split、upper、length、lower、substr、concat_ws

2、round、ceil(4.1231)-向上取整，floor--向下取整，abs、power-幂运算、sqrt-平方、log2，log10

## 条件函数

if、case when、coalesce、isnull、isnotnull、nvl、nullif

```sql
-- if语句
select empno,ename,deptno,if(sal<=1500,1 if(sal<=3000,2,3)) from emp;

-- case when 语句
select empno,ename,deptno case when sal<=1500 then 1
							   when sal<=3000 then 2 else 3 end salLevel 
from emp;

--coalesce,返回第一个非空值
select sal,coalesce(comm,0) from emp;

--isnull(a) isnotnull(a)
select * from emp where isnull(comm); == select * from emp where comm is null;
select * from emp where isnotnull(comm)  == select * from emp where comm is not null;

-- nvl
select sal,nvl(comm,0) from emp;

--nullif(x,y) 相等返回为空，否则返回第一个参数。
select nullif("x","y"),nullif("y","x"),null("x","x") 
```

## UDTF函数

表生成函数，一行输入，多行输出。

### explode

​	炸裂函数，将map/array拆分成多行。

​	select explode(array(1,2,3,4,5))

​	select explode(map('a',1,'b',2,'c',3))

​	**lateral view 语法联用**	

```sql
-- lateral view 基本使用
with t1 as (
	select 'ok' cola,split('www.lagou.com','\\.') colb
)
select cola,word 
	from t1 lateral view explode(colb) t2 as word;
	
select id,tag from tb1 lateral view explode(split(tags,',')) t1 as tag;
```

![image-20210405101953167](.\图片\Hive的lateral语法-map.png)

## 窗口函数-分析函数

```sql
--工资占有数据的百分比
select ename,deptno,sal,sum(sal) over() as total,concat(round(sal/sum(sal) over(),4)*100,'%') ratiosal from emp
-- over没有指定参数，默认是整个数据集。

--partition by 分区字段
--order by 排序字段

-- rows between ... and ...
    --unbounded preceding 组内第一行数据
    --n preceding 组内当前行的前n行数据
    --current row 当前行数据
    --n following 组内当前行的后n行数据
    --unbounded following 组内最后一行数据

--分组累加求和
select ename,deptno,sal,sum(sal) over(partition by deptno order by name) from emp

-- 

```

![image-20210405104542535](.\图片\Hive分析函数.png)

### 分析函数-排名函数

​		通常用于求解TopN的问题。

- row_number()  排名顺序增加，不会有重复。
- rank()       排名相等时，会在名字中留下空位
- dense_rank()  排名相等时，会在名字中不会留下空位

![image-20210405104948404](.\图片\分析函数-排名.png)

### 序列函数

​	lag  ：返回当前数据行的上一行数据

​	lead：返回当前数据行的下一行数据

​	first_value：分组排序后，截止到当前行第一个值

​	last_value：分组排序后，截止到当前行最后一个值

​	ntile(5)：数据分组排序后，顺序切分成n分

![image-20210405110507543](.\图片\分析函数-lag-ldead.png)

# 面试题

## 连续七天登录的用户

​		连续值求解问题。

​		1、使用row_number 给组内的数据排序(rownum)。

​		2、某个列 - 减去数据编号（rownum） = gid，作为下一步分组的依据。

​		3、使用gid将数据分组，求得最终结果

![image-20210405112152608](.\图片\hive-连续7天登录的用户.png)

## TopN问题

![image-20210405134113616](.\图片\TopN问题.png)

## 行列互换

1、列转行

​	![image-20210405134337335](H:\拉勾-大数据系统资料与教程\hive\图片\数据聚合.png)

![image-20210405134709500](.\图片\归并相同数据collect_set-list.png)

2、行转列

​	![image-20210405134802578](.\图片\行转列.png)

![image-20210405135059129](.\图片\列转行.png)

# 自定义函数

![image-20210405135858642](.\图片\自定义函数.png)

## UDF 开发

​	**实现**

​		1、继承org.apache.hadoop.hive.ql.exec.UDF

​		2、需要实现evaluate函数；evaluate函数支持重载

​		3、UDF函数必须有返回类型，可以返回null，但是返回类型不能是void



​	**步骤**

​		1、创建maven工程，添加依赖

​		2、开发java类，继承UDF，实现evaluate方法

​		3、上传到服务器，在hive中加载jar包

​				add jar /home/hadoop/hive.jar

​		4、设置函数与自定义函数之间的关系

​				临时函数：create temporary function  mynvl as 'com.lagou.hive.udf.Nvl';

​				永久函数：上传到hive的lib目录，create function mynvl as 'com.lagou.hive.udf.Nvl' using jar 'hdfs:/usr/hadoop/hiveudf.jar'

​		5、测试使用

# DML命令

​	ACID=原子性、一致性、隔离性、持久性

​	1、Hive支持的是行级别的ACID语义。

​	2、Begin、commit、rollback暂时不支持，所有操作自动提交。

​	3、目前只支持ORC格式

​	4、默认事务是关闭的，需要设置开启，使用事务表必须是分桶的

​	5、事务表只能是内部表。

​	6、表必须开始事务属性，transactional=true

​	7、必须使用事务管理器，org.apache.hadoop.hive.ql.lockmgr.DbTxnManager

​	8、目前支持快照级别的隔离。就是当一次数据查询时，会提供一个数据一致性的快照。

​	9、Load data语句目前不支持事务表

## Hive事务

​	1、并发设置：	hive.support.concurrency=true;

​	2、开启桶：		hive.enforce.bucketing=true

​	3、动态分区：	hive.exec.dynamic.partition.mode=nonstrict;

​	4、事务管理器：hive.txn.manager=org.apache.hadoop.hive.ql.lockmgr.DbTxnManager	

```sql
 create table temp1(
     name string,nid int,phone string) 
 clustered by(nid) into 5 buckets 
 stored as orc 
 tblproperties('transactional'='true');
```

# Hive元数据管理与存储

​	1、derby内嵌模式

​	2、本地服务，使用MySQL作为元数据存储

​	3、远程模式，

​	![image-20210405150926834](.\图片\hive元数据配置.png)

![image-20210405151033788](.\图片\Hive元数据配置——远程模式.png)

## 内嵌模式



 ## **远程模式**-Meta store

​		lsof 查看占用的端口号

​		启用hive metastore  ： nohup hive --service metastore &	

```xml
<!-- 启用 hive metastore服务, 指定thrift服务即可 -->
<property>
    <name>hive.metastore.uris</name>
    <value>thrift://linux121:9083,thrift://linux123:9083</value>
</property>

<!-- 可以使用 hive命令连接到启用了hive metastore的服务，使用Hive，相当于安装了Hive的客户端 -->
```

## HiveServer2

​	1、beeline访问HiveServer2服务；

​	2、jdbc访问HiveServer2服务；

​	3、HiveServer2提供了一种允许远程访问的服务

​	4、基于thrift协议，支持跨平台、语言对Hive访问

```xml
<!-- 修改core-site.xml 文件-->
<property>
    <name>hadoop.proxyuser.root.hosts</name>
    <value>*</value>
</property>
<property>
    <name>hadoop.proxyuser.root.groups</name>
    <value>*</value>
</property>

<!-- 修改hdfs-site.xml 文件-->
<property>
    <name>dfs.webhdfs.enabled</name>
    <value>true</value>
</property>

```
使用beeline工具可以连接到hiveServer2服务，也可以连接到MySQL数据。

​		连接Hive： !connect jdbc:hive2://linux123:10000

​		连接MySQL： !connect jdbc:mysql://linux123:3306

## HCatalog

​	HCataLog提供了统一的元数据服务。如Pig、MapReduce通过HCatalog直接访问存储在HDFS上的底层文件。是用来访问Hive的MetaStore的子项目。

​	**HCatalog提供了hcat命令行工具，这个工具和Hive的命令行工具类似，最大不同就是hcat只接受不会产生MapReduce的任务的命令。，主要是DDL命令**

​	$HIVE_HOME/hcatalog/bin/hcat -e "create table tt(id int)"

​	使用Hcatalog的API。

# 数据存储格式

​	TextFile、Rcfile、orcfile、parquet、sequencefile。

​	非textfile格式表不能直接从文件系统导入表中，需要借助textfile表作为跳板，使用insert ... select... 操作。

	行存储与列存储
行式存储下一张表的数据都是放在一起的，但列式存储下数据被分开保存了。
行式存储：
优点：数据被保存在一起，insert和update更加容易
缺点：选择（selection）时即使只涉及某几列，所有数据也都会被读取
列式存储：
优点：查询时只有涉及到的列会被读取，效率高
缺点：选中的列要重新组装，insert/update比较麻烦

## TextFile

![image-20210405160924594](.\图片\hive数据格式-数据导入.png)

## ORC

​	1、对RcFile优化了的文件。

​	2、文件脚注：

## Parquet

​	spark 格式。支持Hive、parquet、drill。

![image-20210405163415813](.\图片\Parquet格式.png)

## 性能对比

TextFile文件更多的是作为跳板来使用(即方便将数据转为其他格式)
有update、delete和事务性操作的需求，通常选择ORCFile
没有事务性要求，希望支持Impala、Spark，建议选择Parquet

# Hive调优

​		数据倾斜、数据冗余、job或IO过多，MR分配不合理都会影响Hive的效率。

## 架构调优

​		1、Hive支持多种引擎，MapReduce、Tez、Spark和Flink。由：hive.execution.engine属性控制。

​			 Tez是构建于Yarn之上的有向无环图。

​		2、选择优化器

			### **向量优化器-Vectorize与基于成本优化器-CBO**

​			 矢量化查询(要求执行引擎为tez)执行通过一次批量执行1024行而非每一行来提高扫描、聚合和连接等操作。

​				set hive.vectorized.execution.enabled=true;

​				set hive.vectorized.execution.reduce.enabled=true;

​			必须使用ORC格式存储数据。

			### **成本优化器**

​				Hive的CBO优化是基于apache Calcite的，Hive的CBO是通过查询成本会生成有效率的执行计划，最终会减少执行的时间和资源的利用。

```sql
				set hive.cbo.enabled=true;
				-- 默认是false
				set hive.compute.query.using.stats=true;    
				-- 默认是false
				set hive.stats.fetch.column.stats=true;
				-- 默认false
 				set hive.stats.fetch.partition.stats=true;
```

		  ### 分区表

对于一张比较大的表，将其设计成分区表可以提升查询的性能，对于一个特定分区的查询，只会加载对应分区路径的文件数据，所以执行速度会比较快。
分区字段的选择是影响查询性能的重要因素，尽量避免层级较深的分区，这样会造成太多的子文件夹。一些常见的分区字段可以是：

- 日期或时间。如year、month、day或者hour，当表中存在时间或者日期字段时
- 地理位置。如国家、省份、城市等
- 业务逻辑。如部门、销售区域、客户等等

### 分桶表

与分区表类似，分桶表的组织方式是将HDFS上的文件分割成多个文件。

分桶可以加快数据采样，也可以提升join的性能(join的字段是分桶字段)，因为分桶可以确保某个key对应的数据在一个特定的桶内(文件)，巧妙地选择分桶字段可以大幅度提升join的性能。

通常情况下，分桶字段可以选择经常用在过滤操作或者join操作的字段。

### 文件格式



## 参数调优

SET hive.exec.compress.intermediate=true 

-- 中间结果压缩
SET hive.intermediate.compression.codec=org.apache.hadoop.io.compress.SnappyCodec ;
-- 输出结果压缩
SET hive.exec.compress.output=true;
SET mapreduce.output.fileoutputformat.compress.codec =org.apache.hadoop.io.compress.SnappyCodc

### 压缩格式





### 本地模式

​	Hive开启本地模式。

		SET hive.exec.mode.local.auto=true; -- 默认 false
		SET hive.exec.mode.local.auto.inputbytes.max=50000000;
		SET hive.exec.mode.local.auto.input.files.max=5; -- 默认 4
​	启用本地模式的触发条件。

```
	一个作业只要满足下面的条件，会启用本地模式
	1、输入文件的大小小于 hive.exec.mode.local.auto.inputbytes.max 配置的大小
	2、map任务的数量小于 hive.exec.mode.local.auto.input.files.max 配置的大小
	3、reduce任务的数量是1或者0
```
### 严格模式

​	就是不允许用户执行3种有风险的HQL语句，一旦执行就会直接失败。

- 查询分区表时不限定分区列的语句；
- 两表join产生了笛卡尔积的语句；
- 用order by来排序，但没有指定limit的语句。

要开启严格模式，需要将参数 hive.mapred.mode 设为strict(缺省值)。
该参数可以不在参数文件中定义，在执行SQL之前设置(set hive.mapred.mode=nostrict )

### JVM重用

​	当map或者reduce是那种仅运行几秒钟的轻量级作业时，JVM启动进程耗费更多的时间。可以启用Hadoop JVM重用，通过共享JVM以穿行而非并行的方式。

​	set mapreduce.job.jvm.numtasks=5;  每个5个Task重用一个JVM。

​	建议打开。

### 并行执行

​	不同Stage可以实现并行执行。

SET hive.exec.parallel=true; -- 默认false
SET hive.exec.parallel.thread.number=16; -- 默认8

并行执行可以增加集群资源的利用率，如果集群的资源使用率已经很高了，那么并行执行的效果不会很明显。

### 推测执行

​		在分布式集群环境下，因为程序Bug、负载不均衡、资源分布不均等原因，会造成同一个作业的多个任务之间运行速度不一致，有些任务的运行速度可能明显慢于其他任务（比如一个作业的某个任务进度只有50%，而其他所有任务已经运行完毕），则这些任务会拖慢作业的整体执行进度。

​		为了避免这种情况发生，Hadoop采用了推测执行机制，它根据一定的规则推测出“拖后腿”的任务，并为这样的任务启动一个备份任务，让该任务与原始任务同时处理同一份数据，并最终选用最先成功运行完成任务的计算结果作为最终结果。

set mapreduce.map.speculative=true
set mapreduce.reduce.speculative=true
set hive.mapred.reduce.tasks.speculative.execution=true

### 合并小文件

1、在map执行前合并小文件，减少map数

**缺省参数**

set hive.input.format=org.apache.hadoop.hive.ql.io.CombineHiveInputFormat;

2、**在Map-Reduce的任务结束时合并小文件**

```shell
#在 map-only 任务结束时合并小文件，默认true
SET hive.merge.mapfiles = true;

#在 map-reduce 任务结束时合并小文件，默认false
SET hive.merge.mapredfiles = true;

#合并文件的大小，默认256M
SET hive.merge.size.per.task = 268435456;

#当输出文件的平均大小小于该值时，启动一个独立的map-reduce任务进行文件merge
SET hive.merge.smallfiles.avgsize = 16777216;
```

### Fetch模式

Fetch模式是指Hive中对某些情况的查询可以不必使用MapReduce计算。selectcol1, col2 from tab ;
可以简单地读取表对应的存储目录下的文件，然后输出查询结果到控制台。在开启fetch模式之后，在全局查找、字段查找、limit查找等都不启动 MapReduce 。

Default Value: minimal in Hive 0.10.0 through 0.13.1, more inHive 0.14.0 and later
hive.fetch.task.conversion=more

## SQL优化

### 列裁剪和分区裁剪

​		列裁剪是在查询时只读取需要的列；分区裁剪就只读需要的分区。

### sort by替代order by

​		HiveQL中的order by与其他关系数据库SQL中的功能一样，是将结果按某字段全局排序，这会导致所有map端数据都进入一个reducer中，在数据量大时可能会长时间计算不完。

​		如果使用sort by，那么还是会视情况启动多个reducer进行排序，并且保证每个reducer内局部有序。为了控制map端数据分配到reducer的key，往往还要配合distribute by 一同使用。如果不加 distribute by 的话，map端数据就会随机分配到reducer。

### group by代替count(distinct uid)

​	当要统计某一列的去重数时，如果数据量很大，count(distinct) 会非常慢。原因与order by类似，count(distinct)逻辑只会有很少的reducer来处理。

![image-20210405175848307](.\图片\hive优化-group by.png)

### group by 配置调整

​	group by时，如果先起一个combiner在map端做部分预聚合，可以有效减少shuffle的数据量。

​		set hive.map.aggr=true  默认为true

​	Map端进行聚合操作的条目数

​		set hive.groupby.mapaggr.checkinterval=100000

​	默认是10W，20W会启动两个MapTask。

### 数据倾斜

​	group by 时如果某些key对应的数据量过大，就会发生倾斜。Hive自带了一个均衡数据倾斜的配置项。

​			hive.groupby.skewindata , 默认值false。

其实现方法是在group by时启动两个MR job。第一个job会将map端数据随机输入reducer，每个reducer做部分聚合，相同的key就会分布在不同的reducer中。第二个job再将前面预处理过的数据按key聚合并输出结果，这样就起到了均衡的效果。
但是，配置项毕竟是死的，单纯靠它有时不能根本上解决问题，建议了解数据倾斜的细节，并优化查询语句。

### SQL Join的三种方式

#### Common Join



#### map Join

​		小表放到内存中，Hive默认是25M。

​		hive.auto.convert.join=true  控制map join，默认开启

​		hive.smalltable.fizlesize=25000000   控制大小表参数。0.8.1以后由 hive.mapjoin.smalltable.filesize决定。

​		hive.mapjoin.cache.numborws 表示缓存build table的多少行数据到内存，默认值25000。

#### bucket map join

​	仅适用分桶表。

分桶连接：Hive 建表的时候支持hash 分区通过指定clustered by (col_name,xxx ) into number_buckets buckets 关键字.当连接的两个表的join key 就是bucket
column 的时候，就可以通过设置hive.optimize.bucketmapjoin= true 来执行优化。

原理：通过两个表分桶在执行连接时会将小表的每个分桶映射成hash表，每个task节点都需要这个小表的所有hash表，但是在执行时只需要加载该task所持有大表分桶对应的小表部分的hash表就可以，所以对内存的要求是能够加载小表中最大的hash块即可。

注意点：**小表与大表的分桶数量需要是倍数关系**，这个是因为分桶策略决定的，分桶时会根据分桶字段对桶数取余后决定哪个桶的，所以要保证成倍数关系。
优点：比map join对内存的要求降低，能在逐行对比时减少数据计算量（不用比对小表全量）
缺点：只适用于分桶表

#### 分桶表map Join

​	分桶表是基于一列进行hash存储的，因此非常适合抽样，按桶或者按块抽样。对应的配置是

​			hive.optimize.bucketmapjoin

### 倾斜均衡配置项

​		与group by的倾斜配置项目相似，通过

​				hive.optimize.skewjoin来配置，，默认是false。

​		如果开启了，在join过程中Hive会将计数超过阈值 hive.skewjoin.key （默认100000）的倾斜key对应的行临时写进文件中，然后再启动另一个job做map join生成结果。通过 hive.skewjoin.mapjoin.map.tasks 参数还可以控制第二个job的mapper数量，默认10000。

### 处理空值或者无意义值

![image-20210405185635172](.\图片\空值处理.png)

### 单独处理倾斜key

​	如果倾斜的 key 有实际的意义，一般来讲倾斜的key都很少，此时可以将它们单独抽取出来，对应的行单独存入临时表中，然后打上一个较小的随机数前缀（比如0~9），最后再进行聚合。
​	不要一个Select语句中，写太多的Join。一定要了解业务，了解数据。(A0-A9)分成多条语句，分步执行；(A0-A4; A5-A9)；先执行大表与小表的关联；

### Map和Reduce个数调整

#### 调整Map数

​	map任务产生包括：

​		1、输入文件总数：采用的是合并策略。

​		2、输入文件大小：

​		3、HDFS块大小

​	对于小文件采用的策略是合并。

​	对于复杂文件采用的是增加Map数据。

```shell
computeSliteSize(max(minSize, min(maxSize, blocksize))) =blocksize
minSize : mapred.min.split.size （默认值1）
maxSize : mapred.max.split.size （默认值256M）
调整maxSize最大值。让maxSize最大值低于blocksize就可以增加map的个数。
建议用set的方式，针对SQL语句进行调整。
```

#### 调整Reduce数

​	 set mapred.reduce.tasks 直接设定reduce数量。如果未设置此参数，会根据如下参数自行推测。

​	1、hive.exec.reducers.bytes.per.reducer用来设定每个reducer能够处理的最大数据量，默认值256M

​	2、hive.exec.reducers.max用来设定每个Job的最大reducer数量，默认值999（1.2版本以前）或1009（1.2版本以后）

​	3、得出reducers数：

​		reducer_num = min(total_input_size /reducers.bytes.per.reducer, reducers.max) = min(数据数据总量/256M,1009)





# Hive参数

1、set mapreduce.job.reduces=2

2、SET hive.exec.compress.intermediate=true 

3、-- 中间结果压缩
	SET hive.intermediate.compression.codec=org.apache.hadoop.io.compress.SnappyCodec ;
4、-- 输出结果压缩
	SET hive.exec.compress.output=true;
	SET mapreduce.output.fileoutputformat.compress.codec =org.apache.hadoop.io.compress.SnappyCodc

