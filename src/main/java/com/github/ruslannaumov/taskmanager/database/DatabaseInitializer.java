package com.github.ruslannaumov.taskmanager.database;

import com.github.ruslannaumov.taskmanager.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    public static void initialize(){
        String sql= """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL ,
                    status TEXT NOT NULL,
                    created_at TEXT,
                    updated_at TEXT
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Database initialized successfully.");
        } catch (Exception e) {
            logger.error("Error initializing database", e);
        }
    }
}
