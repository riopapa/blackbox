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
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import java.util.ArrayList;

import static com.urrecliner.blackbox.Vars.isCompassShown;
import static com.urrecliner.blackbox.Vars.gpsUpdateTime;
import static com.urrecliner.blackbox.Vars.mActivity;
import static com.urrecliner.blackbox.Vars.speedInt;
import static com.urrecliner.blackbox.Vars.utils;
import static com.urrecliner.blackbox.Vars.vCompass;

class GPSTracker extends Service implements LocationListener {

    private final Context mContext;
    boolean isGPSEnabled = false;
//    boolean isNetworkEnabled = false;
    Location location; // Location
    double latitude = 0, longitude = 0;
    ArrayList<Double> latitudes, longitudes;
    int arraySize = 4;
    int nowDirection = -1, oldDirection = -1;

    private static final float MIN_DISTANCE_DRIVE = 20;
    private static final long MIN_TIME_DRIVE_UPDATES = 2000;
    protected LocationManager locationManager;
//    private boolean blinkGPS = false;

    public GPSTracker(Context context) {
        this.mContext = context;
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
            latitudes.add(latitude + (double) i * 0.00001f); longitudes.add(longitude + (double) i * 0.0001f);
        }
    }

//    public void stopUsingGPS(){
//        if(locationManager != null){
//            locationManager.removeUpdates(GPSTracker.this);
//        }
//    }

    double getLatitude() { return latitude; }
    double getLongitude() { return longitude; }

    @Override
    public void onLocationChanged(Location location) {
//        utils.logBoth("location","location changed "+location.getLatitude()+" x "+location.getLongitude());
        updateCompass(location);
    }

    private void updateCompass(Location location) {
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
            mActivity.runOnUiThread(() -> vCompass.setVisibility(View.VISIBLE));
            isCompassShown = true;
            utils.logBoth("GPSTracker","Activated ..");
        }
        int HIDE_COMPASS = 15;
        if (speedInt < HIDE_COMPASS)
            return;
        float GPSDegree = calcDirection(latitudes.get(0), longitudes.get(0), latitudes.get(2), longitudes.get(2));
//        utils.logBoth("degree",GPSDegree+" : "+latitudes.get(0)+" x "+longitudes.get(0)+
//                " > "+latitude+" x "+longitude);
        if (Float.isNaN(GPSDegree))
            return;
        nowDirection = (int) (GPSDegree % 360 / 18) * 18;
        if (nowDirection != oldDirection) {
            oldDirection = nowDirection;
            mActivity.runOnUiThread(() -> {
//                vSatellite.setImageResource(blinkGPS ? R.mipmap.satellite1 : R.mipmap.satellite2);
                drawCompass(oldDirection);
            });
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

        double Cur_Lat_radian = P1_latitude * CONSTANT2RADIAN;
        double Cur_Lon_radian = P1_longitude * CONSTANT2RADIAN;

        double Dest_Lat_radian = P2_latitude * CONSTANT2RADIAN;
        double Dest_Lon_radian = P2_longitude * CONSTANT2RADIAN;

        // radian distance
        double radian_distance =
                Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian) + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));
        double radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian) * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));        // acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.

        double true_bearing;
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0)
            true_bearing = 360 - radian_bearing * CONSTANT2DEGREE;
        else
            true_bearing = radian_bearing * CONSTANT2DEGREE;
        return (float) true_bearing;
    }

    private int savedDegree;

    void drawCompass (int degree) {
        RotateAnimation ra = new RotateAnimation(
                savedDegree, -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(20);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        vCompass.startAnimation(ra);
        savedDegree = -degree;
    }

}