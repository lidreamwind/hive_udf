package lagou.onemoudle.task.taskone.redis;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TestRedis {
    public static void main(String[] args) {
        // 获取Jedis的配置
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // jedis的集群配置
        HashSet<HostAndPort> jedisHost = new HashSet<>();
        // 设置主机信息
        jedisHost.add(new HostAndPort("192.168.233.166",7001));
        jedisHost.add(new HostAndPort("192.168.233.166",7002));
        jedisHost.add(new HostAndPort("192.168.233.166",7003));
        jedisHost.add(new HostAndPort("192.168.233.166",7004));
        jedisHost.add(new HostAndPort("192.168.233.166",7005));
        jedisHost.add(new HostAndPort("192.168.233.166",7006));
        jedisHost.add(new HostAndPort("192.168.233.166",7007));
        jedisHost.add(new HostAndPort("192.168.233.166",7008));

        JedisCluster jcd = new JedisCluster(jedisHost, jedisPoolConfig);
        HashMap<String, String> rs = new HashMap<>();
        rs.put("name","lph");
        rs.put("age","20");
        rs.put("sex","1");
        //插入
        jcd.hset("hashMap",rs);
        System.out.println("插入成功...");
        System.out.println("开始从redis获取数据....");
        Map<String, String> hashMap = jcd.hgetAll("hashMap");
        for (Map.Entry<String, String> stringStringEntry : hashMap.entrySet()) {
            System.out.println(stringStringEntry.getKey() +" = " + stringStringEntry.getValue());
        }
        jcd.close();

    }
}
