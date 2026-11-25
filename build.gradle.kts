plugins {
    java
    
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "ru.winxboyz.requests"
version = "0.2.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.fancyinnovations.com/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("de.oliver:FancyNpcs:2.0.2.1")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    sourceCompatibility = "21"
    targetCompatibility = "21"
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
