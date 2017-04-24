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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import io.csuoh.hello.R;
import io.csuoh.hello.listeners.OnRecyclerClickListener;
import io.csuoh.hello.models.DatabaseMessage;

public class MessageAdapter extends RecyclerAdapter<DatabaseMessage, MessageAdapter.ViewHolder> {
    private final OnRecyclerClickListener mClickListener;
    private final Context mContext;

    public MessageAdapter(@NonNull List<DatabaseMessage> objects, @NonNull Context context, @Nullable OnRecyclerClickListener clickListener) {
        super(objects);
        mClickListener = clickListener;
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_message, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Timestamp format
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm aa", Locale.US);

        // Save current item
        DatabaseMessage message = mList.get(position);

        // Set values in View
        Glide.with(mContext)
                .load(message.photo)
                .into(holder.mMessageAvatar);
        holder.mMessage.setText(message.message);
        holder.mTimestamp.setText(dateFormat.format(new Date(message.timestamp)));
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final CircleImageView mMessageAvatar;
        final TextView mMessage, mTimestamp;

        ViewHolder(View view) {
            super(view);

            // Add listeners
            view.setOnClickListener(this);

            // Parse Layout elements from View
            mMessageAvatar = (CircleImageView) view.findViewById(R.id.image_message_avatar);
            mMessage = (TextView) view.findViewById(R.id.text_message);
            mTimestamp = (TextView) view.findViewById(R.id.text_message_timestamp);
        }

        @Override
        public void onClick(View v) {
            if (mClickListener != null) {
                mClickListener.onClick(getAdapterPosition());
            }
        }
    }
}
