package com.viral32111

import com.viral32111.plugins.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.application.*
import org.slf4j.event.Level

// Entrypoint
fun main() {

	// Display a startup message
	println( "Hello World!" )

	// Variables for the HTTP server
	val listenAddress = "127.0.0.1"
	val portNumber = 80

	// Create HTTP server
	val server = embeddedServer( Netty,
		host = listenAddress,
		port = portNumber
	) {

		// Log incoming requests
		install( CallLogging ) {
			level = Level.INFO
		}

		// Add default routing code
		setupRouting()

	}

	// Listen for requests, forever
	server.start( wait = true )

}
