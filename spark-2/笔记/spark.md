





# PariRDD聚合操作



 groupByKey、reduceByKey、foldByKey、aggregateByKey

 combineByKey是底层实现 combineByKeyWithClassTag





foldByKey可以定义初值。

aggregateByKey可以定义初始值，定义分区内合并和分区间的数据合并。



# ReduceByKey和GroupByKey & sortByKey

sortBeKey



# Join与Action

cogroup

join

leftOuterJoin

rightOutJoin

fullOuterJoin



collectAsMap

countByKey

lookup(key)



# 输入与输出

## 文本文件

​	textFile(string)  --- wholeTextFiles

​	saveAsTextFile(String)

## csv文件

​	读取CSV（Comma-Separated Values)  / TSV （Tab-Separated Values) 数据和读取JSON数据相似，都需要先把文件当成普通文件文件来读取数据，然后通过每一行进行解析实现对CSV文件的读取。



## JSON文件

![image-20210628213110692](.\图片\spark-json处理方式)

## SequenceFile

SequenceFile文件是Hadoop用来存储二进制形式的key-value对而设计的一种平面文件（Flat File）。

Spark有专门读取SequenceFile的接口。在SparkContext中，可以调用sequenceFile[keyclass,valueclass]；

调用saveAsSequenceFile（path)来保存pariRDD，系统将键和值自动转化为Writable类型。

## 对象文件

对象文件是讲对象序列化后保存的文件，采用java的序列化机制。

通过objectFile[k,v]（path）接受一个路径，读取对象文件，返回对应的RDD，也可以通过调用saveAsObjectFile()实现对对象文件的输出。



## JDBC



# 程序

sc.setLogLevel("WARN")

spark-submit --master local[*] --class cn.lagou.sparkcore.WordCount  orginal-wordcount.jar   /wcinput/*  

spark-submit --master local[*] --class cn.lagou.sparkcore.WordCount  orginal-wordcount.jar   /wcinput/  



this.getClass.getCanonicalName.init   --获取类名



## 圆周率



# 广告数据统计

java8中日期线程不安全，可以使用

joda-time，具有线程不可变，实例无法修改，线程安全。、

arr.combinations(2).foreach(println)

## 共同好友方法


