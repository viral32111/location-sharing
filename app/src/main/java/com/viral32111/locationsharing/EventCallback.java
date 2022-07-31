package com.viral32111.locationsharing;

import java.util.ArrayList;

public interface EventCallback {

	void OnComplete( ArrayList<Event> events );

	void OnException( Exception exception );

}
