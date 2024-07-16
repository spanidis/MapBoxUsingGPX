package el.ps.mymapboxgpxapp;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.ImageHolder;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.extension.style.layers.LayerUtils;
import com.mapbox.maps.extension.style.layers.generated.LineLayer;
import com.mapbox.maps.extension.style.sources.SourceUtils;
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.PuckBearing;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Page2Map extends AppCompatActivity {

    private MapView mapView;
    FloatingActionButton floatingActionButton;
    Double zoomValue = 18.5;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean granted) {
            if (granted) {
                // Location permissions granted, proceed with your logic
                Toast.makeText(Page2Map.this, "Location permission GRANTED.", Toast.LENGTH_SHORT).show();
            } else {
                // Location permissions not granted, handle accordingly
                Toast.makeText(Page2Map.this, "Location permission DENIED.", Toast.LENGTH_SHORT).show();
            }
        }
    });

    private final OnIndicatorBearingChangedListener onIndicatorBearingChangedListener = new OnIndicatorBearingChangedListener() {
        @Override
        public void onIndicatorBearingChanged(double v) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().bearing(v).build());

        }
    };

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = new OnIndicatorPositionChangedListener() {
        @Override
        public void onIndicatorPositionChanged(@NonNull Point point) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().center(point).zoom(zoomValue).build());
            getGestures(mapView).setFocalPoint(mapView.getMapboxMap().pixelForCoordinate(point));
            //Toast.makeText(MainActivity.this, "POINT: "+point.toString(), Toast.LENGTH_SHORT).show();
            latitude = point.latitude();
            longitude = point.longitude();
            //Toast.makeText(MainActivity.this, "lat: "+latitude+" long: "+longitude, Toast.LENGTH_SHORT).show();
        }
    };

    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {
            getLocationComponent(mapView).removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
            getLocationComponent(mapView).removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
            getGestures(mapView).removeOnMoveListener(onMoveListener);
            floatingActionButton.show();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector moveGestureDetector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page2_map);

        mapView = findViewById(R.id.mapView);
        floatingActionButton = findViewById(R.id.focusLocations);
        floatingActionButton.hide();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        mapView.getMapboxMap().loadStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded()  {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                mapView.getMapboxMap().setCamera(new CameraOptions.Builder().zoom(zoomValue).build());
                LocationComponentPlugin locationComponentPlugin = getLocationComponent(mapView);
                locationComponentPlugin.setEnabled(true); //Whether the user location is visible on the map.
                PuckBearing puckBearing = PuckBearing.COURSE;//follow the direction of the user
                locationComponentPlugin.setPuckBearingEnabled(true);//follow the direction of the user
                LocationPuck2D locationPuck2D = new LocationPuck2D();
                locationPuck2D.setBearingImage(ImageHolder.from(R.drawable.baseline_arrow_drop_up_24));//custom drawable for the map marker

                //add all the listeners
                locationComponentPlugin.setLocationPuck(locationPuck2D);
                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener); //Adds a listener that gets invoked when indicator position changes.
                locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener); //Adds a listener that gets invoked when indicator bearing changes.
                getGestures(mapView).addOnMoveListener(onMoveListener);

                floatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
                        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
                        getGestures(mapView).addOnMoveListener(onMoveListener);
                        floatingActionButton.hide();
                    }
                });

                List<Point> points = parseGpx();
                if (points != null) {
                    drawPolyline(style, points);
                }
            }
        });
    }

    private List<Point> parseGpx() {
        List<Point> points = new ArrayList<>();
        try {
            //InputStream inputStream = getAssets().open("gpx2.gpx");
            GlobalData globalData = GlobalData.getInstance();
            InputStream inputStream = globalData.getGlobalXMLObject();
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("trkpt")) {
                    double lat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
                    double lon = Double.parseDouble(parser.getAttributeValue(null, "lon"));
                    points.add(Point.fromLngLat(lon, lat));
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }
        return points;
    }

    private void drawPolyline(@NonNull Style style, List<Point> points) {
        GeoJsonSource source = new GeoJsonSource.Builder("line-source")
                .geometry(LineString.fromLngLats(points))
                .build();
        SourceUtils.addSource(style, source);

        LineLayer lineLayer = new LineLayer("linelayer", "line-source")
                .lineColor("blue")
                .lineWidth(5f);
        LayerUtils.addLayer(style,lineLayer);
    }
}