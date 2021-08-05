- 核心组件：Master、worker、sparkContext等
- 热舞调度
- shuffle原理
- 内存管理
- 数据倾斜的处理
- spark优化

# 

# Spark Runtime

![image-20210801104150425](.\图片\Spark Execution In Spark.png)

## 核心组件

**Master(Cluster Manager)**：集群中的管理节点，管理集群资源，通知 Worker 启动Executor 或 Driver。

**Worker** ：集群中的工作节点，负责管理本节点的资源，定期向Master汇报心跳，接收Master的命令，启动Driver 或 Executor。



**Driver**：执行 Spark 应用中的 main 方法，负责实际代码的执行工作。其主要任务：

- 负责向集群申请资源，向master注册信息
- Executor启动后向 Driver 反向注册
- 负责作业的解析、生成Stage并调度Task到Executor上
- 监控Task的执行情况，执行完毕后释放资源
- 通知 Master 注销应用程序



**Executor**：是一个 JVM 进程，负责执行具体的Task。Spark 应用启动时，Executor节点被同时启动，并且始终伴随着整个 Spark 应用的生命周期而存在。如果有 Executor 节点发生了故障或崩溃， 会将出错节点上的任务调度到其他Executor 节点上继续运行。Executor 核心功能：

- 负责运行组成 Spark 应用的任务，并将结果返回给 Driver 进程
- 通过自身的 Block Manage 为应用程序缓存RDD



## 集群部署模式

![image-20210801152708462](.\图片\集群部署模式.png)

1. master MASTER_URL ：决定了 Spark 任务提交给哪种集群处理
2. deploy-mode DEPLOY_MODE：决定了 Driver 的运行方式，可选值为Client（缺省值） 或 Cluster



## Yarn模式运行机制

### Yarn Cluster（Prod)

![image-20210801163958914](.\图片\Yarn-cluster.png)

1. Client先RM提交请求，并上传jar到HDFS上
2. RM在集群中选择一个NM，在其上启动AppMaster，在AppMaster中实例化SparkContext(Driver)
3. AppMaster向RM注册应用程序，注册的目的是申请资源。RM监控App的运行状态直到结束
4. AppMaster申请到资源后，与NM通信，在Container中启动Executor进程Executor向Driver注册，申请任务
5. Driver对应用进行解析，最后将Task发送到Executor上
6. Executor中执行Task，并将执行结果或状态汇报给Driver
7. 应用执行完毕，AppMaster通知RM注销应用，回收资源



### Yarn Client

![image-20210801164702337](.\图片\Yarn-Client模式.png)



1. 启动应用程序实例化SparkContext，向RM申请启动AppMaster
2. M在集群中选择一个NM，在其上启动AppMaster
3. AppMaster向RM注册应用程序，注册的目的是申请资源。RM监控App的运行状态直到结束
4. AppMaster申请到资源后，与NM通信，在Container中启动Executor进程
5. Executor向Driver注册，申请任务
6. Driver对应用进行解析，最后将Task发送到Executor上
7. Executor中执行Task，并将执行结果或状态汇报给Driver
8. 应用执行完毕，AppMaster通知RM注销应用，回收资源



## Rpc框架



**RPC（Remote Procedure Call）远程过程调用**。两台服务器A、B，A服务器上的应用，想要调用B服务器上应用提供的函数/方法，由于不在一个内存空间，不能直接调用，需要通过网络来表达调用的语义和传达调用的数据。

- RPC接口。让使用者调用远程请求时，就像调用本地函数
- 序列化、反序列化
- 网络传输

![image-20210802203336215](.\图片\Rpc框架.png)

### RpcEvn

RpcEnv是RPC的环境对象，管理着整个 RpcEndpoint 的生命周期，其主要功能有：

根据name或uri注册endpoints、管理各种消息的处理、停止endpoints。

其中RpcEnv只能通过RpcEnvFactory创建得到。 RpcEnv中的核心方法：

```scala
// RpcEndpoint 向 RpcEnv 注册
def setupEndpoint(name: String, endpoint: RpcEndpoint):RpcEndpointRef

// 根据参数信息，从 RpcEnv 中获得一个远程的RpcEndpoint
def setupEndpointRef(address: RpcAddress, endpointName: String):RpcEndpointRef
```

### RpcEndPoint

**RpcEndpoint：表示一个消息通信体，可以接收、发送、处理消息。**

RpcEndpoint的生命周期为：constructor -> onStart -> receive* -> onStop，其中：

- onStart在接收任务消息前调用主要用来执行初始化
- receive 和 receiveAndReply 分别用来接收RpcEndpoint send 或 ask 过来的消息
  - receive方法，接收由RpcEndpointRef.send方法发送的消息，该类消息不需要进行响应消息（Reply），而只是在RpcEndpoint端进行处理
  - receiveAndReply方法，接收由RpcEndpointRef.ask发送的消息，RpcEndpoint端处理完消息后，需要给调用RpcEndpointRef.ask的通信端响
    应消息
- send发送的消息不需要立即处理，ask发送的消息需要立即处理

### RpcEndPointRef

**RpcEndpointRef 是对远程 RpcEndpoint 的一个引用。**当需要向一个具体的RpcEndpoint发送消息时，需要获取到该RpcEndpoint的引用，然后通过该引用发送
消息。

- send 方法发送消息后不等待响应，亦即 Send-and-forget
- ask 方法发送消息后需要等待通信对端给予响应，通过 Future 来异步获取响应结果

### 其他组件

- Dispatcher，消息分发器。针对RpcEndpoint发送/接收到的消息，分发至对应的指令收件箱/发件箱。如果指令接收方是自己存入收件箱，如果指令接收方为
  非自身端点，则放入发件箱
- Inbox，指令消息收件箱。一个本地端点对应一个收件箱，Dispatcher在每次向Inbox存入消息时，将对应EndpointData 加入内部待Receiver Queue中，另外Dispatcher创建时会启动一个单独线程进行轮询Receiver Queue，进行收件箱
  消息消费
- OutBox，指令消息发件箱。一个远程端点对应一个发件箱，当消息放入Outbox后，紧接着将消息通过TransportClient 发送出去TransportClient，Netty通信客户端。根据OutBox消息的receiver信息，请求对应远程TransportServer
- TransportServer，Netty通信服务端。一个RPC端点一个TransportServer，接收远程消息后调用Dispatcher分发消息至对应收发件箱



## Master & Worker

Master是RpcEndpoint，实现了 RpcEndpoint 接口Master的生命周期遵循 constructor -> onStart -> receive* -> onStop 的步骤

Master 的 onStart 方法中最重要的事情是：执行恢复





1. Master、Worker都是RpcEndpoint，实现了 RpcEndpoint 接口。主要任务收发、处理消息；
2. Master、Worker的生命周期都遵循:constructor -> onStart -> receive* ->onStop
3. 在Master的onStart方法中最重要的事情是：执行恢复
4. 在Worker的onStart方法中最重要的事情是：向master注册



## SparkContext

Spark应用程序的第一步就是创建并初始化SparkContext，SparkContext的初始化过程包含了内部组件的创建和准备，主要涉及**网络通信、分布式、消息、存储、计算、调度、缓存、度量、清理、文件服务和UI**等方面。

SparkContext 是 Spark 程序主要功能的入口点，链接Spark集群，创建RDD、累加器和广播变量，一个线程只能运行一个SparkContext。



**SparkContext 内部组件：**

- **SparkConf**。Spark Application 的配置，用来设置 Spark 的 KV 格式的参数。可用通过 new 实例化一个 SparkConf 的对象，这可以把所有的以 spark 开头的属性配置好，使用 SparkConf 进行参数设置的优先级是高于属性文件，通过case class Heartbeat(workerId: String, worker: RpcEndpointRef)new SparkConf(false) 可以在进行单元测试的时候不去读取外部的设置。所有的 setter 方法都支持链式表达。一旦 SparkConf 对象传给 Spark，会被其他组件 clone，并且不能再动态的被任何用户修改
- **SparkEnv**。SparkEnv 是Spark的执行环境对象，其中包括与众多Executor执行相关的对象。Executor 有自己的 Spark 的执行环境 SparkEnv。有了SparkEnv，就可以将数据存储在存储体系中；就能利用计算引擎对计算任务进行处理，就可以在节点间进行通信等。在 local 模式下Driver会创建Executor，
  local-cluster部署模式或者 Standalone 部署模式下 Worker 的CoarseGrainedExecutorBackend 进程中也会创建Executor，所以 SparkEnv存在于 Driver 或者 CoarseGrainedExecutorBackend 进程中。SparkEnv包含了很多重要的组件，完成不同的功能
- **DAGScheduler**。DAG调度器，调度系统中最重要的组件之一，负责创建job，将DAG的RDD划分为不同的stage，提交stage
- **TaskScheduler**。任务调度器，调度系统中最重要的组件之一，按照调度算法对集群管理器已经分配给应用程序的资源进行二次调度后分配任务，TaskScheduler调度的 Task是 DAGScheduler创建的，因此DAGScheduler是TaskScheduler的前置调度器
- **SchedulerBackend**。用于对接不同的资源管理系统
- LiveListenerBus。事件总线。接收各个使用方的事件，以异步的方式对事件进行匹配和处理
- SparkUI。用户界面，依赖计算引擎、调度系统、存储体系、作业、阶段、存储、执行器等组件的监控数据，以SparkListenerEnvent的形式投递给LiveListener，Spark从SparkListener中读取数据
- SparkStatusTracker。状态跟踪器，提供对作业、stage等的监控信息
- _shutdownHookRef：任务退出时执行清理任务
- ConsoleProgressBar：进度条，利用SparkStatusTracker的API，在控制台展示Stage的进度
- ContextCleaner：上下文清理器，用异步方式清理超出应用程序范围的RDD、ShuffleDependency和BroadCast
- EventLoggingListener：将事件日志的监听器，Spark可选组件，spark.eventLog.enabled=true时启动
- ExecutorAllocationManager： Executor动态分配管理器，根据工作负载动态调整Executor的数量，在spark.dynamicAllocation.enabled=true的前提下，和非local模式下或者spark.dynamicAllocation.testing=true时启动
- hadoopConfiguration()： hadoop的配置信息，如果使用的是系统SPARK_YARN_MODE=true或者环境变量SPARK_YARN_MODE=true时，启用yarn配置，否则启用hadoop配置
- heartbeatReceiver(RpcEndpointRef)：心跳接收器，Executor都会向heartbeatReceiver发送心跳信息，heartbeatReceiver接收到信息后，更新executor最后的可见时间，然后传递给taskScheduler做进一步处理



### 三大组件

#### DAGScheduler

负责将 DAG 拆分成不同Stage的具有依赖关系（包含RDD的依赖关系）的多批任务，然后提交给TaskScheduler进行具体处理

#### TaskScheduelr

TaskScheduler（底层调度器，trait，只有一种实现TaskSchedulerImpl）：负责实际每个具体Task的物理调度执行

#### SchedulerBackend

有多种实现，分别对应不同的资源管理器。在Standalone模式下，其实现为：StandaloneSchedulerBackend

![image-20210803202303790](.\图片\task_scheduler.png)

![image-20210803203312053](.\图片\sparkconetext.png)



# 作业调度

## 作业

1、job是以Action方法为界，遇到一个Action方法则触发一个job

2、Stage是job的自己，以宽依赖为界。Stage有两个子类

- ​	   ShuffleMapStage，是其他Stage的输入
  - shuffleMapStage内部的转换操作会组成pipline，连在一起计算
  - 产生map输出文件
- ​	   ResultStage，一个job只有一个ResultStage，最有一个Stage即为ResultStage

3、Task是Stage的子集，以并行度来来区分，分区数多少，则有多少tak



Job触发

Stage划分

## DAGSchedulerEventLog

DAGScheduler内部的事件循环处理器，用于处理DAGSchedulerEvent类型的事件。DAGSchedulerEventProcessLoop 实现了**自 EventLoop**。

​	  

## 调度策略

FIFO和Fair



# Shuffle

## Shuffle的两个阶段

![image-20210803225545962](.\图片\spark-shuffle.png)

![image-20210803225730968](.\图片\spark-shuffle-2.png)

![image-20210803225811887](E:\拉勾-大数据系统资料与教程\spark-scala\图片\shuffle的版本.png)

## Hash Shuffle v1

![image-20210803225855002](.\图片\Hash Shuffle v1-new.png)

问题：

​	1、生成大量文件，占用文件描述符，同时引入DiskObjectWriter带来的Writer Handler的缓存也非常消耗内存

​	2、在Reduce Task时需要合并操作的话，会吧数据放到一个HashMap中进行合并，，如果数据量大，容易引发OOM

## Hash Shuffle v2

![image-20210803230130503](.\图片\hash shuffle v2-new.png)

这减少了文件数，但是假如下游Stage的分区数N很大，还是会在Executor生成N个文件，同样，如果一个Executor上有K个Core，还是会开K*N个Writer Handler，这里仍然容易导致OOM

## Sort Shuffle V1



为了更好地解决上面的问题，Spark 参考了 MapReduce 中 Shuffle 的处理方式，引入基于排序的 Shuffle 写操作机制。每个 Task 不会为后续的每个 Task 创建单独的文件，而是**将所有对结果写入同一个文件**。

该文件中的记录首先是按照 Partition Id 排序，每个 Partition 内部再按照Key 进行排序，Map Task 运行期间会顺序写每个 Partition 的数据，**同时生成一个索引文件**记录每个 Partition 的大小和偏移量。

![image-20210803230530812](.\图片\sort shuffle v1.png)

在 Reduce 阶段，Reduce Task 拉取数据做 Combine 时不再采用 HashMap，而是采用ExternalAppendOnlyMap，该数据结构在做 Combine 时，如果内存不足，会刷写磁盘，避免大数据情况下的 OOM。

总体上看来 Sort Shuffle 解决了 Hash Shuffle 的所有弊端，但是因为需要其 Shuffle过程需要对记录进行排序，所以在性能上有所损失。

**Tungsten-Sort Based Shuffle / Unsafe Shuffle**

从 Spark 1.5.0 开始，Spark 开始了钨丝计划（Tungsten），目的是优化内存和CPU的使用，进一步提升Spark的性能。由于使用了堆外内存，而它基于 JDK Sun
Unsafe API，故 Tungsten-Sort Based Shuffle 也被称为 Unsafe Shuffle。

它的做法是将数据记录用二进制的方式存储，直接在序列化的二进制数据上 Sort 而不是在 Java 对象上，这样一方面可以减少内存的使用和 GC 的开销，另一方面避免Shuffle 过程中频繁的序列化以及反序列化。在排序过程中，它提供 cache-efficientsorter，使用一个 8 bytes 的指针，把排序转化成了一个指针数组的排序，极大的优化了排序性能。

**但是使用 Tungsten-Sort Based Shuffle 有几个限制**，Shuffle 阶段不能有aggregate 操作，分区数不能超过一定大小（2^24-1，这是可编码的最大 Parition
Id），所以像 reduceByKey 这类有 aggregate 操作的算子是不能使用 Tungsten-Sort Based Shuffle，它会退化采用 Sort Shuffle。



## Sort shuffle V2

从 Spark1.6.0 开始，把 Sort Shuffle 和 Tungsten-Sort Based Shuffle 全部统一到Sort Shuffle 中，如果检测到满足 Tungsten-Sort Based Shuffle 条件会自动采用Tungsten-Sort Based Shuffle，否则采用 Sort Shuffle。

从Spark2.0 开始，Spark 把 Hash Shuffle 移除， Spark2.x 中只有一种 Shuffle，即为 Sort Shuffle。



## Shuffle Write

ShuffleWriter(抽象类)，有三个具体实现。

- SortShuffeWriter，sortshuffleWriter需要在Map排序
- UnsafeShuffleWriter，使用Java Unsafe直接操作内存，避免Java对象多余的开销和GC延迟，效率高
- BypassMergeSortShuffleWriter。和Hash Shuffle的实现基本相同，区别在于map task输出汇总一个文件，同时还会产生一个index file

**以上Shuffle有各自的使用场景：**

- 不满足以下条件使用sortShuffle
- 没有Map端聚合，RDD的partitions分区数小于16777216，且Serializer支持relocation【Serializer可以对已经序列化的对象进行排序，这种排序起到的效果和下对数据排序在序列化一直】，使用UnsafeShufferWriter
- 没有Map端聚合操作且Rdd的partition分区数小于200个，使用BypassMergeSortShuffleWriter



### ByPass运行机制

ByPass运行机制的触发条件如下：

- shuffle map task数量<= spark.shuffle.sort.bypassMergeThreshold（默认值200)
- 不是聚合类的shuffle算子



Bypass机制Writer的流程如下：

- ​	每个Map Task为每个下游reduce task创建一个临时磁盘文件，并将数据按照key进行hash然后根据hash值写入内存缓冲，缓冲写满后溢写到磁盘文件
- ​	最后将所有临时磁盘文件合并成一个磁盘文件，并创建索引文件；
- ​	ByPass方式的Shuffle Writer机制与Hash shuffle是类似的，在shuffle过程中会创建很多磁盘文件，最后多了一个磁盘文件的合并过程，Shuffle Read的性能会更好
- ​	ByPass方式与普通的Sort Shuffle方式的不同在于：
  - ​		磁盘写机制不同
  - ​		根据key求hash，减少了数据排序操作，提高了性能



### Shuffle Writer写流程

- **数据先写入一个内存数据结构中**。不同的shuffle算子，可能选用不同的数据结构
  - 如果是reduceByKey聚合类算子，选用Map数据结构，一边通过Map进行聚合，一边写入内存
  - 如果是join类的shuffle算子，那么选用Array结构，直接写入内存
- **检查是否达到内存阈值**，每写一条数据进入内存结构，就会判断一下，是否达到了某一临界阈值。如果达到临界阈值的话，那么就会将内存数据结构中的数据溢写到磁盘，并清空数据结构。
- **数据排序**，在溢写到磁盘文件之前，会先根据key对内存数据结构中已有的数据进行排序，排序过后，会分批将数据写入磁盘文件。默认batch是10000条，排序好的数据会以每批次一万条的形式分批写入磁盘文件
- **数据写入缓冲区**，写入磁盘文件是通过Java的 BufferedOutputStream 实现的。BufferedOutputStream 是Java的缓冲输出流，首先会将数据缓冲在内存中，当内存缓冲满溢之后再一次写入磁盘文件中，这样可以减少磁盘IO次数，提升性能
- **重复写多个临时文件**，一个 Task 将所有数据写入内存数据结构的过程中，会发生多次磁盘溢写操作，会产生多个临时文件
- **临时文件合并**，最后将所有的临时磁盘文件进行合并，这就是merge过程。此时会将之前所有临时磁盘文件中的数据读取出来，然后依次写入最终的磁盘文件之中
- **写索引文件**，由于一个 Task 就只对应一个磁盘文件，也就意味着该task为下游stage的task准备的数据都在这一个文件中，因此还会单独写一份索引文件，其中标识了下游各个 Task 的数据在文件中的 start offset 与 end offset



## Shuffle Reader

### Shuffle MapOutPutTracker

Spark的shuffle过程分为Writer和Reader：

- Writer负责生成中间数据
- Reader负责整合中间数据

而中间数据的元信息，则由**MapOutputTracker**负责管理。 它负责Writer和Reader的沟通。

Shuffle Writer会将中间数据保存到Block里面，然后将数据的位置发送给MapOutputTracker。

Shuffle Reader通过向 MapOutputTracker 获取中间数据的位置之后，才能读取到数据。



Shuffle Reader 需要提供 shuffleId、mapId、reduceId 才能确定一个中间数据：

- shuffleId，表示此次shuffle的唯一id
- mapId，表示map端 rdd 的分区索引，表示由哪个父分区产生的数据
- reduceId，表示reduce端的分区索引，表示属于子分区的那部分数据

![image-20210803234815696](.\图片\shuffle wirter.png)

MapOutputTracker在executor和driver端都存在：

- MapOutputTrackerMaster 和 MapOutputTrackerMasterEndpoint（负责通信） 存在于driver
- MapOutputTrackerWorker 存在于 executor 端
- MapOutputTrackerMaster 负责管理所有 shuffleMapTask 的输出数据，每个shuffleMapTask 执行完后会把执行结果（MapStatus对象）注册到MapOutputTrackerMaster
- MapOutputTrackerMaster 会处理 executor 发送的 GetMapOutputStatuses请求，并返回serializedMapStatus 给 executor 端
- MapOutputTrackerWorker 负责为 reduce 任务提供 shuffleMapTask 的输出数据信息（MapStatus对象）
- 如果MapOutputTrackerWorker在本地没有找到请求的 shuffle 的mapStatus，则会向MapOutputTrackerMasterEndpoint 发送GetMapOutputStatuses 请求获取对应的 mapStatus



### Shuffle Reader读流程

- ​	Map Task执行完成后，将文件位置、计算状态等信息封装到MapStatus对象中，再由本进程中的MapOutPutTrackerWorker对象将其发送给Driver进程的MapOutPutTrackerMaster对象
- ​	Reduce Task开始执行之前会让本进程的MapOutPutTrackerWorker向Driver进程中的MapOutPutTrackerMaster发动请求，获取磁盘文件等信息
- ​	当所有Map Task执行完毕后，Driver进程的MapOutPutTrackerMaster就掌握了所有的shuffle文件的信息，此时MapOutPutTrackerMaster会告诉MapOutPutTrackerWorker磁盘小文件的位置信息
- ​	完成之前的操作后，由BlockTransforService去Executor所在的节点拉取数据，默认会启动五个子线程。每次拉取的数据量不超过48M



## 与Hadoop区别

**共同点：**

二者从功能上看是相似的；从High Level来看，没有本质区别，实现（细节）上有区别

**实现上的区别：**

- Hadoop中有一个Map完成，Reduce便可以去fetch数据了，不必等到所有Map任务完成；而Spark的必须等到父stage完成，也就是父stage的 map 操作全部
  完成才能去fetch数据。这是因为spark必须等到父stage执行完，才能执行子stage，主要是为了迎合stage规则
- Hadoop的Shuffle是sort-base的，那么不管是Map的输出，还是Reduce的输出，都是partition内有序的，而spark不要求这一点
- Hadoop的Reduce要等到fetch完全部数据，才将数据传入reduce函数进行聚合，而 Spark是一边fetch一边聚合



## Shuffle优化

开发过程中对shuffle的优化

- ​	减少shuffle过程中的数据量
- 避免shuffle



### 主要参数优化

#### 调节Map端缓冲区大小

- **spark.shuffle.file.buffer**默认值是32k，shuffle write阶段buffer缓冲大小。将数据写到磁盘文件之前，会先写入Buffer缓冲区，缓冲区写满后才溢写到磁盘
- 调节map缓冲区大小，避免频繁的磁盘IO操作，进行提升整体性能
- 合理设置参数，性能会有1%-5%的提升

#### 调节reduce拉取数据缓冲区大小

- **spark.reducer.maxSizeInFlight**默认值是48M。设置Shffle read阶段buffer缓冲区大小，这个buffer缓冲决定了每次能够拉去多少数据
- 在内存资源充足情况下，可以适当增加参数的大小（如96M），减少拉取数据的次数和网络传输的次数，进而提升性能
- 合理设置参数，性能会有1%-5%的提升

#### 调节reduce端拉取数据重试次数及等待时间

- Shuffle read阶段拉取数据时，如果因为网络异常导致拉取失败，会自动进行重试
- **spark.shuffle.io.maxRetries**，默认值3。最大重试次数
- **spark.shuffle.io.retryWait**，默认值5s。每次重试拉取数据的等待间隔一般调高最大重试次数，不调整时间间隔

#### 调节Sort　Shuffle排序操作阈值

- 如果shuffle reduce task的数量小于阈值，则shuffle write过程中不会进行排序操作，而是直接按未经优化的Hash Shuffle方式写数据，最后将每个task产生的所有临时磁盘文件都合并成一个文件，并创建单独的索引文件
- **spark.shuffle.sort.bypassMergeThreshold**，默认值为200
- 当使用SortShuffleManager时，如果的确不需要排序操作，建议将这个参数调大

#### 调整shuffle内存大小

- Spark给 Shuffle 阶段分配了专门的内存区域，这部分内存称为执行内存
- 如果内存充足，而且很少使用持久化操作，建议调高这个比例，给 shuffle 聚合操作更多内存，以避免由于内存不足导致聚合过程中频繁读写磁盘
- 合理调节该参数可以将性能提升10%左右



# 内存管理

在执行 Spark 的应用程序时，Spark 集群会启动 Driver 和 Executor 两种 JVM 进程：

- Driver为主控进程，负责创建 Spark 上下文，提交 Spark 作业，将作业转化为Task，并在各个 Executor 进程间协调任务的调度
- Executor负责在工作节点上执行具体的计算任务，并将结果返回给 Driver，同时为需要持久化的 RDD 提供存储功能

Driver 的内存管理（缺省值 1G）相对来说较为简单，这里主要针对 Executor 的内存管理进行分析，下文中提到的 Spark 内存均特指 Executor 的内存

## 堆内内存和对外内存

​	作为一个 JVM 进程，Executor 的内存管理建立在 JVM 的内存管理之上，Spark 对JVM 的堆内（On-heap）空间进行了更为详细的分配，以充分利用内存。同时，
Spark 引入了堆外（Off-heap）内存，使之可以直接在工作节点的系统内存中开辟空间，进一步优化了内存的使用。

![image-20210804202212121](.\图片\spark-内存管理-.png)

###　堆内内存

​	堆内内存大小，由Spark应用程序启动的executor-memory 或**spark.executor.memory**参数配置。

​	Executor内运行的并发任务共享Jvm堆内内存。

​	

- ​	缓存RDD数据和广播变量占用的内存被规划为**存储内存**
- ​	执行shuffle时占用的内存被规划为执行内存
- ​	Spark内部的对象实例，或者用户自定义的Spark应用程序中的对象实例，均站剩余的空间



​	Spark对于内存的管理属于规划的管理，对象实例占用内存的申请和释放都有JVM完成，Spark只能在申请后和释放前记录这些内存。

虽然不能精准控制堆内内存的申请和释放，但 Spark 通过对存储内存和执行内存各自独立的规划管理，可以决定是否要在存储内存里缓存新的 RDD，以及是否为新的任务分配执行内存，在一定程度上可以提升内存的利用率，减少异常的出现。

### 堆外内存

为了进一步**优化内存的使用以及提高 Shuffle 时排序的效率**，Spark 引入了堆外（Off-heap）内存，使之可以直接在工作节点的系统内存中开辟空间，**存储经过序**
**列化的二进制数据**。

堆外内存意味着把内存对象分配在 Java 虚拟机的堆以外的内存，这些内存直接受操作系统管理。这样做的结果就是能保持一个较小的堆，以减少垃圾收集对应用的影响。

利用 JDK Unsafe API，Spark 可以直接操作系统堆外内存，减少了不必要的内存开销，以及频繁的 GC 扫描和回收，提升了处理性能。堆外内存可以被精确地申请和释放（堆外内存之所以能够被精确的申请和释放，是由于内存的申请和释放不再通过JVM 机制，而是直接向操作系统申请，JVM 对于内存的清理是无法准确指定时间点的，因此无法实现精确的释放），而且序列化的数据占用的空间可以被精确计算，所以相比堆内内存来说降低了管理的难度，也降低了误差。



在默认情况下**堆外内存并不启用**，可通过配置 **spark.memory.offHeap.enabled** 参数启用，并由 **spark.memory.offHeap.size** 参数设定堆外空间的大小。除了没有other 空间，堆外内存与堆内内存的划分方式相同，所有运行中的**并发任务共享 存储内存 和 执行内存 。**



## 静态内存管理

Spark 2.0 以前版本采用静态内存管理机制。存储内存、执行内存和其他内存的大小在 Spark 应用程序运行期间均为固定的，但用户可以应用程序启动前进行配置，堆内内存的分配如下图所示：

![image-20210804205303616](.\图片\executor-memory-manage.png)

可用的存储内存 = systemMaxMemory * spark.storage.memoryFraction *spark.storage.safetyFraction

可用的执行内存 = systemMaxMemory * spark.shuffle.memoryFraction *spark.shuffle.safetyFraction

systemMaxMemory 为当前 JVM 堆内内存的大小



堆外内存分配较为简单，只有存储内存和执行内存。可用的执行内存和存储内存占用的空间大小直接由参数 **spark.memory.storageFraction** 决定。由于堆外内存占用的空间可以被精确计算，无需再设定保险区域。



静态内存管理机制实现起来较为简单，但如果用户不熟悉 Spark 的存储机制，或没有根据具体的数据规模和计算任务或做相应的配置，很容易造成”一半海水，一半火焰”的局面，即存储内存和执行内存中的一方剩余大量的空间，而另一方却早早被占满，不得不淘汰或移出旧的内容以存储新的内容。由于新的内存管理机制的出现，这种方式目前已经很少有开发者使用，出于兼容旧版本的应用程序的目的，Spark 仍然保留了它的实现。



## 统一内存管理

Spark 2.0 之后引入统一内存管理机制，与静态内存管理的区别在于存储内存和执行内存共享同一块空间，可以动态占用对方的空闲区域，统一内存管理的堆内内存结构如下图所示：

![image-20210804210004044](.\图片\统一内存管理.png)

统一内存管理的堆外内存结构如下图所示：

![image-20210804210127545](.\图片\堆外内存结构.png)



其中最重要的优化在于动态占用机制，其规则如下：

- 设定基本的存储内存和执行内存区域（spark.storage.storageFraction 参数），该设定确定了双方各自拥有的空间的范围
- 双方的空间都不足时，则存储到硬盘；若己方空间不足而对方空余时，可借用对方的空间;（存储空间不足是指不足以放下一个完整的 Block）
- 执行内存的空间被对方占用后，可让对方将占用的部分转存到硬盘，然后”归还”借用的空间
- 存储内存的空间被对方占用后，无法让对方”归还”，因为需要考虑 Shuffle 过程中的很多因素，实现起来较为复杂



![image-20210804210425465](.\图片\堆内内存管理-1.png)

​		**在执行过程中：执行内存的优先级 > 存储内存的优先级（理解）**



   凭借统一内存管理机制，Spark 在一定程度上提高了堆内和堆外内存资源的利用率，降低了开发者维护 Spark 内存的难度，但并不意味着开发者可以高枕无忧。如果存储内存的空间太大或者说缓存的数据过多，反而会导致频繁的全量垃圾回收，降低任务执行时的性能，因为缓存的 RDD 数据通常都是长期驻留内存的。所以要想充分发挥 Spark 的性能，需要开发者进一步了解存储内存和执行内存各自的管理方式和实现原理。

## 存储内存管理

```tex
堆内内存：系统保留(300M)、Other、存储内存、执行内存
堆外内存：存储内存、执行内存

存储内存：RDD缓存的数据 & 共享变量

RDD的持久化
RDD缓存的过程
淘汰与落盘
```

### RDD的持久化

RDD作为 Spark 最根本的数据抽象，是只读的分区记录的集合，只能基于在稳定物理存储中的数据集上创建，或者在其他已有的 RDD 上执行转换操作产生一个新的RDD。转换后的 RDD 与原始的 RDD 之间产生的依赖关系。**凭借Lineage，Spark保证了每一个 RDD 都可以被重新恢复**。但 RDD 的所有转换都是惰性的，即只有当一个返回结果给 Driver 的Action发生时，Spark 才会创建任务读取 RDD，然后真正触发转换的执行。



Task 在启动之初读取一个分区时：

- 先判断这个分区是否已经被持久化
- 如果没有则需要检查 Checkpoint 或按照血统重新计算。如果一个 RDD 上要执行多次Action，可以在第一次行动中使用 persist 或 cache 方法，在内存或磁盘
  中持久化或缓存这个 RDD，从而在执行后面的Action时提升计算速度。



**RDD 的持久化由 Spark 的 Storage【BlockManager】 模块负责，实现了 RDD 与物理存储的解耦合**。Storage 模块负责管理 Spark 在计算过程中产生的数据，将那些在内存或磁盘、在本地或远程存取数据的功能封装了起来。在具体实现时Driver 端和Executor 端 的 Storage 模块构成了主从式架构，即 Driver 端 的 BlockManager 为Master，Executor 端的 BlockManager 为 Slave。



Storage 模块在逻辑上以 Block 为基本存储单位，RDD 的每个Partition 经过处理后唯一对应一个Block。Driver 端的 Master 负责整个 Spark 应用程序的 Block 的元数据信息的管理和维护，而 Executor 端的 Slave 需要将 Block 的更新等状态上报到Master，同时接收Master 的命令，如新增或删除一个 RDD。



#### RDD的缓存过程

```
File => RDD1 => RDD2 =====> RDD3 => RDD4 =====> RDD5 =>Action

RDD缓存的源头：Other (Iterator / 内存空间不连续)
RDD缓存的目的地：存储内存(内存空间连续)
```



**RDD 在缓存到存储内存之前，Partition 中的数据一般以迭代器（Iterator）的数据结构来访问**，这是 Scala 语言中一种遍历数据集合的方法。通过 Iterator 可以获取分区中每一条序列化或者非序列化的数据项(Record)，这些 Record 的对象实例在逻辑上占用了 **JVM 堆内内存的 other** 部分的空间，**同一 Partition 的不同 Record 的存储空间并不连续**。

Block 有序列化和非序列化两种存储格式，具体以哪种方式取决于该 RDD 的存储级别：

- 非序列化的 Block 以 DeserializedMemoryEntry 的数据结构定义，用一个数组存储所有的对象实例
- 序列化的 Block 以 SerializedMemoryEntry 的数据结构定义，用字节缓冲区（ByteBuffer）存储二进制数据



因为不能保证存储空间可以一次容纳 Iterator 中的所有数据，当前的计算任务在Unroll 时要向 MemoryManager 申请足够的 Unroll 空间来临时占位，空间不足则
Unroll 失败，空间足够时可以继续进行。

- 序列化的 Partition，其所需的 Unroll 空间可以直接累加计算，一次申请
- 非序列化的 Partition 则要在遍历 Record 的过程中依次申请，即每读取一条
  Record，采样估算其所需的 Unroll 空间并进行申请，空间不足时可以中断，释放已占用的 Unroll 空间
- 如果最终 Unroll 成功，当前 Partition 所占用的 Unroll 空间被转换为正常的缓
  存 RDD 的存储空间



#### 淘汰与落盘

由于同一个 Executor 的所有的计算任务共享有限的存储内存空间，当有新的 Block需要缓存但是剩余空间不足且无法动态占用时，就要对 LinkedHashMap 中的旧Block 进行淘汰（Eviction），而被淘汰的 Block 如果其存储级别中同时包含存储到磁盘的要求，则要对其进行落盘（Drop），否则直接删除该 Block。

```
Memory_And_Disk => cache => Memory

淘汰：从内存空间中清除
落盘：将存储内存中的数据(RDD缓存的数据)写到磁盘上
```



**存储内存的淘汰规则为：**

- 被淘汰的旧 Block 要与新 Block 的 MemoryMode 相同，即同属于堆外或堆内内存
- 新旧 Block 不能属于同一个 RDD，避免循环淘汰
- 旧 Block 所属 RDD 不能处于被读状态，避免引发一致性问题
- 遍历 LinkedHashMap 中 Block，按照最近最少使用（LRU）的顺序淘汰，直到满足新 Block 所需的空间。其中 LRU 是 LinkedHashMap 的特性。



落盘的流程则比较简单，如果其存储级别符合 _useDisk 为 true 的条件，再根据其_deserialized 判断是否是非序列化的形式，若是则对其进行序列化，最后将数据
存储到磁盘，在 Storage 模块中更新其信息

## 执行内存管理

执行内存主要用来存储任务在执行 Shuffle 时占用的内存，Shuffle 是按照一定规则对 RDD 数据重新分区的过程， Shuffle 的 Write 和 Read 两阶段对执行内存的使用：

**Shuffle Write**

- 在 map 端会采用 ExternalSorter 进行外排，在内存中存储数据时主要占用堆内执行空间。

**Shuffle Read**

- 在对 reduce 端的数据进行聚合时，要将数据交给 Aggregator 处理，在内存中存储数据时占用堆内执行空间
- 如果需要进行最终结果排序，则要将再次将数据交给 ExternalSorter 处理，占用堆内执行空间



在 ExternalSorter 和 Aggregator 中，**Spark 会使用一种叫 AppendOnlyMap 的哈希表在堆内执行内存中存储数据**，但在 Shuffle 过程中所有数据并不能都保存到该哈希表中，当这个哈希表占用的内存会进行周期性地采样估算，当其大到一定程度，无法再从 MemoryManager 申请到新的执行内存时，Spark 就会将其全部内容存储到磁盘文件中，这个过程被称为溢存(Spill)，溢存到磁盘的文件最后会被归并。



Spark 的存储内存和执行内存有着截然不同的管理方式：

- 对存储内存来说，Spark 用一个 **LinkedHashMap** 来集中管理所有的 Block，Block 由需要缓存的 RDD 的 Partition 转化而成；
- 对执行内存来说，Spark 用 **AppendOnlyMap** 来存储 Shuffle 过程中的数据，在 Tungsten 排序中甚至抽象成为页式内存管理，开辟了全新的 JVM 内存管理机制

## BlockManager

BlockManager是一个嵌入在 Spark 中的 key-value型分布式存储系统，也是Master-Slave 结构的，**RDD-cache、 shuffle-output、broadcast** 等的实现都是基
于BlockManager来实现的：

- shuffle 的过程中使用 BlockManager 作为数据的中转站
- 将广播变量发送到 Executor 时， broadcast 底层使用的数据存储层
- spark streaming 一个 ReceiverInputDStream 接收到的数据，先放在BlockManager 中， 然后封装为一个 BlockRdd 进行下一步运算
- 如果对一个 RDD 进行了cache，CacheManager 也是把数据放在了BlockManager 中， 后续 Task 运行的时候可以直接从 CacheManager 中获取到缓存的数据 ，不用再从头计算



![image-20210804213756407](.\图片\blockManager.png)

Driver 上有 BlockManager Master，负责对各个节点上的 BlockManager 内部管理的数据的元数据进行维护，比如 block 的增删改等操作，都在这里维护好元数据的变更。

每个节点都有一个 BlockManager，每个 BlockManager 创建之后，第一件事就是去向 BlockManager Master 进行注册，此时 BlockManager Master 会为其创建对应的 BlockManagerInfo。



Driver的组件为BlockManager Master，负责：

- 各节点上BlockManager内部管理数据的元数据进行维护，如 block 的增、删、改、查等操作
- 只要 BlockManager 执行了数据增、删、改操作，那么必须将 Block 的BlockStatus 上报到BlockManager Master，BlockManager Master会对元数据进行维护



BlockManager运行在所有的节点上，包括所有 Driver 和 Executor 上：

- BlockManager对本地和远程提供一致的 get 和 set 数据块接口，BlockManager本身使用不同的存储方式来存储这些数据，包括memory、disk、off-heap
- BlockManager负责Spark底层数据存储与管理，Driver和Executor的所有数据都由对应的BlockManager进行管理
- BlockManager创建后，立即向 BlockManager Master进行注册，此时BlockManager Master会为其创建对应的BlockManagerInfo
- **BlockManager中有3个非常重要的组件**
  - DiskStore：负责对磁盘数据进行读写
  - MemoryStore：负责对内存数据进行读写
  - BlockTransferService：负责建立到远程其他节点BlockManager的连接，负责对远程其他节点的BlockManager的数据进行读写
- 使用BlockManager进行写操作时，如RDD运行过程中的中间数据，或者执行persist操作，会优先将数据写入内存中。如果内存大小不够，将内存中的部分数
  据写入磁盘；如果persist指定了要replica，会使用BlockTransferService将数据复制一份到其他节点的BlockManager上去
- 使用 BlockManager 进行读操作时，如 Shuffle Read 操作，如果能从本地读取，就利用 DiskStore 或MemoryStore 从本地读取数据；如果本地没有数据，就利用 BlockTransferService 从远程 BlockManager 读取数据



# 数据倾斜

Task之间数据分配的非常不均匀

**key.hashCode % reduce个数 = 分区号**



**数据倾斜有哪些现象**

1. Executor lost、OOM、Shuffle过程出错、程序执行慢
2. 单个Executor执行时间特别久，整体任务卡在某个阶段不能结束
3. 正常运行的任务突然失败大多数 Task 运行正常，个别Task运行缓慢或发生OOM



**数据倾斜造成的危害有哪些**

- 个别任务耗时远高于其它任务，轻则造成系统资源的浪费，使整体应用耗时过大，不能充分发挥分布式系统并行计算的优势
- 个别Task发生OOM，导致整体作业运行失败



## **为什么会发生数据倾斜**

**数据异常**

参与计算的 key 有大量空值(null)，这些空值被分配到同一分区



**Map Task数据倾斜，主要是数据源导致的数据倾斜：**

- 数据文件压缩格式（压缩格式不可切分）
- Kafka数据分区不均匀



**Reduce task数据倾斜（重灾区，最常见）**

- Shuffle （外因）。Shuffle操作涉及到大量的磁盘、网络IO，对作业性能影响极大
- Key分布不均 （内因）



**如何定位发生数据倾斜**

凭借经验或Web UI，找到对应的Stage；再找到对应的 Shuffle 算子



## 数据倾斜的处理

**做好数据预处理：**

- 过滤key中的空值
- 消除数据源带来的数据倾斜（文件采用可切分的压缩方式）

**数据倾斜产生的主要原因：Shuffle + key分布不均**



**处理数据倾斜的基本思路：**

- 消除shuffle
- 减少shuffle过程中传输的数据
- 选择新的可用于聚合或join的Key（结合业务）
- 重新定义分区数
- 加盐强行打散Key



### 避免shuffle

- Map端的join是典型的解决方案
- 可以完全消除Shuffle，进而解决数据倾斜
- 有很强的适用场景(大表和小表关联)，典型的大表与小表的join，其他场景不合适



### 减少 Shuffle 过程中传输的数据

- 使用高性能算子，避免使用groupByKey，用reduceByKey或aggregateByKey替代
- 没有从根本上解决数据分配不均的问题，收效有限，使用场景有限



### 选择新的可用于聚合或join的Key

- 从业务出发，使用新的key去做聚合或join。如当前key是【省 城市 日期】，在业务允许的情况下选择新的key【省 城市 区 日期】，有可能 解决或改善 数据倾斜问题
- 存在的问题：这样的key不好找；或者找到了新的key也不能解决问题



### 改变Reduce的并行度

​	key.hashCode % reduce个数 = 分区号



- 变更 reduce 的并行度。理论上分区数从 N 变为 N-1 有可能解决或改善数据倾斜
- 一般情况下这个方法不管用，数据倾斜可能是由很多key造成的，但是建议试试因为这个方法非常简单，成本极低
- 可能只是解决了这一次的数据倾斜问题，非长远之计
- 缺点：适用性不广；优点：简单



### 加盐强行打散Key

shuffle + key不能分散

#### 两阶段聚合

- 加盐打散key。给每个key都加一个随机数，如10以内的随机数。此时key就被打散了
- 局部聚合。对打上随机数的数据，执行一次聚合操作，得到结果
- 全局聚合。将各个key的前缀去掉，再进行一次聚合操作，得到最终结果

![image-20210804222435758](.\图片\加盐打散key.png)

**两阶段聚合的优缺点：**

- 对于聚合类的shuffle操作导致的数据倾斜，效果不错。通常都可以解决掉数据倾斜，至少是大幅度缓解数据倾斜，将Spark作业的性能提升数倍以上
- 仅适用于聚合类的shuffle操作，适用范围相对较窄。如果是join类的shuffle操作，还得用其他的解决方案



#### 采样倾斜key并拆分join操作

业务场景：两个RDD/两张表进行 join 的时候，数据量都比较大

**使用场景**：计算两个RDD/两张表中的key分布情况。如果出现数据倾斜，是其中一个RDD/Hive表中的少数几个key的数据量过大，而另一个RDD/Hive表中的所有key都分布比较均匀，那么采用这个解决方案比较合适。

![image-20210805202124002](.\图片\图片.png)

处理步骤：
1、对包含少数几个数据量过大的key的那个RDD，通过sample算子采样出一份样本来，然后统计一下每个key的数量，计算出数据量最大的是哪几个key；

2、将这几个key对应的数据从原来的RDD中拆分出来，形成一个单独的RDD，并给每个key都打上n以内的随机数作为前缀，而不会导致倾斜的大部分key形成另外一个RDD；

3、将需要join的另一个RDD，也过滤出来那几个倾斜key对应的数据并形成一个单独的RDD，将每条数据膨胀成n条数据，这n条数据都按顺序附加一个0~n的前缀，不会导致倾斜的大部分key也形成另外一个RDD；

4、再将附加了随机前缀的独立RDD与另一个膨胀n倍的独立RDD进行join，此时就可以将原先相同的key打散成n份，分散到多个task中去进行join了；

5、另外两个普通的RDD就照常join即可；

6、最后将两次join的结果使用union算子合并起来即可，就是最终的join结果



**小结：**
数据倾斜问题的解决没有银弹，通常是找到数据倾斜发生的原因，然后见招拆招；

在实践中很多情况下，如果只是处理较为简单的数据倾斜场景，使用上述方案中的某一种基本就可以解决。

但是如果要处理一个较为复杂的数据倾斜场景，可能需要将多种方案组合起来使用。

需要对这些方案的思路和原理都透彻理解之后，在实践中根据各种不同的情况，灵活运用多种方案，来解决自己遇到的数据倾斜问题。

# Spark优化

**编码优化：**

① RDD复用

② RDD持久化

③ 巧用 filter

④ 选择高性能算子

⑤ 设置合并的并行度

⑥ 广播大变量

⑦ Kryo序列化

⑧ 多使用Spark SQL

⑨ 优化数据结构

⑩ 使用高性能库



## 参数优化

① Shuffle调优

② 内存调优

③ 资源分配

④ 动态资源分配

⑤ 调节本地等待时长

⑥ 调节连接等待时长



![](.\图片\spark调优-1-.png)

```shell
./bin/spark-submit \ 
  --master yarn-cluster \ 
  --num-executors 100 \ 
  --executor-memory 10G \ 
  --executor-cores 4 \ 
  --driver-memory 2G \ 
  --conf spark.memory.fraction=0.7
```

## 动态资源分配

动态资源分配(DRA，dynamic resource allocation)

- 默认情况下，Spark采用资源预分配的方式。即为每个Spark应用设定一个最大可用资源总量，该应用在整个生命周期内都会持有这些资源
- Spark提供了一种机制，使它可以根据工作负载动态调整应用程序占用的资源。这意味着，不使用的资源，应用程序会将资源返回给集群，并在稍后需要时再次请求资源。如果多个应用程序共享Spark集群中的资源，该特性尤为有用
- 动态的资源分配是 executor 级
- 默认情况下禁用此功能，并在所有粗粒度集群管理器上可用（**CDH发行版中默认为true**）
- 在Spark On Yarn模式下使用：
  - num-executors指定app使用executors数量
  - executor-memory、executor-cores指定每个executor所使用的内存、cores



**动态申请executor：**

如果有新任务处于等待状态，并且等待时间超过

**Spark.dynamicAllocation.schedulerBacklogTimeout**(默认1s)，则会依次启动executor，每次启动1、2、4、8…个executor（如果有的话）。启动的间隔由
**spark.dynamicAllocation.sustainedSchedulerBacklogTimeout** 控制 (默认与schedulerBacklogTimeout相同)



**动态移除executor**：

executor空闲时间超过 **spark.dynamicAllocation.executorIdleTimeout** 设置的值(默认60s)，该executor会被移除，除非有缓存数据



**相关参数：**

- spark.dynamicAllocation.enabled = true
- Standalone模式：spark.shuffle.service.enabled = true
- Yarn模式：《Running Spark on YARN》-- Configuring the External Shuffle Service

备注：两个参数默认都是false。external shuffle service 的目的是在移除 Executor的时候，能够保留 Executor 输出的 shuffle 文件。

- spark.dynamicAllocation.executorIdleTimeout（默认60s）。Executor闲置了超过此持续时间，将被删除
- spark.dynamicAllocation.cachedExecutorIdleTimeout（默认infinity）。已缓存数据块的 Executor 闲置了超过此持续时间，则该执行器将被删除
- spark.dynamicAllocation.initialExecutors（默认spark.dynamicAllocation.minExecutors）。初始分配 Executor 的个数。如果设置了--num-executors（或spark.executor.instances）并且大于此值，该参数将作为 Executor 初始的个数
- spark.dynamicAllocation.maxExecutors（默认infinity）。Executor 数量的上限
- spark.dynamicAllocation.minExecutors（默认0）。Executor 数量的下限
- spark.dynamicAllocation.schedulerBacklogTimeout（默认1s）。任务等待时间超过了此期限，则将请求新的Executor



## 调节本地时长

- Spark总是倾向于让所有任务都具有最佳的数据本地性。遵循移动计算不移动数据的思想，Spark希望task能够运行在它要计算的数据所在的节点上，这样可以
  避免数据的网络传输

   **PROCESS_LOCAL > NODE_LOCAL > NO_PREF > RACK_LOCAL > ANY**

- 在某些情况下，可能会出现一些空闲的executor没有待处理的数据，那么Spark可能就会牺牲一些数据本地

- 如果对应节点资源用尽，Spark会等待一段时间(默认3s)。如果等待指定时间后仍无法在该节点运行，那么自动降级，尝试将task分配到比较差的本地化级别所
  对应的节点上；如果当前级别仍然不行，那么继续降级

- 调节本地等待时长。如果在等待时间内，目标节点处理完成了一部分 Task，那么等待运行的 Task 将有机会得到执行，获得较好的数据本地性，提高 Spark 作
  业整体性能

- 根据数据本地性不同，等待的时间间隔也不一致，不同数据本地性的等待时间设置参数

  - spark.locality.wait：设置所有级别的数据本地性，默认是3000毫秒
  - spark.locality.wait.process：多长时间等不到PROCESS_LOCAL就降级，默认为${spark.locality.wait}
  - spark.locality.wait.node：多长时间等不到NODE_LOCAL就降级，默认为${spark.locality.wait}
  - spark.locality.wait.rack：多长时间等不到RACK_LOCAL就降级，默认为${spark.locality.wait}

  

  ## **调节连接等待时长**
  在Spark作业运行过程中，Executor优先从自己本地关联的BlockManager中获取某份数据，如果本地BlockManager没有的话，会通过 TransferService 远程连接其他节点上Executor的BlockManager来获取数据；

  在生产环境下，有时会遇到file not found、file lost这类错误。这些错误很有可能是Executor 的 BlockManager 在拉取数据的时候，无法建立连接，然后超过默认的连接等待时长后，宣告数据拉取失败。如果反复尝试都拉取不到数据，可能会导致Spark作业的崩溃。

  这种情况也可能会导致 DAGScheduler 反复提交几次stage，TaskScheduler返回提交几次task，延长了Spark作业的运行时间；此时，可以考虑调节连接的超时时长，设置：
  **spark.core.connection.ack.wait.timeout = 300s (缺省值120s)**

  调节连接等待时长后，通常可以避免部分的文件拉取失败、文件丢失等报错

  
