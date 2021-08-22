import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}

case class WordCount(name:String, num:Long)
object TaskFive {
  def main(args: Array[String]): Unit = {
    val spark: SparkSession = SparkSession.builder().master("local[*]").appName("taskfive").getOrCreate()
    val df: DataFrame = spark.readStream
      .format("socket")
      .option("host", "linux123")
      .option("port", "9999")
      .load()

    spark.sparkContext.setLogLevel("WARN")


    import spark.implicits._
    val value: Dataset[String] = df.as[String]
    val word: Dataset[(String, Int)] = value.flatMap(_.split(" ")).map((_, 1))
    val rs: Dataset[WordCount] = word.withColumnRenamed("_1","name")
      .groupBy("name")
      .count()
      .withColumnRenamed("count","num")
//      .selectExpr("name","cast(num as int) num")
      .as[WordCount]

    rs.writeStream
      .foreach(new MysqlWriter)
      .outputMode(OutputMode.Update())
      .trigger(Trigger.ProcessingTime(10))
      .start()
      .awaitTermination()

    rs.writeStream
      .format("console")
      .outputMode("update")
      .trigger(Trigger.ProcessingTime(10))
      .option("checkpointLocation","./ckp")
      .start()
      .awaitTermination()
  }
}
