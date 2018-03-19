package com.pshkrh.realtimelist.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pshkrh.realtimelist.MainActivity;
import com.pshkrh.realtimelist.Model.ToDo;
import com.pshkrh.realtimelist.R;

import java.util.List;

/**
 * Created by pshkr on 18-03-2018.
 */

class ListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {

    ItemClickListener mItemClickListener;
    TextView itemTitle, itemDescription,itemUsername;

    public ListItemViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        itemView.setOnCreateContextMenuListener(this);

        itemTitle = (TextView)itemView.findViewById(R.id.item_title);
        itemDescription = (TextView)itemView.findViewById(R.id.item_description);
        itemUsername = (TextView)itemView.findViewById(R.id.item_username);
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.mItemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View view) {
        mItemClickListener.onClick(view,getAdapterPosition(),false);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Select an action");
        menu.add(0,0,getAdapterPosition(),"Delete Task");
    }
}

public class ListItemAdapter extends RecyclerView.Adapter<ListItemViewHolder> {

    MainActivity mainActivity;
    List<ToDo> todoList;

    public ListItemAdapter(MainActivity mainActivity, List<ToDo> todoList) {
        this.mainActivity = mainActivity;
        this.todoList = todoList;
    }

    @Override
    public ListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mainActivity.getBaseContext());
        View view = inflater.inflate(R.layout.list_layout,parent,false);
        return new ListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListItemViewHolder holder, int position) {
        holder.itemTitle.setText(todoList.get(position).getTitle());
        holder.itemDescription.setText(todoList.get(position).getDescription());
        holder.itemUsername.setText(todoList.get(position).getUsername());

        holder.setItemClickListener(new ItemClickListener() {
            @Override
            public void onClick(View view, int position, boolean isLongClick) {
                //When user taps the card, the text enters the MaterialEditText above
                mainActivity.title.setText(todoList.get(position).getTitle());
                mainActivity.description.setText(todoList.get(position).getDescription());

                mainActivity.isUpdate = true;
                mainActivity.idUpdate = todoList.get(position).getId();
                mainActivity.globalUpdateIndex = todoList.get(position).getPosition(position);

            }
        });
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }
}
