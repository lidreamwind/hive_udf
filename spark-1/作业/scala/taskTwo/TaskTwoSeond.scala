package taskTwo

import scala.io.StdIn
import scala.util.Random

class User(name:String, score:Array[Int]){

  def showFist(): Unit ={
      print(s"${name}\t")
    for (elem <- score) {
      print(s"${elem}\t")
    }
  }
  // 人出拳
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
}
class Computer(name:String, score:Array[Int]){
  // 机器出拳
  def getRs(aa:Map[Int,String]) :(Int,String) ={
    val int = new Random().nextInt(3)
    (int,aa(int))
  }

  def showFist(): Unit ={
    print(s"${name}\t")
    for (elem <- score) {
      print(s"${elem}\t")
    }
  }
}
class Game(user:User,computer: Computer,time:Int){
  def startGame(): Unit ={

  }
}
object TaskTwoSeond {
  def main(args: Array[String]): Unit = {
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
      case "y" => val time = 3
        val user = new User("游客", new Array[Int](time))
        val computer = new Computer(roles(roleChoose.toInt).substring(2), new Array[Int](time))
        val game = new Game(user, computer, time)
        game.startGame
      case "n" => println("退出游戏！")
      case _ => println("请输入正确的选择！")
    }
  }
}
