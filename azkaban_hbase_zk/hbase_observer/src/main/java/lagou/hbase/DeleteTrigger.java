package lagou.hbase;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.ByteArrayComparable;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class DeleteTrigger extends BaseRegionObserver {
    //predelete
    //postdelete:通过判断数据是否已经删除来跳出递归

    @Override
    public void postDelete(ObserverContext<RegionCoprocessorEnvironment> e, Delete delete, WALEdit edit, Durability durability) throws IOException {
        final HTableInterface relation = e.getEnvironment().getTable(TableName.valueOf("tbs"));
        final byte[] rowkeyUid = delete.getRow();//获取rowkey
        //获取到所有的cell对象
        final NavigableMap<byte[], List<Cell>> familyCellMap = delete.getFamilyCellMap();
        final Set<Map.Entry<byte[], List<Cell>>> entries = familyCellMap.entrySet();
        for (Map.Entry<byte[], List<Cell>> entry : entries) {
            System.out.println(Bytes.toString(entry.getKey()));//列族信息
            final List<Cell> cells = entry.getValue();
            for (Cell cell : cells) {
                final byte[] rowkey = CellUtil.cloneRow(cell);//rowkey信息
                final byte[] column = CellUtil.cloneQualifier(cell);//列信息
                //验证删除的目标数据是否存在，存在则执行删除否则不执行,必须有此判断否则造成协处理器被循环调用耗尽资源
                final boolean flag = relation.exists(new Get(column).addColumn(Bytes.toBytes("friends"), rowkey));
                if(flag){
                    final Delete myDelete = new Delete(column).addColumn(Bytes.toBytes("friends"), rowkey);
                    relation.delete(myDelete);
                }
            }
        }
    }
}