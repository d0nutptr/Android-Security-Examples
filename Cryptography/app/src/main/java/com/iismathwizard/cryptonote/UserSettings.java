package com.iismathwizard.cryptonote;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;

public class UserSettings {
    private MutableLiveData<Boolean> isUserRegistered = new MutableLiveData<>();
    private static final String USER_HASH = "user_hash";
    private static final String USER_ALGO = "user_algo";

    private SharedPreferences preferences;

    public UserSettings(SharedPreferences preferences) {
        this.preferences = preferences;

        isUserRegistered.postValue(getUserHash().length() > 0);
    }

    public LiveData<Boolean> isUserRegistered() {
        return isUserRegistered;
    }

    public String getUserAlgo() {
        return preferences.getString(USER_ALGO, "");
    }

    public String getUserHash() {
        return preferences.getString(USER_HASH, "");
    }

    public void setUserHash(String hashString) {
        preferences.edit().putString(USER_HASH, hashString).apply();

        isUserRegistered.postValue(getUserHash().length() > 0);
    }

    @SuppressLint("ApplySharedPref")
    public void setUserAlgo(String algo) {
        //we want this to apply immediately that's why we use commit.
        preferences.edit().putString(USER_ALGO, algo).commit();
    }
}
