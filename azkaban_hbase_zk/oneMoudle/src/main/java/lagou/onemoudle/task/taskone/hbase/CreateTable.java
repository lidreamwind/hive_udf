package lagou.onemoudle.task.taskone.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 1. 使用Hbase相关API创建一张结构如上的表
 */
public class CreateTable {
    public static void main(String[] args) throws IOException {
        // 创建表
//         createTable();
        // 初始化数据
//         initDate();
        // 删除数据，用户uid1的用户uid2
        deleteData("uid1","uid2");
    }
    /**
     * 创建用户表
     */
    public static void createTable() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum","linux121,linux122");
        conf.set("hbase.zookeeper.property.clientPort","2181");
        Connection conn = ConnectionFactory.createConnection(conf);

        HBaseAdmin admin = (HBaseAdmin) conn.getAdmin();
        //设置表描述器
        HTableDescriptor table = new HTableDescriptor(TableName.valueOf("tbs"));
        // 列族描述器
        table.addFamily(new HColumnDescriptor("friends"));
        Path path=new Path("hdfs://linux121:9000/jar/hbase_observer-1.0-SNAPSHOT.jar");
        table.setValue("COPROCESSOR$1",path.toString()+"|"+"lagou.hbase.DeleteTrigger|"+ Coprocessor.PRIORITY_USER);
        admin.createTable(table);
        System.out.println("创建表成功！");
        admin.close();
        conn.close();
    }
    /**
     * 初始化数据
     */
    public static void initDate() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum","linux121,linux122");
        conf.set("hbase.zookeeper.property.clientPort","2181");
        Connection conn = ConnectionFactory.createConnection(conf);
        Table tbs = conn.getTable(TableName.valueOf("tbs"));
        //插入用户uid1
        Put uid1 = new Put(Bytes.toBytes("uid1"));
        uid1.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid2"),Bytes.toBytes("uid2"));
        uid1.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid3"),Bytes.toBytes("uid3"));
        uid1.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid4"),Bytes.toBytes("uid4"));
        uid1.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid5"),Bytes.toBytes("uid5"));

        //插入用户uid2
        Put uid2 = new Put(Bytes.toBytes("uid2"));
        uid2.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid1"),Bytes.toBytes("uid1"));
        uid2.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid3"),Bytes.toBytes("uid3"));
        uid2.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid4"),Bytes.toBytes("uid4"));
        uid2.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid5"),Bytes.toBytes("uid5"));
        //插入用户uid3
        Put uid3 = new Put(Bytes.toBytes("uid3"));
        uid3.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid1"),Bytes.toBytes("uid1"));
        uid3.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid2"),Bytes.toBytes("uid2"));
        uid3.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid4"),Bytes.toBytes("uid4"));
        uid3.addColumn(Bytes.toBytes("friends"),Bytes.toBytes("uid5"),Bytes.toBytes("uid5"));

        ArrayList<Put> puts = new ArrayList<>();
        puts.add(uid1);
        puts.add(uid2);
        puts.add(uid3);

        tbs.put(puts);
        System.out.println("插入成功！");
        conn.close();
    }
    /**
     * 删除数据，使用协处理器
     * 删除uid用户为uid2的列
     */
    public static void deleteData(String uid,String user) throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum","linux121,linux122");
        conf.set("hbase.zookeeper.property.clientPort","2181");
        Connection conn = ConnectionFactory.createConnection(conf);

        Table tbs = conn.getTable(TableName.valueOf("tbs"));
        Delete delete = new Delete(Bytes.toBytes(uid));

        delete.addColumn(Bytes.toBytes("friends"),Bytes.toBytes(user));
        tbs.delete(delete);
        System.out.println("删除成功！");
        conn.close();
    }
}
