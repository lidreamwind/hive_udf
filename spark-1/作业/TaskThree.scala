import scala.util.Sorting

case class User(user:String,location:String,time:String,delay:Int)
object TaskThree {
  /**
   * 现有如下数据需要处理：
    字段：用户ID，位置ID，开始时间，停留时长（分钟）
4行样例数据：
     UserA,LocationA,8,60 UserA,LocationA,9,60 UserB,LocationB,10,60 UserB,LocationB,11,80 样例数据中的数据含义是：用户UserA，在LocationA位置，从8点开始，停留了60钟
 处理要求：
    1、对同一个用户，在同一个位置，连续的多条记录进行合并
     2、合并原则：开始时间取最早时间，停留时长累计求和
   */
  def main(args: Array[String]): Unit = {
    val users = new Array[User](4)
    users(0) = User("UserA","LocationA","8",60)
    users(1) = User("UserA","LocationA","9",60)
    users(2) = User("UserB","LocationB","10",60)
    users(3) = User("UserB","LocationB","11",80)

    val stringToUsers = users.groupBy(t => t.user + "-" + t.location)
    val stringToUsers1 = stringToUsers.mapValues(t => t.sortBy(_.time))
    val users1 = stringToUsers1.map(t => {
      val strings = t._1.split("-")
      new User(strings(0), strings(1), t._2.head.time, t._2.map(_.delay).sum)
    })
    users1.foreach(println(_))

  }

}
