package com.viral32111.locationsharing;

public interface PopupCallback {

	void OnAccept();

	default void OnDecline() {};

	default void OnDismiss() {};

}
