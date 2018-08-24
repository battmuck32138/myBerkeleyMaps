
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;


/*
Parses OSM XML files using an XML SAX parser. Used to construct the graph of roads and
intersections for the pathfinding algorithm.
The big-picture here is that some external library (SAX Parser) is going to walk through the XML
file and my override method tells Java what to do every time it encounters an element in the XML file.

OSM documentation on
 <a href="http://wiki.openstreetmap.org/wiki/Key:highway">the highway tag</a>,
 <a href="http://wiki.openstreetmap.org/wiki/Way">the way XML element</a>,
 <a href="http://wiki.openstreetmap.org/wiki/Node">the node XML element</a>,
 <a href="https://docs.oracle.com/javase/tutorial/jaxp/sax/parsing.html">SAX parser tutorial</a>.
 */
public class GraphBuildingHandler extends DefaultHandler {

    /*
    Only allow for non-service roads; this prevents going on pedestrian streets as much as
    possible. Note that in Berkeley, many of the campus roads are tagged as motor vehicle
    roads, but in practice we walk all over them with such impunity that we forget cars can
    actually drive on them.
    */
    private static final Set<String> ALLOWED_HIGHWAY_TYPES = new HashSet<>(Arrays.asList
            ("motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                    "residential", "living_street", "motorway_link", "trunk_link", "primary_link",
                    "secondary_link", "tertiary_link"));
    private String activeState = "";
    private final GraphDB graph;
    private List<Long> possibleConnections = new ArrayList<>();
    private boolean wayIsValid = false;
    private Long currentWayId;


    /*
    Create a new GraphBuildingHandler.
    @param graph The graph to populate with the XML data.
    */
    GraphBuildingHandler(GraphDB graph) {
        this.graph = graph;
    }


    /*
    Call at the beginning of an element.
    uri:         The Namespace URI, or the empty string if the element has no Namespace URI or
                 if Namespace processing is not being performed.
    localName:  The local name (without prefix), or the empty string if Namespace
                 processing is not being performed.
    qName:      The qualified name (with prefix), or the empty string if qualified names are
                 not available. This tells us which element we're looking at.
    attributes: The attributes attached to the element. If there are no attributes, it
                 shall be an empty Attributes object.
    */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        Long currentVertex;
        Double currentLon;
        Double currentLat;

        //Begin to parse XML files.
        if (qName.equals("node")) {
            //We encountered a new <node...> tag.
            activeState = "node";
            currentVertex = Long.parseLong(attributes.getValue("id"));
            currentLon = Double.parseDouble(attributes.getValue("lon"));
            currentLat = Double.parseDouble(attributes.getValue("lat"));
            graph.addVertex(currentVertex, currentLon, currentLat);

        } else if (qName.equals("way")) {
            //We encountered a new <way...> tag.
            activeState = "way";
            currentWayId = Long.parseLong(attributes.getValue("id"));

        } else if (activeState.equals("way") && qName.equals("nd")) {
            //While looking at a way, we found a <nd...> tag.
            long vertexId = Long.parseLong(attributes.getValue("ref"));
            possibleConnections.add(vertexId);

        } else if (activeState.equals("way") && qName.equals("tag")) {
            // While looking at a way, we found a <tag...> tag.
            String k = attributes.getValue("k");
            String v = attributes.getValue("v");
            //Disregard max speed because the data set is missing too many speed limits.

            if (k.equals("highway")) {
                if (ALLOWED_HIGHWAY_TYPES.contains(v)) {
                    wayIsValid = true;
                }
            }
        }
    }


    /*
    Receive notification of the end of an element.
    uri:         The Namespace URI, or the empty string if the element has no
                 Namespace URI of if Namespace processing is not being performed.
    localName:   The local name (without prefix), or the empty string if Namespace
                 processing is not being performed.
    qName:       The qualified name (with prefix), or the empty string if qualified names are
                 not available.
    */
    @Override
    public void endElement(String uri, String localName, String qName) {

        if (qName.equals("way")) {

            if (wayIsValid) {
                graph.populateVertexAdjLists(currentWayId, possibleConnections);
                wayIsValid = false;
            }

            possibleConnections = new ArrayList<>();
        }
    }


}
