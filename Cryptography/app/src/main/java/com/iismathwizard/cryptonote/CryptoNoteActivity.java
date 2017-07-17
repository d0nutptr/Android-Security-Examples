package com.iismathwizard.cryptonote;

import android.arch.lifecycle.LifecycleActivity;

public class CryptoNoteActivity extends LifecycleActivity {
    protected CryptoNoteComponent getComponent(){
        return ((CryptoNoteApplication)getApplication()).getComponent();
    }
}
