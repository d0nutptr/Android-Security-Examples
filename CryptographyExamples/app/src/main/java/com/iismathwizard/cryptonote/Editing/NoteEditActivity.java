package com.iismathwizard.cryptonote.Editing;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.iismathwizard.cryptonote.CryptoNoteActivity;
import com.iismathwizard.cryptonote.Notes.Note;
import com.iismathwizard.cryptonote.Notes.NotesRepository;
import com.iismathwizard.cryptonote.R;

import javax.inject.Inject;

public class NoteEditActivity extends CryptoNoteActivity {
    public static final String EXTRA_ID = "NOTE_ID";

    private int id;
    private Toolbar toolbar;
    private EditText titleField;
    private EditText contentField;

    private Note note;

    @Inject
    public NotesRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_edit);

        getComponent().inject(this);

        id = getIntent().getIntExtra(EXTRA_ID, -1);

        titleField = findViewById(R.id.title);
        contentField = findViewById(R.id.content);
        toolbar = findViewById(R.id.toolbar_note_edit);
        toolbar.inflateMenu(R.menu.edit_menu);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_36dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
                finish();
            }
        });

        repository.getNote(id).observe(this, new Observer<Note>() {
            @Override
            public void onChanged(@Nullable Note note) {
                if(note != null){
                    NoteEditActivity.this.note = note;
                    titleField.setText(note.getTitle());
                    contentField.setText(note.getContents());
                }
            }
        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.remove) {
                    repository.deleteNote(note);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    public void save(){
        note.setTitle(titleField.getText().toString());
        note.setContents(contentField.getText().toString());
        repository.saveNote(note);
    }

    @Override
    public void onBackPressed() {
        save();
        super.onBackPressed();
    }
}
