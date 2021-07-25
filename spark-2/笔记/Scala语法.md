# Scala基本语法

面向对象的、函数式编程、静态类型、并发性

## 类的层次结构

![image-20210614181917699](.\图片\类的层次结构)

1. Null是所有引用类型的子类型
2. Nothing是所有基本类型的子类型



## 操作符

 a + b 等价于 a.+(b)

1 to 10 等价于1.to(10)

**Scala没有提供++、 --操作符，但是可以使用+=、-**

## 输入和输出

通过readLine从控制台读取一行输入。

如果要读取数字、Boolean或者字符，可以使用readInt、readDouble、readByte、readShort、readLong、readFloat、readBoolean或者readChar。
print、println、printf 可以将结果输出到屏幕；

## 插值器

Scala 提供了三种字符串插值器：

1. s 插值器，对内嵌的每个表达式求值，对求值结果调用toString，替换掉字面量中的那些表达式
2. f 插值器，它除s插值器的功能外，还能进行格式化输出，在变量后用%指定输出格式，使用java.util.Formatter中给出的语法
3. raw 插值器，按照字符串原样进行输出

```scala
-- s插值器
val subject = "Spark"
val str1 = s"Hello, $subject"
println(str1)
val arr = (1 to 10).toArray
val str2 = s"arr.length = ${arr.length}"
println(str2)
println(s"The answer is ${6*6}")

-- f插值器
val year=2020
val month=6
val day=9
println(s"$year-$month-$day")
-- yyyy-MM-dd，不足2位用0填充
println(f"$year-$month%02d-$day%02d")

-- raw插值器
println("a\nb\tc")
println(raw"a\nb\tc")
println("""a\nb\tc""")
```

# 控制结构和函数

## if表达式

if表达式有返回值

```scala
-- if 语句有返回值
val x = 10
val s = if (x > 0)
 1
else
 -1

-- 多分支if 语句
val s = if (x==0)
 0
else if (x > 1)
 1
else
 0
-- 如果返回的类型不一致就返回公共的父类
val s = if (x > 0)
 "positive"
else
 -1

-- 缺省 else 语句；s1/s2的返回类型不同
val s1 = if (x > 0) 1 等价 val s1 = if (x > 0) 1 else ()
val s2 = if (x > 0) "positive" 等价 val s2 = if (x > 0) "positive" else
()
```

## for表达式

```scala
-- 基本结构。使用to实现左右两边闭合的访问区间
for (i <- 1 to 10) {
 println(s"i = $i")
}

-- 基本结构。使用until实现左闭右开的访问区间
for (i <- 1 until 10) {
 println(s"i = $i")
}

-- 双重循环。条件之间使用分号分隔
for (i <- 1 until 5; j <- 2 until  5){
 println(i * j )
}

-- 使用变量
for (i <- 1 to 3 ;j = 4-i){
 println(i * j )
}

-- 守卫语句。增加 if 条件语句
for (i <- 1 to 10; j <- 1 to 10 if i==j){
 println(s"i * j = $i * $j = ${i * j}")
}

-- 使用 yield 接收返回的结果，这种形式被称为for推导式
val result = for (i <- 1 to 10) yield i

-- 使用大括号将生成器、守卫、定义包含在其中；并以换行的方式来隔开它们
for { i <- 1 to 3
 from = 4 - i
 j <- from to 3 }
 println(s"i = $i; j = $j")
```

## while循环

Scala提供了与 Java 类似的while和do...while循环。while语句的本身没有任何返回值类型，即while语句的返回结果是Unit类型的 () 。

Scala内置控制结构特地去掉了 break 和 continue。

特殊情况下如果需要终止循环，可以有以下三种方式：

1. 使用Boolean类型的控制变量
2. 使用return关键字
3. 使用breakable和break，需要导入scala.util.control.Breaks包

```scala
// while循环
  var flag = true
  var result = 0
  var n = 0
  while (flag) {
   result += n
   n += 1
   println("res = " + result)
      println("n = " + n)
   if (n == 10) {
    flag = false
  }
 }
  // for循环
  var flag = true
  var res = 0
  for (i <- 0 until 10 if flag) {
   res += i
   println("res = " + res )
   if (i == 5) flag = false
 }
/**
  * 1 + 2 + 3 + 4
  *
  * @return
  */
 def addInner() {
  for (i <- 0 until 10) {
   if (i == 5) {
    return
   }
   res += i
   println("res = " + res)
  }
 }
 def main(args: Array[String]): Unit = {
  addInner()
}
def main(args: Array[String]): Unit = {
  // 需要导包
  import scala.util.control.Breaks._
  var res = 0
  breakable {
   for (i <- 0 until 10) {
    if (i == 5) {
     break
   }
    res += i
  }
 }
  println("res = " + res)
}
```

## 函数

![image-20210614183323633](.\图片\函数.jpg)

## 懒值

当 val 被声明为lazy时(var不能声明为lazy)，它的初始化将被推迟，直到首次对此取值，适用于初始化开销较大的场景。

## 文件操作

导入scala.io.Source后，可引用Source中的方法读取文本文件的内容

```scala
import scala.io.{BufferedSource, Source}
object FileDemo {
 def main(args: Array[String]): Unit = {
  //注意文件的编码格式，如果编码格式不对，那么读取报错
  val file: BufferedSource = Source.fromFile("... ...", "GBK");
  val lines: Iterator[String] = file.getLines()
  for (line <- lines) {
   println(line)
 }
  
  //注意关闭文件
  file.close()
}
}

--读取网络资源
import scala.io.{BufferedSource, Source}
object FileDemo2 {
 def main(args: Array[String]): Unit = {
  val source: BufferedSource = Source.fromURL("http://www.baidu.com")
  val string: String = source.mkString
  println(string)
  source.close()
}
}
```

# 数组和元组

```scala
--数组定义
-- 长度为10的整型数组，初始值为0
val nums = new Array[Int](10)

-- 使用()访问数据元素；下标从0开始
nums(9) = 10

-- 长度为10的字符串数组，初始值为null
val strs = new Array[String](10)‘

-- 省略new关键字，定义数组，scala进行自动类型推断
val arrays = Array(1, 2, 3)

-- 快速定义数组，用于测试
val numsTest = (1 to 100).toArray

----变长数组
-- 长度按需要变换的数组ArrayBuffer。使用ArrayBuffer时，需要导包 import scala.collection.mutable.ArrayBuffer；
import scala.collection.mutable.ArrayBuffer
object VarArrayDemo {
 def main(args: Array[String]){
  // 定义一个空的可变长Int型数组。注意：后面要有小括号
  val nums =  ArrayBuffer[Int]()
  // 在尾端添加元素
  nums += 1
  // 在尾端添加多个元素
  nums += (2,3,4,5)
  // 使用++=在尾端添加任何集合
  nums ++= Array(6,7,8)
 // 这些操作符，有相应的 -= ，--=可以做数组的删减，用法同+=，++=
  
 // 使用append追加一个或者多个元素
  nums.append(1)
  nums.append(2,3)
  
 // 在下标2之前插入元素
  nums.insert(2,20)
  nums.insert(2,30,30)  
  // 移除最后2元素
  nums.trimEnd(2)
  // 移除最开始的一个或者多个元素
  nums.trimStart(1)
  // 从下标2处移除一个或者多个元素
  nums.remove(2)
  nums.remove(2,2)
  
}
}

```

## 数组操作

```
val arr = (1 to 10).toArray

-- 使用for推导式。注意：原来的数组并没有改变
val result1 = for (elem <- arr) yield if (elem % 2 == 0) elem * 2 else 0
val result2 = for (elem <- arr if elem %2 == 0) yield elem * 2

-- scala中的高阶函数
arr.filter(_%2==0).map(_*2)

// 取第一个元素
a1.head
// 取最后一个元素
a1.last
// 除了第一个元素，剩下的其他元素
a1.tail
// 除了最后一个元素，剩下其他元素
a1.init

// 数组常用算法
Array(1,2,3,4,5,6,7,8,9,10).sum //求和
Array(2,3,4).product //元素相乘
Array(1,2,3,4,5,6,7,8,9,10).max  //求最大值 
Array(1,2,3,4,5,6,7,8,9,10).min  //求最小值
Array(1,3,2,7,6,4,8,9,10).sorted  //升序排列

// max、min、sorted方法，要求数组元素支持比较操作
Array(1,2,3,4,5,4,3,2,1).map(_*2)
Array(1,2,3,4,5,4,3,2,1).reduce(_+_)
Array(1,2,3,4,5,4,3,2,1).distinct //数据去重
Array(1,2,3,4,5,4,3,2,1).length
Array(1,2,3,4,5,4,3,2,1).size
Array(1,2,3,4,5,4,3,2,1).indices //数据索引

// count计数，需要注意的是count中必须写条件
Array(1,2,3,4,5,4,3,2,1).count(_>3)
Array(1,2,3,4,5,4,3,2,1).count(_%2==0)

// filter 过滤数据，原始数组中的数据保持不变，返回一个新的数组
Array(1,2,3,4,5,4,3,2,1).filter(_>3)
Array(1,2,3,4,5,4,3,2,1).filterNot(_%2==0)


// 在REPL环境中输入数组名称即可打印数组元素，非常方便
// 在IDEA中，print(a) / print(a.toString)都不能打印数组元素
// 使用mkString / toBuffer 是打印数组元素简单高效的方法
Array(10,9,8,7,6,5,4,3,2,1).toString
Array(10,9,8,7,6,5,4,3,2,1).mkString(" & ")
Array(10,9,8,7,6,5,4,3,2,1).mkString("<", " & ", ">")
Array(10,9,8,7,6,5,4,3,2,1).toBuffer


// take取前4个元素；takeRight取后4个元素
//原始数组中的数据保持不变，返回一个新的数组
Array(1,2,3,4,5,6,7,8,9,10).take(4)
Array(1,2,3,4,5,6,7,8,9,10).takeRight(4)

// takeWhile 从左向右提取列表的元素，直到条件不成立（条件不成立时终止）
Array(1,2,3,4,5,6,1,2,3,4).takeWhile(_<5)

// drop 删除前4个元素；dropRight删除后4个元素；
// dropWhile删除元素，直到条件不成立
Array(1,2,3,4,5,6,7,8,9,10).drop(4)
Array(1,2,3,4,5,6,7,8,9,10).dropRight(4)
Array(1,2,3,4,5,6,1,2,3,4).dropWhile(_<5)

// 将数组分为前n个，与剩下的部分
Array(1,2,3,4,5,6,7,8,9,10).splitAt(4)

// 数组切片。取下标第2到第4的元素（不包括第5个元素）
// 返回结果：Array(2, 3, 4)
Array(0,1,2,3,4,5,6,7,8,9,10).slice(2,5)

// 拉链操作；a1,a2的长度不一致时，截取相同的长度
val a1 = Array("A","B","C")
val a2 = Array(1,2,3,4)
val z1 = a1.zip(a2)

// 拉链操作；a1,a2的长度不一致时，a1用 * 填充，a2用 -1 填充
val z2 = a1.zipAll(a2, "*", -1)
val z3 = a1.zipAll(a2, -1, "*")

// 用数组索引号填充
val z4 = a1.zipWithIndex

// unzip 的逆操作，拆分成2个数组
val (l1,l2) = z4.unzip

// unzip3拆分成3个数组
val (l1,l2,l3) = Array((1, "one", '1'),(2, "two", '2'),(3, "three", '3')).unzip3

// 用于数组的操作符(:+、+:、++)
// :+ 方法用于在尾部追加元素；+: 方法用于在头部追加元素；
// 备注：冒号永远靠近集合类型，加号位置决定元素加在前还是后；
// ++ 该方法用于连接两个集合(数组、列表等)，arr1 ++ arr2；
val a = (1 to 4).toArray
val b = (5 to 8).toArray

// 分别在集合头部、尾部增加元素；连接两个集合
val c = 10 +: a
val d = c :+ 9
val e = a ++ b

// 说明：上述的很多方法不仅仅对Array适用，一般情况下对其他集合类型同样适用。
val list = (1 to 10).toList
list.sum
list.max
list.take(4)
list.drop(4)

//数组排序
val nums = Array(1, 3, 2, 6, 4, 7, 8, 5)
println(nums.sorted.toBuffer) //升序
println(nums.sorted.reverse.toBuffer) //降序
println(nums.sortWith(_ > _).toBuffer) //降序
println(nums.sortWith(_ < _).toBuffer) //升序
```

# 类和对象

## 基础知识

在Scala中，类并不用声明为public；

1. Scala源文件中可以包含多个类，所有这些类都具有公有可见性；
2. val修饰的变量（常量），值不能改变，只提供getter方法，没有setter方法；
3. var修饰的变量，值可以改变，对外提供getter、setter方法；
4. 如果没有定义构造器，类会有一个默认的无参构造器；

```scala
class Person {
 // Scala中声明一个字段，必须显示的初始化，然后根据初始化的数据类型自动推断其类型，字段类型可
以省略
 var name = "jacky"
 
 // _ 表示一个占位符，编译器会根据变量的数据类型赋予相应的初始值
 // 使用占位符，变量类型必须指定
 // _ 对应的默认值:整型默认值0；浮点型默认值0.0；String与引用类型，默认值null; Boolean默
认值false
 var nickName: String = _
 var age=20
 // 如果赋值为null,则一定要加数据类型，因为不加类型, 该字段的数据类型就是Null类型
 // var address = null
 // 改为：
 var address: String = null
 // val修饰的变量不能使用占位符
 val num = 30
 // 类私有字段,有私有的getter方法和setter方法，
 // 在类的内部可以访问，其伴生对象也可以访问
 private var hobby: String = "旅游"
 // 对象私有字段,访问权限更加严格，只能在当前类中访问
 private[this] val cardInfo = "123456"
 //自定义方法
 def hello(message: String): Unit = {
  //只能在当前类中访问cardInfo
  println(s"$message,$cardInfo")
}
 //定义一个方法实现两数相加求和
 def addNum(num1: Int, num2: Int): Int = {
  num1 + num2
}
}


-- 类的实例化以及使用
object ClassDemo {
 def main(args: Array[String]): Unit = {
  //创建对象两种方式，这里都是使用的无参构造器来进行创建对象的
  val person = new Person()
  //创建类的对象时，小括号()可以省略
  val person1 = new Person
  //给类的属性赋值
  person.age = 50
  //注意：如果使用对象的属性加上 _= 给var修饰的属性进行重新赋值，其实就是调用age_=这个
setter方法
  person.age_=(20)
  //直接调用类的属性，其实就是调用getter方法
  println(person.age)
  //调用类中的方法
  person.hello("hello")
  val result = person.addNum(10, 20)
  println(result)
}
}
```

## 自定义getter和setter方法

对于 Scala 类中的每一个属性，编译后会有一个私有的字段和相应的getter、setter方法生成。

自定义变量的getter和setter方法需要遵循以下原则：

- 字段属性名以“_”作为前缀，如： _leg_
- getter方法定义为：def leg = _leg_
- setter方法定义为：def leg_=(newLeg: Int)

```scala
class Dog {
 private var _leg = 0
 //自定义getter方法
 def leg = _leg
 //自定义setter方法
 def leg_=(newLeg: Int) {
  _leg = newLeg
}
}
// 使用自定义getter和setter方法
val dog = new Dog
dog.leg_=(4)
println(dog.leg)
```

##　Bean属性

JavaBean规范把Java属性定义为一堆getter和setter方法。

类似于Java，当将Scala字段标注为 @BeanProperty时，getFoo和setFoo方法会自动生成。

使用@BeanProperty并不会影响Scala自己自动生成的getter和setter方法。

在使用时需要导入包scala.beans.BeanProperty

```scala
import scala.beans.BeanProperty
class Teacher {
  @BeanProperty var name:String = _
}
object BeanDemo{
 def main(args: Array[String]): Unit = {
  val tea: Teacher = new Teacher
  tea.name = "zhagnsan"
  tea.setName("lisi")   //BeanProperty生成的setName方法
  println(tea.getName)  //BeanProperty生成的getName方法
}
}
```

## 构造器

主构造器和辅助构造器。

## 对象和伴生对象

Scala并没有提供Java那样的静态方法或静态字段；

可以采用object关键字实现单例对象，具备和Java静态方法同样的功能；

使用object语法结构【object是Scala中的一个关键字】达到静态方法和静态字段的目的；对象本质上可以拥有类的所有特性，除了不能提供构造器参数；

对于任何在Java中用单例对象的地方，在Scala中都可以用object实现：

- 作为存放工具函数或常量的地方
- 高效地共享单个不可变实例



Scala中的单例对象具有如下特点：

- 1、创建单例对象不需要使用new关键字
- 2、object中只有无参构造器
- 3、主构造代码块只能执行一次，因为它是单例的



### 伴生对象

当单例对象与某个类具有相同的名称时，它被称为这个类的“伴生对象”；

类和它的伴生对象必须存在于同一个文件中，而且可以相互访问私有成员（字段和方法）

```scala
class ClassObject {
 val id = 1
 private var name = "lagou"
 def printName(): Unit ={
  //在ClassObject类中可以访问伴生对象ClassObject的私有字段
  println(ClassObject.CONSTANT + name )
}
}
object ClassObject{
 //伴生对象中的私有字段
 private val CONSTANT = "汪汪汪"
 def main(args: Array[String]) {
  val p = new ClassObject
  //访问伴生类的私有字段name
  p.name = "123"
  p.printName()
}
}
```

**object 中有一个非常重要的特殊方法 -- apply方法；**

1. **apply方法通常定义在伴生对象中**，目的是通过伴生类的构造函数功能，来实现伴生对象的构造函数功能；
2. 通常我们会在类的伴生对象中定义apply方法，**当遇到类名(参数1,...参数n)时apply方法会被调用**；
3. 在创建伴生对象或伴生类的对象时，通常不会使用new class/class() 的方式，而是**直接使用class()隐式的调用伴生对象的 apply 方法**，这样会让对象创建的更加简洁；

```scala
//class Student为伴生类
class Student(name: String, age: Int) {
 private var gender: String = _
 def sayHi(): Unit ={
  println(s"大家好，我是$name,$gender 生")
}
}
//object Student是class class的伴生对象
object Student {
 //apply方法定义在伴生对象中
 def apply(name: String, age: Int): Student = new Student(name, age)
 def main(args: Array[String]): Unit = {
  //直接利用类名进行对象的创建，这种方式实际上是调用伴生对象的apply方法实现的
  val student=Student("jacky",30)
  student.gender="男"
  student.sayHi()
}
```

构造器的执行顺序，先父类后子类。

# 特质

可以当做接口来使用。

trait特质中可以定义抽象方法，与抽象类中的方法一样，只要不给出方法的具体实现即可。

类可以使用extends关键字集成特质。

## Ordered和Ordering

在Java中对象的比较有两个接口，分别是Comparable和Comparator。它们之间的区别在于：

实现Comparable接口的类，重写compareTo()方法后，其对象自身就具有了可比较性；

实现Comparator接口的类，重写了compare()方法后，则提供一个第三方比较器，用于比较两个对象



# 模式匹配

Scala没有Java中的switch case，它有一个更加强大的模式匹配机制，可以应用到很多场合。

Scala的模式匹配可以匹配各种情况，比如变量的类型、集合的元素、有值或无值。

模式匹配的基本语法结构：变量 match { case 值 => 代码 }

模式匹配match case中，只要有一个case分支满足并处理了，就不会继续判断下一个case分支了，不
需要使用break语句。这点与Java不同，Java的switch case需要用break阻止。如果值为下划线，则代
表不满足以上所有情况的时候如何处理。

模式匹配match case最基本的应用，就是对变量的值进行模式匹配。match是表达式，与if表达式一
样，是有返回值的



## 守卫式匹配

```scala
/ 所谓守卫就是添加if语句
object MatchDemo {
 def main(args: Array[String]): Unit = {
  //守卫式
  val character = '*'
  val num = character match {
   case '+' => 1
   case '-' => 2
   case _ if character.equals('*') => 3
   case _ => 4
 }
  println(character + " " + num)
}
```



## 匹配数组、元组、集合

```scala
def main(args: Array[String]): Unit = {
 val arr = Array(0, 3, 5)
 //对Array数组进行模式匹配，分别匹配：
 //带有指定个数元素的数组、带有指定元素的数组、以某元素开头的数组
 arr match {
  case Array(0, x, y) => println(x + " " + y)
  case Array(0) => println("only 0")
  //匹配数组以1开始作为第一个元素
  case Array(1, _*) => println("1 ...")
  case _ => println("something else")
}
 val list = List(3, -1)
 //对List列表进行模式匹配，与Array类似，但是需要使用List特有的::操作符
 //构造List列表的两个基本单位是Nil和::，Nil表示为一个空列表
 //tail返回一个除了第一元素之外的其他元素的列表
 //分别匹配：带有指定个数元素的列表、带有指定元素的列表、以某元素开头的列表
 list match {
  case x :: y :: Nil => println(s"x: $x y: $y")
  case 0 :: Nil => println("only 0")
  case 1 :: tail => println("1 ...")
  case _ => println("something else")
}
 val tuple = (1, 3, 7)
 tuple match {
  case (1, x, y) => println(s"1, $x , $y")
  case (_, z, 5) => println(z)
  case  _ => println("else")
}
}
```

## 样例类

case class样例类是Scala中特殊的类。当声明样例类时，以下事情会自动发生：

- 主构造函数接收的参数通常不需要显式使用var或val修饰，Scala会自动使用val修饰
- 自动为样例类定义了伴生对象，并提供apply方法，不用new关键字就能够构造出相应的对象
- 将生成toString、equals、hashCode和copy方法，除非显示的给出这些方法的定义
- 继承了Product和Serializable这两个特质，也就是说样例类可序列化和可应用Product的方法

case class是多例的，后面要跟构造参数，case object是单例的。

此外，case class样例类中可以添加方法和字段，并且可用于模式匹配

```scala
class Amount
//定义样例类Dollar，继承Amount父类
case class Dollar(value: Double) extends Amount
//定义样例类Currency，继承Amount父类
case class Currency(value: Double, unit: String) extends Amount
//定义样例对象Nothing，继承Amount父类
case object Nothing extends Amount
object CaseClassDemo {
 def main(args: Array[String]): Unit = {
  judgeIdentity(Dollar(10.0))
  judgeIdentity(Currency(20.2,"100"))
  judgeIdentity(Nothing)
}
 //自定义方法，模式匹配判断amt类型
 def judgeIdentity(amt: Amount): Unit = {
  amt match {
   case Dollar(value) => println(s"$value")
   case Currency(value, unit) => println(s"Oh noes,I got $unit")
   case Nothing => println("Oh,GOD!")
 }
}
}
```

## Option与模式匹配

Scala Option选项类型用来表示一个值是可选的，有值或无值。

Option[T] 是一个类型为 T 的可选值的容器，可以通过get()函数获取Option的值。如果值存在，

Option[T] 就是一个 Some。如果不存在，Option[T] 就是对象 None 。

Option通常与模式匹配结合使用，用于判断某个变量是有值还是无值。



# 函数

![image-20210614191527676](.\图片\函数定义.jpg)



val 函数名: (参数类型1，参数类型2) => (返回类型) = 函数字面量

val 函数名 = 函数字面量

函数字面量:(参数1：类型1，参数2：类型2)  =>  函数体

val 函数名 = (参数1：类型1，参数2：类型2)  =>  函数体

```scala
val add1 = (x: Int) => x + 1
val add2 = (x: Int, y: Int) => x + y
val add3 = (x: Int, y: Int, z: Int) => x + y + z
val add4 = (x: Int, y: Int, z: Int) => (x + y, y + z)
```

严格的说：使用 val 定义的是函数(function)，使用 def 定义的是方法(method)。二者在语义上的区别

很小，在绝大多数情况下都可以不去理会它们之间的区别，但是有时候有必要了解它们之间的不同。

Scala中的方法与函数有以下区别：

- Scala 中的方法与 Java 的类似，方法是组成类的一部分
- Scala 中的函数则是一个完整的对象。Scala 中用 22 个特质(从 Function1 到 Function22)抽象出了函数的概念
- Scala 中用 val 语句定义函数，def 语句定义方法



- 方法不能作为单独的表达式而存在，而函数可以；
- 函数必须要有参数列表，而方法可以没有参数列表；
- 方法名是方法调用，而函数名只是代表函数对象本身；
- 在需要函数的地方，如果传递一个方法，会自动把方法转换为函数

```scala
scala> def f1 = double _  //注意：方法名与下划线之间有一个空格
f1: Int => Int
scala> f1
res21: Int => Int = <function1>
```

##  高阶函数

**高阶函数：接收一个或多个函数作为输入 或 输出一个函数。**

函数的参数可以是变量，而函数又可以赋值给变量，由于函数和变量地位一样，所以函数参数也可以是函数；

常用的高阶函数：map、reduce、flatMap、foreach、filter、count … … (接收函数作为参数)



## 闭包

闭包是一种函数，一种比较特殊的函数，它和普通的函数有很大区别

```scala
  // 普通的函数
  val addMore1 = (x: Int) => x + 10
  // 外部变量，也称为自由变量
  var more = 10
  // 闭包
  val addMore2 = (x: Int) => x + more
// 调用addMore1函数
println(addMore1(5))
  // 每次addMore2函数被调用时，都会去捕获外部的自由变量
  println(addMore2(10))
  more = 100
  println(addMore2(10))
  more = 1000
  println(addMore2(10))
```

闭包是在其上下文中引用了自由变量的函数；

闭包引用到函数外面定义的变量，定义这个函数的过程就是将这个自由变量捕获而构成的一个封闭的函数，也可理解为”把函数外部的一个自由变量关闭进来“。

何为闭包？需满足下面三个条件：
1、闭包是一个函数
2、函数必须要有返回值
3、返回值依赖声明在函数外部的一个或多个变量，用Java的话说，就是返回值和定义的全局变量有关

## 柯里化

函数编程中，接收多个参数的函数都可以转化为接收单个参数的函数，这个转化过程就叫柯里化(Currying)。

Scala中，柯里化函数的定义形式和普通函数类似，区别在于柯里化函数拥有多组参数列表，每组参数用小括号括起来。

Scala API中很多函数都是柯里化的形式。

## 部分应用函数

部分应用函数（Partial Applied Function）也叫偏应用函数，与偏函数从名称上看非常接近，但二者之间却有天壤之别。

**部分应用函数是指缺少部分（甚至全部）参数的函数。**

如果一个函数有n个参数, 而为其提供少于n个参数, 那就得到了一个部分应用函数。‘

```
// 定义一个函数
def add(x:Int, y:Int, z:Int) = x+y+z
// Int不能省略
def addX = add(1, _:Int, _:Int)
addX(2,3)
addX(3,4)
def addXAndY = add(10, 100, _:Int)
addXAndY(1)
def addZ = add(_:Int, _:Int, 10)
addZ(1,2)
// 省略了全部的参数，下面两个等价。第二个更常用
def add1 = add(_: Int, _: Int, _: Int)
def add2 = add _
```



##　偏函数

偏函数（Partial Function）之所以“偏”，原因在于它们并不处理所有可能的输入，而只处理那些能与至少一个 case 语句匹配的输入；

在偏函数中只能使用 case 语句，整个函数必须用大括号包围。这与普通的函数字面量不同，普通的函数字面量可以使用大括号，也可以用小括号；

被包裹在大括号中的一组case语句是一个偏函数，是一个并非对所有输入值都有定义的函数；

Scala中的Partial Function是一个trait，其类型为PartialFunction[A,B]，表示：接收一个类型为A的参数，返回一个类型为B的结果。

```scala
// 1、2、3有对应的输出值，其它输入打印 Other
val pf: PartialFunction[Int, String] = {
 case 1 => "One"
 case 2 => "Two"
 case 3 => "Three"
 case _=> "Other"
}
pf(1) // 返回: One
pf(2) // 返回: Two
pf(5) // 返回: Other

需求：过滤List中的String类型的元素，并将Int类型的元素加1。
通过偏函数实现上述需求
package cn.lagou.edu.scala.section3
object PartialFunctionDemo1 {
 def main(args: Array[String]): Unit = {
  // PartialFunction[Any, Int]: 偏函数接收的数据类型是Any，返回类型为Int
  val partialFun = new PartialFunction[Any, Int] {
   // 如果返回true，就调用 apply 构建实例对象；如果返回false，过滤String数据
   override def isDefinedAt(x: Any): Boolean = {
    println(s"x = $x")
    x.isInstanceOf[Int]
  }
   // apply构造器，对传入值+1，并返回
   override def apply(v1: Any): Int = {
    println(s"v1 = $v1")
       v1.asInstanceOf[Int] + 1
  }
 }
  val lst = List(10, "hadoop", 20, "hive", 30, "flume", 40, "sqoop")
  // 过滤字符串，对整型+1
  // collect通过执行一个并行计算（偏函数），得到一个新的数组对象
  lst.collect(partialFun).foreach(println)
  // 实际不用上面那么麻烦
  lst.collect{case x: Int => x+1}.foreach(println)
}
}
```

# 集合

主要内容：

- 1、Scala中的可变和不可变集合
- 2、集合的三大类：Seq、Set、Map
- 3、集合的常用算子
- 4、Scala与Java之间的集合转换



![image-20210614192435752](.\图片\集合.jpg)

![image-20210614192459221](.\图片\不可变集合.jpg)

![image-20210614192554976](.\图片\可变集合.jpg)

# 隐式机制





# Actor

