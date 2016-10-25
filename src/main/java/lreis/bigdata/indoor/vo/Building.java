package lreis.bigdata.indoor.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Q on 2016/6/20.
 */
public class Building {
    static final String[] floorShps = new String[]{
//            "D:\\big_joy\\floors\\20010.dbf",
//            "D:\\big_joy\\floors\\20020.dbf",
//            "D:\\big_joy\\floors\\20030.dbf",
//            "D:\\big_joy\\floors\\20040.dbf"


            "/home/zdq/big_joy/floors/10010.dbf",
            "/home/zdq/big_joy/floors/10020.dbf",
            "/home/zdq/big_joy/floors/20010.dbf",
            "/home/zdq/big_joy/floors/20020.dbf",
            "/home/zdq/big_joy/floors/20030.dbf",
            "/home/zdq/big_joy/floors/20040.dbf",
            "/home/zdq/big_joy/floors/20050.dbf",


//            "/Users/dq/paper_relate/big_joy/floors/10010.dbf",
//            "/Users/dq/paper_relate/big_joy/floors/10020.dbf",
//            "/Users/dq/paper_relate/big_joy/floors/20010.dbf",
//            "/Users/dq/paper_relate/big_joy/floors/20020.dbf",
//            "/Users/dq/paper_relate/big_joy/floors/20030.dbf",
//            "/Users/dq/paper_relate/big_joy/floors/20040.dbf",


    };

    private static class BuildingHolder {
        private static final Building instance = new Building();

        static {
            for (String file : floorShps) {
//            building.addFloor(new Floor(file.substring(18, 23), file));// for windows
            instance.addFloor(new Floor(file.substring(25, 30), file));// for linux
//                instance.addFloor(new Floor(file.substring(38, 43), file));// for mac
            }
        }

    }


    private HashMap<String, Floor> floors = new HashMap<String, Floor>();// get floor by floorNum.
    private Map<String, SemanticCell> semCells = new HashMap<>();

    private Building() {
    }

    /**
     * Query which SemanticCell the point dropped in.
     *
     * @param floor
     * @param positioningPoint
     * @param method
     * @return List<SemanticCell>
     */
    public static SemanticCell querySemCell(Floor floor, PositioningPoint positioningPoint, PositioningPoint.QueryMethod method) {
        if (method == PositioningPoint.QueryMethod.Grid) {
            return floor.queryInGrid(positioningPoint.getPoint());
        } else if (method == PositioningPoint.QueryMethod.STR) {
            return floor.queryInSTR(positioningPoint.getPoint());
        } else {
            return null;
        }

    }

    public static final Building getInstatnce() {
        return BuildingHolder.instance;
    }

    public void addFloor(Floor floor) {
        floors.put(floor.getFloorNum(), floor);

        for (SemanticCell semanticCell : floor.getSemanticCells()) {
            semCells.put(semanticCell.getPolygonNum(), semanticCell);
        }

    }

    public SemanticCell querySemCell(PositioningPoint positioningPoint, PositioningPoint.QueryMethod method) {
        return querySemCell(this.floors.get(positioningPoint.getFloorNum()), positioningPoint, method);
    }

    public Floor getFloor(String floorNum) {
        return floors.get(floorNum);
    }

    public List<SemanticCell> getSemCellsByName(String name) {
        List<SemanticCell> result = new ArrayList<SemanticCell>();
        for (Floor floor : floors.values()) {
            result.addAll(floor.getCellsByName(name));
        }
        return result;
    }

    public SemanticCell getSemCellByNum(String strNum) {
        return semCells.get(strNum);
    }

}
