package com.github.ruslannaumov.taskmanager.model;

public enum TaskStatus {
    TODO("Ожидает"),
    IN_PROGRESS("В работе"),
    DONE("Завершена");

    private final String displayName;

    TaskStatus(String name){
        this.displayName=name;
    }

    public String getDisplayName(){
        return this.displayName;
    }
}
