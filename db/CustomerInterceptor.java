package cn.lagou.dw.flume.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.interceptor.Interceptor;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerInterceptor implements Interceptor {
    @Override
    public void initialize() {

    }

    @Override
    public Event intercept(Event event) {
        String envntBody = new String(event.getBody());
        Map<String, String> headers = event.getHeaders();
        String[] bodyArr = envntBody.split("\\s");
        try {
            String jsonStr = bodyArr[7];
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            String timeStr = jsonObject.getJSONObject("app_active").getString("time");
            long timestamp = Long.parseLong(timeStr);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Instant instant = Instant.ofEpochMilli(timestamp);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            String date = dateTimeFormatter.format(localDateTime);
            headers.put("logtime", bodyArr[0]);
            event.setHeaders(headers);
            event.setBody(jsonStr.getBytes());
        }catch (Exception e){
            headers.put("logtime","unknown");
            event.setHeaders(headers);
            event = null;
        }
        return event;
    }

    @Override
    public List<Event> intercept(List<Event> list) {
        List<Event> rs = new ArrayList<>();
        for (Event event : list) {
            Event intercept = intercept(event);
            if(intercept!=null){
                rs.add(intercept);
            }
        }
        return rs;
    }

    @Override
    public void close() {

    }
    public static class Builder implements Interceptor.Builder{

        @Override
        public Interceptor build() {
            return new CustomerInterceptor();
        }

        @Override
        public void configure(Context context) {

        }
    }

    @Test
    public void testJunit(){
        String str = "2020-08-20 11:56:00.365 [main] INFO  com.lagou.ecommerce.AppStart - {\"app_active\":{\"name\":\"app_active\",\"json\":{\"entry\":\"1\",\"action\":\"0\",\"error_code\":\"0\"},\"time\":1595266507583},\"attr\":{\"area\":\"菏泽\",\"uid\":\"2F10092A1\",\"app_v\":\"1.1.2\",\"event_type\":\"common\",\"device_id\":\"1FB872-9A1001\",\"os_type\":\"0.01\",\"channel\":\"YO\",\"language\":\"chinese\",\"brand\":\"Huawei-9\"}}";
        Map<String, String> map = new HashMap<>();
        Event event = new SimpleEvent();
        event.setHeaders(map);
        event.setBody(str.getBytes(StandardCharsets.UTF_8));

        CustomerInterceptor customerInterceptor = new CustomerInterceptor();
        Event intercept = customerInterceptor.intercept(event);
        Map<String, String> headers = intercept.getHeaders();
        System.out.println(JSON.toJSONString(headers));
    }
}
