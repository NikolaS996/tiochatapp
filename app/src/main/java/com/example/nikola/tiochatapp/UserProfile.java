package com.example.nikola.tiochatapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.nikola.tiochatapp.Common.Common;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

public class UserProfile extends AppCompatActivity {

    EditText edtPassword, edtOldPassword, edtFullName, edtEmail, edtPhone;
    Button btnUpdate, btnCancel;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.user_update_logout:
                logOut();
                break;
            default:
                break;
        }
        return true;
    }

    private void logOut() {
        QBUsers.signOut().performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "You have successfully logged out!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserProfile.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //removing all previous activities
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_update_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //Adding the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.user_update_toolbar);
        toolbar.setTitle("Talk it Out");
        setSupportActionBar(toolbar);

        initViews();

        toolbar.requestFocus();

        loadUserProfile();

        btnCancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String password = edtPassword.getText().toString();
                String oldPassword = edtOldPassword.getText().toString();
                String email = edtEmail.getText().toString();
                String phone = edtPhone.getText().toString();
                String fullName = edtFullName.getText().toString();

                QBUser user = new QBUser();
                user.setId(QBChatService.getInstance().getUser().getId());
                if(!Common.isNullorEmptyString(oldPassword))
                    user.setOldPassword(oldPassword);
                if(!Common.isNullorEmptyString(password))
                    user.setPassword(password);
                if(!Common.isNullorEmptyString(fullName))
                    user.setFullName(fullName);
                if(!Common.isNullorEmptyString(email))
                    user.setEmail(email);
                if(!Common.isNullorEmptyString(phone))
                    user.setPhone(phone);

                final ProgressDialog mDialog = new ProgressDialog(UserProfile.this);
                mDialog.setMessage("Please wait...");
                mDialog.show();
                QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "User " + qbUser.getLogin() + " updated!", Toast.LENGTH_SHORT).show();
                        mDialog.dismiss();
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(UserProfile.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadUserProfile() {

        final QBUser currentUser = QBChatService.getInstance().getUser();

        String fullName = currentUser.getFullName();
        edtFullName.setText(fullName);

        String email = currentUser.getEmail();
        edtEmail.setText(email);

        String phone = currentUser.getPhone();
        edtPhone.setText(phone);
    }

    private void initViews() {
        btnCancel = (Button) findViewById(R.id.update_user_btnCancel);
        btnUpdate = (Button) findViewById(R.id.update_user_btnUpdate);

        edtEmail = (EditText) findViewById(R.id.update_edtEmail);
        edtPhone = (EditText) findViewById(R.id.update_edtPhone);
        edtFullName = (EditText) findViewById(R.id.update_edtFullName);
        edtPassword = (EditText) findViewById(R.id.update_edtPassword);
        edtOldPassword = (EditText) findViewById(R.id.update_edtOldPassword);

    }
}
