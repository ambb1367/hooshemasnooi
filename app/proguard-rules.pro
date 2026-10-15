# Keep model classes used with manual JSON parsing
-keep class ir.hoshemasnooi.app.model.** { *; }

# Keep Kotlin metadata for reflection-free serialization safety
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
