package com.iismathwizard.cryptonote.Login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.iismathwizard.cryptonote.Crypto;
import com.iismathwizard.cryptonote.CryptoNoteActivity;
import com.iismathwizard.cryptonote.List.NotesListActivity;
import com.iismathwizard.cryptonote.R;
import com.iismathwizard.cryptonote.UserSettings;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

public class LoginActivity extends CryptoNoteActivity {
    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    @Inject
    public UserSettings settings;

    private Button authButton;
    private EditText passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authButton = findViewById(R.id.auth_button);
        passwordField = findViewById(R.id.password_field);

        getComponent().inject(this);

        settings.isUserRegistered().observe(this, isUserRegistered -> {
            if(isUserRegistered) {
                authButton.setText(R.string.account_login);
                authButton.setOnClickListener(view -> {
                    String password = passwordField.getText().toString();
                    passwordField.setText("");

                    if(password.length() < MINIMUM_PASSWORD_LENGTH) {
                        passwordField.setError(getResources().getString(R.string.password_length_error));
                        return;
                    }

                    authenticateUser(password);
                });
            } else {
                authButton.setText(R.string.account_create);
                authButton.setOnClickListener(view -> {
                    String password = passwordField.getText().toString();
                    passwordField.setText("");

                    if(password.length() < MINIMUM_PASSWORD_LENGTH) {
                        passwordField.setError(getResources().getString(R.string.password_length_error));
                        return;
                    }

                    createUser(password);
                });
            }
        });
    }

    private void authenticateUser(String password) {
        try {
            if(Crypto.compareUserHash(password, settings.getUserHash())){
                openNotesList();
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private void createUser(String password) {
        try {
            String result = Crypto.generateUserHash(password);
            settings.setUserHash(result);
            Crypto.createEncryptionKey();
            openNotesList();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void openNotesList(){
        Intent intent = new Intent(this, NotesListActivity.class);
        startActivity(intent);
    }
}
