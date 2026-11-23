# 保留 Hilt 生成的入口
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }

# Room 生成的实现
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Kotlin 元数据
-keepattributes *Annotation*, InnerClasses, Signature

# 腾讯云 IM：SDK 通过反射与 JNI 回调，必须整体保留
-keep class com.tencent.imsdk.** { *; }
-keep class com.tencent.imsdk.v2.** { *; }
-dontwarn com.tencent.imsdk.**
