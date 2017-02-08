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

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.List;

@IgnoreExtraProperties
public class DatabaseUser {
    public String name, email, photo;
    public List<Integer> groups;

    public DatabaseUser() {
        // Default constructor required for calls to DataSnapshot.getValue(DatabaseUser.class)
    }

    public DatabaseUser(FirebaseUser user) {
        this(user.getDisplayName(), user.getEmail(), user.getPhotoUrl().toString(), new ArrayList<Integer>());
    }

    public DatabaseUser(String name, String email, String photo, List<Integer> groups) {
        this.name = name;
        this.email = email;
        this.photo = photo;
        this.groups = groups;
    }
}
