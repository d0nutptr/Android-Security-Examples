package com.iismathwizard.cryptonote.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.iismathwizard.cryptonote.Crypto;
import com.iismathwizard.cryptonote.CryptoNoteActivity;
import com.iismathwizard.cryptonote.Editing.NoteEditActivity;
import com.iismathwizard.cryptonote.Notes.Note;
import com.iismathwizard.cryptonote.Notes.NotesRepository;
import com.iismathwizard.cryptonote.R;
import com.iismathwizard.cryptonote.UserData;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class NotesListActivity extends CryptoNoteActivity {
    @Inject
    public UserData userData;

    @Inject
    public NotesRepository repository;

    private Toolbar toolbar;
    private ListView notesList;
    private FloatingActionButton fab;
    private NotesListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_list);

        getComponent().inject(this);

        notesList = findViewById(R.id.notes_list);
        toolbar = findViewById(R.id.toolbar_note_list);
        fab = findViewById(R.id.floatingActionButton);

        toolbar.setTitle("Your Notes");
        toolbar.setTitleTextColor(0xFFFFFFFF);

        adapter = new NotesListAdapter();
        notesList.setAdapter(adapter);
        notesList.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = new Intent(NotesListActivity.this, NoteEditActivity.class);
            intent.putExtra(NoteEditActivity.EXTRA_ID, adapter.getItem(i).getId());
            startActivity(intent);
        });

        repository.getNotes().observe(this, notes -> {
            adapter.notes = notes;
            adapter.notifyDataSetChanged();
        });

        fab.setOnClickListener(view -> new Thread(() -> repository.createNote()).start());
    }

    private class NotesListAdapter extends BaseAdapter {
        private List<Note> notes = new ArrayList<>();

        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public Note getItem(int i) {
            return notes.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View itemView = view;

            if(itemView == null) {
                LayoutInflater inflater = LayoutInflater.from(NotesListActivity.this);
                itemView = inflater.inflate(R.layout.note_list_item, viewGroup, false);
            }

            TextView title = itemView.findViewById(R.id.title);
            TextView date = itemView.findViewById(R.id.date);

            Note note = notes.get(i);

            title.setText(Crypto.decrypt(note.getTitle()));
            date.setText(Crypto.decrypt(note.getDate()));

            return itemView;
        }
    }
}
