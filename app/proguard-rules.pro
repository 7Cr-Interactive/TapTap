# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Enable aggressive optimization modifications by R8
-allowaccessmodification

# Repackage obfuscated classes into top-level package to reduce APK size
-repackageclasses ''

# Preserve line numbers and source file attributes for stack trace demangling
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile