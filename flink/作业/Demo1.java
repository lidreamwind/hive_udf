package table;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import static org.apache.flink.table.api.Expressions.$;

// flink table
public class Demo1 {
    public static void main(String[] args) throws Exception {
        // env
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //table
        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .inStreamingMode()
                .useBlinkPlanner()
                .withBuiltInCatalogName("default_catalog")
                .withBuiltInDatabaseName("default_database")
                .build();
        StreamTableEnvironment tEvn = StreamTableEnvironment.create(env, settings);

        // 生成数据
        DataStreamSource<Tuple2<String, Integer>> data = env.addSource(new SourceFunction<Tuple2<String, Integer>>() {
            @Override
            public void run(SourceContext<Tuple2<String, Integer>> sourceContext) throws Exception {
                Integer num = 0;
                while (true) {
                    num++;
                    sourceContext.collect(new Tuple2<>("name" + num, num));
                    Thread.sleep(1000);
                }
            }

            @Override
            public void cancel() {

            }
        });
        // 转成table类型
        Table table = tEvn.fromDataStream(data, $("name"), $("num"));
        // 查询--table api
        Table selectResult = table.select($("name"), $("num"))
                .filter($("num").mod(2).isEqual(0));

        // table转成dataReam
        DataStream<Tuple2<Boolean, Row>> tuple2DataStream = tEvn.toRetractStream(selectResult, Row.class);
//        tuple2DataStream.print();

        // table -sql
        tEvn.createTemporaryView("nameTable",data,$("name"),$("num"));
//        Table selectResult1 = tEvn.sqlQuery("select * from nameTable where mod(num,2)=0");
        Table selectResult1 = tEvn.sqlQuery("select * from nameTable where num%2=0");
        selectResult1.printSchema();
        // table转成dataReam
        DataStream<Tuple2<Boolean, Row>> tuple2DataStream1 = tEvn.toRetractStream(selectResult1, Row.class);
        tuple2DataStream1.print();


        env.execute();
    }
}
