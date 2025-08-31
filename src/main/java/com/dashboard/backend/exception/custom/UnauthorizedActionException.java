// ===== UnauthorizedActionException.java =====
package com.dashboard.backend.exception.custom;

/**
 * Exception levée lors d'actions non autorisées
 */
public class UnauthorizedActionException extends RuntimeException {
    
    public UnauthorizedActionException(String message) {
        super(message);
    }
    
    public UnauthorizedActionException(String message, Throwable cause) {
        super(message, cause);
    }
}



