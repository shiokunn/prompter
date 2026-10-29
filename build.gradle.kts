// AGP 9.3.1 の組み込み Kotlin を使うため、org.jetbrains.kotlin.android は適用しない。
plugins {
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
