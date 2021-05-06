# Kafka介绍

​		kafka是一个分布式、分区的、多副本的、多生产者、多订阅者，基于zookeeper协调的分布式日志系统，常见用于web/nginx日志、访问日志，消息服务等。

​		主要应用场景是：日志收集系统和消息系统。

![image-20210425215922454](.\图片\kafka-架构.png)

​		有两种主要的消息传递模式：点对点传递模式、发布-订阅模式。

## Kafka优势

​		1、高吞吐量：单机每秒几十万上百万的消息量，即使存储了许多TB的消息，他也保证了稳定的性能

​		2、高性能，单节点支持上千个客户端，保证零停机和零数据丢失

​		3、持久化数据存储：将消息持久化到磁盘。通过将数据持久化到硬盘以及replication'防止数据丢失。

​					1、零拷贝

​					2、顺序读，顺序写

​					3、利用Linux的页缓存

​		4、分布式系统，易于向外扩展。所有的Producer、Broker和Consumer都会有多个，均为分布式的。无需停机即可扩展机器。多个Producer、Consumer可能是不同的应用。

​		5、可靠性-kafka是分布式，分区，复制和容错的。

​		6、客户端状态维护：消息被处理的状态是在Consumer端维护，而不是有server端维护。当失败时能自动平衡。

​		7、支持online和offline的场景。

​		8、支持多种客户端语言。kafka支持java、.Net、PHP、Python等多种语言。

## 应用场景

**日志收集**：收集服务的log

**消息系统**：解耦生产者和消费者、缓存消息等

**用户活动跟踪**：kafka经常被用来记录web用户或者app用户的各种活动，网页浏览、搜索、点击。

**运行指标**:kafka也经常用来记录运营监控数据。生产各种操作的集中反馈，比如预警和报告。

**流式处理**：spark streaming



## 基本架构

**消息和批次**

​		消息由字节数组组成。

​		同一个主题和分区，可以通过批次传递。

**模式**

​		kafka常用Apache Avro。序列化和反序列化。

**主题和分区**

​		主题由分区组成。

​		分区内数据有序。

**生产者和消费者**

​		消费者组，避免重复消费。

**broker和集群**

​		一个独立的kafka服务器称为broker。broker接收来自生产者的消息，为消息设置偏移量，并提交到磁盘保存。

​		**单个broker可以轻松处理数千个分区以及每秒百万级的消息量**

![image-20210425224550873](.\图片\broker关系.png)

​		一个分区可以分配给多个broker，此时会发生分区复制。

​		分区的复制提供了消息冗余，高可用。副本分区不负责处理消息的读写。

## 核心概念

**producer**

​		该角色将消息发布到kafka的topic中。broker接收到生产者发送的消息后，broker将该消息追加到当前用于追加数据的segment文件中。

​		一般情况下，一个消息会被发布到一个特定的主题上。

​		1、默认情况下通过轮询把消息均衡的分布到主题的所有分区上。

​		2、在某些情况下，生产者会把消息直接写到指定的分区。通常通过消息键和分区器来实现的。

​		3、生产者也可以使用自定义的分区器，根据不同的业务规则将消息映射到分区。

**偏移量**

​		1、每个分区都有偏移量，包括生产者偏移量和消费者偏移量。生产者偏移量记录了生产者的写入偏移量。

**副本**

​		AR=ISR+OSR

**高水位线HW**

​		消费者组只能拉取到高水位线之前的数据。

**LEO**

​		当前分区日志中，下一条数据写入的偏移量。

## Kafka单节点搭建

​		配置

​			1、KAFKA_HOME=

​		

​        修改配置文件servers.properties

​			1、**zookeeper.connect=linux121:2181,linux122:2181,linux123:2181/myKafka**

​			2、log.dirs=/va/log/kafka/kafka-logs

## 生产与消费示例

```shell
# 主题查看
	# 查看主题列表
kafka-topics.sh --zookeeper linux121:2181,linux122:2181/myKafka --list	
	# 创建主题
kafka-topics.sh --zookeeper linux121:2181,linux122:2181/myKafka --create --topic topic_1 --partitions 1 --replication-factor 1
	# 查看主题
kafka-topics.sh --zookeeper linux121:2181,linux122:2181/myKafka  --topic topic_1  --describe
	# 删除主题
kafka-topics.sh --zookeeper linux121:2181,linux122:2181/myKafka  --topic topic_1  --delete

# 消费者
	# 消费消息
kafka-console-consumer.sh --bootstrap-server linux123:9092 --topic test --from-beginning
kafka-console-consumer.sh --bootstrap-server linux123:9092 --topic test

# 生产者
	# 生产消息
kafka-console-producer.sh --broker-list linux123:9092 --topic test

```

## 开发施展_消息发送

### 参数

​	生产者对象主要有KafkaProducer、ProducerRecord

​	KafkaProducer用于发送消息的类，ProducerRecord用于封装Kafka的消息。

​	**KafkaProducer**创建需要指定的参数和含义：

​	

| 参数                                  | 说明                                                         |
| ------------------------------------- | ------------------------------------------------------------ |
| bootstrap.servers                     | 配置生产者如何与broker建立连接。该参数设置的是初始化参数。如果生产者需要连接的是Kafka集群，则这里配置集群中几个broker的地址，而不是全部，当生产者连接上此处指定的broker之后，在通过该连接发现集群中的其他节点。 |
| key.serializer                        | 要发送信息的key数据的序列化类。设置的时候可以写类名，也可以使用该类的Class对象 |
| value.serializer                      | 要发送消息的alue数据的序列化类。设置的时候可以写类名，也可以使用<br/>该类的Class对象。 |
| acks                                  | 默认值：all。<br />acks=0：<br/>生产者不等待broker对消息的确认，只要将消息放到缓冲区，就认为消息<br/>已经发送完成。<br/>该情形不能保证broker是否真的收到了消息，retries配置也不会生效。发<br/>送的消息的返回的消息偏移量永远是-1。<br/>acks=1<br/>表示消息只需要写到主分区即可，然后就响应客户端，而不等待副本分区<br/>的确认。<br/>在该情形下，如果主分区收到消息确认之后就宕机了，而副本分区还没来<br/>得及同步该消息，则该消息丢失。<br/>acks=all<br/>首领分区会等待所有的ISR副本分区确认记录。<br/>该处理保证了只要有一个ISR副本分区存活，消息就不会丢失。<br/>这是Kafka最强的可靠性保证，等效于 acks=-1 |
| retries                               | retries重试次数<br/>当消息发送出现错误的时候，系统会重发消息。<br/>跟客户端收到错误时重发一样。<br/>如果设置了重试，还想保证消息的有序性，需要设置<br/>MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION=1，max.in.flight.requests.per.connection=1<br/>否则在重试此失败消息的时候，其他的消息可能发送成功了 |
| request.timeout.ms                    | 客户端等待请求响应的最大时长。如果服务端响应超时，则会重发请求，除非达到重试次数。该设置应该比 replica.lag.time.max.ms (a broker configuration)要大，以免在服务器延迟时间内重发消息。int类型值，默<br/>认：30000，可选值：[0,...] |
| batch.size                            | 消息到达多大一个批次进行发送<br/>当多个消息发送到同一个分区的时候，生产者尝试将多个记录作为一个批来<br/>处理。批处理提高了客户端和服务器的处理效率。<br/>该配置项以字节为单位控制默认批的大小。<br/>所有的批小于等于该值。<br/>发送给broker的请求将包含多个批次，每个分区一个，并包含可发送的数据。<br/>如果该值设置的比较小，会限制吞吐量（设置为0会完全禁用批处理）。如果设置的很大，又有一点浪费内存，因为Kafka会永远分配这么大的内存来参与到消息的批整合中。 |
| linger.ms                             | 消息最多等待多长时间发送<br/>生产者在发送请求传输间隔会对需要发送的消息进行累积，然后作为一个批次发送。一般情况是消息的发送的速度比消息累积的速度慢。有时客户端需要减少请求的次数，即使是在发送负载不大的情况下。该配置设置了一个延迟，生产者不会立即将消息发送到broker，而是等待这么一段时间以累积消息，然后将这段时间之内的消息作为一个批次发送。<br/>该设置是批处理的另一个上限：一旦批消息达到了 batch.size 指定的值，消息批会立即发送，如<br/>果积累的消息字节数达不到 batch.size 的值，可以设置该毫秒值，等待这么长时间之后，也会发送消息批。该属性默认值是0（没有延迟）。如果设置 linger.ms=5 ，则在一个请求发送之前先等待5ms。long型值，默认：0，可选值：[0,...] |
| compression.tppe                      | 生产者发送的所有数据的压缩方式。默认是none，也就是不压缩。<br/>支持的值：none、gzip、snappy和lz4。<br/>压缩是对于整个批来讲的，所以批处理的效率也会影响到压缩的比例。 |
| key.serializer.encoding               |                                                              |
| value.serializer.encoding             |                                                              |
| retry.backoff.ms                      | 在向一个指定的主题分区重发消息的时候，重试之间的等待时间。<br/>比如3次重试，每次重试之后等待该时间长度，再接着重试。在一些失败的<br/>场景，避免了密集循环的重新发送请求。<br/>long型值，默认100。可选值：[0,...] |
| interceptor.classes                   | 在生产者接收到该消息，向Kafka集群传输之前，由序列化器处理之前，可以通过拦截器对消息进行处理。<br/>要求拦截器类必须实现org.apache.kafka.clients.producer.ProducerInterceptor 接口。<br/>默认没有拦截器。<br/>Map<String, Object> configs中通过List集合配置多个拦截器类名。 |
| client.id                             | 生产者发送请求的时候传递给broker的id字符串。用于在broker的请求日志中追踪什么应用发送了什么消息。<br/>一般该id是跟业务有关的字符串 |
| send.buffer.bytes                     | TCP发送数据的时候使用的缓冲区（SO_SNDBUF）大小。如果设置为0，则使用操作系统默认的。 |
| buffer.memory                         | 生产者可以用来缓存等待发送到服务器的记录的总内存字节。如果记录的发送速度超过了将记录发送到服务器的速度，则生产者将阻塞 max.block.ms的时间，此后它将引发异常。此设置应大致对应于生产者将使用的总内存，但并非生产者使用的所有内存都用于缓冲。一些额外的内存将用于压缩（如果启用了压缩）以及维护运行中的请求。long型数据。默认值：33554432，可选值：[0,...] |
| connections.max.idle.ms               | 当连接空闲时间达到这个值，就关闭连接。long型数据，默认：540000 |
| max.block.ms                          | 控制 KafkaProducer.send() 和 KafkaProducer.partitionsFor() 阻塞的时长。当缓存满了或元数据不可用的时候，这些方法阻塞。在用户提供的序列化器和分区器的阻塞时间不计入。long型值，默认：60000，可选值：[0,...] |
| max.request.size                      | 单个请求的最大字节数。该设置会限制单个请求中消息批的消息个数，以免<br/>单个请求发送太多的数据。服务器有自己的限制批大小的设置，与该配置可<br/>能不一样。int类型值，默认1048576，可选值：[0,...] |
| partitioner.class                     | 实现了接口 org.apache.kafka.clients.producer.Partitioner 的分区<br/>器实现类。默认值为：org.apache.kafka.<br/>clients.producer.internals.DefaultPartitioner |
| receive.buffer.bytes                  | TCP接收缓存（SO_RCVBUF），如果设置为-1，则使用操作系统默认的值。<br/>int类型值，默认32768，可选值：[-1,...] |
| security.protocol                     | 跟broker通信的协议：PLAINTEXT, SSL, SASL_PLAINTEXT, SASL_SSL.<br/>string类型值，默认：PLAINTEXT |
| max.in.flight.requests.per.connection | 单个连接上未确认请求的最大数量。达到这个数量，客户端阻塞。如果该值<br/>大于1，且存在失败的请求，在重试的时候消息顺序不能保证。<br/>int类型值，默认5。可选值：[1,...] |
| reconnect.backoff.max.ms              | 对于每个连续的连接失败，每台主机的退避将成倍增加，直至达到此最大<br/>值。在计算退避增量之后，添加20％的随机抖动以避免连接风暴。<br/>long型值，默认1000，可选值：[0,...] |
| reconnect.backoff.ms                  | 尝试重连指定主机的基础等待时间。避免了到该主机的密集重连。该退避时<br/>间应用于该客户端到broker的所有连接。<br/>long型值，默认50。可选值：[0,...] |

其他参数可以从 org.apache.kafka.clients.producer.ProducerConfig 中找到。我们后面的内容会介绍到。
消费者生产消息后，需要broker端的确认，可以同步确认，也可以异步确认。

![image-20210426204948485](.\图片\Kafka写消息架构.png)

消费者生产消息后，需要broker端的确认，可以同步确认，也可以异步确认。

同步确认效率低，异步确认效率高，但是需要设置回调对象。

### 序列化器

​		生产中使用Avro会更多一些。

```java
// 自定义序列化器
package com.lagou.kafka.demo.entity;
public class User {
  private Integer userId;
  private String username;
  public Integer getUserId() {
    return userId;
 }
  public void setUserId(Integer userId) {
    this.userId = userId;
 }
  public String getUsername() {
    return username;
 }
  public void setUsername(String username) {
    this.username = username;
 }
}

package com.lagou.kafka.demo.serializer;
import com.lagou.kafka.demo.entity.User;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
public class UserSerializer implements Serializer<User> {
  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    // do nothing
 }
  @Override
  public byte[] serialize(String topic, User data) {
    try {
      // 如果数据是null，则返回null
      if (data == null) return null;
      Integer userId = data.getUserId();
      String username = data.getUsername();
      int length = 0;
      byte[] bytes = null;
      if (null != username) {
        bytes = username.getBytes("utf-8");
        length = bytes.length;
     }
      ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + length);
      buffer.putInt(userId);
      buffer.putInt(length);
      buffer.put(bytes);
      return buffer.array();
   } catch (UnsupportedEncodingException e) {
      throw new SerializationException("序列化数据异常");
   }
 }
  @Override
  public void close() {
    // do nothing
 }
}
```



### 拦截器

​		过滤数据。

​		实现接口ProducerInterceptor，**需要确保线程安全**

Producer拦截器（interceptor）和Consumer端Interceptor是在Kafka 0.10版本被引入的，主要用于实现Client端的定制化控制逻辑。

对于Producer而言，Interceptor使得用户在消息发送前以及Producer回调逻辑前有机会对消息做一些定制化需求，比如修改消息等。同时，Producer允许用户指定多个Interceptor按序作用于同一条消息从而形成一个拦截链(interceptor chain)。

Intercetpor的实现接口是
org.apache.kafka.clients.producer.ProducerInterceptor，其定义的方法包括：

​	1、onSend(ProducerRecord)：该方法封装进KafkaProducer.send方法中，即运行在用户主线程中。Producer确保在消息被序列化以计算分区前调用该方法。用户可以在该方法中对消息做任何操作，但最好保证不要修改消息所属的topic和分区，否则会影响目标分区的计算。

​	2、onAcknowledgement(RecordMetadata, Exception)：该方法会在消息被应答之前或消息发送失败时调用，并且通常都是在Producer回调逻辑触发之前。

​	3、onAcknowledgement运行在Producer的IO线程中，因此不要在该方法中放入很重的逻辑，否则会拖慢Producer的消息发送效率。

​	4、close：关闭Interceptor，主要用于执行一些资源清理工作。

如前所述，Interceptor可能被运行在多个线程中，因此在具体实现时用户需要自行确保线程安全。另外倘若指定了多个Interceptor，则Producer将按照指定顺序调用它们，并仅仅是捕获每个Interceptor可能抛出的异常记录到错误日志中而非在向上传递。这在使用过程中要特别留意。





### 分区器

​		对数据做分区。

​		默认分区（DefaultPartioner)分区计算：

​			1、如果record提供了分区号，则使用record提供的分区号

​			2、如果record没有提供分区号，则使用key的序列化后值的hash值对分区数量取模

​			3、如果record没有提供分区号，也没有提供key，则使用轮询的方式分配分区号

​					1）首先在可用的分区中分配分区号

​					2）如果没有可用的分区，则在该主题所有分区中分配分区号

​		实现partition接口

```shell
import org.apache.kafka.common.Cluster;

import java.util.Map;

public class MyPartitioner implements Partitioner {
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        return 0;
    }
    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
```

![image-20210427212610454](.\图片\Producer使用分区器.png)

### 原理剖析

![image-20210427214113893](.\图片\Kafka-Producer原理.png)



## 开发实战_消息消费

​		消费者从订阅的主题消费消息，消费消息的偏移量保存在kafka的名字是__consumer_offsets的主题中。



​		消费者还可以将自己的偏移量保存到zookeeper中，需要设置offset.storage=zookeeper

​		推荐使用kafka存储消费者的偏移量。因为zookeeper不适合做高并发。



​		消费者组共享group.id

​		消费组均衡的给消费者分配分区，每个分区只由消费组中的一个消费者消费。

### 参数

| 参数                          | 说明                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| bootstrap.servers             | 向Kafka集群建立初始连接用到的host/port列表。<br/>客户端会使用这里列出的所有服务器进行集群其他服务器的发现，而不<br/>管是否指定了哪个服务器用作引导。<br/>这个列表仅影响用来发现集群所有服务器的初始主机。<br/>字符串形式：host1:port1,host2:port2,...<br/>由于这组服务器仅用于建立初始链接，然后发现集群中的所有服务器，<br/>因此没有必要将集群中的所有地址写在这里。<br/>一般最好两台，以防其中一台宕掉。 |
| key.deserializer              | key的反序列化类，该类需要实现<br/>org.apache.kafka.common.serialization.Deserializer 接口。 |
| value.deserializer            | 实现了 org.apache.kafka.common.serialization.Deserializer 接口的反序列化器，<br/>用于对消息的value进行反序列化 |
| group.id                      | 用于唯一标志当前消费者所属的消费组的字符串。<br/>如果消费者使用组管理功能如subscribe(topic)或使用基于Kafka的偏移量管理策略，该项必须设置。 |
| auto.offset.reset             | 如果kafka没有初始偏移量或者当前偏移量在服务器中不存在，则：<br/>earliest：自动重置偏移量到最早的偏移量情形下，<br/>latest：自动重置偏移量为最新的偏移量<br/>none：如果消费者组（previous）偏移量不存在，则向消费者抛异常<br/>anything：向消费者抛异常<br/> |
| client.id                     | 当从服务器消费消息的时候向服务器发送的id字符串。在ip/port基础上<br/>提供应用的逻辑名称，记录在服务端的请求日志中，用于追踪请求的源 |
| enable.auto.commit            | 如果设置为true，消费者会自动周期性地向服务器提交偏移量。     |
| auto.commit.interval.ms       | 自动提交时间间隔                                             |
| fetch.min.bytes               | 服务器对每个拉取消息的请求返回的数据量最小值。<br/>如果数据量达不到这个值，请求等待，以让更多的数据累积，<br/>达到这个值之后响应请求。<br/>默认设置是1个字节，表示只要有一个字节的数据，<br/>就立即响应请求，或者在没有数据的时候请求超时。<br/>将该值设置为大一点儿的数字，会让服务器等待稍微<br/>长一点儿的时间以累积数据。<br/>如此则可以提高服务器的吞吐量，代价是额外的延迟时间。 |
| fetch.max.wait.ms             | 如果服务器端的数据量达不到 fetch.min.bytes 的话，<br/>服务器端不能立即响应请求。<br/>该时间用于配置服务器端阻塞请求的最大时长。 |
| fetch.max.bytes               | 服务器给单个拉取请求返回的最大数据量。<br/>消费者批量拉取消息，如果第一个非空消息批次的值比该值大，<br/>消息批也会返回，以让消费者可以接着进行。<br/>即该配置并不是绝对的最大值。<br/>broker可以接收的消息批最大值通过<br/>message.max.bytes (broker配置)<br/>或 max.message.bytes (主题配置)来指定。<br/>需要注意的是，消费者一般会并发拉取请求。 |
| connections.max.idle.ms       | 如果设置为true，则消费者的偏移量会周期性地在后台提交。       |
| check.crcs                    | 自动计算被消费的消息的CRC32校验值。<br/>可以确保在传输过程中或磁盘存储过程中消息没有被破坏。<br/>它会增加额外的负载，在追求极致性能的场合禁用。 |
| exclude.internal.topics       | 是否内部主题应该暴露给消费者。如果该条目设置为true，<br/>则只能先订阅再拉取 |
| isolation.level               | 控制如何读取事务消息。<br/>如果设置了 read_committed ，消费者的poll()方法只会<br/>返回已经提交的事务消息。<br/>如果设置了 read_uncommitted (默认值)，<br/>消费者的poll方法返回所有的消息，即使是已经取消的事务消息。<br/>非事务消息以上两种情况都返回。<br/>消息总是以偏移量的顺序返回。<br/>read_committed 只能返回到达LSO的消息。<br/>在LSO之后出现的消息只能等待相关的事务提交之后才能看到。<br/>结果， read_committed 模式，如果有为提交的事务，<br/>消费者不能读取到直到HW的消息。<br/>read_committed 的seekToEnd方法返回LSO。 |
| heartbeat.interval.ms         | 当使用消费组的时候，该条目指定消费者向消费者协调器<br/>发送心跳的时间间隔。<br/>心跳是为了确保消费者会话的活跃状态，<br/>同时在消费者加入或离开消费组的时候方便进行再平衡。<br/>该条目的值必须小于 session.timeout.ms ，也不应该高于<br/>session.timeout.ms 的1/3。<br/>可以将其调整得更小，以控制正常重新平衡的预期时间。 |
| session.timeout.ms            | 当使用Kafka的消费组的时候，消费者周期性地向broker发送心跳表明自己的存在。<br/>如果经过该超时时间还没有收到消费者的心跳，<br/>则broker将消费者从消费组移除，并启动再平衡。<br/>该值必须在broker配置 group.min.session.timeout.ms 和<br/>group.max.session.timeout.ms 之间。 |
| max.poll.records              | 一次调用poll()方法返回的记录最大数量。                       |
| max.poll.interval.ms          | 使用消费组的时候调用poll()方法的时间间隔。<br/>该条目指定了消费者调用poll()方法的最大时间间隔。<br/>如果在此时间内消费者没有调用poll()方法，<br/>则broker认为消费者失败，触发再平衡，<br/>将分区分配给消费组中其他消费者。 |
| max.partition.fetch.bytes     | 对每个分区，服务器返回的最大数量。消费者按批次拉取数据。<br/>如果非空分区的第一个记录大于这个值，批处理依然可以返回，<br/>以保证消费者可以进行下去。<br/>broker接收批的大小由 message.max.bytes （broker参数）或<br/>max.message.bytes （主题参数）指定。<br/>fetch.max.bytes 用于限制消费者单次请求的数据量。 |
| send.buffer.bytes             | 用于TCP发送数据时使用的缓冲大小（SO_SNDBUF），<br/>-1表示使用OS默认的缓冲区大小。**偏移量确认的时候** |
| retry.backoff.ms              | 在发生失败的时候如果需要重试，则该配置表示客户端<br/>等待多长时间再发起重试。<br/>该时间的存在避免了密集循环。 |
| request.timeout.ms            | 客户端等待服务端响应的最大时间。如果该时间超时，<br/>则客户端要么重新发起请求，要么如果重试耗尽，请求失败。 |
| reconnect.backoff.ms          | 重新连接主机的等待时间。避免了重连的密集循环。<br/>该等待时间应用于该客户端到broker的所有连接。 |
| reconnect.backoff.max.ms      | 重新连接到反复连接失败的broker时要等待的最长时间（以毫秒为单位）。<br/>如果提供此选项，则对于每个连续的连接失败，<br/>每台主机的退避将成倍增加，直至达到此最大值。<br/>在计算退避增量之后，添加20％的随机抖动以避免连接风暴。 |
| receive.buffer.bytes          | TCP连接接收数据的缓存（SO_RCVBUF）。<br/>-1表示使用操作系统的默认值。 |
| partition.assignment.strategy | 当使用消费组的时候，分区分配策略的类名。                     |
| metrics.sample.window.ms      | 计算指标样本的时间窗口                                       |
| metrics.recording.level       | 指标的最高记录级别。                                         |
| metrics.num.samples           | 用于计算指标而维护的样本数量                                 |
| interceptor.classes           | 拦截器类的列表。默认没有拦截器<br/>拦截器是消费者的拦截器，该拦截器需要实现<br/>org.apache.kafka.clients.consumer.ConsumerInterceptor 接口。<br/>拦截器可用于对消费者接收到的消息进行拦截处理。 |



### 消费者组



### 心跳机制



### 位移提交

```txt
自动提交位移的顺序
    1、配置 enable.auto.commit = true
    2、Kafka会保证在开始调用poll方法时，提交上次poll返回的所有消息
    3、因此自动提交不会出现消息丢失，但会 重复消费
重复消费举例
    1、 Consumer 每 5s 提交 offset
    2、假设提交 offset 后的 3s 发生了 Rebalance
    3、Rebalance 之后的所有 Consumer 从上一次提交的 offset 处继续消费
因此 Rebalance 发生前 3s 的消息会被重复消费
```

```java
//  同步提交
while (true) {
  ConsumerRecords<String, String> records =consumer.poll(Duration.ofSeconds(1));
  process(records); // 处理消息
  try {
    consumer.commitSync();
 } catch (CommitFailedException e) {
    handle(e); // 处理提交失败异常
 }
}

// 异步提交
while (true) {
  ConsumerRecords<String, String> records = consumer.poll(3_000);
  process(records); // 处理消息
  consumer.commitAsync((offsets, exception) -> {
    if (exception != null) {
      handle(exception);
   }
 });
}

// 工作中的方式
try {
  while(true) {
  ConsumerRecords<String, String> records =
consumer.poll(Duration.ofSeconds(1));
  process(records); // 处理消息
  commitAysnc(); // 使用异步提交规避阻塞
 }
} catch(Exception e) {
  handle(e); // 处理异常
} finally {
  try {
    consumer.commitSync(); // 最后一次提交使用同步阻塞式提交
 } finally {
    consumer.close();
 }
}

```

commitSync在处理完所有消息之后

手动同步提交可以控制offset提交的时机和频率

手动同步：

​	1、调用CommitSync时，Consumer处于阻塞状态，直到Broker返回结果

​	2、会影响TPS

​	3、可以选择拉长提交间隔，但会有以下问题

​			1）会导致Consumer的提交频率下降

​			2）Consumer重启后，会有更多的消息被消费，，有可能数据重复

**提交函数**

1、 public void assign(Collection<TopicPartition> partitions)

2、 public Set<TopicPartition> assignment()

3、 public Map<String, List<PartitionInfo>> listTopics()

4、 public List<PartitionInfo> partitionsFor(String topic)

5、 public Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions)

6、public void seekToEnd(Collection<TopicPartition> partitions)

7、 public void seek(TopicPartition partition, long offset)

8、 public long position(TopicPartition partition)

9、 public void seekToBeginning(Collection<TopicPartition> partitions)

```java
package com.lagou.kafka.demo.consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.util.*;
/**
* # 生成消息文件
* [root@node1 ~]# for i in `seq 60`; do echo "hello lagou $i" >> nm.txt;
done
* # 创建主题，三个分区，每个分区一个副本
* [root@node1 ~]# kafka-topics.sh --zookeeper node1:2181/myKafka --create
--topic tp_demo_01 --partitions 3 --replication-factor 1
* # 将消息生产到主题中
* [root@node1 ~]# kafka-console-producer.sh --broker-list node1:9092 --
topic tp_demo_01 < nm.txt
*
* 消费者位移管理
*/
public class MyConsumerMgr1 {
  public static void main(String[] args) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
"node1:9092");
      configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
StringDeserializer.class);
    configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
StringDeserializer.class);
    KafkaConsumer<String, String> consumer = new KafkaConsumer<String,
String>(configs);
    /**
    * 给当前消费者手动分配一系列主题分区。
    * 手动分配分区不支持增量分配，如果先前有分配分区，则该操作会覆盖之前的分配。
    * 如果给出的主题分区是空的，则等价于调用unsubscribe方法。
    * 手动分配主题分区的方法不使用消费组管理功能。当消费组成员变了，或者集群或主题
的元数据改变了，不会触发分区分配的再平衡。
    *
    * 手动分区分配assign(Collection)不能和自动分区分配
subscribe(Collection, ConsumerRebalanceListener)一起使用。
    * 如果启用了自动提交偏移量，则在新的分区分配替换旧的分区分配之前，会对旧的分区
分配中的消费偏移量进行异步提交。
    *
    */
//    consumer.assign(Arrays.asList(new TopicPartition("tp_demo_01",
0)));
//
//    Set<TopicPartition> assignment = consumer.assignment();
//    for (TopicPartition topicPartition : assignment) {
//      System.out.println(topicPartition);
//    }
    // 获取对用户授权的所有主题分区元数据。该方法会对服务器发起远程调用。
//    Map<String, List<PartitionInfo>> stringListMap =
consumer.listTopics();
//
//    stringListMap.forEach((k, v) -> {
//      System.out.println("主题：" + k);
//      v.forEach(info -> {
//        System.out.println(info);
//      });
//    });
//    Set<String> strings = consumer.listTopics().keySet();
//
//    strings.forEach(topicName -> {
//      System.out.println(topicName);
//    });
//    List<PartitionInfo> partitionInfos =
consumer.partitionsFor("tp_demo_01");
//    for (PartitionInfo partitionInfo : partitionInfos) {
//      Node leader = partitionInfo.leader();
//      System.out.println(leader);
//      System.out.println(partitionInfo);
//      // 当前分区在线副本
//      Node[] nodes = partitionInfo.inSyncReplicas();
//      // 当前分区下线副本
//      Node[] nodes1 = partitionInfo.offlineReplicas();
//    }
      // 手动分配主题分区给当前消费者
    consumer.assign(Arrays.asList(
        new TopicPartition("tp_demo_01", 0),
        new TopicPartition("tp_demo_01", 1),
        new TopicPartition("tp_demo_01", 2)
   ));
    // 列出当前主题分配的所有主题分区
//    Set<TopicPartition> assignment = consumer.assignment();
//    assignment.forEach(k -> {
//      System.out.println(k);
//    });
    // 对于给定的主题分区，列出它们第一个消息的偏移量。
    // 注意，如果指定的分区不存在，该方法可能会永远阻塞。
    // 该方法不改变分区的当前消费者偏移量。
//    Map<TopicPartition, Long> topicPartitionLongMap =
consumer.beginningOffsets(consumer.assignment());
//
//    topicPartitionLongMap.forEach((k, v) -> {
//      System.out.println("主题：" + k.topic() + "\t分区：" +
k.partition() + "偏移量\t" + v);
//    });
    // 将偏移量移动到每个给定分区的最后一个。
    // 该方法延迟执行，只有当调用过poll方法或position方法之后才可以使用。
    // 如果没有指定分区，则将当前消费者分配的所有分区的消费者偏移量移动到最后。
    // 如果设置了隔离级别为：isolation.level=read_committed，则会将分区的消费偏移量移动到
    // 最后一个稳定的偏移量，即下一个要消费的消息现在还是未提交状态的事务消息。
//    consumer.seekToEnd(consumer.assignment());
    // 将给定主题分区的消费偏移量移动到指定的偏移量，即当前消费者下一条要消费的消息偏移量。
    // 若该方法多次调用，则最后一次的覆盖前面的。
    // 如果在消费中间随意使用，可能会丢失数据。
//    consumer.seek(new TopicPartition("tp_demo_01", 1), 10);
//
//    // 检查指定主题分区的消费偏移量
//    long position = consumer.position(new
TopicPartition("tp_demo_01", 1));
//    System.out.println(position);
    consumer.seekToEnd(Arrays.asList(new TopicPartition("tp_demo_01",
1)));
    // 检查指定主题分区的消费偏移量
    long position = consumer.position(new TopicPartition("tp_demo_01",
1));
    System.out.println(position);
    // 关闭生产者
    consumer.close();
 }
}
```

### 再均衡

重平衡其实就是一个协议，它规定了如何让消费者组下的所有消费者来分配topic中的每一个分区。

​	比如一个topic有100个分区，一个消费者组内有20个消费者，在协调者的控制下让组内每一个消费者分
配到5个分区，这个分配的过程就是重平衡。

重平衡的触发条件主要有三个：

1. 消费者组内成员发生变更，这个变更包括了增加和减少消费者，比如消费者宕机退出消费组。
2. 主题的分区数发生变更，kafka目前只支持增加分区，当增加的时候就会触发重平衡
3. 订阅的主题发生变化，当消费者组使用正则表达式订阅主题，而恰好又新建了对应的主题，就会触发重平衡

**避免重新平衡**

​		session.timeout.ms ：规定超时时间，没有接收到心跳则认为机器挂掉。**建议6s**

​		heartbeat.interval.ms ： 控制发送心跳的频率，频率越高越不容易被误判，但也会消耗更多资源。**建议2s**

​		max.poll.interval.ms ：两次拉取数据的时间间隔超过这个参数设置的值，那么消费者就会被踢出消费者组。默认是5分钟。**推荐为消费者处理消息最长耗时累加1分钟**



### 消费者拦截器

消费端定义消息拦截器，需要实现
org.apache.kafka.clients.consumer.ConsumerInterceptor<K, V> 接口。
1. 一个可插拔接口，允许拦截甚至更改消费者接收到的消息。首要的用例在于将第三方组件引入消费者应用程序，用于定制的监控、日志处理等。

2. 该接口的实现类通过configre方法获取消费者配置的属性，如果消费者配置中没有指定clientID，还可以获取KafkaConsumer生成的clientId。获取的这个配置是跟其他拦截器共享的，需要保证不会在各个拦截器之间产生冲突。

3. ConsumerInterceptor方法抛出的异常会被捕获、记录，但是不会向下传播。如果用户配置了错误的key或value类型参数，消费者不会抛出异常，而仅仅是记录下来。

4. ConsumerInterceptor回调发生在org.apache.kafka.clients.consumer.KafkaConsumer

  ​	#poll(long)方法同一个线程。

## 服务器参数

​		需要配置servers.properties

```properties
# zk的连接方式
zookeeper.connect=linux121:2181,linux122:2181,linux123:2181/myKafka

# listners连接方式
	# 监听器名称和安全协议的映射配置。
listener.security.protocol.map=INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
	# 配置连接到Kafka集群的方式,同一个机器有两个ip地址
listners=EXTERNAL://192.168.233.166:9002,INTERNAL://192.168.233.165:9003 
	# 建议使用的连接地址
advertised.listeners=EXTERNAL://192.168.233.166:9002
	# 用于配置broker之间通信使用的监听器名称，该名称必须在advertised.listeners列表中
inter.broker.listener.name=EXTERNAL

# 数据位置
log.dir=/var/log/kafka/kafka-logs
	
```

##  主题

### 参数



# 日志

## 参数

​	

| 配置条目                          | 默认值           | 说明                                                         |
| --------------------------------- | ---------------- | ------------------------------------------------------------ |
| log.index.interval.bytes          | 4096(4k)         | 增加索引项字节间隙密度，会影响索引文件中的区间密度和查询效率 |
| log.segment.bytes                 | 1073741824（1G） | 日志文件最大值                                               |
| log.retention.bytes               |                  | 基于日志大小删除文件                                         |
| log.roll.ms                       |                  | 当前日志分段中消息的最大时间戳与当前系统的时间戳的差值允许的最大范围，单位毫秒 |
| log.roll.hours                    | 168（7天）       | 当前日志分段中消息的最大时间戳与当前系统的时间戳的差值允许的最大范围，单位小时 |
| log.index.size.max.bytes          | 10485760(10M)    | 触发偏移量索引文件或时间戳索引文件分段字节限额               |
| log.cleanup.policy                | delete           | 日志的清理策略                                               |
| log.cleaner.min.compaction.lag.ms |                  | 用于防止对更新超过最小消息进行压缩，如果没有设置，除了最后一个Segment之外，所有segment都有资格进行压缩 |
| log.cleaner.max.compaction.lag.ms |                  | 用于防止低生产速率的日志在无限制的时间内不压缩               |
| cleanup.policy                    |                  | 主题级别的日志清理策略                                       |
| log.retention.hours               | 默认7天          | 日志保留的时间，优先级最低                                   |
| log.retention.minutes             |                  | 日志保留的时间，优先级次之                                   |
| log.retention.ms                  |                  | 日志保留的时间，优先级最高                                   |
| file.delete.delay.ms              |                  | 设置日志删除的延迟时间                                       |



## 	物理存储

​		topic的每个partition都包含一个文件夹：.index索引文件、.log的日志文件.timestamp

​	    每个分区包含多个LogSegment

​		文件名是偏移量

| 后缀名                  | 说明                                           |
| ----------------------- | ---------------------------------------------- |
| .index                  | 偏移量索引文件，偏移量与物理地址之间的映射关系 |
| .timestamp              | 时间戳索引文件，按照时间戳查找对应的关系       |
| .log                    | 日志文件                                       |
| .snapshot               | 快照文件                                       |
| .deleted                |                                                |
| .cleaned                | 日志清理临时文件                               |
| .swap                   | 日至压缩后的临时文件                           |
| leader-epoch-checkpoint | 集群控制器有关                                 |

**切分文件**

​		大于log.segment.bytes	、log.roll.ms 、 log.roll.hours	、  log.index.size.max.bytes的值会切分

​		追加的消息的偏移量与当前日志分段的偏移量之间的差值大于Integer.MAX_VALUE，即要追加的消息的偏移量不能转变为相对偏移量

**为什么是Integer.MAX_VALUE**

​		在偏移量索引文件中，每个索引项目占用8个字节。

​		相对偏移量，记录的是在Integer的数值范围内。

**索引文件切分**

​		文件创建的时候就是最大值。

​		当进行日志滚动和切分的时候，会分配实际大小。

## 文件解析

```shell
# 查看日志 元数据信息，Dump日志
kafka-run-class.sh kafka.tools.DumpLogSegments --files 00000000000000000000.log --print-data-log | head

# 解析索引.index文件
kafka-run-class.sh kafka.tools.DumpLogSegments --files 00000000000000000000.index --print-data-log | head
```



## 偏移量

1、位置索引保存在index文件中

2、log日志more每写入4k（log.index.interval.bytes设定），会写入一条索引信息到index文件中，因此索引文件是稀疏索引

3、log文件中的日志，是顺序写入的，由message+实际offset+position组成

4、索引文件的数据结构，则由相对offset（byte）+posstiont（4byte）组成，由于保存的是相对于第一个消费信息的相对offset，只需要4byte就够了，但在实际查找还需要计算回实际的offset

![image-20210505141933846](.\图片\偏移量.png)



​		

## 时间戳

 		**偏移量索引文件中都是顺序记录offset，但时间戳索引文件中每个追加得索引时间戳必须大于之前追加的索引项，否则不予追加。在kafka 0.11.0.0以后，消息元数据存在若干的时间戳信息。如果broker端参数log.message.timestamp.type设置为LogAppendTime，那么时间戳必定能保持单调增长。反之如果是CreateTime则无法保证有序。**

​		timestamp文件中的offset与index文件中的relativeOffset不是一一对应的。因为数据的写入是各自追加。

​		**通过时间戳方式查找数据，需要查找时间戳索引和偏移量索引两个文件。**

​		时间戳的格式：时间戳（8字节）+偏移量（4字节）



**通过时间戳查找数据**

​		1、查找改时间戳在哪个日志分段中。将此时间戳和每个日志分段中的最大时间戳largestTimpStamp逐一对比，直到不小于此时间戳所对应的日志分段。

​				日志分段中largestTimeStamp的计算是：先查询该日志分段所对应时间戳索引文件的最后一个所以记录，若最后一条索引项的时间戳>0，则取该值，否				取该日志分段的修改时间。

​		2、查找该日志分段的偏移量索引，查找该偏移量对应的物理地址。

​		3、日志文件中从物理地址开始查找此条数据。

## 日志清理

清理策略有两种，日志删除和日志压缩。

日志删除：按照一定的删除策略，将不满足条件的数据进行数据删除。

日志压缩：针对每个消息的key进行整合，对于有相同key而value不同，只保留最后一个版本。

Kafka提供log.cleanup.policy参数进行相应配置，默认值：delete，还可以compact。



主题级别的配置项：cleanup。policy



**基于时间**

日志删除任务会根据log.retention.hours/log.retention.minutes/log.retention.ms设定日志保留的时间节点。log.retention.ms的优先级最高。

1、从日志对象中所维护日志分段的跳跃表中移除待删除的日志分段，保证没有线程对这些日志分段进行读取操作

2、在这些日志分段所有文件，加上.delete后缀

3、交由一个以“delete-file”命名的低延迟任务来删除这些.delete为后缀的文件。延迟时间可以通过file.delete.delay.ms进行设置



**活跃分段也要删除数据**

kafka会先切分出新的日志分段作为活跃日志作为活跃日志分段，该日志分段不删除，删除原来的日志分段。

先腾出地方，再删除。



**基于日志大小**

日志删除任务会检查当前日志的大小是否超过设定值。设定项为log.retention.bytes，单个日志分段的大小由log.segment.bytes进行设定。

1、计算需要被删除的日志总大小（当前日志文件大小（所有分段）-retention值

2、从日志文件第一个LogSegement开始查找可删除的日志分段的文件集合。

3、执行删除



**基于偏移量**

根据日志分段的下一个日志分段的起始偏移量是否>=日志文件的起始偏移量，若是，则可以删除此日志分段。

![image-20210505150035257](.\图片\基于偏移量删除日志.png)

## 日志压缩策略

**1、概念**

​		日志压缩是kafka的一种机制，可以提供较细粒度的记录保留，而不是基于粗粒度的基于时间的保留。

​		对于具有相同的key，而不是数据不同，值保留最后一条数据记录，前面的数据在合适的情况下删除。

**2、应用场景**

​		kafka即使数据源优势存储工具，可以简化技术栈，降低维护成本

​		kafka对于



## 日志压缩

主题的cleanup.policy需要设置为compact。

**key的不为空**

kafka后台进程会定时将Topic遍历两次。

1、log.cleanup.policy设置为compact

2、log.cleaner.min.compaction.lag.ms，用于防止对更新超过最小消息进行压缩，如果没有设置，除了最后一个Segment之外，所有segment都有资格进行压缩

3、log.cleaner.max.compaction.lag.ms，用于防止低生产速率的日志在无限制的时间内不压缩



# 原理

## 零拷贝

零拷贝是减少不必要的拷贝次数。

**传统拷贝**

![image-20210505151827618](.\图片\零拷贝-传统拷贝.png)





## 页缓存机制

页缓存是操作系统实现的一种主要的磁盘缓存，以减少磁盘I/O的操作。

具体来说，就是把磁盘中的数据缓存到内存中，把对磁盘的访问变为对内存的访问。



mmap：虚拟交换内存

写到mmap中的数据并没有真正写到硬盘，操作系统会在程序主动调用flush的时候才把数据真正的写到磁盘。

kafka提供了一个参数（producer.type）来控制是不是主动flush：

​	1、如果kafka写入到mmap之后就立即返回flush然后再返回Producer叫同步（sync）

​	2、如果写入mmap之后，立刻返回Producer，不调用flush叫异步（async）。



mmap的文件映射，在full gc时才会释放。当close时，需要手动清除内存映射文件，可以反复调用sun.msce.Cleaner方法。



## 顺序写入机制

操作系统可以针对线性读写做深层次的优化，比如预读（read-ahead，提前将一个比较大的磁盘块读入内存）和后写（write-head，讲很多小的逻辑写操作合并起来组成一个打的物理写操作）技术。



mmap和sendfile：

1、Linux内核提供、实现零拷贝的API

2、sendfile是将读到内核空间的数据，转到socket buffer，进行网络发送

3、mmap将磁盘文件映射到内存，支持读和写，对内存的操作会放映到磁盘文件上。

4、RocketMQ在消费时，使用了mmap。kafka使用了sendFile。



**kafka速度快是因为**

1、partition顺序读写，充分利用磁盘特性

2、Producer生产的数据持久化broker，采用mmap文件映射，实现顺序的快速写入

3、Customer从broker读取数据，使用sendfile，加快了数据的读取



## 事务相关配置

**场景**

1、如Producer发的多条消息组成一个事务，这些消息需要对consumer同时可见或者同时不可见。

2、Producer可能会给多个topic，多个partition发消息，这些消息也需要能放在一个事务里面，这就形成了一个典型的分布式事务。

3、kafka的应用场景经常是应用先消费一个topic，然后在处理发送到另一个topic，这个consume-transform-produce过程需要先放到一个事务里面，比如在消息处理或者发送的过程中如果失败了，消费偏移量也不能提交。

4、producer或者consumer所在的应用可能会挂掉，新的producer启动以后需要知道怎么处理之前未完成的事务。

5、在一个原子操作中，根据包含的操作类型，可以分为三种情况前两种情况是事务引入的场景，最后一种没用。

### 概念和推导

1、因为Producer发送消息可能是分布式事务，所以引入了常用的2PC，所以有事务协调者（Transaction Coordinator）。Transaction Coordinator和之前为了解决脑裂和惊群问题引入的Group Coordinator在选举上类似。

2、事务管理中事务日志是必不可少的，Kafka使用一个内容topic来保存事务日志，这个设计和之前使用内部topic保存偏移量的设计保持一致。事务日志是Transaction Coordinator管理的状态的持久化，因为不需要回溯事务的历史状态，所以事务日志只保存最近的事务状态。**__transaction_state**

3、因为事务存在commit和abort两种操作，而客户端read committed和read uncommitted两种隔离级别，所以消息丢列必须能标识事务状态，这个被称作Control Message。

4、Producer挂掉重启或漂移到其他机器需要能关联之前的未完成事务，所有需要一个唯一标识在进行关联，这个就是TransactionalId，一个Producer挂了，另一个具有相同TransactionalId的producer能够接着处理这个事务未完成的状态。kafka目前没有引入全局序，所以也没有transaction id，这个transactionalID是用户提前配置的。

5、TransactionalId能关联producer，也需要避免两个使用相同的TransactionalId的producer同时存在，所以引入了produer epoch来保证对应一个TransactionalId只有一个活跃的producer epoch。



**事务消息定义**

生产者可以显式的发起事务会话，在这些会话中发送（事务）消息，并提交或中止事务，有如下要求：

​	1、原子性：消费者的应用程序不应暴露于**未提交的事务**的消息中

​	2、持久性：Broker不能丢失任何已提交的事务

​	3、排序：事务消费者应在每个分区中以原始顺序查看事务消息

​	4、交织：每个分区都应该能够接收来自事务性生产者和非事务生产者的消息

​	5、事务中不应有重复的消息



### 事务配置

1、创建消费者代码，需要：

​		1）将配置中的自动提交属性（auto.commit)进行关闭

​		2）在代码里不能使用手动提交commitSync和commitAsync

​		3）设置isolation.level：READ_COMMITTED或READ_UNCOMMITTED

2、创建生产者，代码如下，需要：

​		1）配置transactional.id属性

​		2）配置enable.idempotence属性



### 事务协调器

事务协调器是__transactiona_state主题特定分区的Leader分区所在的Broker。他负责初始化、提交以及回滚事务。

事务协调器在内存的管理状态如下：

​	1、对应在在处理的事务的第一个消息的HW。事务协调器周期性的将HW写到ZK。

​	2、事务控制日志中存储对应于日志HW的所有正在处理的事务

​	3、事务消息主题分区的列表

​			事务的超时时间

​			事务关联的producer Id

![image-20210505171734333](.\图片\事务流程.png)

**初始阶段**

1、Producer：计算哪个Broker作为事务协调器

2、Producer：向事务协调器发送Begin Transaction（producerId，generation，partitions..）请求，当然也可以发送另一个包含事物过期时间的。如果生产者需要将消费者状态作为事务的一部分提交事务的，则需要在Begin Transaction中包含对应的__consumer_offsets主题分区信息。

3、Broker：生成事务ID

4、Coordinator：向事务协调主题追加BEGIN（TxId，producerId，generation，paritions..）消息，然后发送响应给生产者

5、Producer：读取响应（包含了事务ID，TxID）

6、Coordinator（and followers)：在内存更新当前事务的待确认事务状态和数据分区信息

**发送阶段**

Producer：发送事务笑死给主题Leader分区所在的Broker。每个消息需要包含TxId和TxCtl字段。TxCtl仅用于标记事务的最终状态（提交还是中止）。生产者请求也封装了生产者ID，但是不追加到日志中。

**结束阶段**

1、Producer：发送OffsetCommitRequest请求提交与事务状态结束关联的输入状态（如下一个事务输入从哪儿开始）

2、Producer：发送CommitTransaction（TxID，producerId，generation）请求给事务协调器并响应等待。（如果响应中没有错误信息，表示将提交该事务）

3、Coordinator：向事务控制主题追加PREPARE_COMMIT（TxId）请求并向生产者发送响应。

4、Coordinator：向事务涉及的每个Leader分区（事务的业务数据的目标主题）的Broker发送一个CommitTransaction（TxId，partitions...）请求。

5、事务业务数据的目标主题Leader分区Broker：

​			1）如果是非__consumer_offsets主题的Leader分区，已收到CommitTransaction（TxID，partition1，partition2,...）请求就会向对应的分区Broker发送空（null）消息（没有key/value）并给该消息设置TxId和TxCtl（设置为COMMITED）字段。Leader分区的Broker给协调器发送响应。

​			2）如果是__consumer_offsets主题的Leader分区，追加消息，该消息的key是G-LASG-COMMIT，value值就是TxId的值。同事给该消息设置TxId和TxCtl字段。Broker相协调器发送响应。

6、Coordinator：向事务控制主题发送COMMITED（TxId）请求。

7、Coordinator（adn followers）：尝试更新HW。

###  参数

**Broker Config**

| 配置项                                   | 说明                                                         |
| ---------------------------------------- | ------------------------------------------------------------ |
| transactional.id.timeout.ms              | 在ms中，事务协调器在生产者TransactionalId提前过期之前等待的最长时间。并且没有从该生产者TransactionalId接收到任何事务状态更新。默认是604800000（7天）。这允许每周一次的生产者作业维护他的id |
| max.transaction.timeout.ms               | 事务允许的最大超时。如果客户端请求的事务时间超过此时间，broke将在InitPidRequest中返回InvalidTransactionTimeout错误。这可以防止客户机超时过大，从而导致用户无法从事务中包含的主题读取内容。默认值为900000(15分钟)。这是消息事务。需要发送的时间的保守上限。 |
| transaction.state.log.replication.factor | 事务状态topic的副本数量。默认值：3                           |
| transaction.state.log.num.partitions     | 事务状态主题的分区数。默认值：50                             |
| transaction.state.log.min.isr            | 事务状态主题的每个分区ISR最小数量。默认值2                   |
| transaction.state.log.segment.bytes      | 事务状态主题的segment大小。默认值104857600字节               |

**Producer configs**

| 配置项                 | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| enable.idempotence     | 开启幂等                                                     |
| transaction.timeout.ms | 事务超时时间<br/>事务协调器在主动中止正在进行的事务之前等待生产者更新事务状态的最长时间。这个配置值将与InitPidRequest一起发送到事<务协调器。如果该值大于max.transaction.timeout。在broke中设置ms时，请求将失败，并出现InvalidTransactionTimeout错<误。<br/>默认是60000。这使得交易不会阻塞下游消费超过一分钟，这在实时应用程序中通常是允许的。 |
| transactional.id       | 用于事务性交付的TransactionalId。这支持跨多个生产者会话的可靠性语义，因为它允许客户端确保使用相同TransactionalId的事务在启动任何新事务之前已经完成。如果没有提供TransactionalId，则生产者仅限于幂等交付。 |

**consumer configs**

| 配置项          | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| isolation.level | - read_uncommitted:以偏移顺序使用已提交和未提交的消息。<br/>- read_committed:仅以偏移量顺序使用非事务性消息或已提交事务性消息。为了维护偏移排序，这个设置意味着我们必须在使用者中缓冲消息，直到看到给定事务中的所有消息。 |

### 幂等

保证在消息重复的时候，消费者不会重复处理。即使在消费者收到重复消息的时候，重复处理，也需要保证最终结果的一致性。

所谓幂等性，数学概念：f(f(x))=f(x)



Kafka为了实现幂等性，在底层设计架构中引入了ProducerID和SequenceNumber。

1、ProducerId：在每个新的Producer初始化时，会被分配一个唯一的ProducerID，这个ProducerID对客户端使用者是不可见的。

2、SequenceNumber：对于每个ProducerId，Producer发送数据的每个Topic和Partition都对应一个从0开始递增的SequenceNumber值。



### 事务操作

![image-20210505184757278](.\图片\事务操作.png)

**生产者**

```java
package kafka.transaction;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

public class MyTransactionalProducer {
    public static void main(String[] args) {
        Map<String,Object> configs = new HashMap<>();

        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"linux123:9092");
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        // 提供客户端ID
        configs.put(ProducerConfig.CLIENT_ID_CONFIG, "tx_producer");
        // 事务ID
         configs.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my_tx_id");
        // 要求ISR都确认
        configs.put(ProducerConfig.ACKS_CONFIG, "all");

        KafkaProducer<String, String> producer = new KafkaProducer<>(configs);
        
        //初始化事务
        producer.initTransactions();
        //开启事务
        producer.beginTransaction();
        try {
            producer.send(new ProducerRecord<>("top_tx_01", "tx_msg_02"));
            producer.commitTransaction();
        } catch (Exception exception){
            producer.abortTransaction();
        }finally {
            // 关闭生产者
            producer.close();
        }

    }
}

```



**生产消费者和消费者模式**

创建主题

```shell
kafka-topic.sh --zookeeper linux123:2181/myKafka --create --topic tp_tx_out_01 --partitions --replication-factor 1
kafka-console-consumer.sh --bootstrap-server linxu123:9092 --topic tp_tx_out_01 --isolation-level read_commited --from-beginning
```

代码

```java
package kafka.transaction;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MyTransactional {
    public static KafkaProducer<String,String> getProducer(){
        Map<String, Object> confis = new HashMap<>();
        confis.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"linux123:9092");
        confis.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        confis.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class);

        //设置生产者id
        confis.put(ProducerConfig.CLIENT_ID_CONFIG,"tx_producer_01");
        // 设置事务id
        confis.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG,"tx_id_02");

        //是否启用幂等性
        confis.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,true);

        KafkaProducer<String, String> producer = new KafkaProducer<>(confis);
        return producer;
    }
    public static KafkaConsumer<String,String> getConsumer(String consumerGroupID){
        Map<String, Object> confis = new HashMap<>();

        confis.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"linux123:9092");
        confis.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
        confis.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringSerializer.class);

        //消费者组id
        confis.put(ConsumerConfig.GROUP_ID_CONFIG,consumerGroupID);
        // 消费者偏移量的自动确认
        confis.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);

        confis.put(ConsumerConfig.CLIENT_ID_CONFIG,"consumer_client_02");

        //只读取已经提交的数据
//        confis.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG,"read_committed");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(confis);
        return consumer;
    }

    public static void main(String[] args) {
        String comsumerGroupId = "consumer_grp_id_02";
        KafkaProducer<String, String> producer = getProducer();
        KafkaConsumer<String, String> consumer = getConsumer(comsumerGroupId);

        producer.initTransactions();
        consumer.subscribe(Collections.singletonList("tp_tx_01"));
        ConsumerRecords<String, String> records = consumer.poll(1000);
        //开启事务
        producer.beginTransaction();
        try {
            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
            for (ConsumerRecord<String, String> record : records) {
                producer.send(new ProducerRecord<String, String>("tp_tx_out_01", record.key(), record.value()));

                offsets.put(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 1)); //偏移量表示下一条要消费的消息
            }
            // 将该消息的偏移量提交作为事务的一部分，随事务提交和回滚（不提交消费偏移量
            producer.sendOffsetsToTransaction(offsets, comsumerGroupId);
            producer.commitTransaction();
        }catch (Exception exception){
            exception.printStackTrace();
            producer.abortTransaction();
        }finally {
            producer.close();
            consumer.close();
        }

    }
}

```

### 集群控制器

​		控制器就是一个broker。

​		控制器除了一般的broker的功能，还负责leader分区的选举。

​		选举使用zookeeper的分布式锁，zookeeper的临时节点功能。



​		控制器的脑裂，使用epoch来控制。

## 可靠性保证

replica.lag.time.max.ms默认大小为10000毫秒。

当超过这个指定的值，及判定副本失效。

失效副本：

​	1、Follower副本进程卡主，在一段时间内没有向Leader发起同步请求，比如频繁的Full GC.

​	2、Follower副本进程同步太慢，在一段时间内都无法追上Leader副本，比如IO开销过大

​	

失效副本的分区个数是用于衡量Kafka性能指标的重要部分。Kafka本身提供了一个相关的指标，即UnderReplicatedPartitions，可以通过JMX访问。

​	kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions



## 一致性保证

min.insync.replicas=1

引入Leader Epoch解决数据丢失的问题。是一对值<epoch,offset>，offset是leader的消息位置。



##  消费重复场景及解决

1、启用Kafka的幂等性

​		enable.idempotence=true

​		ack=all

​		retries>1

![image-20210506215536977](.\图片\消息不一致情况.png)



2、ack=0不重试，会丢失消息

```
allprojects {
	repositories {
	maven { url 'https://maven.aliyun.com/repository/public/' }
	maven { url 'https://maven.aliyun.com/nexus/content/repositories/google' }
	maven { url 'https://maven.aliyun.com/nexus/content/groups/public/'
}
	maven { url 'https://maven.aliyun.com/nexus/content/repositories/jcenter'}
	all { ArtifactRepository repo ->
	  if (repo instanceof MavenArtifactRepository) {
		def url = repo.url.toString()
		if (url.startsWith('https://repo.maven.apache.org/maven2/') || url.startsWith('https://repo.maven.org/maven2') ||url.startsWith('https://repo1.maven.org/maven2') || url.startsWith('https://jcenter.bintray.com/')) {
          //project.logger.lifecycle "Repository ${repo.url} replaced by $REPOSITORY_URL."
          remove repo
       }
     }
   }
 }
  buildscript {
	repositories {
		maven { url 'https://maven.aliyun.com/repository/public/'}
		maven { url 'https://maven.aliyun.com/nexus/content/repositories/google' }
		maven { url 'https://maven.aliyun.com/nexus/content/groups/public/' }
		maven { url 'https://maven.aliyun.com/nexus/content/repositories/jcenter'}
		all { ArtifactRepository repo ->
		if (repo instanceof MavenArtifactRepository) {
          def url = repo.url.toString()
          if (url.startsWith('https://repo1.maven.org/maven2') || url.startsWith('https://jcenter.bintray.com/')) {
            //project.logger.lifecycle "Repository ${repo.url} replaced by $REPOSITORY_URL."
            remove repo
         }
       }
     }
   }
 }
}
```



