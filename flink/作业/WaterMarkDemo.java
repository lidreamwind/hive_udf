package windows;

import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

// 任务2，3，4一起吧，正在运行
public class WaterMarkDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        // 设定water mark的周期
        env.getConfig().setAutoWatermarkInterval(1000);
        env.setParallelism(1);
        DataStreamSource<String> data = env.addSource(new SourceFunction<String>() {
            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                Integer i = 0;
                while (true) {
                    ctx.collect(String.valueOf(i+"数据源,"+System.currentTimeMillis()));
                    i++;
                    Thread.sleep(1000);
                }
            }

            @Override
            public void cancel() {

            }
        });
        //获取窗口
        SingleOutputStreamOperator<Tuple2<String, Long>> maped = data.map(new MapFunction<String, Tuple2<String, Long>>() {
            @Override
            public Tuple2<String, Long> map(String str) throws Exception {
                String[] strs = str.split(",");
                return new Tuple2<>(strs[0], Long.parseLong(strs[1]));
            }
        });

        SingleOutputStreamOperator<Tuple2<String, Long>> watermarks = maped.assignTimestampsAndWatermarks(new WatermarkStrategy<Tuple2<String, Long>>() {
            @Override
            public WatermarkGenerator<Tuple2<String, Long>> createWatermarkGenerator(WatermarkGeneratorSupplier.Context context) {
                return new WatermarkGenerator<Tuple2<String, Long>>() {
                    private long maxTimeStamp = Long.MIN_VALUE;

                    @Override
                    public void onEvent(Tuple2<String, Long> event, long l, WatermarkOutput watermarkOutput) {
                        //找出最大事件时间
                        maxTimeStamp = Math.max(maxTimeStamp, event.f1);
                    }

                    @Override
                    public void onPeriodicEmit(WatermarkOutput watermarkOutput) {
                        long maxOutOfOrderness = 3000;
                        watermarkOutput.emitWatermark(new Watermark(maxTimeStamp - maxOutOfOrderness));
                    }
                };
            }
        }.withTimestampAssigner(new SerializableTimestampAssigner<Tuple2<String, Long>>() {
            @Override
            public long extractTimestamp(Tuple2<String, Long> stringLongTuple2, long l) {
                return stringLongTuple2.f1;
            }
        }));
        watermarks.print();
        KeyedStream<Tuple2<String, Long>, String> keyed = watermarks.keyBy(value -> value.f0);

        WindowedStream<Tuple2<String, Long>, String, TimeWindow> windowedStream = keyed.window(TumblingEventTimeWindows.of(Time.seconds(5)));
        SingleOutputStreamOperator<String> rs = windowedStream.apply(new WindowFunction<Tuple2<String, Long>, String, String, TimeWindow>() {
            @Override
            public void apply(String s, TimeWindow window, Iterable<Tuple2<String, Long>> input, Collector<String> out) throws Exception {
                String key = s;
                ArrayList<Long> list = new ArrayList<>();
                Iterator<Tuple2<String, Long>> iterator = input.iterator();
                while (iterator.hasNext()) {
                    Tuple2<String, Long> next = iterator.next();
                    list.add(next.f1);
                }
                Collections.sort(list);
                String result = "key:" + key + "......." + "list.size:" + list.size() + "..... list:" + list.get(0);
            }
        });
        rs.print();

        env.execute("sss");
    }
}
