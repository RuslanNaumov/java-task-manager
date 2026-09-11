package com.github.ruslannaumov.taskmanager.util;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DatabaseConnection {
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(AppConfig.getDbUrl());
    }
}
