package com.buildud.exception;

public class BudNettyServerException extends Exception {

    public BudNettyServerException(String message){
        super(message);
    }

    public BudNettyServerException(String message, Throwable cause){
        super(message,cause);
    }

}
