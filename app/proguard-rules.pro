-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature
-renamesourcefileattribute SourceFile

-keep class com.mehmetokyay.flownote.model.** { *; }

-keep class com.applandeo.materialcalendarview.** { *; }
-dontwarn com.applandeo.materialcalendarview.**

-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
