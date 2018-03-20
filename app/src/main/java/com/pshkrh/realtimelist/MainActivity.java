package com.pshkrh.realtimelist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.pshkrh.realtimelist.Adapter.ListItemAdapter;
import com.pshkrh.realtimelist.Model.ToDo;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dmax.dialog.SpotsDialog;
import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    List<ToDo> mToDoList = new ArrayList<>();
    FirebaseFirestore db;
    FirebaseUser user;
    public static final String ANONYMOUS = "Anonymous";

    RecyclerView listItem;
    RecyclerView.LayoutManager mLayoutManager;

    FloatingActionButton fab;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get username and group code from SplashActivity's Intent
        username = getIntent().getStringExtra("Username");
        if(username == null){
            username = ANONYMOUS;
        }
        groupCode = getIntent().getStringExtra("groupCode");

        //Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
                    setData(title.getText().toString(),description.getText().toString(),username);
                    title.setText("");
                    description.setText("");
                }
                else{
                    updateData(title.getText().toString(),description.getText().toString());
                    isUpdate = !isUpdate;
                }
            }
        });

        listItem = (RecyclerView)findViewById(R.id.recycler);
        listItem.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        listItem.setLayoutManager(mLayoutManager);

        loadData(); //Load data from FireStore
    }

    private void updateData(String title, String description) {
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
                        }
                    });

            db.collection("ToDoList").document(idUpdate)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                            Log.e("Todo","Local Update");
                            loadData();
                        }
                    });
        }
        else{
            Toasty.error(MainActivity.this,"You cannot edit someone else's task", Toast.LENGTH_LONG, true).show();
        }

    }

    private void setData(String title, String description, String username) {
        final String id = UUID.randomUUID().toString();
        Map<String,Object> todo = new HashMap<>();
        todo.put("id",id);
        todo.put("title",title);
        todo.put("description",description);
        todo.put("username",username);

        db.collection(groupCode).document(id)
                .set(todo)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //Refresh the data
                        loadData();
                        idUpdate = id;
                    }
                });
    }

    private void loadData(){
        if(!((Activity) mContext).isFinishing())
        {
            mAlertDialog.show();
        }
        if(mToDoList.size()>0){
            mToDoList.clear();
        }
        //Collection Listener
        db.collection(groupCode)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            System.err.println("Listen failed: " + e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    if(!((Activity) mContext).isFinishing())
                                    {
                                        mAlertDialog.show();
                                    }
                                    String tempId = dc.getDocument().getData().get("id").toString();
                                    String tempTitle = dc.getDocument().getData().get("title").toString();
                                    String tempDesc = dc.getDocument().getData().get("description").toString();
                                    String tempUser = dc.getDocument().getData().get("username").toString();
                                    ToDo todo = new ToDo(tempId,tempTitle,tempDesc,tempUser);
                                    mToDoList.add(todo);
                                    mListItemAdapter = new ListItemAdapter(MainActivity.this, mToDoList);
                                    listItem.setAdapter(mListItemAdapter);
                                    mAlertDialog.dismiss();
                                    break;
                                case MODIFIED:
                                    String tempUpdateTitle = dc.getDocument().getData().get("title").toString();
                                    String tempUpdateDesc = dc.getDocument().getData().get("description").toString();
                                    globalUpdateData(tempUpdateTitle,tempUpdateDesc);
                                    break;
                                case REMOVED:
                                    globalDeleteTask(globalDeleteIndex);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals("Delete Task")){
            deleteTask(item.getOrder());
            globalDeleteIndex = item.getOrder();
        }
        return super.onContextItemSelected(item);
    }

    private void deleteTask(final int index) {
        String author = mToDoList.get(index).getUsername();
        if(username.equals(author)){
            db.collection(groupCode)
                    .document(mToDoList.get(index).getId())
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if(mToDoList.size()>0){
                                mToDoList.clear();
                            }
                            loadData();
                            globalDeleteIndex = index;
                            Toasty.info(MainActivity.this, "Deleted Task", Toast.LENGTH_SHORT, true).show();
                        }
                    });
        }
        else{
            Toasty.error(MainActivity.this,"You cannot delete someone else's task",Toast.LENGTH_SHORT,true).show();
        }

    }

    private void globalDeleteTask(int index){
            db.collection(groupCode)
                    .document(mToDoList.get(index).getId())
                    .delete()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if(mToDoList.size()>0){
                                mToDoList.clear();
                            }
                            loadData();
                            //Toasty.info(MainActivity.this, "Deleted Task", Toast.LENGTH_SHORT, true).show();
                        }
                    });
    }

    private void globalUpdateData(String title, String description) {
        String author = mToDoList.get(globalUpdateIndex).getUsername();
        String globalUpdateId = mToDoList.get(globalUpdateIndex).getId();
            if(!((Activity) mContext).isFinishing())
            {
                mAlertDialog.show();
            }
            db.collection(groupCode).document(globalUpdateId)
                    .update("title",title,"description",description)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toasty.info(MainActivity.this, "Updated Task", Toast.LENGTH_SHORT, true).show();
                            //globalUpdateId = idUpdate;
                        }
                    });

            db.collection("ToDoList").document(globalUpdateId)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                            Log.e("Todo","Global Update");
                            loadData();
                        }
                    });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.second_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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



}
