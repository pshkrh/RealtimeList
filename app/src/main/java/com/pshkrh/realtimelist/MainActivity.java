package com.pshkrh.realtimelist;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.pshkrh.realtimelist.Adapter.ListItemAdapter;
import com.pshkrh.realtimelist.Model.ToDo;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmax.dialog.SpotsDialog;
import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    List<ToDo> mToDoList = new ArrayList<>();
    FirebaseFirestore db;
    FirebaseUser user;

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mDocsStorageReference;

    public static final String ANONYMOUS = "Anonymous";

    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;
    private static final int RC_PDF_PICKER = 3;

    RecyclerView listItem;
    RecyclerView.LayoutManager mLayoutManager;

    FloatingActionButton fab;
    ProgressBar mProgressBar;

    public MaterialEditText title, description;
    public boolean isUpdate = false;
    public String idUpdate = "";
    public int globalDeleteIndex = 0;
    public int globalUpdateIndex = 0;

    ListItemAdapter mListItemAdapter;

    SpotsDialog mAlertDialog;

    public String username;
    public String groupCode;

    Context mContext = this;

    ImageButton attach;

    Uri imageUri;
    Uri pdfUri;

    String imgName;
    String pdfName;

    TextView attachedFile;

    String finalAttachmentName="NONE";

    public DrawerLayout mDrawerLayout;
    public ActionBarDrawerToggle mActionBarDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,R.string.open,R.string.close);

        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);
        mActionBarDrawerToggle.syncState();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        attachedFile = (TextView)findViewById(R.id.attached_filename);

        mProgressBar.setVisibility(View.INVISIBLE);

        //Get username and group code from SplashActivity's Intent
        username = getIntent().getStringExtra("Username");
        if(username == null){
            username = ANONYMOUS;
        }
        groupCode = getIntent().getStringExtra("groupCode");

        //Initialize Firestore
        db = FirebaseFirestore.getInstance();

        //Initialize Firebase Storage
        mFirebaseStorage = FirebaseStorage.getInstance();
        mDocsStorageReference = mFirebaseStorage.getReference().child("docs");

        //Initialize all views
        mAlertDialog = new SpotsDialog(this);
        title = (MaterialEditText)findViewById(R.id.task_title);
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        description = (MaterialEditText)findViewById(R.id.task_description);
        description.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredTask = title.getText().toString();
                String enteredDescription = description.getText().toString();
                if(TextUtils.isEmpty(enteredTask)){
                    Toasty.info(MainActivity.this, "You must enter a task first", Toast.LENGTH_LONG).show();
                    return;
                }
                if(TextUtils.isEmpty(enteredDescription)){
                    Toasty.info(MainActivity.this, "You must enter a description first", Toast.LENGTH_LONG).show();
                    return;
                }
                if(enteredTask.length() < 5){
                    Toasty.info(MainActivity.this, "Task length must be more than 5 characters", Toast.LENGTH_LONG).show();
                    return;
                }
                if(enteredDescription.length() < 5){
                    Toasty.info(MainActivity.this, "Description length must be more than 5 characters", Toast.LENGTH_LONG).show();
                    return;
                }
                if(!isUpdate){
                    if(!((Activity) mContext).isFinishing())
                    {
                        mAlertDialog.show();
                    }
                    addTasks(title.getText().toString(),description.getText().toString(),username);
                    title.setText("");
                    description.setText("");
                    attachedFile.setText(getString(R.string.attach));
                    pdfName="";
                    imgName="";
                }
                else{
                    updateTasks(title.getText().toString(),description.getText().toString());
                    isUpdate = !isUpdate;
                }
            }
        });

        attach = findViewById(R.id.attach);
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final CharSequence docs[] = new CharSequence[] {"Image", "PDF"};

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Attach a document");
                builder.setItems(docs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(docs[which] == "Image"){
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/jpeg");
                            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
                        }
                        else{
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                            startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PDF_PICKER);
                        }
                    }
                });
                builder.show();
            }
        });

        listItem = (RecyclerView)findViewById(R.id.recycler);
        listItem.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        listItem.setLayoutManager(mLayoutManager);

        //Firestore Collection Listener
        db.collection(groupCode)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("ToDoList", "Listen failed.", e);
                            return;
                        }
                        Log.d("ToDoList", "Listener Ran");
                        loadTasks();
                    }
                });
    }


    //
    //
    //Add Tasks
    //
    //

    private void addTasks(String title, String description, String username) {
        final String id = UUID.randomUUID().toString();
        Map<String,Object> todo = new HashMap<>();
        todo.put("id",id);
        todo.put("title",title);
        todo.put("description",description);
        todo.put("username",username);
        todo.put("date", FieldValue.serverTimestamp());
        todo.put("file",finalAttachmentName);
        db.collection(groupCode).document(id)
                .set(todo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Refresh the data
                        Log.d("ToDoList","Added Task");
                        mListItemAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("ToDoList","Add Task Failed");
                    }
                });
    }

    //
    //
    //Update Tasks
    //
    //

    private void updateTasks(String title, String description) {
        String author = mToDoList.get(globalUpdateIndex).getUsername();
        if(username.equals(author)){
            if(!((Activity) mContext).isFinishing())
            {
                mAlertDialog.show();
            }
            db.collection(groupCode).document(idUpdate)
                    .update("title",title,"description",description)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toasty.info(MainActivity.this, "Updated Task", Toast.LENGTH_SHORT, true).show();
                            //globalUpdateId = idUpdate;
                            mListItemAdapter.notifyDataSetChanged();
                        }
                    });

            /*db.collection(groupCode).document(idUpdate)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                            Log.e("Todo","Local Update");
                            //loadTasks();
                            //loadData();
                        }
                    });*/
        }
        else{
            Toasty.error(MainActivity.this,"You cannot edit someone else's task", Toast.LENGTH_LONG, true).show();
        }

    }

    //
    //
    //Delete Tasks
    //
    //

    private void deleteTasks(final int index) {
        String author = mToDoList.get(index).getUsername();
        if(username.equals(author)){
            db.collection(groupCode)
                    .document(mToDoList.get(index).getId())
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mListItemAdapter.notifyDataSetChanged();
                            Toasty.info(MainActivity.this, "Deleted Task", Toast.LENGTH_SHORT, true).show();
                        }
                    });
        }
        else{
            Toasty.error(MainActivity.this,"You cannot delete someone else's task",Toast.LENGTH_SHORT,true).show();
        }

    }

    //
    //
    //Load Tasks
    //
    //

    public void loadTasks(){
        if(!((Activity) mContext).isFinishing()) {
            mAlertDialog.show();
        }
        if(mToDoList.size()>0){
            mToDoList.clear();
            mListItemAdapter.notifyDataSetChanged();
            Log.i("ToDoList", "List cleared");
        }
        db.collection(groupCode)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                ToDo todo = new ToDo(document.getString("id"),
                                        document.getString("title"),
                                        document.getString("description"),
                                        document.getString("username"),
                                        document.getString("file"));

                                Log.i("ToDoList", "Adding item to list");
                                mToDoList.add(todo);
                                //Log.d("ToDoList", document.getId() + " => " + document.getData());
                            }
                            mListItemAdapter = new ListItemAdapter(MainActivity.this,mToDoList);
                            listItem.setAdapter(mListItemAdapter);
                            mAlertDialog.dismiss();
                            Log.d("ToDoList","LoadTask ran");

                        } else {
                            Log.d("ToDoList", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    //
    //
    //Override Methods
    //
    //



    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals("Delete Task")){
            deleteTasks(item.getOrder());
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.second_menu,menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(mActionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }

        switch(item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
                return true;
            case R.id.change_group_menu:
                intent = new Intent(MainActivity.this,GroupCodeActivity.class);
                intent.putExtra("Username",username);
                startActivity(intent);
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_PHOTO_PICKER) {
            mProgressBar.setVisibility(View.VISIBLE);
            fab.setEnabled(false);
            final Uri selectedImageUri = data.getData();
            StorageReference photoRef = mDocsStorageReference.child(selectedImageUri.getLastPathSegment());

            photoRef.putFile(selectedImageUri).addOnSuccessListener
                    (this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mProgressBar.setVisibility(View.GONE);
                            fab.setEnabled(true);
                            imageUri = taskSnapshot.getDownloadUrl();
                            imgName = selectedImageUri.getLastPathSegment();
                            finalAttachmentName = imgName;
                            attachedFile.setText(imgName);
                        }
                    });
        }
        else if(requestCode == RC_PDF_PICKER){
            mProgressBar.setVisibility(View.VISIBLE);
            fab.setEnabled(false);
            final Uri selectedPdfUri = data.getData();
            StorageReference photoRef = mDocsStorageReference.child(selectedPdfUri.getLastPathSegment());

            photoRef.putFile(selectedPdfUri).addOnSuccessListener
                    (this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mProgressBar.setVisibility(View.GONE);
                            fab.setEnabled(true);
                            pdfUri = taskSnapshot.getDownloadUrl();
                            pdfName = selectedPdfUri.getLastPathSegment();
                            finalAttachmentName = pdfName;
                            attachedFile.setText(pdfName);
                        }
                    });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        switch(id){
            case R.id.nav_todo:
                Toast.makeText(mContext, "You are here!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_chat:
                Toast.makeText(mContext, "Group chat coming soon", Toast.LENGTH_SHORT).show();
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
        }

        return false;
    }
}