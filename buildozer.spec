[app]

title = JARVIS
package.name = jarvis
package.domain = org.jarvis

source.dir = .
source.include_exts = py,png,jpg,jpeg,kv,atlas

version = 1.0

requirements = python3,kivy

orientation = portrait
fullscreen = 0

android.api = 35
android.minapi = 23
android.archs = arm64-v8a
android.sdk_path = /usr/local/lib/android/sdk

[buildozer]

log_level = 2
warn_on_root = 1
