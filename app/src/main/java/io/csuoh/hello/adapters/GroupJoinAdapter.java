/*
 * Copyright (C) 2017  Kyle Colantonio <kyle10468@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.csuoh.hello.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.csuoh.hello.R;
import io.csuoh.hello.models.DatabaseGroup;

public class GroupJoinAdapter extends RecyclerAdapter<DatabaseGroup, GroupJoinAdapter.ViewHolder> {

    public GroupJoinAdapter(@NonNull final List<DatabaseGroup> objects) {
        super(objects);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_group_join, parent, false));
    }

    @Override
    public void onBindViewHolder(GroupJoinAdapter.ViewHolder holder, int position) {
        // Save current item
        DatabaseGroup group = mList.get(position);

        // Set custom user String
        String users;
        if (group.users == null || group.users.size() < 1) { // No users
            users = "No users in chat";
        } else if (group.users.size() == 1) { // Exactly 1 user
            users = group.users.size() + " user in chat";
        } else { // 2 or more users
            users = group.users.size() + " users in chat";
        }

        // Set values in View
        holder.mName.setText(group.name);
        holder.mDescription.setText(group.description);
        holder.mUsers.setText(users);
        holder.mTimestamp.setText(group.timestamp);
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView mName, mDescription, mUsers, mTimestamp;

        ViewHolder(View view) {
            super(view);

            // Add listeners
            view.setOnClickListener(this);

            // Parse Layout elements from View
            mName = (TextView) view.findViewById(R.id.text_group_name);
            mDescription = (TextView) view.findViewById(R.id.text_group_description);
            mUsers = (TextView) view.findViewById(R.id.text_group_users);
            mTimestamp = (TextView) view.findViewById(R.id.text_group_timestamp);
        }

        @Override
        public void onClick(View v) {
            // TODO: Join and open group chat
            int position = getAdapterPosition();
            Log.d("GroupAdapter", "POSITION = " + position + "\n" + v.toString());
        }
    }
}
