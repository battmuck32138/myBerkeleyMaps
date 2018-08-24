
import java.util.HashMap;
import java.util.Map;


/*
This class provides all code necessary to take a query box and produce
a query result. The getMapRaster method must return a Map containing all
seven of the required fields, otherwise the front end code will
not draw the output correctly.
*/
public class Rasterer {

    private static final double ROOT_ULLAT = 37.892195547244356;
    private static final double ROOT_ULLON = -122.2998046875;
    private static final double ROOT_LRLAT = 37.82280243352756;
    private static final double ROOT_LRLON = -122.2119140625;
    private static final int TILE_SIZE = 256;
    private double totLongBerkeley = Math.abs(ROOT_LRLON - ROOT_ULLON);
    private Map<String, Object> results = new HashMap<>();
    private int zoomDepth;


    /*
    Takes a user query and finds the grid of images that best matches the query. These
    images are then combined into one big image (rastered) by the front end.
    The grid of images obey the following properties, where image in the
    grid is referred to as a "tile".

    The tiles collected covers the most longitudinal distance per pixel
    (LonDPP) possible, while still covering less than or equal to the amount of
    longitudinal distance per pixel in the query box for the user viewport size.
    Contains all tiles that intersect the query bounding box that fulfill the above condition.
    The tiles are arranged in-order to reconstruct the full image.

    @param params Map of the HTTP GET request's query parameters - the query box and
    the user viewport width and height.
    @return A map of results for the front end as specified:
    "render_grid"   : String[][], the files to display.
    "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image.
    "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image.
    "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image.
    "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image.
    "depth"         : Number, the depth of the nodes of the rastered image.
    "query_success" : Boolean, whether the query was able to successfully complete;
    String key set for params: "lrlon", "ullon", "w", "h", "ullat", "lrlat"
    */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {

        double boxLrLon = params.get("lrlon");
        double boxUlLon = params.get("ullon");
        double boxUlLat = params.get("ullat");
        double boxLrLat = params.get("lrlat");
        double width = params.get("w");

        if (boxLrLon < boxUlLon || boxLrLat > boxUlLat) {
            System.out.println("Bad input!");
            results.put("query_success", false);

        } else {
            results.put("query_success", true);
        }

        double queryBoxLonDpp = lonDpp(boxLrLon, boxUlLon, width);
        zoomDepth = calcZoomDepth(queryBoxLonDpp);
        results.put("depth", zoomDepth);
        findBoxIntersections(boxUlLon, boxUlLat, boxLrLon, boxLrLat);
        return results;
    }


    /*
    Longitudinal distance per pixel
    Returns units of longitude per pixel
    */
    private double lonDpp(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }


    /*
    Finds the appropriate zoom depth d for the image
    files in the data set i.e. d1_x0_y0
    */
    private int calcZoomDepth(double queryBoxLonDpp) {

        for (int i = 0; i < 8; i++) {
            double lrlon = ROOT_ULLON + (totLongBerkeley / Math.pow(2, i));
            double lonDppforTile = lonDpp(lrlon, ROOT_ULLON, TILE_SIZE);

            if (lonDppforTile <= queryBoxLonDpp) {
                return i;
            }

        }
        return 7;
    }


    /*
    Identifies and stores the image file names of all tiles that intersect the query box.
    Calculates the latitudes and longitudes of the upper left and lower right tiles.
    */
    private void findBoxIntersections(
            double ullon, double ullat, double lrlon, double lrlat) {

        boolean firstIntersect = false;
        int mapDimension = (int) Math.pow(2, zoomDepth);
        double tileWidth = totLongBerkeley / Math.pow(2, zoomDepth);
        double tileHeight = Math.abs(ROOT_ULLAT - ROOT_LRLAT) / Math.pow(2, zoomDepth);
        int numTilesAboveBox = (int) (Math.abs(ROOT_ULLAT - ullat) / tileHeight);
        int numtilesBelowBox = (int) (Math.abs(lrlat - ROOT_LRLAT) / tileHeight);
        int numTilesLeftBox = (int) (Math.abs(ROOT_ULLON - ullon) / tileWidth);
        int numtilesRightBox = (int) (Math.abs(lrlon - ROOT_LRLON) / tileWidth);
        int numRows = mapDimension - numTilesAboveBox - numtilesBelowBox;
        int numColumns = mapDimension - numTilesLeftBox - numtilesRightBox;
        int row = 0;
        int col = 0;
        String[][] grid = new String[numRows][numColumns];
        double currentLeftLon = ROOT_ULLON;
        double currentTopLat = ROOT_ULLAT;

        //Establish x and y for the file names.
        for (int y = 0; currentTopLat > lrlat; y++) {
            for (int x = 0; currentLeftLon < lrlon; x++) {

                //Check to see if query box and tile intersect.
                if (ullon <= (currentLeftLon + tileWidth) && lrlon >= currentLeftLon
                        && ullat >= (currentTopLat - tileHeight) && currentTopLat >= lrlat) {

                    //Identify the first tile that intersects the query box.
                    if (!firstIntersect) {
                        results.put("raster_ul_lon", currentLeftLon);
                        results.put("raster_ul_lat",  currentTopLat);
                        firstIntersect = true;
                    }

                    //Identify the last tile that intersects the query box.
                    if (col == numColumns - 1 && row == numRows - 1) {
                        results.put("raster_lr_lon", currentLeftLon + tileWidth);
                        results.put("raster_lr_lat", currentTopLat - tileHeight);
                    }

                    //Add file names of intersecting tiles to the grid of tile names.
                    if (col < numColumns) {
                        grid[row][col] = "d" + zoomDepth + "_x" + x + "_y" + y + ".png";
                        col++;

                    } else {
                        row++;
                        col = 0;
                        grid[row][col] = "d" + zoomDepth + "_x" + x + "_y" + y + ".png";
                        col++;
                    }

                }
                currentLeftLon += tileWidth;

            }
            currentLeftLon = ROOT_ULLON;
            currentTopLat -= tileHeight;

        }
        results.put("render_grid", grid);
    }


}
