# Models
-keepattributes Signature
-keepclassmembers class io.csuoh.hello.models.** {
  *;
}

# Picasso
-dontwarn com.squareup.okhttp.**

# Parcel
-keep interface org.parceler.Parcel
-keep @org.parceler.Parcel class * { *; }
-keep class **$$Parcelable { *; }

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule