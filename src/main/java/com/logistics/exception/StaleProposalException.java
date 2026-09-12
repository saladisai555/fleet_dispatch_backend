package com.logistics.exception;

public class StaleProposalException extends RuntimeException {
    public StaleProposalException(String message) { super(message); }
}