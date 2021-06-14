object TaskOne {
  /**
   * 作业要求：每瓶啤酒2元，3个空酒瓶或者5个瓶盖可换1瓶啤酒。100元最多可喝多少瓶啤酒？
   * （不允许借啤酒）思路：利用递归算法，一次性买完，然后递归算出瓶盖和空瓶能换的啤酒数
   */
    def getBeer(total:Int,a:Int,b:Int):Int={
      var rs = 0
      val aRs = total + a
      val brs = total + b
      if (aRs/3 + brs/5>0) {
        rs = aRs/3 + brs/5 + getBeer(aRs/3 + brs/5,aRs%3,brs%5)
      }
      rs
    }
  def main(args: Array[String]): Unit = {
    val totalNum = 50 + getBeer(100 / 2, 0, 0)
    println(s"总共可以兑换的啤酒是：$totalNum")
  }

}
