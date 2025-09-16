// src/main/java/com/dashboard/backend/exception/custom/AttendanceException.java
package com.dashboard.backend.exception.custom;

public class AttendanceException extends RuntimeException {
    
    public AttendanceException(String message) {
        super(message);
    }
    
    public AttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}