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
	val httpListenPort: Int = dotenv[ "HTTP_LISTEN_NUMBER" ].toIntOrNull() ?: 80

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

		// Add default routing code
		setupRouting()

	}

	// Listen for requests, forever
	server.start( wait = true )

}
