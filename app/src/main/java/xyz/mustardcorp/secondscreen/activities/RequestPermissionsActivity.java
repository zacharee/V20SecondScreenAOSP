package xyz.mustardcorp.secondscreen.activities;

import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Activity to request runtime permissions directly from {@link xyz.mustardcorp.secondscreen.services.SignBoardService}
 */

public class RequestPermissionsActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();

        if (extras != null && extras.getString("permission") != null) {
            ActivityCompat.requestPermissions(this,
                    new String[]{extras.getString("permission")},
                    10001);
        }
        finish();
    }
}
