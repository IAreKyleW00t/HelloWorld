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

import org.parceler.Parcel;

import java.util.HashMap;
import java.util.Map;

@Parcel
@IgnoreExtraProperties
public class DatabaseGroup {
    public int id = -1;
    public String name = null, description = null, last_message = null;
    public long timestamp =  -1L;
    public Map<String, Boolean> users = new HashMap<>();

    public DatabaseGroup() {
        // Default constructor required for calls to DataSnapshot.getValue(DatabaseGroup.class)
    }

    public DatabaseGroup(int id, String name, String description, String last_message, long timestamp, Map<String, Boolean> users) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.last_message = last_message;
        this.timestamp = timestamp;
        this.users = users;
    }
}
