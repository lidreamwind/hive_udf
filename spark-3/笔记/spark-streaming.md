# Spark Straming

## 定义

- Spark Streaming类似于Apache Storm（来一条数据处理一条，延迟低，响应快，低吞吐量），用于流式数据的处理；
- Spark Streaming具有有高吞吐量和容错能力强等特点；
- Spark Streaming支持的数据输入源很多，例如：Kafka（最重要的数据源）、Flume、Twitter 和 TCP 套接字等；
- 数据输入后可用高度抽象API，如：map、reduce、join、window等进行运算；
- 处理结果能保存在很多地方，如HDFS、数据库等；
- Spark Streaming 能与 MLlib 以及 Graphx 融合。



- Spark Streaming 与 Spark 基于 RDD 的概念比较类似；
- Spark Streaming使用离散化流（Discretized Stream）作为抽象表示，称为DStream。
- DStream是随时间推移而收到的数据的序列。在内部，每个时间区间收到的数据都
- 作为 RDD 存在，DStream 是由这些 RDD 所组成的序列。



Spark Streaming**使用 mini-batch 的架构，把流式计算当作一系列连续的小规模批处理来对待**。

Spark Streaming从各种输入源中读取数据，并把数据分组为小的批次。新的批次按均匀的时间间隔创建出来。

在每个时间区间开始的时候，一个新的批次就创建出来，在该区间内收到的数据都会被添加到这个批次中。在时间区间结束时，批次停止增长。

时间区间的大小是由批次间隔这个参数决定的。批次间隔一般设在500毫秒到几秒之间，由开发者配置。

每个输入批次都形成一个RDD，以 Spark 作业的方式处理并生成其他的 RDD。 处理的结果可以以批处理的方式传给外部系统。

![image-20210726205058994](.\图片\spark-streaming-RDD抽象.png)

![image-20210726205440267](.\图片\spark-streaming-架构.png)

![image-20210726210005282](.\图片\spark-streaming-流程.png)

## 优缺点

**优点**	

- Spark Streaming 内部的实现和调度方式高度依赖 Spark 的 DAG 调度器和RDD，这就决定了 Spark Streaming 的设计初衷必须是粗粒度方式的。同时，
  由于 Spark 内部调度器足够快速和高效，可以快速地处理小批量数据，这就获得准实时的特性
- Spark Streaming 的粗粒度执行方式使其确保 ”处理且仅处理一次” 的特性（EOS），同时也可以更方便地实现容错恢复机制
- 由于 Spark Streaming 的 DStream 本质是 RDD 在流式数据上的抽象，因此基于 RDD 的各种操作也有相应的基于 DStream 的版本，这样就大大降低了用户
  对于新框架的学习成本，在了解 Spark 的情况下用户将很容易使用 SparkStreaming
- 由于 DStream 是在 RDD 上的抽象，那么也就更容易与 RDD 进行交互操作，在需要将流式数据和批处理数据结合进行分析的情况下，将会变得非常方便

**缺点**

- Spark Streaming 的粗粒度处理方式也造成了不可避免的延迟。在细粒度处理方式下，理想情况下每一条记录都会被实时处理，而在 Spark Streaming 中，数据需要汇总到一定的量后再一次性处理，这就增加了数据处理的延迟，这种延迟是由框架的设计引入的，并不是由网络或其他情况造成



## Structured Streaming

Spark Streaming计算逻辑是把数据按时间划分为DStream，存在以下问题：

- 框架自身只能根据 Batch Time 单元进行数据处理，很难处理基于eventtime（即时间戳）的数据，很难处理延迟，乱序的数据
- 流式和批量处理的 API 不完全一致，两种使用场景中，程序代码还是需要一定的转换
- 端到端的数据容错保障逻辑需要用户自己构建，难以处理增量更新和持久化存储等一致性问题



基于以上问题，提出了下一代 Structure Streaming 。将数据源映射为一张无界长度的表，通过表的计算，输出结果映射为另一张表。

以结构化的方式去操作流式数据，简化了实时计算过程，同时还复用了 Catalyst 引擎来优化SQL操作。此外还能支持增量计算和基于event time的计算。

## 数据源

基础数据源：文件数据流、socket数据流、RDD队列流；这些数据源主要用于测试。

```scala
<dependency>
  <groupId>org.apache.spark</groupId>
  <artifactId>spark-streaming_2.12</artifactId>
  <version>${spark.version}</version>
</dependency>
```

### 文件流

文件数据流：通过 textFileStream(directory) 方法进行读取 HDFS 兼容的文件系统文件.

- 不支持嵌套目录
- 文件需要有相同的数据格式
- 文件进入 directory 的方式需要通过移动或者重命名来实现
- 一旦文件移动进目录，则不能再修改，即便修改了也不会读取新数据
- 文件流不需要接收器（receiver），不需要单独分配CPU核



```scala
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
object FileDStream {
 def main(args: Array[String]): Unit = {
  Logger.getLogger("org").setLevel(Level.ERROR)
  val conf = new
SparkConf().setAppName(this.getClass.getCanonicalName).setMaster(
"local[*]")
  // 创建StreamingContext
  // StreamingContext是所有流功能函数的主要访问点，这里使用多个执行线程和
2秒的批次间隔来创建本地的StreamingContext
  // 时间间隔为2秒,即2秒一个批次
  val ssc = new StreamingContext(conf, Seconds(5))
  // 这里采用本地文件，也可以采用HDFS文件
  val lines = ssc.textFileStream("data/log/")
  val words = lines.flatMap(_.split("\\s+"))
  val wordCounts = words.map(x => (x, 1)).reduceByKey(_ + _)
  // 打印单位时间所获得的计数值
  wordCounts.print()
  ssc.start()
  ssc.awaitTermination()
}
}
```

### Socket数据流

随后可以在nc窗口中随意输入一些单词，监听窗口会自动获得单词数据流信息，在监听窗口每隔x秒就会打印出词频统计信息，可以在屏幕上出现结果。

Spark Streaming可以通过Socket端口监听并接收数据，然后进行相应处理；新开一个命令窗口，启动 nc 程序：

![image-20210726232024714](E:\拉勾-大数据系统资料与教程\spark-scala\图片\socket-stream.png)

```scala
package cn.lagou.streaming
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
object SocketDStream {
 def main(args: Array[String]): Unit = {
  Logger.getLogger("org").setLevel(Level.ERROR)
  val conf = new
SparkConf().setAppName(this.getClass.getCanonicalName).setMaster(
"local[*]")
  // 创建StreamingContext
  val ssc = new StreamingContext(conf, Seconds(1))
  val lines = ssc.socketTextStream("linux122", 9999)
  val words = lines.flatMap(_.split("\\s+"))
  val wordCounts = words.map(x => (x.trim, 1)).reduceByKey(_ +
_)
  // 打印单位时间所获得的计数值
  wordCounts.print()
  ssc.start()
  ssc.awaitTermination()
}
}
```

### RDD队列流

调试Spark Streaming应用程序的时候，可使用streamingContext.queueStream(queueOfRDD) 创建基于RDD队列的DStream；

![image-20210726232920610](.\图片\RDD队列流.png)

### ConstanInputStream

```scala
package cn.lagou.streaming
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.ConstantInputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
object BlackListFilter1 {
 def main(args: Array[String]) {
  // 初始化
  val conf = new
SparkConf().setAppName(this.getClass.getCanonicalName).setMaster(
"local[2]")
  val ssc = new StreamingContext(conf, Seconds(10))
  ssc.sparkContext.setLogLevel("WARN")
  // 黑名单数据
  val blackList = Array(("spark", true), ("scala", true))
  val blackListRDD = ssc.sparkContext.makeRDD(blackList)
  // 生成测试DStream。使用ConstantInputDStream
  val strArray: Array[String] = "spark java scala hadoop kafka hive hbase zookeeper"
  .split("\\s+")
  .zipWithIndex
  .map { case (word, idx) => s"$idx $word" }
  val rdd = ssc.sparkContext.makeRDD(strArray)
  val clickStream = new ConstantInputDStream(ssc, rdd)
  // 流式数据的处理
  val clickStreamFormatted = clickStream.map(value =>
(value.split(" ")(1), value))
  clickStreamFormatted.transform(clickRDD => {
   // 通过leftOuterJoin操作既保留了左侧RDD的所有内容，又获得了内容是否在黑名单中
   val joinedBlackListRDD: RDD[(String, (String,
Option[Boolean]))] = clickRDD.leftOuterJoin(blackListRDD)
   joinedBlackListRDD.filter { case (word, (streamingLine,
flag)) =>
    if (flag.getOrElse(false)) false
    else true
  }.map { case (word, (streamingLine, flag)) => streamingLine
}
 }).print()
  // 启动流式作业
  ssc.start()
  ssc.awaitTermination()
}
}
```





## 转换操作

DStream上的操作与RDD的类似，分为 Transformations（转换）和 OutputOperations（输出）两种，此外转换操作中还有一些比较特殊的方法，如：updateStateByKey、transform 以及各种 Window 相关的操作。



DStream 的转化操作可以分为 无状态(stateless) 和 有状态(stateful) 两种：

- 无状态转化操作。每个批次的处理不依赖于之前批次的数据。常见的 RDD 转化操作，例如 map、filter、reduceByKey 等
- 有状态转化操作。需要使**用之前批次的数据 或者是 中间结果来计算当前批次的数**据。有状态转化操作包括：**基于滑动窗口的转化操作** 或 **追踪状态变化的转化**
  **操作**

### 无状态操作

**无状态转化操作就是把简单的 RDD 转化操作应用到每个批次上，也就是转化DStream 中的每一个 RDD**

常见的无状态转换包括：map、flatMap、filter、repartition、reduceByKey、groupByKey；直接作用在DStream上



重要的转换操作：**transform**。通过对源DStream的每个RDD应用RDD-to-RDD函数，创建一个新的DStream。支持在新的DStream中做任何RDD操作。



### 有状态操作

#### 窗口操作

窗口大小、滑动间隔。

Window Operations可以设置**窗口大小**和**滑动窗口间隔**来动态的获取当前Streaming的状态。

基于窗口的操作需要两个参数：

- **窗口长度**(windowDuration)。控制每次计算最近的多少个批次的数据
- **滑动间隔**(slideDuration)。用来控制对新的 DStream 进行计算的间隔

两者都必须是 StreamingContext 中批次间隔(batchDuration)的整数倍

reduceByKeyAndWindow()



##### **updateStateByKey**

- 为Streaming中每一个Key维护一份state状态，state类型可以是任意类型的，可以是自定义对象；更新函数也可以是自定义的
- 通过更新函数对该key的状态不断更新，对于每个新的batch而言，SparkStreaming会在使用updateStateByKey 的时候为已经存在的key进行state的状态更新
- 使用 updateStateByKey 时要**开启 checkpoint 功能**



**统计全局的key的状态，但是就算没有数据输入，也会在每一个批次的时候返回之前的key的状态。**

**这样的缺点：如果数据量很大的话，checkpoint 数据会占用较大的存储，而且效率也不高。**



##### mapWithState

也是用于全局统计key的状态。如果没有数据输入，便不会返回之前的key的状态，有一点增量的感觉。

这样做的好处是，只关心那些已经发生的变化的key，对于没有数据输入，则不会返回那些没有变化的key的数据。即使数据量很大，checkpoint也不会像updateStateByKey那样，占用太多的存储。

### DStream存储

输出操作定义 DStream 的输出操作。

**与 RDD 中的惰性求值类似，如果一个 DStream 及其派生出的 DStream 都没有被执行输出操作，那么这些 DStream 就都不会被求值。**

**如果 StreamingContext 中没有设定输出操作，整个流式作业不会启动。**

![image-20210727223823908](.\图片\Spark-dstream输出.png)

# Spark Streaming 与Kafka

官网：http://spark.apache.org/docs/2.4.5/streaming-kafka-integration.html 

![image-20210727233213713](.\图片\spark-stream连接kafka.png)

## kafka-08接口

基于 Receiver 的方式使用 **Kafka 旧版消费者高阶API**实现。

对于所有的 Receiver，通过 Kafka 接收的数据被存储于 Spark 的 Executors上，底层是写入BlockManager中，默认200ms生成一个block（spark.streaming.blockInterval）。然后由 Spark Streaming 提交的 job 构建BlockRDD，最终以 Spark Core任务的形式运行。对应 Receiver方式，有以下几
点需要注意：

- Receiver 作为一个常驻线程调度到 Executor上运行，占用一个cpu
- Receiver 个数由KafkaUtils.createStream调用次数决定，一次一个 Receiverkafka中的topic分区并不能关联产生在spark streaming中的rdd分区。增加在
  KafkaUtils.createStream()中的指定的topic分区数，仅仅增加了单个receiver消费的topic的线程数，它不会增加处理数据中的并行的spark的数量。【 即：
  topicMap[topic,num_threads]中，value对应的数值是每个topic对应的消费线程数】
- receiver默认200ms生成一个block，可根据数据量大小调整block生成周期。一个block对应RDD一个分区。
- receiver接收的数据会放入到BlockManager，每个 Executor 都会有一个BlockManager实例，由于数据本地性，那些存在 Receiver 的 Executor 会被调度执行更多的 Task，就会导致某些executor比较空闲
- 默认情况下，Receiver是可能丢失数据的。可以通过设置spark.streaming.receiver.writeAheadLog.enable为true开启预写日志机制，将数据先写入一个可靠地分布式文件系统(如HDFS)，确保数据不丢失，但会损失一定性能



**Kafka-08 接口（Receiver方式）：**

- Offset保存在ZK中，系统管理
- 对应Kafka的版本 0.8.2.1+
- 接口底层实现使用 Kafka 旧版消费者高阶API
- DStream底层实现为BlockRDD

![image-20210727234602656](.\图片\Reciver模式.png)



Direct Approach是 Spark Streaming不使用Receiver集成kafka的方式，在企业生产环境中使用较多。相较于Receiver，有以下特点：

- 不使用 Receiver。减少不必要的CPU占用；减少了 Receiver接收数据写入BlockManager，然后运行时再通过blockId、网络传输、磁盘读取等来获取数据
  的整个过程，提升了效率；无需WAL，进一步减少磁盘IO；

- Direct方式生的RDD是KafkaRDD，它的分区数与 Kafka 分区数保持一致，便于把控并行度

  注意：在 Shuffle 或 Repartition 操作后生成的RDD，这种对应关系会失效

- 可以手动维护offset，实现 Exactly Once 语义

![image-20210727235108083](.\图片\direct Kafka.png)

## Kafka-010接口

Spark Streaming与kafka 0.10的整合，和0.8版本的 Direct 方式很像。Kafka的分区和Spark的RDD分区是一一对应的，可以获取 offsets 和元数据，API 使用起来没有显著的区别。

```scala
    <dependency>
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-streaming-kafka-0-10_2.12</artifactId>
      <version>${spark.version}</version>
    </dependency>
```

使用kafka010接口从 Kafka 中获取数据：

- Kafka集群
- kafka生产者发送数据
- Spark Streaming程序接收数

```scala
package cn.lagou.Streaming.kafka
import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer,ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
object KafkaProducer {
 def main(args: Array[String]): Unit = {
  // 定义 kafka 参数
  val brokers = "linux121:9092,linux122:9092,linux123:9092"
  val topic = "topicB"
  val prop = new Properties()
  prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
  prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,classOf[StringSerializer])
  prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,classOf[StringSerializer])
  // KafkaProducer
  val producer = new KafkaProducer[String, String](prop)
  for (i <- 1 to 1000000){
   val msg = new ProducerRecord[String, String](topic,i.toString, i.toString)
   // 发送消息
   producer.send(msg)
   println(s"i = $i")
   Thread.sleep(100)
 }
  producer.close()
}
}
```

```scala
package cn.lagou.Streaming.kafka
import org.apache.kafka.clients.consumer.{ConsumerConfig,ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.InputDStream

import org.apache.spark.streaming.kafka010.{ConsumerStrategies,
KafkaUtils, LocationStrategies}
object KafkaDStream1 {
 def main(args: Array[String]): Unit = {
  // 初始化
  Logger.getLogger("org").setLevel(Level.ERROR)
  val conf: SparkConf = new SparkConf()
  .setMaster("local[2]")
  .setAppName(this.getClass.getCanonicalName)
  val ssc = new StreamingContext(conf, Seconds(2))
  // 定义kafka相关参数
  val kafkaParams: Map[String, Object] =getKafkaConsumerParams()
  val topics: Array[String] = Array("topicB")
  // 从 kafka 中获取数据
  val dstream: InputDStream[ConsumerRecord[String, String]] =KafkaUtils.createDirectStream(
   ssc,
   LocationStrategies.PreferConsistent,
   ConsumerStrategies.Subscribe[String, String](topics,kafkaParams)
 )
  // DStream输出
  dstream.foreachRDD{(rdd, time) =>
   if (!rdd.isEmpty()) {
    println(s"*********** rdd.count = ${rdd.count()}; time =
$time ***********")
  }
 }
  ssc.start()
  ssc.awaitTermination()
}
 def getKafkaConsumerParameters(groupId: String): Map[String,Object] = {
  Map[String, Object](
   ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG ->"linux121:9092,linux122:9092,linux123:9092",
   ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG ->classOf[StringDeserializer],   
   ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG ->classOf[StringDeserializer],
   ConsumerConfig.GROUP_ID_CONFIG -> groupId,
   ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
   ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> (false:java.lang.Boolean)
 )
}
}
```

**LocationStrategies(本地策略)**

- LocationStrategies.PreferBrokers：如果 Executor 在 kafka 集群中的某些节点上，可以使用这种策略。此时Executor 中的数据会来自当前broker节点
- LocationStrategies.PreferConsistent：大多数情况下使用的策略，将Kafka分区均匀的分布在Spark集群的 Executor上
- LocationStrategies.PreferFixed：如果节点之间的分区有明显的分布不均，使用这种策略。通过一个map指定将 topic 分区分布在哪些节点中

**ConsumerStrategies(消费策略)**

- ConsumerStrategies.Subscribe，用来订阅一组固定topic
- ConsumerStrategies.SubscribePattern，使用正则来指定感兴趣的topic
- ConsumerStrategies.Assign，指定固定分区的集合这三种策略都有重载构造函数，允许指定特定分区的起始偏移量；使用 Subscribe
  或 SubscribePattern 在运行时能实现分区自动发现。

### kafka日常操作

```sh
# 检查 topic 的最大offset
kafka-run-class.sh kafka.tools.GetOffsetShell \
--broker-list linux121:9092,linux122:9092,linux123:9092 --topic topicA --time -1

# 列出所有的消费者
kafka-consumer-groups.sh --bootstrap-server linux121:9092  --list

# 检查消费者的offset
kafka-consumer-groups.sh --bootstrap-server linux121:9092,linux122:9092,linux123:9092 \
--describe --group mygroup01

# 重置消费者offset
kafka-consumer-groups.sh --bootstrap-server linux121:9092,linux122:9092,linux123:9092 \
--group group01 --reset-offsets --execute --to-offset 0 --topic topicA

# 创建 topic
kafka-topics.sh --zookeeperlinux121:2181,linux122:2181,linux123:2181 \
--create --topic topicB --replication-factor 2 --partitions 3

# 显示 topic 信息
kafka-topics.sh --zookeeperlinux121:2181,linux122:2181,linux123:2181 --topic topicA -- describe
```



### offset管理



#### **1、获取偏移量(Obtaining Offsets)**

Spark Streaming与kafka整合时，允许获取其消费的 offset ，具体方法如下

```scala
stream.foreachRDD { rdd =>
 val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
 rdd.foreachPartition { iter =>
  val o: OffsetRange = offsetRanges(TaskContext.get.partitionId)
  println(s"${o.topic} ${o.partition} ${o.fromOffset} ${o.untilOffset}")
}
}
```

注意：对HasOffsetRanges的类型转换只有在对 createDirectStream 调用的第一个方法中完成时才会成功，而不是在随后的方法链中。RDD分区和Kafka分区之间的对应关系在 shuffle 或 重分区后会丧失，如reduceByKey 或 window

#### **2、存储偏移量(Storing Offsets)**

在Streaming程序失败的情况下，Kafka交付语义取决于**如何以及何时**存储偏移量。Spark输出操作的语义为 at-least-once。

如果要实现EOS语义(Exactly Once Semantics)，必须在**幂等的输出之后存储偏移量或者 将存储偏移量与输出放在一个事务中**。可以按照增加可靠性（和代码复杂度）的顺序使用以下选项来存储偏移量



**Checkpoint**

Checkpoint是对Spark Streaming运行过程中的元数据和每RDDs的数据状态保存到一个持久化系统中，当然这里面也包含了offset，一般是HDFS、S3，如果应用程序或集群挂了，可以迅速恢复。

**如果Streaming程序的代码变了，重新打包执行就会出现反序列化异常的问题**。这是**因为Checkpoint首次持久化时会将整个 jar 包序列化，以便重启时恢复**。重新打包之后，新旧代码逻辑不同，就会报错或仍然执行旧版代码。

要解决这个问题，只能将HDFS上的checkpoint文件删除，但这样也会同时删除Kafka 的offset信息。

**Kafka**

默认情况下，消费者定期自动提交偏移量，它将偏移量存储在一个特殊的Kafka主题中（__consumer_offsets）。但在某些情况下，这将导致问题，因为消息可能已经被消费者从Kafka拉去出来，但是还没被处理。

可以将 enable.auto.commit 设置为 false ，在 Streaming 程序输出结果之后，手动提交偏移到kafka。

与检查点相比，使用Kafka保存偏移量的优点是无论应用程序代码如何更改，偏移量仍然有效。

```scala
stream.foreachRDD { rdd =>
 val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
 // 在输出操作完成之后，手工提交偏移量；此时将偏移量提交到 Kafka 的消息队列中
 stream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
}
```

与HasOffsetRanges一样，只有在createDirectStream的结果上调用时，转换到CanCommitOffsets才会成功，而不是在转换之后。commitAsync调用是线程安全
的，但必须在输出之后执行

**自定义存储**

Offsets可以通过多种方式来管理，但是一般来说遵循下面的步骤:

- 在 DStream 初始化的时候，需要指定每个分区的offset用于从指定位置读取数据
- 读取并处理消息
- 处理完之后存储结果数据
- 用虚线圈存储和提交offset，强调用户可能会执行一系列操作来满足他们更加严格的语义要求。这包括幂等操作和通过原子操作的方式存储offset
- 将 offsets 保存在外部持久化数据库如 HBase、Kafka、HDFS、ZooKeeper、Redis、MySQL ... ...

![image-20210728220853355](.\图片\stream-kafka-offset.png)

可以将 Offsets 存储到HDFS中，但这并不是一个好的方案。因为HDFS延迟有点高，此外将每批次数据的offset存储到HDFS中还会带来小文件问题；

可以将 Offset 存储到保存ZK中，但是将ZK作为存储用，也并不是一个明智的选择，同时ZK也不适合频繁的读写操作；



##### Redis

要想将Offset保存到外部存储中，关键要实现以下几个功能：

- Streaming程序启动时，从外部存储获取保存的Offsets（执行一次）
- 在foreachRDD中，每个批次数据处理之后，更新外部存储的offsets（多次执行）

```shell
1、数据结构选择：Hash；key、field、value

Key：kafka:topic:TopicName:groupid
Field：partition
Value：offset

2、从 Redis 中获取保存的offsets

3、消费数据后将offsets保存到redis
```

```scala
// 案例一：使用自定义的offsets，从kafka读数据；处理完数据后打印offsets

package cn.lagou.Streaming.kafka
import org.apache.kafka.clients.consumer.{ConsumerConfig,ConsumerRecord}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, TaskContext}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies,HasOffsetRanges, KafkaUtils, LocationStrategies, OffsetRange}
import org.apache.spark.streaming.{Seconds, StreamingContext}

// 使用自定义的offsets，从kafka读数据；处理完数据后打印offsets
object KafkaDStream2 {
 def main(args: Array[String]): Unit = {
     
  // 初始化
  Logger.getLogger("org").setLevel(Level.ERROR)
  val conf: SparkConf = new SparkConf()
  .setMaster("local[2]")
  .setAppName(this.getClass.getCanonicalName)
  val ssc = new StreamingContext(conf, Seconds(2))
     
  // 定义kafka相关参数
  val kafkaParams: Map[String, Object] = getKafkaConsumerParams()
  val topics: Array[String] = Array("topicB")
     
  // 从指定的位置获取kafka数据
  val offsets: collection.Map[TopicPartition, Long] = Map(
   new TopicPartition("topicB", 0) -> 100,
   new TopicPartition("topicB", 1) -> 200,
   new TopicPartition("topicB", 2) -> 300
 )
  // 从 kafka 中获取数据
  val dstream: InputDStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
   ssc,
   LocationStrategies.PreferConsistent,
   ConsumerStrategies.Subscribe[String, String](topics,kafkaParams, offsets)
 )
     
  // DStream输出
 dstream.foreachRDD{(rdd, time) =>
     
   // 输出结果
   println(s"*********** rdd.count = ${rdd.count()}; time = $time ***********")
   // 输出offset
   val offsetRanges: Array[OffsetRange] =rdd.asInstanceOf[HasOffsetRanges].offsetRanges
   rdd.foreachPartition { iter =>    
       val o: OffsetRange = offsetRanges(TaskContext.get.partitionId)
    // 输出kafka消费的offset
    println(s"${o.topic} ${o.partition} ${o.fromOffset} ${o.untilOffset}")
  }
 }
  ssc.start()
  ssc.awaitTermination()
}
    
 def getKafkaConsumerParams(groupId: String = "mygroup1"):
Map[String, Object] = {
  Map[String, Object](
   ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG ->"linux121:9092,linux122:9092,linux123:9092",
   ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG ->classOf[StringDeserializer],
   ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG ->classOf[StringDeserializer],
   ConsumerConfig.GROUP_ID_CONFIG -> groupId,
   ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> (false:java.lang.Boolean))
}
}
// kafka命令，检查 topic offset的值
// kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list linux121:9092,linux122:9092,linux123:9092 --topic topicB --time -1
```



# Spark Graph

## 概述

GraphX 是 Spark 一个组件，专门用来表示图以及进行图的并行计算。GraphX 通过重新定义了图的抽象概念来拓展了 RDD： 定向多图，其属性附加到每个顶点和边。

为了支持图计算， GraphX 公开了一系列基本运算符（比如：mapVertices、mapEdges、subgraph）以及优化后的 Pregel API 变种。此外，还包含越来越多的
图算法和构建器，以简化图形分析任务。

GraphX在图顶点信息和边信息存储上做了优化，使得图计算框架性能相对于原生RDD实现得以较大提升，接近或到达 GraphLab 等专业图计算平台的性能。GraphX最大的贡献是，在Spark之上提供一栈式数据解决方案，可以方便且高效地完成图计算的一整套流水作业。





Pregel 是 Google 于 2010 年在 SIGMOD 会议上发表的《Pregel: A System forLarge-Scale Graph Processing》论文中提到的海量并行图挖掘的抽象框架，Pregel与 Dremel 一样，是 Google 新三驾马车之一，它基于 BSP 模型（Bulk Synchronous Parallel，整体同步并行计算模型），将计算分为若干个超步（super
step），在超步内，通过消息来传播顶点之间的状态。**Pregel 可以看成是同步计算**，**即等所有顶点完成处理后再进行下一轮的超步**，Spark 基于 Pregel 论文实现的海量并行图挖掘框架 GraphX。



## 图计算模式

目前基于图的并行计算框架已经有很多，比如来自Google的Pregel、来自Apache开源的图计算框架Giraph / HAMA以及最为著名的GraphLab，其中Pregel、HAMA和Giraph都是非常类似的，都是基于BSP模式。

**BSP即整体同步并行，它将计算分成一系列超步的迭代。从纵向上看，它是一个串行模式，而从横向上看，它是一个并行的模式，每两个超步之间设置一个栅栏**
**（barrier），即整体同步点，确定所有并行的计算都完成后再启动下一轮超步**



**每一个超步包含三部分内容：**

- 计算compute：每一个processor利用上一个超步传过来的消息和本地的数据进行本地计算
- 消息传递：每一个processor计算完毕后，将消息传递个与之关联的其它processors
- 整体同步点：用于整体同步，确定所有的计算和消息传递都进行完毕后，进入下一个超步

## GraphX架构

![image-20210728234213769](.\图片\spark-graphX架构.png)

## 存储模式

巨型图的存储总体上有边分割和点分割两种存储方式。2013年，GraphLab2.0将其存储方式由边分割变为点分割，在性能上取得重大提升，目前基本上被业界广泛接受并使用。

**边分割（Edge-Cut）**：每个顶点都存储一次，但有的边会被打断分到两台机器上。这样做的好处是节省存储空间；坏处是对图进行基于边的计算时，对于一条
两个顶点被分到不同机器上的边来说，要跨机器通信传输数据，内网通信流量大

**点分割（Vertex-Cut）**：每条边只存储一次，都只会出现在一台机器上。邻居多的点会被复制到多台机器上，增加了存储开销，同时会引发数据同步问题。好
处是可以大幅减少内网通信量

![image-20210728234728829](.\图片\graphX-顶点分割图.png)

虽然两种方法互有利弊，但现在是**点分割占上风**，各种分布式图计算框架都将自己底层的存储形式变成了点分割。主要原因有以下两个：

- 磁盘价格下降，存储空间不再是问题，而内网的通信资源没有突破性进展，集群计算时内网带宽是宝贵的，时间比磁盘更珍贵。这点就类似于常见的空间换时间的策略；
- 在当前的应用场景中，绝大多数网络都是“无尺度网络”，遵循幂律分布，不同点的邻居数量相差非常悬殊。而边分割会使那些多邻居的点所相连的边大多数被分到不同的机器上，这样的数据分布会使得内网带宽更加捉襟见肘，于是边分割存储方式被渐渐抛弃了



## 核心数据结构

```
核心数据结构包括：graph、vertices、edges、triplets（三元组结构)
```

GraphX API 的开发语言目前仅支持 Scala。GraphX 的核心数据结构 Graph 由 RDD封装而成

### graph

![image-20210728235002856](.\图片\graph图结构.png)

triplets代表边点三元组。



## 计算

- 图的定义
- 属性操作
- 转换操作
- 结构操作
- 关联操作
- 聚合操作
- Pregel API

```xml
    <dependency>
      <groupId>org.apache.spark</groupId>
      <artifactId>spark-graphx_2.12</artifactId>
      <version>${spark.version}</version>
    </dependency>
```





