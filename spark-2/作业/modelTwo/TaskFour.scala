package modelTwo

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.expressions.{Window, WindowSpec}
import org.apache.spark.sql.functions.rank
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
// 作业四
object TaskFour {
  def main(args: Array[String]): Unit = {

    Logger.getLogger(TaskOne.getClass).setLevel(Level.ERROR)
    val spark: SparkSession = SparkSession.builder()
      .master("local[*]")
      .appName("task-ip-area")
      .config("spark.some.config.option","some-value")
      .getOrCreate()
    spark.sparkContext.setLogLevel("ERROR")

    import spark.implicits._
    val df: DataFrame = Seq(("2019-03-04","2020-02-03"),
      ("2020-04-05","2020-08-04"),
      ("2019-10-09","2020-06-11"))
      .toDF("sdate", "edate")

    df.createOrReplaceTempView("data")

    spark.sql(
      """
        |select sdate,row_number() over(order by sdate)  rn
        |from
        |   (select sdate
        |     from data
        |   union all
        |     select edate
        |     from data
        |    ) t
        |order by 1
        |""".stripMargin).createOrReplaceTempView("tt")
    spark.sql(
      """
        |select t1.sdate,case when t2.sdate is null then t1.sdate else t2.sdate end sdate2
        |   from tt t1
        |     left outer join tt t2
        |        on t2.rn=t1.rn+1
        |""".stripMargin).show(30)

    // 窗口函数实现
    val dfD: DataFrame = df.select("sdate").union(df.select("edate")).toDF("sdate")
    val wiOne: WindowSpec = Window.orderBy("sdate")
    val frame: DataFrame = dfD.select($"sdate", rank().over(wiOne).alias("rn"))
    val frame2: DataFrame = frame.withColumnRenamed("sdate", "ss")
      .withColumnRenamed("rn", "r1")
    val frame1: DataFrame = frame.join(frame2,$"rn"===$"r1"-1,"left")

    frame1.selectExpr("sdate","case when ss is null then sdate else ss end as st").show(20)
    spark.stop()
  }

}
