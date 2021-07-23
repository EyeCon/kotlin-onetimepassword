plugins {
  kotlin("jvm") version "1.5.20"
}

allprojects {
  group = "dev.turingcomplete"
  version = "2.1.0"

  repositories {
    mavenLocal()
    mavenCentral()
  }
}

configure<JavaPluginExtension> {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("commons-codec:commons-codec:1.12")

  val jUnitVersion = "5.5.1"
  testImplementation("org.junit.jupiter:junit-jupiter-params:$jUnitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
