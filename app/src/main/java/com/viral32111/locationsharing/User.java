package com.viral32111.locationsharing;

import com.javadocmd.simplelatlng.LatLng;

import java.sql.Timestamp;

public class User {
	public int Identifier;
	public String Name;
	public LatLng Position;
	public Timestamp Since;

	public User( int identifier, String name, double latitude, double longitude, Timestamp recorded ) {
		Identifier = identifier;
		Name = name;
		Position = new LatLng( latitude, longitude );
		Since = recorded;
	}
}
