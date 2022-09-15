// Project group & version
group = "com.viral32111"
version = "0.0.1"

// Repositories for plugins
repositories {
	mavenCentral()
}

// Configure plugin dependencies
plugins {
	application

	kotlin( "jvm" ) version "1.7.10"
	id( "io.ktor.plugin" ) version "2.1.0"
}

// Configure application extension
application {
	mainClass.set( "com.viral32111.ApplicationKt" )

	val isDevelopment: Boolean = project.ext.has( "development" )
	applicationDefaultJvmArgs = listOf( "-Dio.ktor.development=$isDevelopment" )
}

dependencies {

	// Ktor Core
	implementation( "io.ktor:ktor-server-core-jvm:2.1.0" )

	// Ktor Call Logging - https://ktor.io/docs/call-logging.html#add_dependencies
	implementation( "io.ktor:ktor-server-call-logging:2.1.0" )

	// Ktor Json - https://ktor.io/docs/serialization.html#add_json_dependency
	implementation( "io.ktor:ktor-server-content-negotiation:2.1.0" )
	implementation( "io.ktor:ktor-serialization-gson:2.1.0" )

	// Ktor Sessions - https://ktor.io/docs/sessions.html#add_dependencies
	implementation( "io.ktor:ktor-server-sessions:2.1.0" )

	// Ktor Default Headers - https://ktor.io/docs/default-headers.html#add_dependencies
	implementation( "io.ktor:ktor-server-default-headers:2.1.0" )

	// Netty Engine
	implementation( "io.ktor:ktor-server-netty-jvm:2.1.0" )

	// Logback
	implementation( "ch.qos.logback:logback-classic:1.2.11" )

	// Exposed
	implementation( "org.jetbrains.exposed:exposed-core:0.39.2" )
	implementation( "org.jetbrains.exposed:exposed-dao:0.39.2" )
	implementation( "org.jetbrains.exposed:exposed-jdbc:0.39.2" )

	// MySQL
	implementation(  "mysql:mysql-connector-java:8.0.30" )

	// dotenv - https://github.com/cdimascio/dotenv-kotlin
	implementation( "io.github.cdimascio:dotenv-kotlin:6.3.1" )

	// Unit Tests
	testImplementation( "io.ktor:ktor-server-tests-jvm:2.1.0" )
	testImplementation( "org.jetbrains.kotlin:kotlin-test-junit:1.7.10" )

}
