package modelTwo

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable
// 作业三
object TaskThree {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("task-three")
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")

    val click: RDD[String] = sc.textFile("data/click.log")
    val imp: RDD[String] = sc.textFile("data/imp.log")

    val clickAid: RDD[String] = click.map(x => {
      val buffer: mutable.Buffer[String] = x.split("\\s+").toBuffer
      buffer.tail.toString().split("&")(2)
    })
    val clickRs: RDD[(String, Int)] = clickAid.map((_, 1)).reduceByKey(_ + _)

    val impAid: RDD[String] = imp.map(line => {
      val buffer: mutable.Buffer[String] = line.split("\\s+").toBuffer
      buffer.tail.toString().split("&")(2)
    })
    val impRs: RDD[(String, Int)] = impAid.map((_, 1)).reduceByKey(_ + _)

    val result: RDD[(String, (Option[Int], Option[Int]))] = clickRs.fullOuterJoin(impRs)
    val rs: RDD[(String, Int, Int)] = result.map(line => {
      val value: String = line._1
      val value1: Option[Int] = line._2._1
      val value2: Option[Int] = line._2._2
      value1 match {
        case Some(v1) => value2 match {
          case Some(a) => (value, v1, a)
          case _ => (value, v1, 0)
        }
        case _ => value2 match {
          case Some(a) => (value, 0, a)
          case _ => (value, 0, 0)
        }
      }
    })

    rs.take(3).foreach(println)
    rs.saveAsTextFile("data/click")

      // 只有一次shuffle
  }

}
