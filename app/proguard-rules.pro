# Add project specific ProGuard rules here.

# Room entities/DAOs — keep field names & structure intact for reflection-based mapping.
-keep class com.vigyan.juniorcollege.data.local.entity.** { *; }
-keep class com.vigyan.juniorcollege.data.local.dao.** { *; }
-keep class com.vigyan.juniorcollege.data.local.VigyanDatabase { *; }

# Kotlin enums used as Room TypeConverters — keep valueOf()/values() reachable.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# OpenCSV (only direct CSVReader/CSVWriter usage here, but keep it safe from shrinking).
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# AndroidX Security / Keystore-backed crypto.
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# WorkManager workers are instantiated via reflection by their class name.
-keep class com.vigyan.juniorcollege.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker { public <init>(...); }

# Kotlin coroutines / Flow internals occasionally need this on aggressive shrinkers.
-dontwarn kotlinx.coroutines.**
