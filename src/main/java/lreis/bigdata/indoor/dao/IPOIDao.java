package lreis.bigdata.indoor.dao;

import lreis.bigdata.indoor.vo.PositioningPoint;
import lreis.bigdata.indoor.vo.SemStop;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by dq on 4/29/16.
 */
public interface IPOIDao {

    boolean insertPOI(PositioningPoint positioningPoint) throws IOException;

    @NotNull
    /**
     * return a list of cells the mac has been , maybe and allow return to a cell on second time ,so use a list.
     *
     * @param mac
     * @param beginTimeStamp
     * @param endTimeStamp
     * @return
     * @throws IOException
     */
    List<SemStop> getStops(String mac, Long beginTimeStamp, Long endTimeStamp) throws IOException, SQLException, ClassNotFoundException;

    List<SemStop> getStops(String mac) throws SQLException, IOException, ClassNotFoundException;

    @NotNull
    List<PositioningPoint> getTraceByMac(String mac, Long beginTimeStamp, Long endTimeStamp) throws SQLException, IOException;


    void close() throws IOException, SQLException, ClassNotFoundException;
}
