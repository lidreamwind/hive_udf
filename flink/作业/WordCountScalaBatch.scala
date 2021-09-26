import org.apache.flink.api.scala._

//d第一题，已经运行成功
object WordCountScalaBatch {
  def main(args: Array[String]): Unit = {

    val input= "flink_data/input/hello.txt"
    val outputPath = "flink_data/output"

    val environment: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment
    val text: DataSet[String] = environment.readTextFile(input)
    val out: DataSet[(String, Int)] = text.flatMap(_.split(" ")).map((_, 1)).groupBy(0).sum(1)
    out.print();
    out.writeAsCsv(outputPath,"\n"," ").setParallelism(1)
    environment.execute("job-name")
  }

}
