package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.speedInt;
import static com.urrecliner.blackbox.Vars.utils;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class GPSTracker extends Service implements LocationListener {

    static boolean isCompassShown = false;
    Context gContext;
    Activity gActivity;
//    Location prevLoc;
    double latitude = 0, longitude = 0;
    int nowSpeed = 0;
    ArrayList<Double> latitudes, longitudes;
//    int speedOld2, speedOld1, speedNew;
    final int ARRAY_SIZE = 4;
    int nowDirection, oldDirection = -99;       // nowDirection = 0 ~ 360/22.5
    ImageView [] newsView;
    private static final long MIN_TIME_DRIVE_UPDATES = 800;
    private static final float MIN_DISTANCE_DRIVE = 10;
    protected LocationManager locationManager;
    TextView speedView;

    void init(Activity activity, Context context) {
        gContext = context;
        gActivity = activity;
        speedView = gActivity.findViewById(R.id.textSpeed);
        newsView = new ImageView[5];
        for (int i = 0; i < 5; i++) {
            newsView[i] = gActivity.findViewById(newsIds[i]);
//            if (i == 0 || i == 4)
//                newsView[i].setAlpha(.3f);
//            else if (i == 1 || i == 3)
//                newsView[i].setAlpha(.6f);
//            else
//                newsView[i].setAlpha(1f);
//            newsView[i].setVisibility(View.VISIBLE);
//            if (!isCompassShown) {
//                ImageView iv = newsView[i];
//                iv.setVisibility(View.INVISIBLE);
//            }
        }
        latitude = 37.3926; longitude = 127.1267; nowSpeed = 1;
        latitudes = new ArrayList<>(); longitudes = new ArrayList<>();
        for (int i = 0; i < ARRAY_SIZE; i++) {
            latitudes.add(latitude + (double) i * 0.0001f); longitudes.add(longitude + (double) i * 0.0001f); }
//        gActivity.runOnUiThread(() -> {
//            for (int i = 0; i < 5; i++) {
//                ImageView v = newsView[i];
//                v.setVisibility(View.VISIBLE);
//            }
//            isCompassShown = true;
//        });
    }

    void askLocation() {

        locationManager = (LocationManager) gContext.getSystemService(LOCATION_SERVICE);
        if (locationManager == null) {
            utils.logBoth("GPS", "Location Manager null");
            return;
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            utils.logBoth("GPS", "isProviderEnabled False");
            return;
        }
        if (ActivityCompat.checkSelfPermission(gContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(gContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            utils.logBoth("GPS", "checkSelfPermission Error");
            return;
        }
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_DRIVE_UPDATES,
                MIN_DISTANCE_DRIVE, this);
        if (locationManager != null) {
            LinearLayoutCompat newsLine = mActivity.findViewById(R.id.newsLine);
            newsLine.setVisibility(View.VISIBLE);
        } else
            utils.logBoth("GPS","locationManager Null");
    }

    double getLatitude() { return latitude; }
    double getLongitude() { return longitude; }

    @Override
    public void onLocationChanged(Location newLoc) {
//        utils.logBoth("loc Speed="+newLoc.hasSpeed(),"New Loc "+newLoc.getLatitude()+" x "+newLoc.getLongitude());

        if (!newLoc.hasSpeed())
            return;
        nowSpeed = (int) (newLoc.getSpeed() * 3.6f);
        if (speedInt != nowSpeed) {
            speedInt = nowSpeed;
            gActivity.runOnUiThread(() -> speedView.setText("" + speedInt));
        }
        if (speedInt < 10) // if speed is < xx then no update, OBD should be connected
            return;
        latitude = newLoc.getLatitude();
        longitude = newLoc.getLongitude();
        latitudes.remove(0); longitudes.remove(0);
        latitudes.add(latitude); longitudes.add(longitude);
        latitudes.set(1, ((latitudes.get(0)+ latitudes.get(2))/2+ latitudes.get(1))/2);
        latitudes.set(2, ((latitudes.get(1)+ latitudes.get(3))/2+ latitudes.get(2))/2);
        longitudes.set(1, ((longitudes.get(0)+ longitudes.get(2))/2+ longitudes.get(1))/2);
        longitudes.set(2, ((longitudes.get(1)+ longitudes.get(3))/2+ longitudes.get(2))/2);
        float GPSDegree = calcDirection(latitudes.get(0), longitudes.get(0), latitudes.get(2), longitudes.get(2));
        if (Float.isNaN(GPSDegree))
            return;
        nowDirection = (int) (((360+GPSDegree) % 360) / 22.5);
        if (nowDirection == oldDirection)
            return;
        oldDirection = nowDirection;
        gActivity.runOnUiThread(() -> {
            for (int i = 0; i < 5; i++) {
                ImageView v = newsView[i];
                if (i == 2)
                    v.setImageResource(yellowBlur[i + oldDirection]);
                else
                    v.setImageResource(yellowBright[i + oldDirection]);   // 흐리게
            }
        });
    }

    public void stopGPS() {
        locationManager.removeUpdates(this);
    }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onProviderEnabled(String provider) { }

//    @Override
//    public void onStatusChanged(String provider, int status, Bundle extras) { }
//

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private float calcDirection(double P1_latitude, double P1_longitude, double P2_latitude, double P2_longitude)
    {
        final double CONSTANT2RADIAN = (3.141592 / 180);
        final double CONSTANT2DEGREE = (180 / 3.141592);

        double lat1Rad = P1_latitude * CONSTANT2RADIAN;
        double lng1Rad = P1_longitude * CONSTANT2RADIAN;

        double lat2Rad = P2_latitude * CONSTANT2RADIAN;
        double lng2Rad = P2_longitude * CONSTANT2RADIAN;

        double radian_distance =
                Math.acos(Math.sin(lat1Rad) * Math.sin(lat2Rad) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.cos(lng1Rad - lng2Rad));
        double radian_bearing = Math.acos((Math.sin(lat2Rad) - Math.sin(lat1Rad) * Math.cos(radian_distance)) / (Math.cos(lat1Rad) * Math.sin(radian_distance)));

        double true_bearing;
        if (Math.sin(lng2Rad - lng1Rad) < 0)
            true_bearing = 360 - radian_bearing * CONSTANT2DEGREE;
        else
            true_bearing = radian_bearing * CONSTANT2DEGREE;
        return (float) true_bearing;
    }

    private final int[] yellowBlur = {R.mipmap.yellow_nw, R.mipmap.yellow_i, R.mipmap.yellow_n,
            R.mipmap.yellow_i, R.mipmap.yellow_ne, R.mipmap.yellow_i, R.mipmap.yellow_e,
            R.mipmap.yellow_i, R.mipmap.yellow_se, R.mipmap.yellow_i, R.mipmap.yellow_s,
            R.mipmap.yellow_i, R.mipmap.yellow_sw, R.mipmap.yellow_i, R.mipmap.yellow_w,
            R.mipmap.yellow_i, R.mipmap.yellow_nw, R.mipmap.yellow_i, R.mipmap.yellow_n,
            R.mipmap.yellow_i, R.mipmap.yellow_ne};
    private final int[] yellowBright = {R.mipmap.green_nw, R.mipmap.green_i, R.mipmap.green_n,
            R.mipmap.green_i, R.mipmap.green_ne, R.mipmap.green_i, R.mipmap.green_e,
            R.mipmap.green_i, R.mipmap.green_se, R.mipmap.green_i, R.mipmap.green_s,
            R.mipmap.green_i, R.mipmap.green_sw, R.mipmap.green_i, R.mipmap.green_w,
            R.mipmap.green_i, R.mipmap.green_nw, R.mipmap.green_i, R.mipmap.green_n,
            R.mipmap.green_i, R.mipmap.green_ne};

    private final int[] newsIds = { R.id.news_0, R.id.news_1, R.id.news_2, R.id.news_3, R.id.news_4};
}