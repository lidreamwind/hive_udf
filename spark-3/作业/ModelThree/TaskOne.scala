package ModelThree

import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.json4s.native.Serialization

// 启用stream 消费数据，并生成json
case class Sample(commandid:String,housedid:String,gathertime:String,srcip:String,destip:String,
                  srcport:String,destport:String,domainname:String,proxytype:String,proxyip:String,
                  title:String,content:String,url:String,logid:String)
object TaskOne {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.WARN)

    val conf: SparkConf = new SparkConf()
      .setAppName("Task-Three-stream")
      .setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))

    val groupId= "kafka-task-three-consumer01"
    val topics = Array("taskThreeProducerTest")

    val kafkaParams: Map[String, Object] = Map[String, Object](
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "linux121:9092,linux122:9092,linux123:9092",
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG ->  classOf[StringDeserializer],
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG ->classOf[StringDeserializer],
      ConsumerConfig.GROUP_ID_CONFIG -> groupId,
      ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
      ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> (false:java.lang.Boolean)
    )

    val kafkaRDD: InputDStream[ConsumerRecord[String,String]] = KafkaUtils.createDirectStream(ssc,
      LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, String](topics, kafkaParams)
    )
   kafkaRDD.foreachRDD( rdd => {
     rdd.foreachPartition( fp =>{
       val producer = KafkaTaskProduer
       fp.foreach(msg =>{
         val strs: Array[String] = msg.value().replace("<<<!>>>", "").split(",")
         val sample = Sample(strs(0),strs(1),strs(2),strs(3),strs(4),strs(5),strs(6),strs(7),strs(8),strs(9),strs(10),
           strs(11),strs(12),strs(13))
         // 样例类转json
         implicit  val formats = org.json4s.DefaultFormats
         val jsonString: String = Serialization.write(sample)

         producer.sendMsg(jsonString)
         Thread.sleep(500)
       })
       producer.closeConection()
     })
   })

    ssc.start()
    ssc.awaitTermination()

  }

}
