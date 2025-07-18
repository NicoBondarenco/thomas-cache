dependencies {
    implementation(project(":cache-handler"))
    implementation(libs.cache.caffeine.core)
    implementation(libs.cache.caffeine.coroutines)
}