package modelTwo
//作业二
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
case class Log(ip: String,hit:String,re_time:String,request_time:String,method:String,url:String,status:String,re_size:String, agent:String)
object TaskTwo {
  def main(args: Array[String]): Unit = {
    Logger.getLogger(TaskOne.getClass).setLevel(Level.ERROR)
    val spark: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("task-ip-area")
      .config("spark.some.config.option","some-value")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    //读取文件
    val orgValue: Dataset[String] = spark.read.textFile("data/cdn.txt")
    // 转换DataSet或者DataFrame需要导入隐式转换
    import spark.implicits._
    val value: Dataset[Log] = orgValue.map(x => {
      val str: Array[String] = x.split("\\s+")
      Log(str(0), str(1), str(2), str(3), str(4), str(5), str(6), str(7), str(8))
    })

    println("=========== 独立ip个数：",value.select($"ip").distinct().count())
    value.show(1)
    val vedio: Dataset[Log] = value.filter("status like '%mp4%'")
    println("=========== 每个视频独立ip数量：",vedio.groupBy("status","ip").count().show(20))




    vedio.groupBy()


    spark.stop()
  }
}
