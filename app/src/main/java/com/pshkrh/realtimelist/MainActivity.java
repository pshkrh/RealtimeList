package com.pshkrh.realtimelist;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.sip.SipSession;
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
import android.webkit.MimeTypeMap;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
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

    private int FLAG = 0;

    private static final int RESULT_OK = 4;

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

    ImageButton paperclip;

    String imageUri="NONE";

    String imgName;
    String pdfName;

    public String updateTitle="";
    public String updateDescription="";

    TextView attachedFile;
    TextView progressText;

    //String finalAttachmentName="NONE";

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

        navigationView.setCheckedItem(R.id.nav_todo);

        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        attachedFile = (TextView)findViewById(R.id.attached_filename);
        progressText = (TextView)findViewById(R.id.progress_percent);

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
        mDocsStorageReference = mFirebaseStorage.getReference().child("docs/");

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
                }
                else{
                    updateTasks(title.getText().toString(),description.getText().toString());
                    isUpdate = !isUpdate;
                }
                title.setText("");
                description.setText("");
                progressText.setText("");
                attachedFile.setText(getString(R.string.attach));
                imageUri="NONE";
                isUpdate=false;
            }
        });

        attach = findViewById(R.id.attach);
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        attachedFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        listItem = (RecyclerView)findViewById(R.id.recycler);
        listItem.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        listItem.setLayoutManager(mLayoutManager);

        //Firestore Collection Listener
        /*db.collection(groupCode)
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
                        imageUri="NONE";
                    }
                });*/

        load2();
        //loadTasks();
    }


    public String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    public String returnImageUri(){
        return imageUri;
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
        todo.put("file",imageUri);
        db.collection(groupCode).document(id)
                .set(todo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Refresh the data
                        Log.d("ToDoList","Added Task");
                        //mListItemAdapter.notifyDataSetChanged();
                        //collectionListener();
                        load2();
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
                            //mListItemAdapter.notifyDataSetChanged();
                            //collectionListener();
                            load2();
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
                            //mListItemAdapter.notifyDataSetChanged();
                            Toasty.info(MainActivity.this, "Deleted Task", Toast.LENGTH_SHORT, true).show();
                            //collectionListener();
                            load2();
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
        //imageUri="NONE";
        if(mToDoList.size()>0){
            mToDoList.clear();
            //mListItemAdapter.notifyDataSetChanged();
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

                                Log.i("ToDoList", "Adding item to list (loadtask)");
                                mToDoList.add(todo);
                                //Log.d("ToDoList", document.getId() + " => " + document.getData());
                            }
                            mListItemAdapter = new ListItemAdapter(MainActivity.this,mToDoList);
                            listItem.setAdapter(mListItemAdapter);
                            mAlertDialog.dismiss();
                            Log.d("ToDoList","LoadTask ran");
                            FLAG=1;

                        } else {
                            Log.d("ToDoList", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    //
    //
    // Load Task 2
    //
    //

    public void load2(){
        //if(FLAG==1) {
            db.collection(groupCode)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            mToDoList.clear();
                            Log.i("ToDoList", "List cleared in load2");
                            if (e != null) {
                                Log.w("ToDoList", "Listen failed.", e);
                                return;
                            }

                            //List<String> cities = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                if (doc.get("id") != null) {
                                    ToDo test = new ToDo(doc.get("id").toString(), doc.get("title").toString(), doc.get("description").toString(), doc.get("username").toString(), doc.get("file").toString());
                                    mToDoList.add(test);
                                }
                            }
                            mListItemAdapter = new ListItemAdapter(MainActivity.this, mToDoList);
                            listItem.setAdapter(mListItemAdapter);
                            mAlertDialog.dismiss();
                            Log.d("ToDoList", "Load2 Ran");
                        }
                    });
       // }
    }

    /*public void load3(){
        db.collection("cities")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {

                        mToDoList.clear();
                        Log.i("ToDoList", "List cleared in load3");
                        if (e != null) {
                            Log.w("ToDoList", "Listen failed.", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {

                                case ADDED:
                                    QueryDocumentSnapshot doc = dc.getDocument();
                                    ToDo test = new ToDo(doc.get("id").toString(), doc.get("title").toString(), doc.get("description").toString(), doc.get("username").toString(), doc.get("file").toString());
                                    mToDoList.add(test);
                                    break;
                                case MODIFIED:
                                    loadTasks();
                                    Log.d("ToDoList", "Modified: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    loadTasks();
                                    Log.d("ToDoList", "Removed: " + dc.getDocument().getData());
                                    break;
                            }
                        }

                        mListItemAdapter = new ListItemAdapter(MainActivity.this, mToDoList);
                        listItem.setAdapter(mListItemAdapter);
                        mAlertDialog.dismiss();
                        Log.d("ToDoList", "Load3 Ran");

                    }
                });
    }*/



    //
    //
    // Collection Listener Function
    //
    //

    public void collectionListener() {
        db.collection(groupCode)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("ToDoList", "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Map<String,Object> m = dc.getDocument().getData();
                                    Log.i("ToDoList",m.get("Title").toString());
                                    //mToDoList.add();
                                    break;
                            }
                        }

                    }
                });
    }
                /*.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("ToDoList", "Listen failed.", e);
                            return;
                        }
                        Log.d("ToDoList", "Listener Ran");
                        loadTasks();
                        imageUri="NONE";
                    }
                });*/

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
        else if(item.getTitle().equals("Edit Task")){
            isUpdate = true;
            int index = item.getOrder();
            idUpdate = mToDoList.get(index).getId();
            updateTitle = mToDoList.get(index).title;
            if(!updateTitle.equals("")){
                title.setText(updateTitle);
            }
            updateDescription = mToDoList.get(index).description;
            if(!updateDescription.equals("")){
                description.setText(updateDescription);
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(mActionBarDrawerToggle.onOptionsItemSelected(item)){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            mProgressBar.setVisibility(View.VISIBLE);
            fab.setVisibility(View.INVISIBLE);
            final Uri selectedImageUri = data.getData();
            StorageReference photoRef = mDocsStorageReference.child(selectedImageUri.getLastPathSegment());

            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener
                    (this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mProgressBar.setVisibility(View.GONE);
                            fab.setVisibility(View.VISIBLE);
                            imageUri = taskSnapshot.getDownloadUrl().toString();
                            Toasty.success(mContext, "File Uploaded Successfully!", Toast.LENGTH_SHORT).show();

                            imgName = selectedImageUri.getLastPathSegment();
                            //finalAttachmentName = imgName;
                            attachedFile.setText(imgName);
                            progressText.setText("");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mProgressBar.setVisibility(View.GONE);
                            fab.setVisibility(View.VISIBLE);
                            imageUri="NONE";
                            Toasty.error(mContext, "Upload failed, please try again", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            String per = ((int) progress) + "%";
                            progressText.setText(per);
                        }
                    });
        }
        /*else if(requestCode == RC_PDF_PICKER){
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
        }*/
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
                Toast.makeText(mContext, "You are here!", Toast.LENGTH_SHORT).show();
                break;

            case R.id.nav_chat:
                Intent intent = new Intent(MainActivity.this,ChatActivity.class);
                intent.putExtra("Group Code",groupCode);
                intent.putExtra("Username",username);
                startActivity(intent);
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
                intent = new Intent(MainActivity.this,GroupCodeActivity.class);
                intent.putExtra("Username",username);
                startActivity(intent);
                finish();
                break;

            case R.id.nav_signout:
                AuthUI.getInstance().signOut(this);
                intent = new Intent(MainActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
                break;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(MainActivity.this,GroupCodeActivity.class);
        startActivity(intent);
    }
}