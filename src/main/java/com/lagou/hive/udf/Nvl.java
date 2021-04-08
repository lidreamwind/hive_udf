package com.lagou.hive.udf;

import org.apache.hadoop.hive.ql.exec.UDF;
import org.apache.hadoop.io.Text;

public class Nvl extends UDF {
    public Text evaluate(Text x,Text y){
        if(x == null || x.toString().trim().length()==0){
            return y;
        }
        return x;
    }
}
