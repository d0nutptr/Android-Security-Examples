package com.iismathwizard.cryptonote;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class UserData {
    private MutableLiveData<Boolean> isUserRegistered = new MutableLiveData<>();
    private static final String USER_HASH = "user_hash";
    private SharedPreferences preferences;

    public UserData(SharedPreferences preferences) {
        this.preferences = preferences;

        isUserRegistered.postValue(preferences.getString(USER_HASH, "").length() > 0);
    }

    public LiveData<Boolean> isUserRegistered() {
        return isUserRegistered;
    }

    public String getUserHash() {
        try {
            return Crypto.decryptHash(preferences.getString(USER_HASH, ""));
        } catch (GeneralSecurityException | IOException e) {
            return null;
        }
    }

    public void setUserHash(String hashString) {
        /*
        We need to provide some form of integrity on the user's hash to prevent someone stealing the
        device, running adb shell, run-as 'application-id', and editing the stored hash. If the hash
        is altered, then anyone else can log in. We store an encryption key in the keystore to prevent
        this. AES-GCM can detect alterations. An altered hash would prevent anyone from logging in.
        The confidentiality this also provides is just a nice touch which prevents offline cracking.

        Alternative solutions are:
        * Make user encryption key rely on user's password (pbkdf2)
        * Do signing instead of AEAD (confidentiality property was nice so I chose AEAD)
        */
        try {
            preferences.edit().putString(USER_HASH, Crypto.encryptHash(hashString)).apply();
        } catch (GeneralSecurityException | IOException e) {
            return;
        }

        isUserRegistered.postValue(hashString.length() > 0);
    }
}
