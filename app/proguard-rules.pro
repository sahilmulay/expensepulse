# Proguard rules for ExpensePulse
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
