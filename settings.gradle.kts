pluginManagement {
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        mavenCentral()
    }
}
rootProject.name = "Hoshemasnooi"
include(":app")
