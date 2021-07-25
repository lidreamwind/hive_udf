# 序列化

Driver与Exector通信。

1. 需要实现Seriable接口
2. 使用样例类

# RDD依赖关系

RDD支持粗粒度转换，即在大量记录上执行的单个操作。将创建RDD的一系列Lineage(血统)记录下来，以便恢复失去的分区。



宽窄依赖

![image-20210630230830566](.\图片\看窄依赖.jpg)

窄依赖：1:1 或者n:1

宽依赖：n:m，意味着shuffle



## DAG有向无环图

Driver Program：初始化一个SparkContext即生成一个Spark应用。

job：一个Action算子就会生成一个job。

stage：根据RDD之间的依赖关系的不同将不同job划分成不同的Stage，遇到一个宽依赖则划分成一个stage。

task：stage是一个taskSet，任务调度的最小单位。

task是Spark调度任务的最小单位，每个stage包含许多task，这些task执行的计算逻辑相同，计算的数据是不同的。



**查看rdd的依赖RDD**

rdd3.dependencies(0).rdd.collect

**查看RDD的血缘关系**

rdd1.toDebugString

**RDD的优先位置**

rdd2.preferredLocations(rdd2.partitions(0))

hdfs fsck /wcinput/wc.txt -files -blocks  -location



# RDD缓存

**persist、cache、unpersist，都是transformation**

缓存是将计算结果写入不同的介质，用户定义可定义存储级别，存储级别定义了缓存的介质，**目前支持 内存、对外内存、磁盘**

**缓存有可能会丢失，或者存储于内存的数据由于内存不足而被删除**



cache是persits的简写形式。cache存储到内存中。

![image-20210701215155596](.\图片\persis的内存存储.jpg)



# RDD容错

**涉及的算子：checkpoint，也是transformation**

**本质是通过将RDD写入高可靠的磁盘，主要目的是为了容错**

可以斩断了依赖。



**使用场景**

1. ADG的Lineage过长，如果重算，开销过大
2. 在宽依赖上做Checkpoint则获得的收益更大



sc.setCheckpointDir("/temp/checkpoint")

rdd2.checkpoint

# RDD的分区

默认分区数：spark.default.parallelism = 2



若配置文件中spark-default.conf中没有显示的配置，则按照如下规则取值。

**本地模式**

```shell
spark-shell --master local[N] 	 spark.default.parallelism=N

spark-shell --master local   		spark.default.parallelism = 1
```

**伪分布式模式（x为本机上启动的executor数，y为每个executor使用的core数，z为每个executor使用的内存）**

```shell
spark-shell --master local-cluster[x,y,z]   spark.default.parallelism=x * y
```

**分布式模式（yarn&standalone）**

```shell
spark.default.parallelism = max (应用程序持有executor的core总数，2)
```

# RDD的分区器

key-value的 RDD才可能有分区器。

**分区器：HashPartitioner 和 RangePartitioner**

Range分区应用于排序操作。

使用partitionBy()主动使用分区器

partitionBy(new org.apache.spark.RangePartitioner(2,rdd1)

partitionBy(new org.apache.spark.HashPartitioner(2))



**sortBy调用RangerPartitioner操作，里面有collect算子，会触发job**

## 自定义分区器

继承Partitioner方法

重写numPartitions，定义分区个数

重写getPartition(key:Any)



# 广播变量和累加器

广播变量(broadcast variables)

累加器(accumulators)



广播变量在driver定义，给每个Executor一个变量。

若不是广播变量，每个task都要传输一次。

![image-20210706213758390](.\图片\广播变量.png)

**设置切块大小**

sc.hadoopConfiguration.setLong(fs.local.block.size,128x1024x1024)

## Map端Join

1）shuffle的数据少一点

```scala
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
object JoinDemo {
 def main(args: Array[String]): Unit = {
  val conf = new
SparkConf().setMaster("local[*]").setAppName(this.getClass.get
CanonicalName.init)
  val sc = new SparkContext(conf)
  // 设置本地文件切分大小
  sc.hadoopConfiguration.setLong("fs.local.block.size",
128*1024*1024)
  // map task：数据准备
  val productRDD: RDD[(String, String)] =
sc.textFile("data/lagou_product_info.txt")
  .map { line =>
    val fields = line.split(";")
   (fields(0), line)
  }
  val orderRDD: RDD[(String, String)] =
sc.textFile("data/orderinfo.txt",8 )
  .map { line =>
    val fields = line.split(";")
   (fields(2), line)
  }
  // join有shuffle操作
      val resultRDD = productRDD.join(orderRDD)
  println(resultRDD.count())
  Thread.sleep(1000000)
  sc.stop()
}
}


import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
object MapSideJoin {
 def main(args: Array[String]): Unit = {
  val conf = new
SparkConf().setMaster("local[*]").setAppName(this.getClass.get
CanonicalName.init)
  val sc = new SparkContext(conf)
  // 设置本地文件切分大小
  sc.hadoopConfiguration.setLong("fs.local.block.size",
128*1024*1024)
  // 数据合并：有大量的数据移动
  val productRDD: RDD[(String, String)] =
sc.textFile("data/lagou_product_info.txt")
  .map { line =>
    val fields = line.split(";")
   (fields(0), line)
  }
  val productBC = sc.broadcast(productRDD.collectAsMap())
  // map task：完成数据的准备
  val orderRDD: RDD[(String, String)] =
sc.textFile("data/orderinfo.txt",8 )
  .map { line =>
    val fields = line.split(";")
   (fields(2), line)
  }
  // map端的join
  val resultRDD = orderRDD.map{case (pid, orderInfo) =>
   val productInfo = productBC.value
      (pid, (orderInfo, productInfo.getOrElse(pid, null)))
 }
  println(resultRDD.count())
  Thread.sleep(1000000)
  sc.stop()
}
}
```

## 累加器

累加器是lazy的，需要Action触发；Action触发一次，执行一次，触发多次，执行多次。

**累加器有一个比较经典的应用场景是用来在Spark Streaming应用中记录某些事件的数量**



Spark内置了三种类型的累加器

1. **LongAccumulator用来累加整数型**
2. **DoubleAccumulator用来累加浮点型**
3. **CollectionAccumulator用来累加集合元素**



##　TopN优化

```scala
package cn.lagou.sparkcore
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import scala.collection.immutable
object TopN {
 def main(args: Array[String]): Unit = {
  // 创建SparkContext
  val conf = new
SparkConf().setAppName(this.getClass.getCanonicalName.init).setMaster("local[*]")
  val sc = new SparkContext(conf)
  sc.setLogLevel("WARN")
  val N = 9
     // 生成数据
  val random = scala.util.Random
  val scores: immutable.IndexedSeq[String] = (1 to
50).flatMap { idx =>
  (1 to 2000).map { id =>
    f"group$idx%2d,${random.nextInt(100000)}"
  }
 }
  val scoresRDD: RDD[(String, Int)] = sc.makeRDD(scores).map
{ line =>
   val fields: Array[String] = line.split(",")
  (fields(0), fields(1).toInt)
 }
  scoresRDD.cache()
  // TopN的实现
  // groupByKey的实现，需要将每个分区的每个group的全部数据做shuffle
  scoresRDD.groupByKey()
  .mapValues(buf =>
buf.toList.sorted.takeRight(N).reverse)
  .sortByKey()
  .collect.foreach(println)
  println("******************************************")
     
     
  // TopN的优化
  scoresRDD.aggregateByKey(List[Int]())(
  (lst, score) => (lst :+ score).sorted.takeRight(N),
  (lst1, lst2) => (lst1 ++ lst2).sorted.takeRight(N)
 ).mapValues(buf => buf.reverse)
  .sortByKey()
  .collect.foreach(println)
  // 关闭SparkContext
  sc.stop()
}
}
```

# Standalone作业模式提交

Driver、Master、Worker、Executor



SparkContext中的三大组件：

- **DAGScheduler**：负责将DAG拆分成若干个Stage
- **TaskScheduler**：将DAGScheduler提交的Stage（TaskSet）进行优先级排序，再将task发送到Executor。
- **SchedulerBackend**：定义了许多与Executor事件相关的处理，包括：新的Executor注册进来的时候记录executor的信息，增加全局的资源量（核数）；executor更细状态，若任务完成的话，回收core；其他停止executor、remove executor等事件。

![image-20210707213958080](.\图片\standalone任务提交.png)

# Spark原理初步-RDD编程优化

## RDD复用

在开发过程中，对于同一份数据，只应该创建一个RDD，不要创建多个RDD来代表同一份数据。



## RDD缓存/持久化

- 当多次对同一个RDD执行算子操作时，每一次都会对这个RDD以之前的父RDD重新计算一次，这种情况是要避免的，对同一个RDD的重复计算是对资源的极大浪费
- 对多次使用的RDD进行持久化，通过久化将公共RDD的数据缓存到内存/磁盘中，之后对于公共RDD的计算都会从内存/磁盘中直接获取RDD数据
- RDD的持久化是可以进行序列化的，当内存无法将RDD的数据完整的进行存放的时候，可以考虑使用序列化的方式减小数据体积，将数据完整存储在内存中

## 巧用filter

1. 尽可能早的执行filter操作，过滤无用数据
2. 在filter过滤掉较多数据后，使用 coalesce 对数据进行重分区

## 使用高性能算子

1、避免使用groupByKey，根据场景选择使用高性能的聚合算子 reduceByKey、aggregateByKey

2、coalesce、repartition，在可能的情况下优先选择没有shuffle的操作

3、foreachPartition 优化输出操作

4、map、mapPartitions，选择合理的选择算子mapPartitions性能更好，但数据量大时容易导致OOM

5、用 repartitionAndSortWithinPartitions 替代 repartition + sort 操作

6、合理使用 cache、persist、checkpoint，选择合理的数据存储级别

7、filter的使用

8、减少对数据源的扫描(算法复杂了)

## 设置合理的并行度

Spark作业中的并行度指各个stage的task的数量

设置合理的并行度，让并行度与资源相匹配。简单来说就是在资源允许的前提下，并行度要设置的尽可能大，达到可以充分利用集群资源。合理的设置并行度，可以提升整个Spark作业的性能和运行速度

## 广播大变量

- 默认情况下，task中的算子中如果使用了外部变量，每个task都会获取一份变量的复本，这会造多余的网络传输和内存消耗
- 使用广播变量，只会在每个Executor保存一个副本，Executor的所有task共用此广播变量，这样就节约了网络及内存资源

# shuffle原理

经历了Hash、sort、Tungsten-Sort（可以使用堆外内存）三个阶段。

Spark 1.1以前是Hash shuffle。

spark1.1引入了Sort shuffle。

spark1.6将Tungsten-Sort并入Sort shuffle

spark2.0 Hash shuffle退出历史舞台。



## Hash Shuffle

Hash Shuffle有两个版本Hash Shuffle V1 和Hash Shuffle V2.。

### v1

生成海量小文件。优势是规避了排序。

![image-20210707230117632](.\图片\Hash Shuffle v1.png)



### V2

​	减少了文件的数量，每一个executor生成对应的reduce文件个数。海量小文件问题得到一定程度的缓解。

​	优势，规避了排序，提高了性能。

![image-20210707230247222](.\图片\Hash Shuffle v2.png)

## Sort Base Shuffle

Sort Base Shuffle大大减少了文件数，提高shuffle的效率。

![image-20210707230536275](.\图片\Sort shuffle.png)

Spark shuffle与Hadoop shuffle从目的、意义、功能上看是类似的，实现细节上有区别。



# Spark SQL

 spark sql兼容HiveQL，可以使用Hive的UDFs和SerDes，可以使用Hive的MetaStore。

![image-20210708224656053](.\图片\Spark SQL.png)

优势：

- 写更少的代码
- 读更少的数据（Spark sql的表数据在内存中存储不使用原生态的JVM对象存储方式，而是采用内存列存储）
- 提供更好的性能（字节码生成技术、SQL优化）

## DataFrame和DataSet

Spark SQL提供了两个新的抽象，分别是DataFrame和DataSet。

### DataFrame

DataFrame的前身是SchemaRDD。spark 1.3更名为DataFrame。不继承RDD，自己实现了RDD的大部分功能。



- DF也可以看做分布式Row对象的集合，提供了由列组成的详细模式信息，使其可以得到优化。DF不仅比RDD有更多地算子，还可以进行执行计划的优化
- DF更像传统的二维表格，除了数据结构外，还记录数据的结构信息
- DF也支持嵌套数据结构（struct、array和map）
- DF的API提供的是一套高层的关系操作，比函数式RDD 编程更加友好，门槛更低
- **DF的劣势在于编译期缺少类型安全检查，导致运行时出错**

### DataSet

​	DataSet做了系列化，速度会更快。

​	DataSet是spark 1.6添加的新的接口。

​	**与DF相比，保存了类型信息，是强类型的，提供了编译时类型检查。**

​	调用DataSet的方法会先生成逻辑计划，然后Spark的优化器会进行优化，最终生成物理计划，然后提交到集群中运行。

​	**DataSet包含了DF的功能，在Spark2.0中两者得到了统一：DataFrame表示为DataSet[Row]，即DataSet的子集**

![image-20210712102709330](.\图片\DataSet和RataFrame.png)

### Row&Schema

DataFrame = Rdd[row] + schema，DataFrame前身是SchemaRDD



**三者的共性**

1、RDD、DF、DataSet都是Spark平台下的分布式弹性数据集

2、三个都有共同概念，如分区、持久化、容错等；有许多共同的函数，map、filter、sortBy

3、三个都有惰性机制，只有在遇到Action算子时，才会开始真正的计算

4、**对DataFrame和DataSet进行操作，许多都需要这个包支持，Import spark.implicits._**



**三个区别**

1. 与RDD和DataSet不同，DataFrame每一行的类型固定为Row，只有通过解析才能过去各个字段的值
2. DataFrame和Dataset均支持SparkSQL操作、



**DataSet（DataSet=RDD[case class].toDs)**

**DataFrame(DataFrame=RDD[Row]+Schema)**



## SparkSession



在 Spark 2.0 之前：

- SQLContext 是创建 DataFrame 和执行 SQL 的入口
- HiveContext通过Hive sql语句操作Hive数据，兼Hhive操作，HiveContext继承自SQLContext

![image-20210712173037705](.\图片\spark-session.png)

在 Spark 2.0 之后：

1. 将这些入口点统一到了SparkSession，SparkSession 封装了 SqlContext 及HiveContext；
2. 实现了 SQLContext 及 HiveContext 所有功能；
3. 通过SparkSession可以获取到SparkConetxt；

```scala
import org.apache.spark.sql.SparkSession
val spark = SparkSession
    .builder()
    .appName("Spark SQL basic example")
    .config("spark.some.config.option", "some-value")
    .getOrCreate()
// For implicit conversions like converting RDDs to DataFrames
import spark.implicits._
```

## DF和DataSet

DF是一种特殊的DS。

**1、由Range生成DataSet**

```scala
val numDS = spark.range(5, 100, 5)
// orderBy 转换操作；desc：function；show：Action
numDS.orderBy(desc("id")).show(5)
// 统计信息
numDS.describe().show
// 显示schema信息
numDS.printSchema
// 使用RDD执行同样的操作
numDS.rdd.map(_.toInt).stats
// 检查分区数
numDS.rdd.getNumPartitions
```

**2、由集合生成DataSet**

```scala
case class Person(name:String, age:Int, height:Int)
// 注意 Seq 中元素的类型
val seq1 = Seq(Person("Jack", 28, 184), Person("Tom", 10,
144), Person("Andy", 16, 165))
val ds1 = spark.createDataset(seq1)
// 显示schema信息
ds1.printSchema
ds1.show
val seq2 = Seq(("Jack", 28, 184), ("Tom", 10, 144), ("Andy",
16, 165))
val ds2 = spark.createDataset(seq2)
ds2.show
```

**3、由集合生成DF**

```scala
val lst = List(("Jack", 28, 184), ("Tom", 10, 144), ("Andy",
16, 165))
val df1 = spark.createDataFrame(lst).
// 改单个字段名时简便
withColumnRenamed("_1", "name1").
withColumnRenamed("_2", "age1").
withColumnRenamed("_3", "height1")
df1.orderBy("age1").show(10)
// desc是函数，在IDEA中使用是需要导包
import org.apache.spark.sql.functions._
df1.orderBy(desc("age1")).show(10)



// 修改整个DF的列名
val df2 = spark.createDataFrame(lst).toDF("name", "age","height")
```

**4、将RDD转换为DF**

```scala
import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
val arr = Array(("Jack", 28, 184), ("Tom", 10, 144), ("Andy",
16, 165))
val rdd1 = sc.makeRDD(arr).map(f=>Row(f._1, f._2, f._3))
val schema = StructType( 
      StructField("name", StringType,false) ::
      StructField("age",  IntegerType, false) ::
      StructField("height", IntegerType, false) ::
 Nil)
val schema1 = (new StructType).
add("name", "string", false).
add("age", "int", false).
add("height", "int", false)
// RDD => DataFrame，要指明schema
val rddToDF = spark.createDataFrame(rdd1, schema)
rddToDF.orderBy(desc("name")).show(false)



import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
val arr1 = Array(("Jack", 28, null), ("Tom", 10, 144),("Andy", 16, 165))
val rdd1 = sc.makeRDD(arr1).map(f=>Row(f._1, f._2, f._3))
val structType = StructType(StructField("name", StringType,false) ::
              StructField("age", IntegerType,false) ::
              StructField("height", IntegerType,false) :: Nil)
// false 说明字段不能为空
val schema1 = structType
val df1 = spark.createDataFrame(rdd1, schema1)
// 下一句执行报错(因为有空字段)
df1.show
// true 允许该字段为空，语句可以正常执行
val schema2 = StructType( StructField("name", StringType,false) ::
            StructField("age",  IntegerType,false) ::
            StructField("height", IntegerType,true) ::  Nil)
val df2 = spark.createDataFrame(rdd1, schema2)
df2.show




// IDEA中需要，spark-shell中不需要
import spark.implicits._
val arr2 = Array(("Jack", 28, 150), ("Tom", 10, 144), ("Andy",16, 165))
val rddToDF = sc.makeRDD(arr2).toDF("name", "age", "height")

case class Person(name:String, age:Int, height:Int)

val arr2 = Array(("Jack", 28, 150), ("Tom", 10, 144), ("Andy",16, 165))

val rdd2: RDD[Person] =spark.sparkContext.makeRDD(arr2).map(f=>Person(f._1, f._2,f._3))

val ds2 = rdd2.toDS() // 反射推断，spark 通过反射从caseclass的定义得到类名

val df2 = rdd2.toDF() // 反射推断
ds2.printSchema
df2.printSchema
ds2.orderBy(desc("name")).show(10)
df2.orderBy(desc("name")).show(10)
```

### **RDD转DataSet**

DataSet = RDD[case class]

DataFrame = RDD[Row] + Schema



### **读取文件-CSV**

```scala
  val schema = "name string, age int, job string"
  val df3 = spark.read
  .options(Map(("delimiter", ";"), ("header", "true")))
  .schema(schema)
  .csv("data/people2.csv")
  df3.printSchema()


  val df4 = spark.read
  .option("delimiter", ";")
  .option("header", "true")
  .option("inferschema", "true")
  .csv("data/people2.csv")
  df4.printSchema()
  df4.show
```

![image-20210712190216914](.\图片\RDD-DF-DS互相转化.png)

**SparkSQL提供了一个领域特定语言(DSL)以方便操作。**

## Action操作

**与RDD类似**

show、collect、collectAsList、head、first、count、take、takeAsList、reduce

**与Schema相关**

printSchema、explain、columns、dtypes、col

## Tranformation操作

### **与RDD类似的操作**

map、filter、flatMap、mapPartitions、sample、 randomSplit、 limit、distinct、dropDuplicates、describe



randomSplit按照比重分配数据

Array(dfx,dfy,dfz) = df1.randomSplit(Array(0.5,0.6,0.7))

### **存储相关**

cacheTable、persist、checkpoint、unpersist、cache

​		**Dataset 默认的存储级别是 MEMORY_AND_DISK**

```scala
import org.apache.spark.storage.StorageLevel
spark.sparkContext.setCheckpointDir("hdfs://linux121:9000/checkpoint")

df1.show()
df1.checkpoint()
df1.cache()
df1.persist(StorageLevel.MEMORY_ONLY)
df1.count()
df1.unpersist(true)
df1.createOrReplaceTempView("t1")
spark.catalog.cacheTable("t1")
spark.catalog.uncacheTable("t1")

```

### 与select相关

列的多种表示、select、selectExpr、drop、withColumn、withColumnRenamed、cast（内置函数）

```scala
// 列的多种表示方法。使用""、$""、'、col()、ds("")
// 注意：不要混用；必要时使用spark.implicitis._；并非每个表示在所有的地方都有效
df1.select($"ename", $"hiredate", $"sal").show
df1.select("ename", "hiredate", "sal").show
df1.select('ename, 'hiredate, 'sal).show
df1.select(col("ename"), col("hiredate"), col("sal")).show
df1.select(df1("ename"), df1("hiredate"), df1("sal")).show

// 下面的写法无效，其他列的表示法有效
df1.select("ename", "hiredate", "sal"+100).show
df1.select("ename", "hiredate", "sal+100").show

// 这样写才符合语法
df1.select($"ename", $"hiredate", $"sal"+100).show
df1.select('ename, 'hiredate, 'sal+100).show

// 可使用expr表达式(expr里面只能使用引号)
df1.select(expr("comm+100"), expr("sal+100"),expr("ename")).show
df1.selectExpr("ename as name").show
df1.selectExpr("power(sal, 2)", "sal").show
df1.selectExpr("round(sal, -3) as newsal", "sal","ename").show


// drop、withColumn、 withColumnRenamed、casting
// drop 删除一个或多个列，得到新的DF
df1.drop("mgr")
df1.drop("empno", "mgr")

// withColumn，修改列值
val df2 = df1.withColumn("sal", $"sal"+1000)
df2.show

// withColumnRenamed，更改列名
df1.withColumnRenamed("sal", "newsal")

// 备注：drop、withColumn、withColumnRenamed返回的是DF
// cast，类型转换
df1.selectExpr("cast(empno as string)").printSchema
```

### where相关

where == filter

```scala
// where操作
df1.filter("sal>1000").show
df1.filter("sal>1000 and job=='MANAGER'").show

// filter操作
df1.where("sal>1000").show
df1.where("sal>1000 and job=='MANAGER'").show
```

### groupby相关

groupBy、agg、max、min、avg、sum、count（后面5个为内置函数）

```scala
// groupBy、max、min、mean、sum、count（与df1.count不同）
df1.groupBy("Job").sum("sal").show
df1.groupBy("Job").max("sal").show
df1.groupBy("Job").min("sal").show
df1.groupBy("Job").avg("sal").show
df1.groupBy("Job").count.show

// 类似having子句
df1.groupBy("Job").avg("sal").where("avg(sal) > 2000").show
df1.groupBy("Job").avg("sal").where($"avg(sal)" > 2000).show


// agg
df1.groupBy("Job").agg("sal"->"max", "sal"->"min", "sal"->"avg", "sal"->"sum", "sal"->"count").show
df1.groupBy("deptno").agg("sal"->"max", "sal"->"min", "sal"->"avg", "sal"->"sum", "sal"->"count").show

// 这种方式更好理解
df1.groupBy("Job").agg(max("sal"), min("sal"), avg("sal"),sum("sal"), count("sal")).show

// 给列取别名
df1.groupBy("Job").agg(max("sal"), min("sal"), avg("sal"),sum("sal"), count("sal")).withColumnRenamed("min(sal)","min1").show

// 给列取别名，最简便
df1.groupBy("Job").agg(max("sal").as("max1"),min("sal").as("min2"),avg("sal").as("avg3"),sum("sal").as("sum4"),count("sal").as("count5")).show

df1.groupBy("Job").agg(max("sal").as("max1"),min("sal").as("min2"),avg("sal").as("avg3"),sum("sal").as("sum4"),count("sal").as("count5")).where("min2>2000").show
```

### orderBy相关

orderBy == sort

```scala
// orderBy
df1.orderBy("sal").show
df1.orderBy($"sal").show
df1.orderBy($"sal".asc).show

// 降序
df1.orderBy(-$"sal").show
df1.orderBy('sal).show

df1.orderBy(col("sal")).show
df1.orderBy(df1("sal")).show

df1.orderBy($"sal".desc).show
df1.orderBy(-'sal).show

df1.orderBy(-'deptno, -'sal).show
// sort，以下语句等价
df1.sort("sal").show
df1.sort($"sal").show
df1.sort($"sal".asc).show
df1.sort('sal).show
df1.sort(col("sal")).show
df1.sort(df1("sal")).show

df1.sort($"sal".desc).show
df1.sort(-'sal).show
df1.sort(-'deptno, -'sal).show
```

### join相关

```scala
// 1、笛卡尔积
df1.crossJoin(df1).count

// 2、等值连接（单字段）（连接字段empno，仅显示了一次）
df1.join(df1, "empno").count

// 3、等值连接（多字段）（连接字段empno、ename，仅显示了一次）
df1.join(df1, Seq("empno", "ename")).show
df1.join(df1, List("empno", "ename")).show

// 定义第一个数据集
case class StudentAge(sno: Int, name: String, age: Int)
val lst = List(StudentAge(1,"Alice", 18), StudentAge(2,"Andy",19), StudentAge(3,"Bob", 17), StudentAge(4,"Justin", 21),StudentAge(5,"Cindy", 20))
val ds1 = spark.createDataset(lst)
ds1.show()
// 定义第二个数据集
case class StudentHeight(sname: String, height: Int)
val rdd = sc.makeRDD(List(StudentHeight("Alice", 160),StudentHeight("Andy", 159), StudentHeight("Bob", 170),StudentHeight("Cindy", 165), StudentHeight("Rose", 160)))
val ds2 = rdd.toDS

// 备注：不能使用双引号，而且这里是 ===
ds1.join(ds2, $"name"===$"sname").show
ds1.join(ds2, 'name==='sname).show
ds1.join(ds2, ds1("name")===ds2("sname")).show
ds1.join(ds2, ds1("sname")===ds2("sname"), "inner").show

// 多种连接方式
ds1.join(ds2, $"name"===$"sname").show
ds1.join(ds2, $"name"===$"sname", "inner").show

ds1.join(ds2, $"name"===$"sname", "left").show
ds1.join(ds2, $"name"===$"sname", "left_outer").show

ds1.join(ds2, $"name"===$"sname", "right").show
ds1.join(ds2, $"name"===$"sname", "right_outer").show

ds1.join(ds2, $"name"===$"sname", "outer").show
ds1.join(ds2, $"name"===$"sname", "full").show
ds1.join(ds2, $"name"===$"sname", "full_outer").show
```

### 与集合相关

union==unionAll（过期）、intersect、except

```scala
// union、unionAll、intersect、except。集合的交、并、差
val ds3 = ds1.select("name")
val ds4 = ds2.select("sname")

// union 求并集，不去重
ds3.union(ds4).show

// unionAll、union 等价；unionAll过期方法，不建议使用
ds3.unionAll(ds4).show

// intersect 求交
ds3.intersect(ds4).show

// except 求差
ds3.except(ds4).show
```

### 空值处理

na.fill、na.drop、na.replace

```scala
// NaN (Not a Number)  非法数字
math.sqrt(-1.0)
math.sqrt(-1.0).isNaN()
df1.show

// 删除所有列的空值和NaN
df1.na.drop.show

// 删除某列的空值和NaN
df1.na.drop(Array("mgr")).show
df1.na.drop(List("mgr")).show

// 对全部列填充；对指定单列填充；对指定多列填充
df1.na.fill(1000).show
df1.na.fill(1000, Array("comm")).show
df1.na.fill(Map("mgr"->2000, "comm"->1000)).show

// 对指定的值进行替换
df1.na.replace("comm" :: "deptno" :: Nil, Map(0 -> 100, 10 ->100)).show

// 查询空值列或非空值列。isNull、isNotNull为内置函数
df1.filter("comm is null").show
df1.filter($"comm".isNull).show
df1.filter(col("comm").isNull).show

df1.filter("comm is not null").show
df1.filter(col("comm").isNotNull).show
```

### 窗口函数

一般情况下窗口函数不用 DSL 处理，直接用SQL更方便

参考源码Window.scala、WindowSpec.scala（主要）

```scala
import org.apache.spark.sql.expressions.Window
val w1 = Window.partitionBy("cookieid").orderBy("createtime")
val w2 = Window.partitionBy("cookieid").orderBy("pv")
val w3 = w1.rowsBetween(Window.unboundedPreceding,Window.currentRow)
val w4 = w1.rowsBetween(-1, 1)

// 聚组函数【用分析函数的数据集】
df.select($"cookieid", $"pv",sum("pv").over(w1).alias("pv1")).show
df.select($"cookieid", $"pv",sum("pv").over(w3).alias("pv1")).show
df.select($"cookieid", $"pv",sum("pv").over(w4).as("pv1")).show

// 排名
df.select($"cookieid", $"pv",rank().over(w2).alias("rank")).show
df.select($"cookieid", $"pv",dense_rank().over(w2).alias("denserank")).show
df.select($"cookieid", $"pv",row_number().over(w2).alias("rownumber")).show

// lag、lead
df.select($"cookieid", $"pv", lag("pv",2).over(w2).alias("rownumber")).show
df.select($"cookieid", $"pv", lag("pv",-2).over(w2).alias("rownumber")).show
```

内建函数：http://spark.apache.org/docs/latest/api/sql/index.html

## SQL语句

总体而言：SparkSQL与HQL兼容；与HQL相比，SparkSQL更简洁。

createTempView、createOrReplaceTempView、spark.sql("SQL")

```scala
package cn.lagou.sparksql
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, SparkSession}

case class Info(id: String, tags: String)

object SQLDemo {
 def main(args: Array[String]): Unit = {
  val spark = SparkSession
  .builder()
  .appName("Demo1")
  .master("local[*]")
  .getOrCreate()
     
  val sc = spark.sparkContext
  sc.setLogLevel("warn")
     
  import spark.implicits._
  val arr = Array("1 1,2,3", "2 2,3", "3 1,2")
  val rdd: RDD[Info] = sc.makeRDD(arr).map { line =>
   val fields: Array[String] = line.split("\\s+")
   Info(fields(0), fields(1))
      }
     
  val ds: Dataset[Info] = spark.createDataset(rdd)
  ds.createOrReplaceTempView("t1")
     
  spark.sql(
   """
    |select id, tag
    | from t1
    |    lateral view explode(split(tags, ",")) t2 as
tag
    |""".stripMargin).show
  spark.sql(
   """
    |select id, explode(split(tags, ","))
    | from t1
    |""".stripMargin).show
  spark.close()
}
}
```

## 输入与输出

SparkSQL内建支持的数据源包括：Parquet、JSON、CSV、Avro、Images、BinaryFiles（Spark 3.0）。

**其中Parquet是默认的数据源。**

```scala
// 内部使用
DataFrameReader.format(args).option("key","value").schema(args).load()

// 开发API
SparkSession.read
```



### Parquet

```scala
val df1 = spark.read.format("parquet").load("data/users.parquet")
// Use Parquet; you can omit format("parquet") if you wish as it's the default
val df2 = spark.read.load("data/users.parquet")

// 直接创建表的方式
spark.sql(
   """
    |CREATE OR REPLACE TEMPORARY VIEW users
    |USING parquet
    |OPTIONS (path "data/users.parquet")
    |""".stripMargin
 )
  spark.sql("select * from users").show

// 另一种方式
import spark._
sql("""
 |CREATE OR REPLACE TEMPORARY VIEW users
    |USING parquet
    |OPTIONS (path "data/users.parquet")
    |""".stripMargin
)


df.write.format("parquet")
.mode("overwrite")
.option("compression", "snappy")
.save("data/parquet")

// 内部使用
DataFrameWriter.format(args)
.option(args)
.bucketBy(args)
.partitionBy(args)
.save(path)
```

### CSV

```scala
val df3 = spark.read.format("csv")
.option("inferSchema", "true")
.option("header", "true")
.load("data/people1.csv")

val schema = "name string, age int, job string"
val df3 = spark.read
.options(Map(("delimiter", ";"), ("header", "true")))
.schema(schema)
.csv("data/people2.csv")
df3.printSchema()



 // CSV
  val fileCSV = "data/people1.csv"
  val df = spark.read.format("csv")
  .option("header", "true")
  .option("inferschema", "true")
  .load(fileCSV)

  spark.sql(
   """
    |CREATE OR REPLACE TEMPORARY VIEW people
    | USING csv
    |options(path "data/people1.csv",
    |    header "true",
    |    inferschema "true")
    |""".stripMargin)

  spark.sql("select * from people")
  .write
  .format("csv")
  .mode("overwrite")
  .save("data/csv")
```

![image-20210713220307028](.\图片\csv.png)

### JSON

```scala
val df4 = spark.read.format("json").load("data/emp.json")



```



### JDBC

```scala
val jdbcDF = spark
.read
.format("jdbc")
.option("url", "jdbc:mysql://linux123:3306/ebiz?useSSL=false")
//&useUnicode=true
.option("driver", "com.mysql.jdbc.Driver")
.option("dbtable", "lagou_product_info")
.option("user", "hive")
.option("password", "12345678")
.load()

jdbcDF.show()
jdbcDF.write
.format("jdbc")
.option("url", "jdbc:mysql://linux123:3306/ebiz?useSSL=false&characterEncoding=utf8")
.option("user", "hive")
.option("password", "12345678")
.option("driver", "com.mysql.jdbc.Driver")
.option("dbtable", "lagou_product_info_back")
.mode("append")
.save


-- 创建表
create table lagou_product_info_back as
select * from lagou_product_info;

-- 检查表的字符集
show create table lagou_product_info_back;
show create table lagou_product_info;

-- 修改表的字符集
alter table lagou_product_info_back convert to character set utf8;
```

备注：如果有中文注意表的字符集，否则会有乱码

- SaveMode.ErrorIfExists（默认）。若表存在，则会直接报异常，数据不能存入数据库
- SaveMode.Append。若表存在，则追加在该表中；若该表不存在，则会先创建表，再插入数据
- SaveMode.Overwrite。先将已有的表及其数据全都删除，再重新创建该表，最后插入新的数据
- SaveMode.Ignore。若表不存在，则创建表并存入数据；若表存在，直接跳过数据的存储，不会报错

![image-20210713221534444](.\图片\MySQL.png)

## UDF & UDAF

UDF(User Defined Function)，自定义函数。函数的输入、输出都是一条数据记录，类似于Spark SQL中普通的数学或字符串函数。实现上看就是普通的Scala函数；

UDAF（User Defined Aggregation Funcation），用户自定义聚合函数。函数本身作用于数据集合，能够在聚合操作的基础上进行自定义操作（多条数据输入，一条数据输出）；类似于在group by之后使用的sum、avg等函数；

用Scala编写的UDF与普通的Scala函数几乎没有任何区别，唯一需要多执行的一个步骤是要在SQLContext注册它。

```scala
def len(bookTitle: String):Int = bookTitle.length
spark.udf.register("len", len _)  // 方法转函数
val booksWithLongTitle = spark.sql("select title, author from books where len(title) > 10")

package cn.lagou.sparksql
import org.apache.spark.sql.{Row, SparkSession}
class UDF {
 def main(args: Array[String]): Unit = {
  val spark = SparkSession.builder()
  .appName(this.getClass.getCanonicalName)
  .master("local[*]")
  .getOrCreate()
  spark.sparkContext.setLogLevel("WARN")
     
  val data = List(("scala", "author1"), ("spark","author2"),("hadoop", "author3"), ("hive", "author4"),  ("strom", "author5"), ("kafka", "author6"))
  val df = spark.createDataFrame(data).toDF("title","author")
  df.createTempView("books")
     
  // 定义函数并注册
  def len1(bookTitle: String):Int = bookTitle.length
  spark.udf.register("len1", len1 _)
     
  // UDF可以在select语句、where语句等多处使用
  spark.sql("select title, author, len1(title) from books").show
  spark.sql("select title, author from books where len1(title)>5").show
     
  // UDF可以在DataFrame、Dataset的API中使用
  import spark.implicits._
  df.filter("len1(title)>5").show
     
  // 不能通过编译
  // df.filter(len1($"title")>5).show
     
  // 能通过编译，但不能执行
  // df.select("len1(title)").show
     
  // 不能通过编译
  // df.select(len1($"title")).show
     
  // 如果要在DSL语法中使用$符号包裹字符串表示一个Column，需要用udf方法来接收函数。这种函数无需注册
  import org.apache.spark.sql.functions._
  // val len2 = udf((bookTitle: String) => bookTitle.length)
  // val a:(String) => Int = (bookTitle: String) =>bookTitle.length
  // val len2 = udf(a)
     
  val len2 = udf(len1 _)
  df.filter(len2($"title") > 5).show
  df.select($"title", $"author", len2($"title")).show
     
  // 不使用UDF
  df.map{case Row(title: String, author: String) => (title,author, title.length)}.show
  spark.stop()
}
}


```

**UDAF**

- inputSchema用于定义与DataFrame列有关的输入样式
- bufferSchema用于定义存储聚合运算时产生的中间数据结果的Schema
- dataType标明了UDAF函数的返回值类型
- deterministic是一个布尔值，用以标记针对给定的一组输入，UDAF是否总是生成相同的结果
- initialize对聚合运算中间结果的初始化
- update函数的第一个参数为bufferSchema中两个Field的索引，默认以0开始；
- UDAF的核心计算都发生在update函数中；update函数的第二个参数input:
- Row对应的并非DataFrame的行，而是被inputSchema投影了的行
- merge函数负责合并两个聚合运算的buffer，再将其存储到MutableAggregationBuffer中
- evaluate函数完成对聚合Buffer值的运算，得到最终的结果

```scala
package cn.lagou.sparksql 

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{Row, SparkSession, TypedColumn}
import org.apache.spark.sql.expressions.
{MutableAggregationBuffer, UserDefinedAggregateFunction}
import org.apache.spark.sql.types.{DataType, DoubleType,StringType, StructType}

class TypeUnsafeUDAF extends UserDefinedAggregateFunction {
 // UDAF与DataFrame列有关的输入样式
 override def inputSchema: StructType = new
StructType().add("sales", DoubleType).add("saledate",StringType)
 // 缓存中间结果
 override def bufferSchema: StructType = new
StructType().add("year2019", DoubleType).add("year2020",DoubleType)
 // UDAF函数的返回值类型
 override def dataType: DataType = DoubleType
 // 布尔值，用以标记针对给定的一组输入，UDAF是否总是生成相同的结果。通常用true
 override def deterministic: Boolean = true
 // initialize就是对聚合运算中间结果的初始化
 override def initialize(buffer: MutableAggregationBuffer):Unit = {
  buffer(0) = 0.0
  buffer(1) = 0.0
}
 // UDAF的核心计算都发生在update函数中。
 // 扫描每行数据，都会调用一次update，输入buffer（缓存中间结果）、
input（这一行的输入值）
 // update函数的第一个参数为bufferSchema中两个Field的索引，默认以0开始
 // update函数的第二个参数input: Row对应的是被inputSchema投影了的行。
 // 本例中每一个input就应该只有两个Field的值，input(0)代表销量，
input(1)代表销售日期
 override def update(buffer: MutableAggregationBuffer, input:Row): Unit = {
  val salenumber = input.getAs[Double](0)
  // 输入的字符串为：2014-01-02，take(4) 取出有效的年份
     
     input.getString(1).take(4) match {
   case "2019" => buffer(0) = buffer.getAs[Double](0) +
salenumber
   case "2020" => buffer(1) = buffer.getAs[Double](1) +
salenumber
   case _ => println("ERROR!")
 }
}
 // 合并两个分区的buffer1、buffer2，将最终结果保存在buffer1中
 override def merge(buffer1: MutableAggregationBuffer,
buffer2: Row): Unit = {
  buffer1(0) = buffer1.getDouble(0) + buffer2.getDouble(0)
  buffer1(1) = buffer1.getDouble(1) + buffer2.getDouble(1)
}
 // 取出buffer（缓存的值）进行运算，得到最终结果
 override def evaluate(buffer: Row): Double = {
  println(s"evaluate : ${buffer.getDouble(0)},
${buffer.getDouble(1)}")
  if (buffer.getDouble(0) == 0.0) 0.0
  else (buffer.getDouble(1) - buffer.getDouble(0)) /
buffer.getDouble(0)
}
}
object TypeUnsafeUDAFTest{
 def main(args: Array[String]): Unit = {
  Logger.getLogger("org").setLevel(Level.WARN)
  val spark = SparkSession.builder()
  .appName(s"${this.getClass.getCanonicalName}")
  .master("local[*]")
  .getOrCreate()
  val sales = Seq(
  (1, "Widget Co",     1000.00, 0.00,   "AZ", "2019-01-02"),
  (2, "Acme Widgets",   2000.00, 500.00,  "CA", "2019-02-01"),
  (3, "Widgetry",     1000.00, 200.00,  "CA", "2020-01-11"),
  (4, "Widgets R Us",   2000.00, 0.0,   "CA", "2020-02-19"),
  (5, "Ye Olde Widgete",  3000.00, 0.0,   "MA", "2020-02-28"))
     
     val salesDF = spark.createDataFrame(sales).toDF("id","name", "sales", "discount", "state", "saleDate")
  salesDF.createTempView("sales")
  val userFunc = new TypeUnsafeUDAF
  spark.udf.register("userFunc", userFunc)
  spark.sql("select userFunc(sales, saleDate) as rate from sales").show()
  spark.stop()
}
}
```



## Join

- BroadCast Hash Join （BHJ）
- SHuffle Hash Join
- Shuffle sort Merge Join （SMJ）
- Shuffle-and-replicate nested loop join 又称笛卡尔积
- Broadcast nested



**MapJoin**

spark.sql.autoBroadcastJoinThreshold来配置，默认是10MB

​	-1，默认关闭连接方式

​	只能用于join

**shuffle Hash  Join**

1. 等值连接
2. spark.sql.join.preferSortMergeJoin参数必须设置为false，参数是 Spark 2.0.0引入的，默认值为true，也就是sort Merge Join
3. 小表的的大小（plan.stats.sizeInBytes)必须小于spark.sql.autoBroadcastJoinThreshold*spark.sql.shuffle.partitions(default 200) = 200G
4. 小表大小stats.sizeInBytes的三倍必须小于等于大表的大小(stats.sizeInBytes)，也就是a.stats.sizeInBytes*3 <= b.stats.sizeInBytes



**Shuffle Sort Merge Join**

大表和大表Join



# Spark SQL解析

1、Spark SQL比RDD效率要高一些，RDD的最佳实践。



**Spark SQL对SQL语句的处理和关系型数据库类似，即词法/语法解析、绑定、优化和执行**。

Spark SQL会将SQL语句解析成一棵树，然后使用规则（Rule）对Tree进行绑定、优化等处理过程。

**Spark SQL由Core、Catalyst、Hive、Hive-ThriftServer四部分构成：**

- Core：负责处理数据的输入和输出，如果获取数据，查询结果输出程DataFrame等。
- Catalyst：负责整个查询过程，包括解析、绑定和优化。
- Hive：负责对Hive数据进行处理
- Hive-ThriftServer：主要用于对Hive的访问

![image-20210725113004435](.\图片\Spark-sql.png)

![image-20210725113047856](.\图片\spark-sql-2.png)

Spark SQL 中的Catalyst框架大部分逻辑都是在一个 Tree类型的数据结构上做各种优化。

![image-20210725113251111](.\图片\spark-Session-function.png)

​	![image-20210725113730384](.\图片\queryExecutin.png)

