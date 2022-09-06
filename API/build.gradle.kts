// Variables from gradle.properties
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project
val exposedVersion: String by project

plugins {
	application

	kotlin( "jvm" ) version "1.7.10"
	id( "io.ktor.plugin" ) version "2.1.0"
}

group = "com.viral32111"
version = "0.0.1"

application {
	mainClass.set( "com.viral32111.ApplicationKt" )

	val isDevelopment: Boolean = project.ext.has( "development" )
	applicationDefaultJvmArgs = listOf( "-Dio.ktor.development=$isDevelopment" )
}

repositories {
	mavenCentral()
}

dependencies {

	// Ktor Core
	implementation( "io.ktor:ktor-server-core-jvm:$ktorVersion" )

	// Ktor Call Logging - https://ktor.io/docs/call-logging.html#add_dependencies
	implementation( "io.ktor:ktor-server-call-logging:$ktorVersion" )

	// Ktor Json - https://ktor.io/docs/serialization.html#add_json_dependency
	implementation( "io.ktor:ktor-server-content-negotiation:$ktorVersion" )
	implementation( "io.ktor:ktor-serialization-gson:$ktorVersion" )

	// Netty Engine
	implementation( "io.ktor:ktor-server-netty-jvm:$ktorVersion" )

	// Logback
	implementation( "ch.qos.logback:logback-classic:$logbackVersion" )

	// Exposed
	implementation( "org.jetbrains.exposed:exposed-core:$exposedVersion" )
	implementation( "org.jetbrains.exposed:exposed-dao:$exposedVersion" )
	implementation( "org.jetbrains.exposed:exposed-jdbc:$exposedVersion" )

	// MySQL
	implementation( "mysql:mysql-connector-java:8.0.30" )

	// dotenv - https://github.com/cdimascio/dotenv-kotlin
	implementation( "io.github.cdimascio:dotenv-kotlin:6.3.1" )

	// Unit Tests
	testImplementation( "io.ktor:ktor-server-tests-jvm:$ktorVersion" )
	testImplementation( "org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion" )

}