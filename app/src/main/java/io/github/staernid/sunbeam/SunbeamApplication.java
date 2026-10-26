package io.github.staernid.sunbeam;

import android.app.Application;
import android.widget.Toast;

import io.github.staernid.sunbeam.profiles.ProfilesManager;

public class SunbeamApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (getBaseContext() != null) {
            ProfilesManager profilesManager = ProfilesManager.getInstance();
            if (!profilesManager.load(this)) {
                Toast.makeText(this, R.string.profile_manager_failed_to_load, Toast.LENGTH_LONG).show();
            }
        }
    }
}