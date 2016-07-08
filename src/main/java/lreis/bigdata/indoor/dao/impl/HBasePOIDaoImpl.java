package lreis.bigdata.indoor.dao.impl;


import lreis.bigdata.indoor.dao.IPOIDao;
import lreis.bigdata.indoor.vo.Building;
import lreis.bigdata.indoor.vo.POI;
import lreis.bigdata.indoor.vo.TraceNode;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by dq on 4/29/16.
 */
public class HBasePOIDaoImpl implements IPOIDao {

    protected final String tableName = "pois";
    protected final String idxTableName = "idx_mac";


    protected Connection conn;

    protected byte[] columnFamily = Bytes.toBytes("data");


    protected BufferedMutator poiTable = null;
    protected BufferedMutator idxTable = null;


    public HBasePOIDaoImpl(Connection conn) throws IOException {
        super();
        this.conn = conn;

        poiTable = this.conn.getBufferedMutator(TableName.valueOf(this.tableName));
        idxTable = this.conn.getBufferedMutator(TableName.valueOf(this.idxTableName));

    }

    public boolean insertPOI(POI poi) throws IOException {

        if (poi == null
                ) {
            return false;
        }

        String rowkey =
                POI.calRowkey(poi);
        if (rowkey == null) {
            return false;
        }


        byte[] bRow = Bytes.toBytes(rowkey);

        Put put = new Put(bRow);

        put.addColumn(this.columnFamily, Bytes.toBytes("time"), Bytes.toBytes(new Timestamp(poi.getTime() * 1000).toString()));
        put.addColumn(this.columnFamily, Bytes.toBytes("mac"), Bytes.toBytes(poi.getMac()));
        put.addColumn(this.columnFamily, Bytes.toBytes("floor"), Bytes.toBytes(poi.getFloorNum()));
        put.addColumn(this.columnFamily, Bytes.toBytes("x"), Bytes.toBytes((Integer.toString((int) (poi.getX() * 1000)))));
        put.addColumn(this.columnFamily, Bytes.toBytes("y"), Bytes.toBytes((Integer.toString((int) (poi.getY() * -1000)))));




        try {
            poiTable.mutate(put);
//            poiTable.flush();
//     poiTable.put(put);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // mac index
        String idxRow = rowkey.substring(14) + rowkey.substring(4, 14) + rowkey.substring(0, 4);
        this.add2Index(idxRow);
        return true;
    }


    @Override
    public List<TraceNode> getBeenToCellsByMac(String mac, Long beginTimeStamp, Long endTimeStamp) throws IOException, SQLException {

        if (mac == null || mac.equals("") || mac.length() != 12) {
            return null;
        }
        if (beginTimeStamp > endTimeStamp) {
            return null;
        }

        List<TraceNode> result = new ArrayList<TraceNode>();

        FilterList fl = new FilterList(FilterList.Operator.MUST_PASS_ALL);// must between beginTime and endTime

        BinaryPrefixComparator comp = new BinaryPrefixComparator(Bytes.toBytes(String.format("%s%d0000", mac, beginTimeStamp)));
        RowFilter filter = new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, comp);



        BinaryPrefixComparator comp2 = new BinaryPrefixComparator(Bytes.toBytes(String.format("%s%d0300", mac, endTimeStamp)));
        RowFilter filter2 = new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL, comp2);

        fl.addFilter(filter);
        fl.addFilter(filter2);


        Scan scan = new Scan();
        scan.setStartRow(Bytes.toBytes(String.format("%s%d0000", mac, beginTimeStamp)));
        scan.setStopRow(Bytes.toBytes(String.format("%s%d0300", mac, endTimeStamp)));

//        scan.setFilter(new WhileMatchFilter(fl));
        scan.setFilter(fl);

        scan.addFamily(this.columnFamily);
        Table table = this.conn.getTable(TableName.valueOf(this.idxTableName));
        ResultScanner resultScanner = table.getScanner(scan);

        TraceNode last = null;
        for (Iterator<Result> it = resultScanner.iterator(); it.hasNext(); ) {
            Result res = it.next();
            String row = new String(res.getRow());
            if (last == null || Integer.parseInt(row.substring(22)) != last.getPolygonNum()) {
                last.setExitTime(Long.parseLong(row.substring(12,22)));
                last = new TraceNode(Integer.parseInt(row.substring(22)), Long.parseLong(row.substring(12, 22)), Long.parseLong(row.substring(12, 22)));
                result.add(last);
            } else {
                last.setExitTime(Long.parseLong(row.substring(12, 22)));
            }
        }


        return result;
    }

    @Override
    public List<POI> getTraceByMac(String mac, Long beginTimeStamp, Long endTimeStamp) throws SQLException, IOException {

        if (mac == null || mac.equals("") || mac.length() != 12) {
            return null;
        }
        if (beginTimeStamp > endTimeStamp) {
            return null;
        }

        List<POI> res = new ArrayList<POI>();

        FilterList fl = new FilterList(FilterList.Operator.MUST_PASS_ALL);// must between beginTime and endTime

        BinaryPrefixComparator comp = new BinaryPrefixComparator(Bytes.toBytes(String.format("%s%d", mac, beginTimeStamp)));
        RowFilter filter = new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, comp);

        BinaryPrefixComparator comp2 = new BinaryPrefixComparator(Bytes.toBytes(String.format("%s%d", mac, endTimeStamp)));
        RowFilter filter2 = new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL, comp2);

        fl.addFilter(filter);
        fl.addFilter(filter2);


        Scan scan = new Scan();
//        scan.setFilter(fl);
        scan.setFilter(new WhileMatchFilter(fl));
        scan.addFamily(this.columnFamily);
        Table table = this.conn.getTable(TableName.valueOf(this.idxTableName));

        ResultScanner resultScanner = table.getScanner(scan);
        for (Iterator<Result> it = resultScanner.iterator(); it.hasNext(); ) {
            Result result = it.next();
            String row = new String(result.getRow());

            long time = Timestamp.valueOf(new String(result.getValue(Bytes
                            .toBytes
                                    ("data"),
                    Bytes
                            .toBytes
                                    ("time")))).getTime();
            int x = Integer.parseInt(new String(result.getValue(Bytes
                            .toBytes
                                    ("data"),
                    Bytes
                            .toBytes
                                    ("x"))));
            int y = Integer.parseInt(new String(result.getValue(Bytes
                            .toBytes
                                    ("data"),
                    Bytes
                            .toBytes
                                    ("y"))));

            String floor = new String(result.getValue(Bytes.toBytes("data"), Bytes
                    .toBytes
                            ("floor")));


            POI poi = new POI(mac, time, (float) x / 1000, (float) y / -1000, floor);
            poi.setCellIn(Building.getInstatnce().getCellByNum(Integer.parseInt(row
                    .substring(0, 4))));

            res.add(poi);

            Collections.sort(res, new POI());

        }


        return res;
    }


    private boolean add2Index(String row ) throws IOException {



        Put p = new Put(Bytes.toBytes(row));
        p.addColumn(Bytes.toBytes("data"),Bytes.toBytes(""),Bytes.toBytes(""));
        idxTable.mutate(p);
        return true;


    }


    @Override
    public void close() throws IOException {
        this.poiTable.close();
        this.idxTable.close();
        this.conn.close();;
    }


}
