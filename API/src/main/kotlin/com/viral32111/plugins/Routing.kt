package com.viral32111.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// Default routing
fun Application.setupRouting() {
	log.info( "Setting up default routing." )

	routing {

		// Hello World
		get( "/hello" ) {
			call.respondText( "Hello World!", ContentType.Text.Plain )
		}

	}
}
