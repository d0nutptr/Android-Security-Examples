package com.iismathwizard.cryptonote;

import android.app.Application;

public class CryptoNoteApplication extends Application {
    public static final String PREFERENCES = "applicationPreferences";
    private CryptoNoteComponent component;

    @Override
    public void onCreate() {
        super.onCreate();

        component = DaggerCryptoNoteComponent.builder()
                .userModule(new UserModule(this, getSharedPreferences(PREFERENCES, MODE_PRIVATE)))
                .build();
    }

    public CryptoNoteComponent getComponent() {
        return component;
    }
}
