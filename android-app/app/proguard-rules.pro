# Add project specific ProGuard rules here.
# Minification is off by default (see app/build.gradle.kts). If you turn it on, keep the
# LangCalcTools class and its @Tool-annotated methods, since LiteRT-LM's tool-calling
# machinery inspects them via reflection.
-keep class com.jedick.langcalc.engine.LangCalcTools { *; }
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
