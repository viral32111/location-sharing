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

// Entrypoint
fun main() {

	// Display a startup message
	println( "Hello World!" )

	// Variables for the HTTP server
	val listenAddress = "127.0.0.1"
	val portNumber = 80

	// Initialize MySQL database connection - https://ktor.io/docs/interactive-website-add-persistence.html#startup
	DatabaseFactory.initialize(
		serverAddress = "",
		serverPort = 3306,
		userName = "",
		userPassword = "",
		databaseName = ""
	)

	// Create HTTP server
	val server = embeddedServer( Netty,
		host = listenAddress,
		port = portNumber
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
