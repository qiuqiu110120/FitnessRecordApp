package com.example.fitnessrecord.data.local.entity

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_DELETE,
    CONFLICT,
}
