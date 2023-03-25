package org.nanick.servingcelllocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

public class CellLocationGPS {

    private final Context context;
    private final TelephonyManager telephonyManager;
    private final LocationManager locationManager;
    private final LocationListener locationListener;
    private final PhoneStateListener phoneStateListener;
    private boolean gpsEnabled = false;
    private boolean networkEnabled = false;
    public TextView[] textViews;

    public CellLocationGPS(Context context, TextView[] results) {
        this.textViews = results;
        this.context = context;
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListenerImpl();
        phoneStateListener = new PhoneStateListenerImpl(this.textViews);
    }

    @SuppressLint("MissingPermission")
    public void start() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            Log.e("CellLocationGPS", "No location providers enabled.");
        }
    }

    public void stop() {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        locationManager.removeUpdates(locationListener);
    }

    private class PhoneStateListenerImpl extends PhoneStateListener {
        public TextView[] datac;
        public PhoneStateListenerImpl(TextView[] dc){
            this.datac = dc;
        }
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            @SuppressLint("MissingPermission") CellLocation cellLocation = telephonyManager.getCellLocation();
            if (cellLocation instanceof android.telephony.gsm.GsmCellLocation) {
                android.telephony.gsm.GsmCellLocation gsmCellLocation = (android.telephony.gsm.GsmCellLocation) cellLocation;
                int cid = gsmCellLocation.getCid();
                int lac = gsmCellLocation.getLac();
                String networkOperator = telephonyManager.getNetworkOperator();
                if (networkOperator != null && networkOperator.length() >= 3) {
                    int mcc = Integer.parseInt(networkOperator.substring(0, 3));
                    int mnc = Integer.parseInt(networkOperator.substring(3));
                    Location location = getLastKnownLocation();
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.i("CellLocationGPS",
                                String.format(
                                        "Cell location: mcc=%d, mnc=%d, lac=%d, cid=%d, latitude=%f, longitude=%f",
                                        mcc, mnc, lac, cid, latitude, longitude));
                        String dataString = "Cell location:\n  mcc: " + mcc + ",\n  mnc: " + mnc + ",\n  lac: " + lac + ",\n  cid: " + cid + ",\n  lat: " + latitude + ",\n  lng: " + longitude + "\n";
                        this.datac[0].setText(mcc+"");
                        this.datac[1].setText(mnc+"");
                        this.datac[2].setText(lac+"");
                        this.datac[3].setText(cid+"");
                        this.datac[4].setText(latitude+"");
                        this.datac[5].setText(longitude+"");
                    } else {
                        Log.e("CellLocationGPS", "Unable to get location.");
                    }
                } else {
                    Log.e("CellLocationGPS", "Invalid network operator.");
                }
            } else {
                Log.e("CellLocationGPS", "Unsupported cell location type.");
            }
        }

    }

    private class LocationListenerImpl implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                Log.i("CellLocationGPS", String.format("Location: latitude=%f, longitude=%f", location.getLatitude(),
                        location.getLongitude()));
            } else {
                Log.e("CellLocationGPS", "Location is null.");
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                gpsEnabled = false;
            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                networkEnabled = false;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                gpsEnabled = true;
            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                networkEnabled = true;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Do nothing
        }

    }

    @SuppressLint("MissingPermission")
    private Location getLastKnownLocation() {
        Location gpsLocation = null;
        Location networkLocation = null;
        if (gpsEnabled) {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (networkEnabled) {
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (gpsLocation == null && networkLocation == null) {
            return null;
        } else if (gpsLocation != null && networkLocation != null) {
            long gpsTime = gpsLocation.getTime();
            long networkTime = networkLocation.getTime();
            if (gpsTime > networkTime) {
                return gpsLocation;
            } else {
                return networkLocation;
            }
        } else if (gpsLocation != null) {
            return gpsLocation;
        } else {
            return networkLocation;
        }
    }
}