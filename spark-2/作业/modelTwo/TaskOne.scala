package modelTwo

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession, expressions}
// 模块二作业，，作业1
object TaskOne {
  def main(args: Array[String]): Unit = {
    Logger.getLogger(TaskOne.getClass).setLevel(Level.ERROR)
    val spark: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("task-ip-area")
      .config("spark.some.config.option","some-value")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    // 加载ip数据进行处理
    val ip: DataFrame = spark.read
      .option("delimiter", "|")
      .option("header", "false")
      .option("inferschema", "true")
      .csv("data/ip.dat")
    //定义udf函数，将用来处理字段对比
    def getIp(ip: String):String = ip.split("\\.").toBuffer.take(3).mkString(".")
    // 注册函数,spark sql可以使用
    spark.udf.register("getIp",getIp _)
    //注册函数，dsl可以用
    import org.apache.spark.sql.functions._
    val getIps = udf(getIp _)
    // 可以使用$转义
    import spark.implicits._
    // 过滤ip所需要的字段，并将字段进行格式化
    val cityIp: DataFrame = ip.selectExpr("_c0", "_c1", "_c7")
      .withColumnRenamed("_c0", "s_ip")
      .withColumnRenamed("_c1", "e_ip")
      .withColumnRenamed("_c7", "city")
      .selectExpr("getIp(s_ip) as s_ip","getIp(e_ip) as e_ip","city")
//      .select(getIps($"s_ip"),getIps($"e_ip"),$"city")
    ip.printSchema()
    // 读取http数据
    val http: DataFrame = spark.read
      .option("delimiter", "|")
      .option("header", "false")
      .schema("timeStr string,ip string,web string, visData string,browse string")
      .csv("data/http.log")
    val httpIP: DataFrame = http.withColumn("newIp", getIps($"ip"))
     // 注册为表，使用sql查询结果
    httpIP.createOrReplaceTempView("http")
    cityIp.createOrReplaceTempView("city")
    val rs: DataFrame = spark.sql(
      """
        |select city,count(*) as num from http join city
        |where http.newIp=city.s_ip or http.newIp=e_ip
        |group by city
        |""".stripMargin)
    rs.show()

    spark.stop()
  }

}
