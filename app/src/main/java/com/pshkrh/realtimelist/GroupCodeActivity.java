package com.pshkrh.realtimelist;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;

import java.security.acl.Group;

import es.dmoral.toasty.Toasty;

public class GroupCodeActivity extends AppCompatActivity {

    public static final String ANONYMOUS = "Anonymous";
    private static final String TAG = "SelectActivity";
    private String mUsername;
    public String imgUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_code);
        mUsername = getIntent().getStringExtra("Username");
        imgUrl = getIntent().getStringExtra("Image Url");

        Button chatBtn = (Button) findViewById(R.id.button2);
        final EditText edit = (EditText) findViewById(R.id.editText);

        chatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String txt = edit.getText().toString();
                if (txt.matches("")) {
                    Toasty.warning(GroupCodeActivity.this, "Please enter a Group Code", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(GroupCodeActivity.this, MainActivity.class);
                    intent.putExtra("groupCode", txt);
                    intent.putExtra("Username", mUsername);
                    intent.putExtra("Image Url", imgUrl);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                Intent intent = new Intent(GroupCodeActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed(){
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}
