/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.parse.FunctionCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseCloud;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import java.util.ArrayList;
import java.util.HashMap;


import java.lang.Object;



public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener{

    private TextView lblLocation;

    private GoogleApiClient mGoogleApiClient;

    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;

    double latitude, longitude;

    float lat, longt, distance;

    EditText mEdit;

    final HashMap<String, Float> params = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

      lblLocation = (TextView) findViewById(R.id.lblLocation);

      if (checkGooglePlayServices()) {
          buildGoogleApiClient();

          //prepare connection request
          int permissionCheck = ContextCompat.checkSelfPermission
                  (this, Manifest.permission.ACCESS_FINE_LOCATION);

          if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
              //Execute location service call if user has explicitly granted ACCESS_FINE_LOCATION
              createLocationRequest();
          }

      }

    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

     /* Check if Google Play services are available on the device*/

    private boolean checkGooglePlayServices() {

        int checkGooglePlayServices = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {

            GooglePlayServicesUtil.getErrorDialog(checkGooglePlayServices,
                    this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();

            return false;
        }

        return true;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_RECOVER_PLAY_SERVICES) {

            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Google Play Services must be installed.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    public void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /* On connection success with Google Play Services, retrieve our coordinates*/
    @Override
    public void onConnected(Bundle bundle) {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            lblLocation.setText(latitude + ", " + longitude);

        } else {

            lblLocation.setText("(Couldn't get the location. Make sure is enabled on the device)");
        }
    startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();

        //Connect if Google Api Client
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }

    }

    /*Coordinates will be updated periodically*/

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(20000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();

        lblLocation.setText(latitude + ", " + longitude);

    }


    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null) {

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

    }

        /* Connecting the app with Parse and retreaving list of users*/


    /*When the button is pressed, collect the distance entered on the textfield and call
    the function usersInDistanceOfGeoPoint*/

    public void buttonPressed(View v){

        //Is current location available ?
        if (mLastLocation==null){
            Toast.makeText(MainActivity.this.getBaseContext(), "Current location is not available. Search interrupted",
                    Toast.LENGTH_SHORT).show();

            return;

        }

        mEdit   = (EditText)findViewById(R.id.editText);
        distance = Float.valueOf(mEdit.getText().toString());

        //Convert parameters to float and put them into the hashmap object
        putParameters();

        ParseCloud.callFunctionInBackground("usersInDistanceOfGeoPoint", params, new FunctionCallback<Object>() {

            @Override
            public void done(Object object, com.parse.ParseException e) {

                if (e == null) {
                    cloudFunctionSucceeded(object);


                } else {
                    cloudFunctionFailed();

                }
            }


        });


    }

    private void cloudFunctionSucceeded(Object object) {

        //Arraylist that will display the users in a ListView
        ArrayList<String> listUsers=new ArrayList<String>();

        Toast.makeText(MainActivity.this.getBaseContext(), "Retrieving users...",
                Toast.LENGTH_SHORT).show();


        String objectInText = object.toString();
        String userToAdd = "";


        //Get users id within the data recieved
        for (int i=0;i<objectInText.length();i++){
            if (objectInText.charAt(i)=='@'){
                int j=i+1;
                while((objectInText.charAt(j)!=',') && (objectInText.charAt(j)!=']') ){
                    userToAdd+=objectInText.charAt(j);
                    j++;
                }

                listUsers.add(userToAdd);
                userToAdd="";
            }
        }

        //Create new intent and run the new activity, sending the arraylist along with the users
        Intent intent = new Intent(this, ShowListUsers.class);
        intent.putStringArrayListExtra("users_list",listUsers);
        startActivity(intent);

    }

    private void cloudFunctionFailed() {
        Toast.makeText(MainActivity.this.getBaseContext(),"Error. Try again",
                Toast.LENGTH_SHORT).show();

    }


    public void putParameters(){

        //Cloud function must recieve float numbers
        lat = (float) latitude;
        longt = (float) longitude;

        params.put("latitude", lat);
        params.put("longitude", longt);
        params.put("distance",distance);
    }


}