package com.ketch.android.api

enum class MigrationOption(val value: Int) {
    MIGRATE_DEFAULT(0),
    MIGRATE_NEVER(1),
    MIGRATE_FROM_ALLOW(2),
    MIGRATE_FROM_DENY(3),
    MIGRATE_ALWAYS(4)
}
