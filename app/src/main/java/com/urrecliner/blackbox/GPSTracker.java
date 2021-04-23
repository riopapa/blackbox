package com.urrecliner.blackbox;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static com.urrecliner.blackbox.Vars.isCompassShown;
import static com.urrecliner.blackbox.Vars.gpsUpdateTime;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.speedInt;
import static com.urrecliner.blackbox.Vars.utils;

class GPSTracker extends Service implements LocationListener {

    private final Context mContext;
    boolean isGPSEnabled = false;
    Location location;
    double latitude = 0, longitude = 0;
    ArrayList<Double> latitudes, longitudes;
    int arraySize = 4;
    int nowDirection, oldDirection = -99;
    ImageView [] newsView;
    private static final float MIN_DISTANCE_DRIVE = 20;
    private static final long MIN_TIME_DRIVE_UPDATES = 2000;
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        this.mContext = context;
    }

    void init() {
        newsView = new ImageView[5];
        for (int i = 0; i < 5; i++) {
            newsView[i] = mActivity.findViewById(newsIds[i]);
//            if (!isCompassShown) {
//                ImageView iv = newsView[i];
//                iv.setVisibility(View.INVISIBLE);
//            }
        }
    }

    void askLocation() {

        latitude = 0; longitude = 0;
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            assert locationManager != null;
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (isGPSEnabled) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_DRIVE_UPDATES,
                        MIN_DISTANCE_DRIVE, this);
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                }
            }
        }
        catch (Exception e) {
            utils.logE("GPS", "Exception", e);
        }
        latitudes = new ArrayList<>(); longitudes = new ArrayList<>();
        for (int i = 0; i < arraySize; i++) {
            latitudes.add(latitude + (double) i * 0.00001f); longitudes.add(longitude + (double) i * 0.0001f); }

    }

    double getLatitude() { return latitude; }
    double getLongitude() { return longitude; }

    @Override
    public void onLocationChanged(Location location) {
//        utils.logBoth("location","location changed "+location.getLatitude()+" x "+location.getLongitude());
        latitude = location.getLatitude();
        longitude = location.getLongitude();
//        utils.log("gps"," lat "+latitude+" x "+longitude);
        latitudes.remove(0); longitudes.remove(0);
        latitudes.add(latitude); longitudes.add(longitude);
        latitudes.set(1, ((latitudes.get(0)+ latitudes.get(2))/2+ latitudes.get(1))/2);
        latitudes.set(2, ((latitudes.get(1)+ latitudes.get(3))/2+ latitudes.get(2))/2);
        longitudes.set(1, ((longitudes.get(0)+ longitudes.get(2))/2+ longitudes.get(1))/2);
        longitudes.set(2, ((longitudes.get(1)+ longitudes.get(3))/2+ longitudes.get(2))/2);

        gpsUpdateTime = System.currentTimeMillis();
        if (!isCompassShown) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 5; i++) {
                        ImageView v = newsView[i];
                        v.setVisibility(View.VISIBLE);
                    }
                }
            });
            isCompassShown = true;
            utils.logBoth("GPSTracker","Run ..");
        }
        if (speedInt < 10) // if speed is < xx then no update, OBD should be connected
            return;
        float GPSDegree = calcDirection(latitudes.get(0), longitudes.get(0), latitudes.get(2), longitudes.get(2));
        if (Float.isNaN(GPSDegree))
            return;
        nowDirection = (int) (((360+GPSDegree) % 360) / 22.5);
        if (nowDirection != oldDirection) {
            oldDirection = nowDirection;
//            utils.logBoth("NEWS","GPSDegree="+GPSDegree+" nowDirection="+nowDirection);
            mActivity.runOnUiThread(() -> drawCompass(oldDirection));
        }
    }

    @Override
    public void onProviderDisabled(String provider) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

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

    private final int[] yellows = {R.mipmap.yellow_nw, R.mipmap.yellow_i, R.mipmap.yellow_n,
            R.mipmap.yellow_i, R.mipmap.yellow_ne, R.mipmap.yellow_i, R.mipmap.yellow_e,
            R.mipmap.yellow_i, R.mipmap.yellow_se, R.mipmap.yellow_i, R.mipmap.yellow_s,
            R.mipmap.yellow_i, R.mipmap.yellow_sw, R.mipmap.yellow_i, R.mipmap.yellow_w,
            R.mipmap.yellow_i, R.mipmap.yellow_nw, R.mipmap.yellow_i, R.mipmap.yellow_n,
            R.mipmap.yellow_i, R.mipmap.yellow_ne};
    private final int[] greens = {R.mipmap.green_nw, R.mipmap.green_i, R.mipmap.green_n,
            R.mipmap.green_i, R.mipmap.green_ne, R.mipmap.green_i, R.mipmap.green_e,
            R.mipmap.green_i, R.mipmap.green_se, R.mipmap.green_i, R.mipmap.green_s,
            R.mipmap.green_i, R.mipmap.green_sw, R.mipmap.green_i, R.mipmap.green_w,
            R.mipmap.green_i, R.mipmap.green_nw, R.mipmap.green_i, R.mipmap.green_n,
            R.mipmap.green_i, R.mipmap.green_ne};

    private final int[] newsIds = { R.id.news_0, R.id.news_1, R.id.news_2, R.id.news_3, R.id.news_4};

    void drawCompass (int dirIdx) { // 0: N, 8: S

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 5; i++) {
                        ImageView v = newsView[i];
                        utils.logBoth("NEWS "+speedInt,dirIdx+" "+(i+dirIdx));
                        switch (i) {
                            case 0:
                            case 4:
                                v.setImageResource(yellows[i + dirIdx]);
                                break;
                            default:
                                v.setImageResource(greens[i + dirIdx]);
                                break;
                        }
                    }
                }
            });

//        RotateAnimation ra = new RotateAnimation(
//                savedDegree, -degree,
//                Animation.RELATIVE_TO_SELF, 0.5f,
//                Animation.RELATIVE_TO_SELF, 0.5f);
//        ra.setDuration(20);
//
//        // set the animation after the end of the reservation status
//        ra.setFillAfter(true);
//        vWheel.startAnimation(ra);
//        savedDegree = -degree;
    }

}