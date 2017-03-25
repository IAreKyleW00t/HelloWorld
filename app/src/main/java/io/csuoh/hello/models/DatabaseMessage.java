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
package io.csuoh.hello.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DatabaseMessage {
    public String user, photo, message, timestamp;

    public DatabaseMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(DatabaseMessage.class)
    }

    public DatabaseMessage(String user, String photo, String message, String timestamp) {
        this.user = user;
        this.photo = photo;
        this.message = message;
        this.timestamp = timestamp;
    }
}
