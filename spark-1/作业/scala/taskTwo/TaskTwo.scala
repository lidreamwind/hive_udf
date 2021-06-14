package taskTwo

import scala.collection.mutable.ArrayBuffer
import scala.io.StdIn
import scala.util.Random
//第二题
object TaskTwo {
  /**
   * 第二题、人机猜拳
   * 1.1 作业需求
   * 1. 选取对战角色
   * 2. 开始对战，用户出拳，与对手进行比较，提示胜负信息
   * 3. 猜拳结束算分，平局都加一分，获胜加二分，失败不加分
   * 4 . 循环对战，当输入“n”时，终止对战，并显示对战结果
   * 5. 游戏结束后显示得分
   */
  // 定义方法，获得输出结果
  def getGameRs(gameRule: Map[Int, String], stepRsPerson: String): Int = {
    stepRsPerson.toInt - 1 match {
      case 0 => println(s"你出拳:${gameRule(0).substring(2)}")
        0
      case 1 => println(s"你出拳:${gameRule(1).substring(2)}")
        1
      case 2 => println(s"你出拳:${gameRule(2).substring(2)}")
        2
      case _ => println("输入不符合规范，默认出布！")
        2
    }
  }

  def main(args: Array[String]): Unit = {

    // 定义函数，比较结果,返回结果：游客胜利：1 ， 电脑胜利：2 平局：0
    val getResult = (person: Int, computor: Int) => {
      if (person - computor > 0) 1
      else if (person - computor < 0) 2
      else 0
    }
    // 定义函数，输出结果
    val showRs = (result: scala.collection.mutable.Map[String, ArrayBuffer[Int]], compu: String) => {
      println("姓名\t等分\t胜局\t和局\t负局")
      // 定义的数组依次是 等分 胜局 和局 负局
      var person = ArrayBuffer[Int](0, 0, 0, 0)
      var compuP = ArrayBuffer[Int](0, 0, 0, 0)
      for ((p, c) <- (result("游客") zip result(compu))) {
        //游客赢了
        if (p - c > 0) {
          person(0) += p
          compuP(0) += c

          person(1) += 1
          compuP(3) += 1
          // 电脑赢了
        } else if (p - c < 0) {
          person(0) += p
          compuP(0) += c

          compuP(1) += 1
          person(3) += 1
          //平局
        } else {
          person(0) += p
          compuP(0) += c
          person(2) += 1
          compuP(2) += 1
        }
      }
      print("游客\t")
      for (elem <- person) {
        print(s"${elem}\t")
      }
      println("")
      print(s"${compu}\t")
      for (elem <- compuP) {
        print(s"${elem}\t")
      }
    }

    // 定义循环游戏函数
    val playGame = (rolesP: String) => {
      var isStartNext = "y"
      var rs = scala.collection.mutable.Map[String, ArrayBuffer[Int]]("游客" -> (new ArrayBuffer[Int]()), rolesP -> new ArrayBuffer[Int]())
      val gameRules = Map[Int, String](0 -> "1.剪刀", 1 -> "2.石头", 2 -> "3.布")
      while (isStartNext.equalsIgnoreCase("y")) {
        println("请出拳！1.剪刀  2.石头  3.布")
        val stepRs = getGameRs(gameRules, StdIn.readLine())

        val computorNum = Random.nextInt(3)
        val computor = gameRules(computorNum).substring(2)
        println(s"${rolesP}出拳:${computor}")
        val compareRs = getResult(stepRs, computorNum)
        compareRs match {
          case 0 => println("结果：平局！下次继续努力！")
            rs("游客") += 1
            rs(rolesP) += 1
          case 1 => println("恭喜，你赢啦！")
            rs("游客") += 2
            rs(rolesP) += 0
          case 2 => println("很遗憾，你输了！")
            rs("游客") += 0
            rs(rolesP) += 2
        }
        println("是否开始下一轮游戏（y/n)")
        isStartNext = StdIn.readLine()
      }
      println("退出游戏！")
      println("-----------------------------------------")
      println(s"${rolesP} VS 游客")
      println(s"对战次数${rs(rolesP).size}次")
      println("")
      println("")
      showRs(rs, rolesP)

    }
    // 游戏开始
    val roles = Map[Int, String](1 -> "1.刘备", 2 -> "2.关羽", 3 -> "3.张飞")
    println("*****************************************************")
    println("**********************猜拳，开始***********************")
    println("*****************************************************")
    println("请选择对战角色：1.刘备  2.关羽  3.张飞")
    val roleChoose = StdIn.readLine()
    println(s"你选择了与${roles(roleChoose.toInt).substring(2)}作战")
    println("要开始了吗?y/n")
    val isStart = StdIn.readLine()
    isStart match {
      case "y" => playGame(roles(roleChoose.toInt).substring(2))
      case "n" => println("退出游戏！")
      case _ => println("请输入正确的选择！")
    }
  }

}
