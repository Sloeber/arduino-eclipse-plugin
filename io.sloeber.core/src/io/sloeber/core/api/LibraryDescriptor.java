package io.sloeber.core.api;

import io.sloeber.core.managers.Library;

public class LibraryDescriptor {
	private Library library=null;

	public LibraryDescriptor(Library value) {
		this.library=value;
	}

	public Library toLibrary() {
		return this.library;
	}

}
