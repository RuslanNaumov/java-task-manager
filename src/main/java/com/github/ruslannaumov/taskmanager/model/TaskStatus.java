package com.github.ruslannaumov.taskmanager.model;

public enum TaskStatus {
    PENDING("Ожидает"),
    ACTIVE("В работе"),
    DONE("Завершена"),
    CANCEL("Отмена");

    private final String displayName;

    TaskStatus(String name){
        this.displayName=name;
    }

    public String getDisplayName(){
        return this.displayName;
    }
}
