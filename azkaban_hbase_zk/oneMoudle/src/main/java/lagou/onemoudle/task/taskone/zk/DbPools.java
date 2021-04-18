package lagou.onemoudle.task.taskone.zk;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * 获取线程池对象,单例模式，避免过多线程访问
 * 线程池使用c3p0
 */
public class DbPools {
    // 单例对象
    private static DbPools dbPools;
    // 数据库连接池对象
    private volatile ComboPooledDataSource cbpds;
    // 连接池参数
    private HashMap<String, String> dbParams = new HashMap<>();
    // zk连接客户端
    private ZkClient zkClient;
    // zookeeper中的节点连接信息
    private String dbZkPath = "/db";
    private String zkString;

    //获取数据库连接
    public Connection getConnection() throws SQLException {
        Connection connection = this.cbpds.getConnection();
        return connection;
    }

    //首次初始化数据库链接信息
    private DbPools(String zkString) throws PropertyVetoException {
        // 获取zkClient连接客户端
        this.zkClient = new ZkClient(zkString);
        // 首次需要初始化,数据库连接参数
        this.getConfigZk();
        //初始化数据库连接池信息
        this.init();
        //监听zk
        this.addListonForZk();
        this.zkString = zkString;
    }
    // 获取单例对象
    public static DbPools getInstance(String zkString) throws PropertyVetoException {
        if(dbPools == null){
            synchronized (DbPools.class){
                if(dbPools == null){
                    dbPools = new DbPools(zkString);
                }
            }
        }
        return dbPools;
    }
    //获取zookeeper连接信息
    private void getConfigZk(){
        //若节点不存在，则创建默认值
        if(!this.zkClient.exists(dbZkPath)){
            //创建连接信息
            zkClient.createPersistent("/db/url", true);
            zkClient.writeData("/db/url", "jdbc:mysql://linux123:3306/hue");
            dbParams.put("url","jdbc:mysql://linux123:3306/hue");
            //创建用户连接密码
            zkClient.createPersistent("/db/passwd");
            zkClient.writeData("/db/passwd", "root");
            dbParams.put("passwd","root");
            //创建连接用户
            zkClient.createPersistent("/db/user");
            zkClient.writeData("/db/user", "root");
            dbParams.put("user","root");
            //创建连接驱动
            zkClient.createPersistent("/db/driver");
            zkClient.writeData("/db/driver", "com.mysql.jdbc.Driver");
            dbParams.put("driver","com.mysql.jdbc.Driver");
        }else {
        //节点存在，则直接读取连接信息
            List<String> children = zkClient.getChildren(dbZkPath);
            for (String child : children) {
                //TODO....获取值的结果
                String value = zkClient.readData(dbZkPath + "/" + child);
                dbParams.put(child.toLowerCase(),value);
            }
        }
    }

    // 初始化数据库连接对象
    public void init() throws PropertyVetoException {
        // 数据库连接信息
        System.out.println("当前数据库连接信息是：url=="+dbParams.get("url")+"   driver=="+dbParams.get("driver")+"  " +
                "user=="+dbParams.get("user")+"     passwd=="+dbParams.get("passwd"));
        // 为空，重置连接池
        if(cbpds == null){
            cbpds = new ComboPooledDataSource();
        }else {
            cbpds.resetPoolManager();
        }
        cbpds.setJdbcUrl(dbParams.get("url"));
        cbpds.setDriverClass(dbParams.get("driver"));
        cbpds.setUser(dbParams.get("user"));
        cbpds.setPassword(dbParams.get("passwd"));
    }
    //监视zookeeper的连接信息
    //此方法只能引用某一个节点
    private void addListonForZk(){
        zkClient.subscribeDataChanges(dbZkPath + "/passwd", new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
                System.out.println("==================================> " + s);
                // 首次需要初始化,数据库连接参数
                dbParams.put("passwd",o.toString());
//                getConfigZk();
                //初始化数据库连接池信息
                init();
            }

            @Override
            public void handleDataDeleted(String s) throws Exception {
                System.out.println("-----------------> 节点删除，造成错误！");
            }
        });
    }

    // 释放资源
    @Override
    protected void finalize() throws Throwable {
        this.cbpds.close();
        this.zkClient.unsubscribeAll();
        this.zkClient.close();
        super.finalize();
    }
}
