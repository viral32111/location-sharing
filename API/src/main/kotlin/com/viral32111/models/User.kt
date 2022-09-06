package com.viral32111.models

import org.jetbrains.exposed.sql.*

data class User(
	val identifier: Int,
	val name: String,
	val age: Int
)

object Users : Table() {
	val identifier = integer( "Identifier" ).autoIncrement()
	val name = varchar( "Name", 50 )
	val age = integer( "Age" )

	override val primaryKey = PrimaryKey( identifier )
}
