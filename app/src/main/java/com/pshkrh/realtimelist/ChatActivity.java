package com.pshkrh.realtimelist;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pshkrh.realtimelist.Adapter.MessageAdapter;
import com.pshkrh.realtimelist.Model.Message;
import com.pshkrh.realtimelist.Model.ToDo;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "ChatActivity";
    List<Message> chatList = new ArrayList<>();

    public static final String ANONYMOUS = "Anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public Context mContext = this;

    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private MessageAdapter mMessageAdapter;

    private ProgressBar mProgressBar;
    private MaterialEditText mMessageEditText;
    private ImageButton mSendButton;
    private String mUsername;
    private String groupCode;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;

    public DrawerLayout mDrawerLayout;
    public ActionBarDrawerToggle mActionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.chat_drawer_layout);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,R.string.open,R.string.close);

        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);
        mActionBarDrawerToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_chat);

        groupCode = getIntent().getStringExtra("Group Code");
        mUsername = getIntent().getStringExtra("Username");

        String groupName = groupCode + "'s Group Chat";

        setTitle(groupName);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(groupCode);

        attachReadListener();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.VISIBLE);

        mMessageEditText = (MaterialEditText) findViewById(R.id.chat_edittext);
        mSendButton = (ImageButton) findViewById(R.id.send_button);

        mRecyclerView = (RecyclerView)findViewById(R.id.chat_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message(mMessageEditText.getText().toString(), mUsername);
                mMessagesDatabaseReference.push().setValue(message);
                // Clear input box
                mMessageEditText.setText("");
            }
        });
    }

    /*
    Required Methods
     */

    private void onSignedInInitialize(String username){
        mUsername = username;
        attachReadListener();
    }

    private void onSignedOutCleanup(){
        mUsername = ANONYMOUS;
        chatList.clear();
        mMessageAdapter.notifyDataSetChanged();

    }

    private void attachReadListener(){
        if(mChildEventListener == null){
            mChildEventListener  = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Message message = dataSnapshot.getValue(Message.class);
                    mMessageAdapter = new MessageAdapter(ChatActivity.this, chatList);
                    mRecyclerView.setAdapter(mMessageAdapter);
                    chatList.add(message);
                    mRecyclerView.getLayoutManager().scrollToPosition(mRecyclerView.getAdapter().getItemCount()-1);
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener(){
        if(mChildEventListener!=null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // set item as selected to persist highlight
        item.setChecked(true);
        // close drawer when item is tapped
        mDrawerLayout.closeDrawers();
        item.setChecked(false);
        int id = item.getItemId();

        switch(id){
            case R.id.nav_todo:
                Intent intent = new Intent(ChatActivity.this,MainActivity.class);
                intent.putExtra("groupCode",groupCode);
                intent.putExtra("Username",mUsername);
                startActivity(intent);
                break;

            case R.id.nav_chat:
                Toast.makeText(mContext, "You are here!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_about:
                Toast.makeText(mContext, "Made by Pushkar Kurhekar", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_contact:
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                emailIntent.setType("vnd.android.cursor.item/email");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {"dev@pshkrh.com"});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Feedback / Query regarding To-Do List");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
                startActivity(Intent.createChooser(emailIntent, "Send mail using..."));
                break;

            case R.id.nav_change_group:
                intent = new Intent(ChatActivity.this,GroupCodeActivity.class);
                intent.putExtra("Username",mUsername);
                startActivity(intent);
                finish();
                break;

            case R.id.nav_signout:
                AuthUI.getInstance().signOut(this);
                intent = new Intent(ChatActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
                break;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mActionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

    }
}
