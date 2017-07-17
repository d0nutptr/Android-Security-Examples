package com.iismathwizard.cryptonote;


import com.iismathwizard.cryptonote.Editing.NoteEditActivity;
import com.iismathwizard.cryptonote.List.NotesListActivity;
import com.iismathwizard.cryptonote.Login.LoginActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {UserModule.class})
public interface CryptoNoteComponent {
    void inject(LoginActivity activity);
    void inject(NotesListActivity activity);
    void inject(NoteEditActivity activity);
}
