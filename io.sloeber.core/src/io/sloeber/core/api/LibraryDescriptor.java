package io.sloeber.core.api;

import io.sloeber.core.api.Json.library.LibraryJson;

public class LibraryDescriptor {
	private LibraryJson library=null;

	public LibraryDescriptor(LibraryJson value) {
		this.library=value;
	}

	public LibraryJson toLibrary() {
		return this.library;
	}

}
