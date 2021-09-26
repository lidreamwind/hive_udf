# 概述

## 什么事Flink

Apache Flink是一个框架和分布式处理引擎，用于对无界和有节数据流进行有状态计算。Flink被设计在所有常见的集群环境中运行，以内存执行速度和任意规模来执行计算。

 ## 特点

- 批流一体：统一批处理、流处理
- 分布式：Flink程序可以运行在多台机器上
- 高性能：处理性能比较高
- 高可用：Flink支持高可用性（HA）
- 准确：Flink可以保证数据处理的准确性

## 应用场景

**Flink主要应用于流失数据分析场景**

数据无处不在，绝大多数的企业所采取的处理数据的架构都会分为：事务型处理、分析型处理。

### 事务型处理

**OLTP**  联机事务处理过程。

流程审批、数据录入、填报等。

ERP-CRM-OA



### 分析型处理

**OLAP--联机分析系统**

分析报表，分析决策

![image-20210824211022360](.\图片\数仓架构.png)

- 架构复杂。数据在Hbase、消息队列、HDFS间流转，涉及环节太多，运维成本高。
- 时效性低
- 难以应对后续的更新

### 应用场景

- 实时ETL，集成现有的数据通道和SQL灵活的加工能力，对流式数据进行实时清洗、归并和结构化处理。同时，对离线数仓进行有效的补充和优化，并为数据实时传输提供计算通道。
- 实时报表，实时化采集、加工流失数据存储；实时监控和展现业务、客户各类指标，让数据化运营实时化。
- 监控预警，对系统和用户行为进行实时监测和分析，以便即使发现危险行为，如计算机网络入侵、诈骗预警
- 在线系统，实时计算各类指标数据，并利用实时结果及时调整在线系统的相关策略，在各类内容投放、智能推送领域有大量应用，如客户浏览商品的同时推荐相关商品。

## 核心组成及生态发展

![image-20210824212336121](.\图片\Flink架构.png)

- Deploy层
  - 可以启动单个JVM，让Flink以Local模式运行
  - Flink可以以Standalone集群模式运行，同时也支持Flink On Yarn，Flink应用可以提交到Yarn上运行
  - Flink可以运行到GCE（谷歌云）和EC2（亚马逊云服务）
- Core层（runtime）：在Runtime之上提供了两套API，DataStream APi（流处理）和DataSet APi（批处理）
- APIs & Libraries层：核心API之上又扩展了一些高阶的库和API
  - CEP流处理
  - Table API和SQL
  - Flink ML机器学习库
  - Gelly图计算

![image-20210824212959076](.\图片\Flink生态.png)

## 处理模型：流处理与批处理

Flink专注于无限流处理，有限流是无限流处理的一种特殊情况。

**无限流处理**：

- 输入的数据没有尽头，像水流一样源源不断。
- 数据处理从当前或者过去的某一个时间点开始，持续不停地进行

**有限流处理**：

- 从某一个时间点开始处理数据，然后在另一个时间点结束
- 输入的数据可能本身是有限的

**有状态的流处理**：

![image-20210824213817108](.\图片\流处理状态.png)

## 流处理引擎的技术选型

流处理引擎有，flink、spark streaming、storm、trident。

- 流数据处理进行状态管理，选择使用Trident、Spark Streaming或者Flink
- 消息投递需要保证At-least-once（至少一次）或者Exactly-once（仅一次），不能选择Strom
- 对于小型独立项目，有低延迟需要，可以选择使用strom，更简单
- 如果项目已经引入了大框架Spark，实时处理需要可以满足的话，建议直接使用Spark streaming
- 消息投递要满足Exactly-once，数据量大、又高吞吐、低延迟要求，要进行状态管理或窗口统计，建议使用Flink



# 第二节：Flink体系结构及安装部署

## 第一节 重要角色

Flink程序的基本构建是流和转换。

**addSource是自定义数据源。**



经典的Master/Slave架构。

- Job Manager（Master），

  - 协调分布式执行，用来调度task，协调检查点（checkpoint），协调失败时恢复
  - Flink运行时至少存在一个master处理器，如果配置高可用模式则会存在多个master处理器，他们其中有一个是leader，而其他都是standby
  - jobmanager接收的应用包括jar和JobGraph

- TaskManager（slave），也称之为worker

  - 主要职责是从JobManager接收任务，并部署和启动任务，接收上游的数据并处理
  - Task Manager是在JVM中的一个或多个线程中执行任务的工作节点
  - TaskManager在启动的时候会向ResourceManager注册自己的资源信息（slot的数量等）

- ResourceManager

  - 针对不同的环境和资源提供着，如（yarn，Me搜索，Kubernets或独立部署），Flink提供了不同的ResourceManager。
  - 作用，负责管理Flnk的处理资源单元---slot

- Dispatcher

  作用：提供一个REST接口来让我们提交需要执行的应用。

  一旦一个应用提交执行，Dispatcher会启动一个JobManager，并将应用转交给他。

  Dispatcher还会启动一个WebUI来提供有关作业执行信息。

  注意：某些应用的提交执行的方式，有可能用不到Dispatcher。

![image-20210825221100957](.\图片\各角色的关系.png)

## 运行架构

### 程序结构

Source、Sink、Transformation

###  Task和SubTask

Task是一个阶段多个功能仙童的SubTask的结合，类似于Spark中的TaskSet。

SubTask（子任务），是Flink中任务最小执行单元，是一个Java类的实例，这个Java类中有属性和方法，完成计算逻辑。

​	比如，一个执行操作map，分布式的场景下会在多个线程中同时执行，每个线程中执行的都叫做一个SubTask。

### operation chain（操作器链）



### 任务槽和槽共享

任务槽也叫作task-slot，槽共享也叫 slot sharing。

​	  Flink的所有操作都称之为Operator，客户端在提交任务的时候会对Operator进行优化操作，能进行合并的Operator会被合并为一个Operator，合并后的Operator称为Operator chain，实际上就是一个执行链，每个执行链会在TaskManager上一个独立的线程中执行。



每个TaskManager是一个JVM的进程，可以再不同的线程中执行一个或多个子任务。

为了控制一个worker能接受多个少个task。worker通过task slot 来进行控制（一个woker至少有一个task slot）。

**任务槽**

​	每个task slot表示TaskManager拥有资源的一个固定大小的子集。一般来说，我们分配槽的个数都是和CPU的核数相等，比如6核就分配6核。

​	Flink将进程的内存划分到多个slot中。假设一个TaskManager机器有三个slot，那么每个slot占有三分之一的内存。

内存被划分到不同的slot之后可以获得如下好处:

- TaskManager最多能同时并发执行的任务是可以控制的，那就是3个，因为不能超过slot的数量
- slot有独占的内存空间，这样在一个TaskManager中可以运行多个不同的作业，作业之间不受影响

**槽共享**



### 数据传输

为了有效利用网络资源和提高吞吐量，Flink在处理任务间的数据传输过程中，采用了缓冲区机制。



## 第二节 安装

### Standalone模式安装

flink-conf.yaml

```` shell
# master
jobmanager.rpc.address: hdp-1
# task manager的slot数量
taskmanager.numberOfTaskSlots: 2
````

masters

slaves

zoo.cfg

# API

![image-20210901212727834](.\图片\文件)

## DataSoruce

textFile/hdfs : readTextFile

内存中：env.fromCollection(ArrayList<~>)

**自定义数据源**

非并行的数据源：implements SourceFunction

并行的数据源：implements ParallelSourceFunction 或者  extends RichParallelSourceFunction



内置了连接器

​	Kafka

​	ElasticSearch

​	HDFS




## Flink窗口机制

- 滚动窗口

  - 每两个窗口无重复数据

  基于时间驱动

  基于事件驱动

- 滑动窗口
- 会话窗口



# Flink Time

# Time

- 事件时间
- 摄入时间
- 处理时间



## WanterMark

watermark解决数据延迟性问题。

水印时间=事件时间-允许延迟的时间



## State-状态管理与原理剖析

state：用来保存计算结果或缓存数据。

有状态计算和无状态计算。

无状态：独立

根据数据结构的不同，Flink定义了多种State，应用于不同场景。

- ValueState：即类型为T的单值状态。这个状态与对应的key绑定，是最简单的状态了。它可以通过update方法更新状态值，通过value()方法获取状态值。
- ListStage：即key上的状态值为一个列表。可以通过add方法往列表中附加值；也可以通过get方法返回一个Iterable<T>来遍历状态值
- ReducingState：这种状态通过用户传入的reduceFunction，每次调用add方法添加值的时候，会调用reduceFunction，最后合并到一个单一的状态值。
- FoldingState：跟ReducingState有点类似，不过他的状态值 类型可以与add方法中传入的元素类型不同（这种状态会在Flink未来版本中删除）
- MapState，即状态值为一个map。用过通过put或者putall方法添加元素。

**state按照是否有key划分为keyedState和OperatorState**

Keyed State：KeyedStream流上的每一个key都对应一个State。



RichFunction方法。



Operator State代码：

​	1、实现checkpointedFunction，或者ListCheckPointed

​	2、两个方法，initializeState/snapshotState

​			initializeState：方法内，实例化状态

​			snapshotState：将操作的最新数据放到最新的检查点中

​	3、invoke，每来一个算子调用一次，并把所有的数据都放到缓存器中。

## State-广播变量

看具体代码。



# 第三节-Flink并行度

- env.setParallelism(4) ---使用电脑的核数来设置并行度
- 对算子设置并行度。
- client客户端级别
- 系统级别-flink-conf.yaml

![image-20210922214604273](.\图片\并行度设置)

## Flink连接器



## CEP

**CEP即Complex Event Processing-复杂事件处理，Flink CEP是在Flink中实现的复杂时间处理（CEP）库。处理事件的规则，被叫做“模式”（pattern），Flink CEP提供了Pattern API，用于对输入流数据进行复杂事件规则定义，用来提取符合规则的时间序列。**

​	Pattern API 大致分为三种：个体模式、组合模式，模式组。



在网站的访问日志中寻找那些使用脚本或者工具爆破登录的用户；

在大量的订单交易中发现那些虚假交易（超时未支付）或发现活跃用户；

快递运输中发现那些滞留很久没有签收的包裹。



### CEP特征

目标：从有序的简单事件流中发现一些高阶特征

输入：一个或多个简单事件构成的事件流

处理：识别简单事件之间的内在联系，多个符合 一定规则的简单事件构成复杂事件

输出：满足规则的复杂事件

### 功能

![image-20210925110857274](.\图片\cep.jpeg)

### 主要组件

Flink CEP提供了专门的Flink CEP library，包含Event Stream、Pattern定义，Pattern检测和生成Alert。



![image-20210925111032703](.\图片\cep-component.jpeg)

### Pattern API

用于对输入流数据进行复杂事件规则定义，用来提取符合规则的事件序列。

####  个体模式

![image-20210925111403784](.\图片\cep-pattern-个体.jpeg)





#### 组合模式



#### 模式组

#### 模式条件-检测



每个模式都需要指定触发条件，作为模式是否接受事件进入的判断依据。where or 

![image-20210925111547774](.\图片\cep-pattern-个体-条件.jpeg)

![image-20210925111851821](.\图片\cep-模式匹配.jpeg)

![image-20210925112116431](.\图片\cep-注意事项.jpeg)

**检测**

![image-20210925112239489](.\图片\cep-模式检测.jpeg)

#### 事件提取

![image-20210925112329842](.\图片\cep-事件提取.jpeg)

process 

select

![image-20210925112406965](.\图片\cep-事件提取-超时.jpeg)

#### CEP-NFA 非确定有限自动机

Flink CEP在运行时会将用户的逻辑转化为一个这样的NFA Graph（nfa对象）。

![image-20210925112602771](.\图片\cep-nfa.jpeg)

#### CEP案例

![image-20210925114605215](.\图片\cep-案例.jpeg)



# Flink Table



![image-20210926214021494](.\图片\flink-table-kafka-register.jpeg)

![image-20210926214358746](.\图片\flink-table-kafka-register-idea.jpeg)

## 输出表

![image-20210926215414454](.\图片\flink-table-path-output.jpeg)







