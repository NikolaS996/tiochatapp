package com.example.nikola.tiochatapp;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.nikola.tiochatapp.Adapter.ListUsersAdapter;
import com.example.nikola.tiochatapp.Common.Common;
import com.example.nikola.tiochatapp.Holder.QBChatMessagesHolder;
import com.example.nikola.tiochatapp.Holder.QBUsersHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.utils.DialogUtils;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;

import java.util.ArrayList;
import java.util.List;

public class ListUsersActivity extends AppCompatActivity {

    ListView lstUsers;
    Button btnCreateChat;

    String mode = "";
    QBChatDialog qbChatDialog;
    List<QBUser> userAdd = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_users);

        mode = getIntent().getStringExtra(Common.UPDATE_MODE);
        qbChatDialog = (QBChatDialog) getIntent().getSerializableExtra(Common.UPDATE_DIALOG_EXTRA);



        lstUsers = (ListView) findViewById(R.id.lstUsers);
        lstUsers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        btnCreateChat = (Button)findViewById(R.id.btn_create_chat);
        btnCreateChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mode == null) {

                    int countChoice = lstUsers.getCount();

                    if (lstUsers.getCheckedItemPositions().size() == 1) {
                        createPrivateChat(lstUsers.getCheckedItemPositions());
                    } else if (lstUsers.getCheckedItemPositions().size() > 1) {
                        createGroupChat(lstUsers.getCheckedItemPositions());
                    } else {
                        Toast.makeText(ListUsersActivity.this, "Please select a friend to chat with", Toast.LENGTH_SHORT);
                    }
                }
                else if(mode.equals(Common.UPDATE_ADD_MODE) && qbChatDialog != null){
                    if(userAdd.size() > 0){
                        QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();

                        int countChoice = lstUsers.getCount();
                        if(countChoice > 0) {
                            SparseBooleanArray checkItemPositions = lstUsers.getCheckedItemPositions();
                            for (int i = 0; i < countChoice; i++) {
                                if (checkItemPositions.get(i)) {
                                    QBUser user = (QBUser) lstUsers.getItemAtPosition(i);
                                    requestBuilder.addUsers(user);
                                }
                            }

                            //Call services
                            QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    Toast.makeText(getBaseContext(), "User successfully added", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });
                        }
                        else
                            Toast.makeText(getBaseContext(), "Nobody was added.", Toast.LENGTH_SHORT).show();
                    }
                }

                else if(mode.equals(Common.UPDATE_REMOVE_MODE) && qbChatDialog != null){
                    if(userAdd.size() > 0){
                        QBDialogRequestBuilder requestBuilder = new QBDialogRequestBuilder();
                        int countChoice = lstUsers.getCount();
                            SparseBooleanArray checkItemPositions = lstUsers.getCheckedItemPositions();
                            for (int i = 0; i < countChoice; i++) {
                                if (checkItemPositions.get(i)) {
                                    QBUser user = (QBUser) lstUsers.getItemAtPosition(i);
                                    requestBuilder.removeUsers(user);
                                }
                            }

                            //Call services
                            QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    Toast.makeText(getBaseContext(), "User successfully removed!", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onError(QBResponseException e) {
                                }
                            });


                    }
                }
            }

        });

        if(mode == null && qbChatDialog == null)
            retrieveAllUser();
        else{
            if(mode.equals(Common.UPDATE_ADD_MODE))
                loadAvailableUsersList();
            else if(mode.equals(Common.UPDATE_REMOVE_MODE))
                loadUsersInGroupList();
        }
    }

    private void loadUsersInGroupList() {
        btnCreateChat.setText("Remove user");
        QBRestChatService.getChatDialogById(qbChatDialog.getDialogId()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                List<Integer> occupantsId = qbChatDialog.getOccupants();
                List<QBUser> listAlreadyInGroupUsers = QBUsersHolder.getInstance().getUsersByIds(occupantsId);
                ArrayList<QBUser> users = new ArrayList<QBUser>();
                users.addAll(listAlreadyInGroupUsers);

                ListUsersAdapter adapter = new ListUsersAdapter(getBaseContext(), users);
                lstUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                userAdd = users;
            }

            @Override
            public void onError(QBResponseException e) {
                Toast.makeText(ListUsersActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void loadAvailableUsersList() {
        btnCreateChat.setText("Add user");

        QBRestChatService.getChatDialogById(qbChatDialog.getDialogId()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                ArrayList<QBUser> listUsers = QBUsersHolder.getInstance().getAllUsers();
                List<Integer> occupantsId = qbChatDialog.getOccupants();
                List<QBUser> listAlreadyInChatGroupUsers = QBUsersHolder.getInstance().getUsersByIds(occupantsId);

                //Removing all users that are already in the chat group
                for(QBUser user : listAlreadyInChatGroupUsers)
                    listUsers.remove(user);

                if(listUsers.size() > 0){
                    ListUsersAdapter adapter = new ListUsersAdapter(getBaseContext(), listUsers);
                    lstUsers.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    userAdd = listUsers;
                }

            }

            @Override
            public void onError(QBResponseException e) {
                Toast.makeText(ListUsersActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createGroupChat(SparseBooleanArray checkedItemPositions) {
        final ProgressDialog mDialog = new ProgressDialog(ListUsersActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int countChoice = lstUsers.getCount();
        ArrayList<Integer> occupantIdsList = new ArrayList<>();

        for(int i = 0 ; i < countChoice ; i++){
            if(checkedItemPositions.get(i))
            {
                QBUser user = (QBUser) lstUsers.getItemAtPosition(i);
                occupantIdsList.add(user.getId());
            }
        }

        //Creating chat dialog
        QBChatDialog dialog = new QBChatDialog();
        dialog.setName(Common.createChatDialogName(occupantIdsList));
        dialog.setType(QBDialogType.GROUP);
        dialog.setOccupantsIds(occupantIdsList);

        QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                mDialog.dismiss();
                Toast.makeText(getBaseContext(), "Chat dialog successfully created", Toast.LENGTH_SHORT).show();

                //Sending system message to recipient
                QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                QBChatMessage qbChatMessage = new QBChatMessage();
                qbChatMessage.setBody(qbChatDialog.getDialogId());
                for(int i = 0 ; i < qbChatDialog.getOccupants().size() ; i++) {
                    qbChatMessage.setRecipientId(qbChatDialog.getOccupants().get(i));
                    try {
                        qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                    } catch (SmackException.NotConnectedException e) {
                        e.printStackTrace();
                    }
                }

                finish();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());
            }
        });
    }

    private void createPrivateChat(SparseBooleanArray checkedItemPositions) {

        final ProgressDialog mDialog = new ProgressDialog(ListUsersActivity.this);
        mDialog.setMessage("Please wait...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int countChoice = lstUsers.getCount();

        for(int i = 0 ; i < countChoice ; i++){
            if(checkedItemPositions.get(i))
            {
                final QBUser user = (QBUser) lstUsers.getItemAtPosition(i);
                QBChatDialog dialog = DialogUtils.buildPrivateDialog(user.getId());

                QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        mDialog.dismiss();
                        Toast.makeText(getBaseContext(), "Private chat dialog successfully created", Toast.LENGTH_SHORT).show();

                        //Sending system message to recipient
                        QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                        QBChatMessage qbChatMessage = new QBChatMessage();
                        qbChatMessage.setRecipientId(user.getId());
                        qbChatMessage.setBody(qbChatDialog.getDialogId());
                        try {
                            qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }

                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR", e.getMessage());
                    }
                });
            }
        }

    }

    private void retrieveAllUser() {
        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {

                QBUsersHolder.getInstance().putUsers(qbUsers);

                ArrayList<QBUser> qbUserWithoutCurrent = new ArrayList<QBUser>();
                for(QBUser user : qbUsers)
                {
                    if(!user.getLogin().equals(QBChatService.getInstance().getUser().getLogin()))
                        qbUserWithoutCurrent.add(user);
                }

                ListUsersAdapter adapter = new ListUsersAdapter(getBaseContext(), qbUserWithoutCurrent);
                lstUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());
            }
        });
    }
}
