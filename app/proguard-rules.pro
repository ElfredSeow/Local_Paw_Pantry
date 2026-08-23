# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# -----------------------------------------------------------------------
# NOTE: isMinifyEnabled is currently false for the release build type (see
# app/build.gradle.kts), so R8 does not run today and none of the rules
# below are exercised yet. They are added now, ahead of minification ever
# being turned on, because both Converters.kt (the Room TypeConverter) and
# CsvHelper.kt (CSV import/export) round-trip a Gson-serialized
# List<Reminder> using `object : TypeToken<List<Reminder>>() {}` - a
# pattern that breaks silently under R8 without these rules: the generic
# signature gets stripped (Gson can no longer tell it's deserializing a
# List<Reminder> rather than a raw List) and/or Reminder's fields get
# renamed (Gson matches JSON keys by field name, so renaming desyncs it
# from data written by an earlier, differently-obfuscated build).
#
# These rules have NOT been exercised against a real minified build in
# this environment - that would mean flipping isMinifyEnabled to true,
# which is explicitly out of scope for this change. Treat this as a
# well-sourced starting point (the Gson section mirrors Gson's own
# published R8/ProGuard guidance closely), not a guarantee of
# completeness. Re-verify with a real `assembleRelease` before shipping a
# minified build.
# -----------------------------------------------------------------------

# --- Gson ----------------------------------------------------------------
# Gson uses generic type information stored in a class file when working
# with fields/types; R8 strips that by default. This is what lets
# `object : TypeToken<List<Reminder>>() {}` (in Converters.kt and
# CsvHelper.kt) still resolve to List<Reminder> instead of a raw List.
-keepattributes Signature

# TypeToken subclasses like the one above are anonymous inner classes;
# Gson's generic-signature lookup needs the inner-class/enclosing-method
# relationship preserved too, not just the Signature attribute itself.
-keepattributes InnerClasses,EnclosingMethod

# Not used by any model today, but keeping annotations is required if a
# field ever gets a Gson @SerializedName (or any other) annotation.
-keepattributes *Annotation*

# Gson's UnsafeAllocator references sun.misc.Unsafe, which isn't part of
# the Android platform API; this is Gson's own long-standing recommended
# rule to silence the resulting R8 warning.
-dontwarn sun.misc.**

# Keep TypeToken itself, and any subclass of it (including the anonymous
# ones created via `object : TypeToken<...>() {}`), so its generic
# signature survives shrinking.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# No custom (de)serializers/adapters exist today, but keep the hooks in
# case one is added later - R8 can otherwise strip an unreferenced
# implementation that Gson only ever looks up via @JsonAdapter/registration.
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Preserve @SerializedName-annotated fields from being renamed/removed, if
# any model ever adds one.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson (de)serializes enums by name via values()/valueOf(); keep both if a
# model class ever adds an enum field (none currently do).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- App model classes -----------------------------------------------------
# Reminder is serialized to/from JSON by Gson in two places: the Room
# TypeConverter in Converters.kt, and CSV import/export in CsvHelper.kt.
# Gson maps JSON keys to this class's field names by reflection, so those
# names must stay stable across an obfuscated build - both for data an
# obfuscated build itself writes, and for rows/CSV files already written
# by an earlier (differently- or non-) obfuscated build.
-keepclassmembers class com.example.foodtracker.data.Reminder {
    <fields>;
}

# FoodItem and Category are Room @Entity classes, not Gson types - Room's
# KSP-generated DAO code binds to their fields directly at compile time,
# not via reflection, so they are not at risk the same way Reminder is.
# Kept anyway, defensively: they define the on-disk column shape and it
# costs nothing to protect their field names too.
-keepclassmembers class com.example.foodtracker.data.FoodItem {
    <fields>;
}
-keepclassmembers class com.example.foodtracker.data.Category {
    <fields>;
}

# --- Room ------------------------------------------------------------------
# androidx.room:room-runtime already ships its own consumer ProGuard rules
# for the core library, and this project's generated Database/DAO/Impl
# classes (KSP output) are referenced directly - not reflectively - from
# FoodDatabase.getDatabase(), so they don't need explicit keep rules
# beyond what Room already bundles. Kept as a defensive backstop only.
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
