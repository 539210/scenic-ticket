package com.scenicticket.dao;

import com.scenicticket.exception.DBException;
import com.scenicticket.util.MySQLDBUtil;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseDAO {
    protected Connection getConnection() {
        try {
            return MySQLDBUtil.getConnection();
        } catch (SQLException e) {
            throw new DBException("Failed to obtain MySQL connection.", e);
        }
    }
}
