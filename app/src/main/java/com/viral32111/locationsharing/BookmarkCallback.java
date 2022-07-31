package com.viral32111.locationsharing;

import java.util.ArrayList;

public interface BookmarkCallback {

	void OnComplete( ArrayList<Bookmark> bookmarks );

	void OnException( Exception exception );

}
