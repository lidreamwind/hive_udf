package ModelThree

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import java.util.Properties

//第一题
object KafkaProducer {
  def main(args: Array[String]): Unit = {
    val brokers = "linux121:9092,linux122:9092"
    val topic = "taskThreeProducerTest"
    val prop = new Properties()

    prop.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,brokers)
    prop.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,classOf[StringSerializer])
    prop.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,classOf[StringSerializer])

    val producer = new KafkaProducer[String,String](prop)
    var i = 13
    scala.io.Source.fromFile("data/sample.log").getLines().foreach(line => {
        val msg = new ProducerRecord[String, String](topic, line)
        producer.send(msg)
        println(s"i= ${i+1}")
        i += 1
        Thread.sleep(1000)
      }
    )

    producer.close()
  }

}
