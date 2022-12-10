/**
 Licensed under the GNU General Public License version 3
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.gnu.org/licenses/gpl-3.0.html

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 **/
package it.osm.gtfs.command;

import it.osm.gtfs.input.GTFSParser;
import it.osm.gtfs.input.OSMParser;
import it.osm.gtfs.output.OSMRelationImportGenerator;
import it.osm.gtfs.utils.GTFSImportSettings;
import it.osm.gtfs.model.BoundingBox;
import it.osm.gtfs.model.Route;
import it.osm.gtfs.model.Stop;
import it.osm.gtfs.model.StopsList;
import it.osm.gtfs.model.Trip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.common.collect.Multimap;
import picocli.CommandLine;

//TODO: to remove this command and integrate into the fullrels command like --stops for only stops relation generation
@CommandLine.Command(name = "stoprels", description = "Generate STOP-ONLY relations")
public class GTFSGenerateRoutesBaseRelations implements Callable<Void> {

    @Override
    public Void call() throws IOException, ParserConfigurationException, SAXException {
        Map<String, Stop> osmstops = OSMParser.applyGTFSIndex(OSMParser.readOSMStops(GTFSImportSettings.getInstance().getOutputPath() +  GTFSImportSettings.OSM_STOP_FILE_NAME));
        Map<String, Route> routes = GTFSParser.readRoutes(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_ROUTES_FILE_NAME);
        Map<String, StopsList> stopTimes = GTFSParser.readStopTimes(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_STOP_TIME_FILE_NAME, osmstops);
        List<Trip> trips = GTFSParser.readTrips(GTFSImportSettings.getInstance().getGTFSPath() +  GTFSImportSettings.GTFS_TRIPS_FILE_NAME,
                routes, stopTimes);
        BoundingBox bb = new BoundingBox(osmstops.values());

        //sorting set
        Multimap<String, Trip> grouppedTrips = GTFSParser.groupTrip(trips, routes, stopTimes);
        Set<String> keys = new TreeSet<String>(grouppedTrips.keySet());

        new File(GTFSImportSettings.getInstance().getOutputPath() + "relations").mkdirs();

        int id = 10000;
        for (String k:keys){
            Collection<Trip> allTrips = grouppedTrips.get(k);
            Set<Trip> uniqueTrips = new HashSet<Trip>(allTrips);

            for (Trip trip:uniqueTrips){
                int count = Collections.frequency(allTrips, trip);

                Route r = routes.get(trip.getRoute().getId());
                StopsList s = stopTimes.get(trip.getTripID());

                FileOutputStream f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "relations/r" + id + " " + r.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + "_" + count + ".osm");
                f.write(OSMRelationImportGenerator.getRelation(bb, s, null, trip, r).getBytes());
                f.close();
                f = new FileOutputStream(GTFSImportSettings.getInstance().getOutputPath() + "relations/r" + id++ + " " + r.getShortName().replace("/", "B") + " " + trip.getName().replace("/", "_") + "_" + count + ".txt");
                f.write(s.getRelationAsStopList(trip, r).getBytes());
                f.close();
            }
        }
        return null;
    }
}
