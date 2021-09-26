package windows;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Random;

public class SlideWindowDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<String> data = env.addSource(new SourceFunction<String>() {
            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                Integer i = 0;
                while (true) {
                    ctx.collect(String.valueOf(i+"数据源"));
                    i++;
                    Thread.sleep(1000);
                }
            }

            @Override
            public void cancel() {

            }
        });
        //获取窗口
        SingleOutputStreamOperator<Tuple3<String, String, String>> mapred = data.map(new MapFunction<String, Tuple3<String, String, String>>() {
            @Override
            public Tuple3<String, String, String> map(String s) throws Exception {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                long time = System.currentTimeMillis();
                String dataTime = sdf.format(time);
                Random random = new Random();
                int randomNum = random.nextInt(5);
                return new Tuple3<>(s, dataTime, String.valueOf(randomNum));
            }
        });

        KeyedStream<Tuple3<String, String, String>, String> keybed = mapred.keyBy(value -> value.f0);

        // 按照时间划分窗口
        WindowedStream<Tuple3<String, String, String>, String, TimeWindow> timeWindow = keybed.timeWindow(Time.seconds(5),Time.seconds(2));
        SingleOutputStreamOperator<String> applyed = timeWindow.apply(new WindowFunction<Tuple3<String, String, String>, String, String, TimeWindow>() {
            @Override
            public void apply(String key, TimeWindow window, Iterable<Tuple3<String, String, String>> input, Collector<String> out) throws Exception {
                Iterator<Tuple3<String, String, String>> iterator = input.iterator();
                StringBuilder sb = new StringBuilder();
                while (iterator.hasNext()) {
                    Tuple3<String, String, String> next = iterator.next();
                    sb.append(next.f0 + "..." + next.f1 + "..." + next.f2);
                }
                String s1 = key + "..." + window.getStart() + "..." + sb;
                out.collect(s1);
            }
        });
        applyed.print();


        env.execute("sss");

    }
}
