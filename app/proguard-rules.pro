# LedgerPad R8 rules

# --- kotlinx.serialization: keep @Serializable classes, companions and generated serializers ---
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class jp.tpp.t9s.ledgerpad.**$$serializer { *; }
-keepclassmembers class jp.tpp.t9s.ledgerpad.** {
    *** Companion;
}
-keepclasseswithmembers class jp.tpp.t9s.ledgerpad.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class jp.tpp.t9s.ledgerpad.** { *; }

# --- androidx.work / room (WorkManager internal DB) ---
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- Google Play Billing ---
-keep class com.android.billingclient.api.** { *; }

# --- Google Mobile Ads ---
-keep public class com.google.android.gms.ads.** { public *; }
