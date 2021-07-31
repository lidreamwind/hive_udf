package ModelThree

import org.apache.log4j.{Level, Logger}
import org.apache.spark.graphx.{Edge, Graph, VertexId}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
//第二题
object TaskTwo {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.WARN)

    val conf: SparkConf = new SparkConf().setAppName(this.getClass.getName).setMaster("local[*]")
    val sc = new SparkContext(conf)

    val verArray:Array[(VertexId,String)] = Array((1L, "SFO"), (2L, "ORD"), (3L, "DFW"))
    val verRDD: RDD[(Long, String)] = sc.makeRDD(verArray)

    val edgeArray:Array[Edge[Long]] = Array(Edge(1L, 2L,1800),Edge(2L, 3L, 800),Edge(3L, 1L, 1400))
    val edgeRdd: RDD[Edge[Long]] = sc.makeRDD(edgeArray)

    val graph: Graph[String, VertexId] = Graph(verRDD, edgeRdd)

    //
    println("所有的定点------------------")
    graph.vertices.foreach(println)
    println("所有的边。。。。。")
    graph.edges.foreach(println)
    println("所有的triplets。。。。。。")
    graph.triplets.foreach(println)
    println("顶点数量..........")
    println(graph.vertices.count())
    println("求边的数量。。。。。")
    println(graph.edges.count())
    println("机场距离大于1000的。。。。。")
    graph.triplets.filter(_.attr>1000).foreach(println)

  }

}
