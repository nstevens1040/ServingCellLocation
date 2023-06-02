package org.nanick.servingcelllocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.TextView;
import android.view.View;
import android.app.Activity;
import androidx.core.app.ActivityCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

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
        EventBus.getDefault().register(this);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gpsEnabled && !networkEnabled) {
            Log.e("CellLocationGPS", "No location providers enabled.");
        }
    }

    public void stop() {
        EventBus.getDefault().unregister(this);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        locationManager.removeUpdates(locationListener);
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EnbEvent event) {
        TextView[] value = event.getOo();
    }

    private class PhoneStateListenerImpl extends PhoneStateListener {
        public TextView[] datac;
        public TelephonyManager.CellInfoCallback cellInfoCallback;

        public PhoneStateListenerImpl(TextView[] dc) {
            this.datac = dc;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            EnbEvent msgEvent = new EnbEvent();
            msgEvent.tv = this.datac;
            EventBus.getDefault().postSticky(msgEvent);
            @SuppressLint("MissingPermission") CellLocation cellLocation = telephonyManager.getCellLocation();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                cellInfoCallback = new TelephonyManager.CellInfoCallback() {
                    @Override
                    public void onCellInfo(List<CellInfo> cellInfo) {
                        EnbEvent msg = EventBus.getDefault().getStickyEvent(EnbEvent.class);
                        TextView[] datac = msg.tv;
                        CellIdentityLte lte = (CellIdentityLte)cellInfo.get(0).getCellIdentity();
                        String pci = lte.getPci()+"";
                        if (cellLocation instanceof GsmCellLocation) {
                            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;

                            double signalDbm = 0.0;
                            int cid = gsmCellLocation.getCid();
                            int lac = gsmCellLocation.getLac();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                signalDbm = signalStrength.getCellSignalStrengths().get(0).getDbm();
                            }
                            String networkOperator = telephonyManager.getNetworkOperator();
                            if (networkOperator != null && networkOperator.length() >= 3) {
                                int mcc = Integer.parseInt(networkOperator.substring(0, 3));
                                int mnc = Integer.parseInt(networkOperator.substring(3));
                                Location location = getLastKnownLocation();
                                if (location != null) {
                                    double latitude = location.getLatitude();
                                    double longitude = location.getLongitude();
                                    String dataString = "Cell location:\n  signal: " + signalDbm + " dBm\n  mcc: " + mcc + ",\n  mnc: " + mnc + ",\n  lac: " + lac + ",\n  cid: " + cid + ",\n  lat: " + latitude + ",\n  lng: " + longitude + "\n";
                                    Log.i("CellLocationGPS",dataString);

                                    datac[0].setText(signalDbm+" dBm");
                                    datac[1].setText(mcc+"");
                                    datac[2].setText(mnc+"");
                                    datac[3].setText(lac+"");
                                    datac[4].setText(cid+"");
                                    datac[5].setText(pci);
                                    datac[6].setText(latitude+"");
                                    datac[7].setText(longitude+"");
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
                };
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                telephonyManager.requestCellInfoUpdate(context.getMainExecutor(), cellInfoCallback);
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