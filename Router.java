
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Collections;
import java.util.Objects;


/*
This class provides a shortest path, source and target, method for finding routes between two points on the
map.  I modified a version of DijkstraSP algorithm (from Algorithms by Sedgewick and Wayne)
into an A* type algorithm to optimize efficiency.
*/
public class Router {

    private static int indexOfVert = 0;
    private static int[] edgeTo;
    private static double[] distTo;
    private static IndexMinPQ<Double> pq;
    private static long source;
    private static long target;


    /*
    Return a List of longs representing the shortest path from the node
    closest to a start location and the node closest to the destination location.
    graph:            The graph to use.
    startLongitude:   The longitude of the start location.
    startLatitude:    The latitude of the start location.
    endLongitude:     The longitude of the destination location.
    endLatitude:      The latitude of the destination location.
    */
    public static List<Long> shortestPath(GraphDB graph, double startLongitude, double startLatitude,
                                          double endLongitude, double endLatitude) {
        int numVertices = graph.numVertices();
        pq = new IndexMinPQ<>(numVertices);
        edgeTo = new int[numVertices];
        distTo = new double[numVertices];
        source = graph.closest(startLongitude, startLatitude);
        target = graph.closest(endLongitude, endLatitude);

        for (int i = 0; i < numVertices; i++) {
            distTo[i] = Double.POSITIVE_INFINITY;
        }

        graph.indexVertexMap.put(indexOfVert, source);  //source is at index 0;
        graph.vertexIndexMap.put(source, indexOfVert);
        distTo[indexOfVert] = 0.0;
        pq.insert(indexOfVert, 0.0);
        indexOfVert++;
        graph.indexVertexMap.put(indexOfVert, target);  //target is at index 1;
        graph.vertexIndexMap.put(target, indexOfVert);
        indexOfVert++;

        while (!pq.isEmpty()) {
            relax(graph, pq.delMin());
        }

        return path(graph);
    }


    /*
    Helper for shortestPath()
    */
    private static void relax(GraphDB graph, int current) {
        GraphDB.Vertex vertex = graph.getVertex(graph.indexVertexMap.get(current));
        int neighborIndex;

        if (vertex.id == target) {
            return;
        }

        for (long neighbor : vertex.adj) {

            double weight = graph.distance(vertex.id, neighbor);

            if (graph.vertexIndexMap.containsKey(neighbor)) {
                neighborIndex = graph.vertexIndexMap.get(neighbor);

            } else {
                graph.indexVertexMap.put(indexOfVert, neighbor);
                graph.vertexIndexMap.put(neighbor, indexOfVert);
                neighborIndex = indexOfVert;
                indexOfVert++;
            }

            if (distTo[current] + weight < distTo[neighborIndex]) {
                distTo[neighborIndex] = distTo[current] + weight;
                edgeTo[neighborIndex] = current;
                //The heuristic is what pulls the path towards the target node and really optimizes efficiency.
                double heuristic = distTo[neighborIndex] + euclideanDist(neighbor, source, graph);

                if (pq.contains(neighborIndex)) {
                    pq.change(neighborIndex, heuristic);

                } else {
                    pq.insert(neighborIndex, heuristic);
                }
            }
        }
    }


    /*
    The source vertex id is always at index 0 of indexVertexMap in GraphDB.
    The target vertex id is always at index 1 of indexVertexMap in GraphDB.
    */
    private static List<Long> path(GraphDB g) {
        ArrayList<Long> path = new ArrayList<>();
        long sourceId = g.indexVertexMap.get(0);
        long vertex = g.indexVertexMap.get(1);

        while (vertex != sourceId) {
            path.add(vertex);
            int nextVertexIndex = edgeTo[g.vertexIndexMap.get(vertex)];
            vertex = g.indexVertexMap.get(nextVertexIndex);
        }

        path.add(sourceId);
        indexOfVert = 0;
        g.vertexIndexMap = new HashMap<>();
        g.indexVertexMap = new HashMap<>();
        Collections.reverse(path);
        return path;
    }


    private static double euclideanDist(long v1Id, long v2Id, GraphDB g) {
        GraphDB.Vertex v1 = g.getVertex(v1Id);
        GraphDB.Vertex v2 = g.getVertex(v2Id);
        double squared = Math.pow(v1.lon - v2.lon, 2) + Math.pow(v1.lat - v2.lat, 2);
        return Math.sqrt(squared);
    }


    /*
    Not implemented yet.
    Create the list of directions corresponding to a route on the graph.
    g The graph to use.
    route The route to translate into directions. Each element
    corresponds to a node from the graph in the route.
    Returns A list of NavigatiionDirection objects corresponding to the input route.
    */
    static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
       return null;
    }


    /*
    Class to represent a navigation direction, which consists of 3 attributes:
    a direction to go, a way, and the distance to travel for.
    */
    public static class NavigationDirection {

        //Integer constants representing directions.
        static final int START = 0;
        static final int STRAIGHT = 1;
        static final int SLIGHT_LEFT = 2;
        static final int SLIGHT_RIGHT = 3;
        static final int RIGHT = 4;
        static final int LEFT = 5;
        static final int SHARP_LEFT = 6;
        static final int SHARP_RIGHT = 7;
        //Number of directions supported.
        static final int NUM_DIRECTIONS = 8;
        //A mapping of integer values to directions.
        static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];
        //Default name for an unknown way.
        static final String UNKNOWN_ROAD = "unknown road";
        int direction;  //The direction a given NavigationDirection represents.
        String way;  //The name of the way I represent.
        double distance;  //The distance along this way I represent.
        
        //Static initializer.
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }


        //Create a default, anonymous NavigationDirection.
        NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }


        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }


        /*
        Takes the string representation of a navigation direction and converts it into
        a Navigation Direction object.
        @param dirAsString The string representation of the NavigationDirection.
        @return A NavigationDirection object representing the input string.
        */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();

            if (m.matches()) {
                String direction = m.group(1);

                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;

                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;

                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;

                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;

                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;

                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;

                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;

                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;

                } else {
                    return null;
                }
                nd.way = m.group(2);

                try {
                    nd.distance = Double.parseDouble(m.group(3));

                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;

            } else {
                // not a valid nd
                return null;
            }
        }


        @Override
        public boolean equals(Object o) {

            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }

            return false;
        }


        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }


    }


}
