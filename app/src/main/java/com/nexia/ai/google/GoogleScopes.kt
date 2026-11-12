package com.nexia.ai.google

import com.google.android.gms.common.api.Scope

/** Request minimum scopes per feature; never request all permissions at once. */
enum class GoogleFeature(val label: String, val scope: String) {
    GMAIL("Gmail (lectura)", "https://www.googleapis.com/auth/gmail.readonly"),
    DRIVE("Drive (metadatos)", "https://www.googleapis.com/auth/drive.metadata.readonly"),
    CALENDAR("Calendar (lectura)", "https://www.googleapis.com/auth/calendar.readonly"),
    TASKS("Tasks (lectura)", "https://www.googleapis.com/auth/tasks.readonly"),
    CONTACTS("Contacts (lectura)", "https://www.googleapis.com/auth/contacts.readonly")
}

fun GoogleFeature.asScope(): Scope = Scope(scope)
