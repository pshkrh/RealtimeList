package com.pshkrh.realtimelist.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pshkrh.realtimelist.ChatActivity;
import com.pshkrh.realtimelist.MainActivity;
import com.pshkrh.realtimelist.Model.Message;
import com.pshkrh.realtimelist.Model.ToDo;
import com.pshkrh.realtimelist.R;

import java.util.List;

class MessageViewHolder extends RecyclerView.ViewHolder {

    TextView messageText, messageSender;

    public MessageViewHolder(View messageView) {
        super(messageView);
        messageText = (TextView)messageView.findViewById(R.id.message_text);
        messageSender = (TextView)messageView.findViewById(R.id.message_sender);
    }

}

public class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    ChatActivity mChatActivity;
    List<Message> messages;

    public MessageAdapter(ChatActivity chatActivity, List<Message> messages) {
        this.mChatActivity = chatActivity;
        this.messages = messages;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mChatActivity.getBaseContext());
        View view = inflater.inflate(R.layout.chat_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        holder.messageText.setText(messages.get(position).getText());
        holder.messageSender.setText(messages.get(position).getSender());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

}
