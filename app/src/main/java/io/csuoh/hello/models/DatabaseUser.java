/**
 * Group 18
 * Kyle Colantonio, 2595744
 * 4/28/2017
 *
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
public class DatabaseUser {
    public String name = null, photo = null;
    public Map<String, Boolean> groups = new HashMap<>();

    public DatabaseUser() {
        // Default constructor required for calls to DataSnapshot.getValue(DatabaseUser.class)
    }

    public DatabaseUser(String name, String photo, Map<String, Boolean> groups) {
        this.name = name;
        this.photo = photo;
        this.groups = groups;
    }
}
