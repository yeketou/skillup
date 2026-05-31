package com.skillup.communication.domain;

public enum MessageStatus {
    PENDING,    // created but not yet dispatched
    SENT,       // successfully accepted by the WhatsApp provider
    FAILED,     // provider returned an error or network issue
    DELIVERED   // delivery confirmed via webhook (Phase 9)
}
