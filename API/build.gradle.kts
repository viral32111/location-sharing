// Variables from gradle.properties
val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

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

	// Ktor Call Logging
	implementation( "io.ktor:ktor-server-call-logging:$ktorVersion" )

	// Netty Engine
	implementation( "io.ktor:ktor-server-netty-jvm:$ktorVersion" )

	// Logback
	implementation( "ch.qos.logback:logback-classic:$logbackVersion" )

	// Unit Tests
	testImplementation( "io.ktor:ktor-server-tests-jvm:$ktorVersion" )
	testImplementation( "org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion" )

}