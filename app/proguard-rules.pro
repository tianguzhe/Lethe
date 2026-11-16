# Shizuku starts the UserService by class name in its own process, so R8 must not rename or strip it.
-keep class io.github.xiaolong125.lethe.service.UninstallService { <init>(android.content.Context); *; }
