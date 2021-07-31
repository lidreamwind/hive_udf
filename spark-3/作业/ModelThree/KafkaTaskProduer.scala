package ModelThree

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties

object KafkaTaskProduer {
  val brokers = "linux121:9092,linux122:9092"
  val topic = "taskThreeProducer"
  val prop = new Properties()

  prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,brokers)
  prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,classOf[StringSerializer])
  prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,classOf[StringSerializer])

  val producer = new KafkaProducer[String,String](prop)
  var i = 1
  def sendMsg(message:String): Unit ={
    val msg:ProducerRecord[String,String] = new ProducerRecord[String, String](topic, i.toString, message)
    producer.send(msg)
    println(s"----已经发送消息到Kafka-topic-${i}：${topic}")
    i += 1
  }
  def closeConection(): Unit ={
    producer.close()
  }

  override def finalize(): Unit = {
    super.finalize()
    producer.close()
  }

}
