import org.apache.spark.sql.ForeachWriter

import java.sql.{Connection, DriverManager, PreparedStatement}

object MysqlWriter {
  private val max=1//连接池总数
  private val connectionNum=1//每次产生连接数
  private val pool=new java.util.LinkedList[Connection]()
  private var conNum=0//当前连接池已经产生的连接数

  //获取连接
  def getConnections(): Connection ={
    //同步代码块
    AnyRef.synchronized({
      //加载驱动
      for(i<-1 to connectionNum){
        val conn = DriverManager.getConnection(
          "jdbc:mysql://linux123:3306/spark",
          "spark",
          "spark"
        )
        pool.push(conn)
        conNum+=1
      }
      pool.poll()
    })
  }
  //加载驱动
  def GetConn(): Unit ={
    //控制加载
    if (conNum<max && pool.isEmpty){
      Class.forName("com.mysql.jdbc.mysql.")

    }else if(conNum>=max&&pool.isEmpty){
      println("Jdbc Pool had no connection now,please wait a moments")
      Thread.sleep(2000)
      GetConn()
    }
  }

  //还连接
  def returnConn(conn:Connection): Unit ={
    pool.push(conn)
  }
}

class MysqlWriter extends ForeachWriter[WordCount]{
  var connection: Connection = null
  var statement: PreparedStatement = null
  override def open(partitionId: Long, epochId: Long): Boolean = {
    connection = MysqlWriter.getConnections()
    true
  }

  override def process(value: WordCount): Unit = {

    statement = connection.prepareStatement("insert wordcount values(?,?)")
    statement.setString(1,value.name)
    statement.setLong(2,value.num)
    statement.execute()
  }

  override def close(errorOrNull: Throwable): Unit = {
//    statement.closeOnCompletion()
    MysqlWriter.returnConn(connection)
  }
}
