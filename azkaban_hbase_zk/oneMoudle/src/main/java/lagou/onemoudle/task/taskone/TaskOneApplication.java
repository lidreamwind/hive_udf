package lagou.onemoudle.task.taskone;

import lagou.onemoudle.task.taskone.zk.DbPools;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@SpringBootApplication
public class TaskOneApplication {

    public static void main(String[] args) throws PropertyVetoException, SQLException {
//        SpringApplication.run(TaskOneApplication.class, args);
        DbPools instance = DbPools.getInstance("linux121:2181,linux122:2181");
        testDbPools(instance);
        while (true){
            testDbPools(instance);
        }
    }

    public static void testDbPools(DbPools instance) throws SQLException {
        Connection connection = instance.getConnection();
        ResultSet resultSet = connection.prepareStatement("select user()").executeQuery();
        while (resultSet.next()){
            System.out.println("---------------------> "+ resultSet.getString(1));
        }
    }

}
