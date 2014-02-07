/**
 * Copyright (C) 2007 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       * Apache License, version 2.0
 *       * Apache Software License, version 1.0
 *       * GNU Lesser General Public License, version 3
 *       * Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       * Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.webapp.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.n52.wps.webapp.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.collect.ImmutableMap;

/**
 * An implementation for the {@link UserDAO} interface. This implementation uses
 * JDBC through Spring's
 * {@code NamedParameterJdbcTemplate}.
 */
@Repository("userDAO")
public class JdbcUserDAO implements UserDAO {
    private static final String USERNAME = "username";
    private static final String USER_ID = "user_id";
    private static final String PASSWORD = "password";
    private static final String ROLE = "role";

    private static final String GET_BY_ID
            = "SELECT * FROM users " +
              "WHERE user_id = :user_id";
    private static final String GET_BY_NAME
            = "SELECT * FROM users " +
              "WHERE username = :username";
    private static final String GET_ALL
            = "SELECT * FROM users";
    private static final String INSERT
            = "INSERT INTO users (username, password, role) " +
              "VALUES (:username, :password, :role)";
    private static final String UPDATE
            = "UPDATE users " +
              "SET password = :password, role = :role " +
              "WHERE user_id = :user_id";
    private static final String DELETE
            = "DELETE FROM users " +
              "WHERE user_id = :user_id";

    @Autowired
    private NamedParameterJdbcTemplate template;

    @Override
    public User getUserById(int userId) {
        List<User> users = template.query(GET_BY_ID,
                                          ImmutableMap.of(USER_ID, userId),
                                          UserRowMapper.INSTANCE);
        if (users.isEmpty()) {
            return null;
        } else if (users.size() == 1) {
            return users.get(0);
        } else {
            return null;
        }
    }

    @Override
    public User getUserByUsername(String username) {
        List<User> users = template.query(GET_BY_NAME,
                                          ImmutableMap.of(USERNAME, username),
                                          UserRowMapper.INSTANCE);
        if (users.isEmpty()) {
            return null;
        } else if (users.size() == 1) {
            return users.get(0);
        } else {
            return null;
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = template.query(GET_ALL, UserRowMapper.INSTANCE);
        if (users.isEmpty()) {
            return null;
        } else {
            return users;
        }
    }

    @Override
    public void insertUser(User user) {
        template.update(INSERT, ImmutableMap.of(USERNAME, user.getUsername(),
                                                PASSWORD, user.getPassword(),
                                                ROLE, user.getRole()));

    }

    @Override
    public void updateUser(User user) {
        template.update(UPDATE, ImmutableMap.of(USER_ID, user.getUserId(),
                                                PASSWORD, user.getPassword(),
                                                ROLE, user.getRole()));
    }

    @Override
    public void deleteUser(int userId) {
        template.update(DELETE, ImmutableMap.of(USER_ID, userId));
    }

    private static class UserRowMapper implements RowMapper<User> {
        private static final UserRowMapper INSTANCE = new UserRowMapper();

        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User u = new User();
            u.setUserId(rs.getInt(USER_ID));
            u.setUsername(rs.getString(USERNAME));
            u.setPassword(rs.getString(PASSWORD));
            u.setRole(rs.getString(ROLE));
            return u;
        }
    }

}
