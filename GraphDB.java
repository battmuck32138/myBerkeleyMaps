import org.xml.sax.SAXException;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.List;
import java.util.ArrayList;


/*
Graph for storing all of the intersection (vertex) and road (edge) information.
Uses GraphBuildingHandler to convert the XML files into a graph.
Code includes the vertices, adjacent, distance, closest, lat, and lon methods.
*/
public class GraphDB {

    private Map<Long, Vertex> vertexMap = new HashMap<>();
    HashMap<Integer, Long> indexVertexMap = new HashMap<>();
    HashMap<Long, Integer> vertexIndexMap = new HashMap<>();


    /*
    constructor creates and starts an XML parser.
    @param dbPath Path to the XML file to be parsed.
    */
    GraphDB(String dbPath) {

        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        clean();
    }


    public int numVertices() {
        return vertexMap.size();
    }


    /*
    Remove nodes with no connections from the graph.
    While this does not guarantee that any two nodes in the remaining graph are connected,
    we can reasonably assume this since typically roads are connected.
    */
    private void clean() {
        ArrayList<Long> keys = new ArrayList<>(vertexMap.keySet());

        for (long key : keys) {
            Vertex v = vertexMap.get(key);

            if (v.adj.size() == 0) {
                vertexMap.remove(key);
            }
        }
    }


    /*
    Returns an iterable of all vertex IDs in the graph.
    @return An iterable of id's of all vertices in the graph.
    */
    Iterable<Long> vertices() {
        return vertexMap.keySet();
    }


    /*
    Returns ids of all vertices adjacent to v.
    @param v The id of the vertex we are looking adjacent to.
    @return An iterable of the ids of the neighbors of v.
    */
    Iterable<Long> adjacent(long v) {
        return vertexMap.get(v).adj;
    }


    /*
    Returns the great-circle distance between vertices v and w in miles.
    Assumes the lon/lat methods are implemented properly.
    <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
    @param v The id of the first vertex.
    @param w The id of the second vertex.
    @return The great-circle distance between the two locations from the graph.
    */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }


    //Helper for great-circle distance.
    private static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);
        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }


    /*
    Returns the vertex closest to the given longitude and latitude.
    @param lon The target longitude.
    @param lat The target latitude.
    @return The id of the node in the graph closest to the target.
    */
    long closest(double lon, double lat) {
        double minDist = Double.MAX_VALUE;  //dummy value
        long closest = Long.MAX_VALUE;  //dummy value

        for (Vertex v : vertexMap.values()) {
            double dist = distance(lon, lat, v.lon, v.lat);

            if (dist <= minDist) {
                minDist = dist;
                closest = v.id;
            }

        }
        return closest;
    }


    /*
    Gets the longitude of a vertex.
    @param v The id of the vertex.
    @return The longitude of the vertex.
    */
    double lon(long v) {
        return vertexMap.get(v).lon;
    }


    /*
    Gets the latitude of a vertex.
    @param v The id of the vertex.
    @return The latitude of the vertex.
    */
    double lat(long v) {
        return vertexMap.get(v).lat;
    }


    void addVertex(long id, double lon, double lat) {
        Vertex v = new Vertex(id, lon, lat);
        vertexMap.put(v.id, v);
    }


    void populateVertexAdjLists(Long id, List<Long> vertexList) {

        for (int i = 0; i < vertexList.size() - 1; i++) {
            Vertex a = vertexMap.get(vertexList.get(i));
            Vertex b = vertexMap.get(vertexList.get(i + 1));
            a.connectTwoVertices(b.id);
        }
    }


    public Vertex getVertex(long id) {
        return vertexMap.get(id);
    }


    /****************************************************************************
     * Helper Classes
     ***************************************************************************/

    /*
    Vertices are drivable intersections in Berkeley.
     */
    public class Vertex {

        long id;  //vertex id from osm file.
        double lon;
        double lat;
        ArrayList<Long> adj = new ArrayList<>();


        Vertex(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
        }


        void connectTwoVertices(long vertexId) {
            adj.add(vertexId);
            vertexMap.get(vertexId).adj.add(this.id);
        }


    }


}
