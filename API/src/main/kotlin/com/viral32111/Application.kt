package com.viral32111

import com.viral32111.dao.DatabaseFactory
import com.viral32111.plugins.*
import io.ktor.serialization.gson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.event.Level
import java.text.DateFormat
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.sessions.*
import java.net.http.HttpHeaders

// Entrypoint
fun main() {

	// Display a startup message
	println( "Hello World!" )

	// Load variables from .env file
	val dotenv = dotenv() {
		ignoreIfMalformed = true
		ignoreIfMissing = true
	}

	// Variables for running the HTTP server
	val httpListenAddress: String = dotenv[ "HTTP_LISTEN_ADDRESS" ] ?: "127.0.0.1"
	val httpListenPort: Int = dotenv[ "HTTP_LISTEN_PORT" ].toIntOrNull() ?: 80

	// Variables for connecting to the MySQL database server
	val databaseServerAddress: String = dotenv[ "DATABASE_SERVER_ADDRESS" ]
	val databaseServerPort: Int = dotenv[ "DATABASE_SERVER_PORT" ].toIntOrNull() ?: 3306
	val databaseUserName: String = dotenv[ "DATABASE_USER_NAME" ]
	val databaseUserPassword: String = dotenv[ "DATABASE_USER_PASSWORD" ]
	val databaseName: String = dotenv[ "DATABASE_NAME" ]

	// Initialize MySQL database connection - https://ktor.io/docs/interactive-website-add-persistence.html#startup
	DatabaseFactory.initialize(
		serverAddress = databaseServerAddress,
		serverPort = databaseServerPort,
		userName = databaseUserName,
		userPassword = databaseUserPassword,
		databaseName = databaseName
	)

	// Create HTTP server
	val server = embeddedServer( Netty,
		host = httpListenAddress,
		port = httpListenPort
	) {

		// Log incoming requests - https://ktor.io/docs/call-logging.html#install_plugin
		install( CallLogging ) {
			level = Level.INFO
		}

		// Setup content negotiation - https://ktor.io/docs/serialization.html#register_json
		install( ContentNegotiation ) {

			// JSON
			gson() {
				setDateFormat( DateFormat.LONG )
				setPrettyPrinting()
			}

		}

		// Setup sessions - https://ktor.io/docs/sessions.html#install_plugin
		// https://github.com/ktorio/ktor-documentation/blob/main/codeSnippets/snippets/session-header-server/src/main/kotlin/com/example/Application.kt
		install( Sessions ) {
			header<MySession>( "Session-Token", SessionStorageMemory() ) // Storing in-memory is only intended for development
		}

		// Set default response headers
		install( DefaultHeaders ) {
			header( "X-Example", "I am a header on every response." )
		}

		// Add default routing code
		setupRouting()

	}

	// Listen for requests, forever
	server.start( wait = true )

}

data class MySession(
	val identifier: Int,
	val data: String
)
