package io.sloeber.core.api;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

import io.sloeber.core.managers.LibraryIndex;
import io.sloeber.core.managers.Manager;

public class LibraryManager {
    public static LibraryTree getLibraryTree() {
	return new LibraryTree();

    }

    public static class LibraryTree {

	private TreeMap<String, Category> categories = new TreeMap<>();

	public class Category implements Comparable<Category>, Node {
	    private String name;
	    protected TreeMap<String, Library> libraries = new TreeMap<>();

	    public Category(String name) {
		this.name = name;
	    }

	    @Override
	    public String getName() {
		return this.name;
	    }

	    public Collection<Library> getLibraries() {
		return this.libraries.values();
	    }

	    @Override
	    public int compareTo(Category other) {
		return this.name.compareTo(other.name);
	    }

	    @Override
	    public boolean hasChildren() {
		return !this.libraries.isEmpty();
	    }

	    @Override
	    public Object[] getChildren() {
		return this.libraries.values().toArray();
	    }

	    @Override
	    public Object getParent() {
		return LibraryTree.this;
	    }
	}

	public class Library implements Comparable<Library>, Node {
	    private String name;
	    private Category category;
	    protected TreeSet<VersionNumber> versions = new TreeSet<>();
	    protected String version;
	    private String tooltip;

	    public Library(Category category, String name, String tooltip) {
		this.category = category;
		this.name = name;
		this.tooltip = tooltip;
	    }

	    public Collection<VersionNumber> getVersions() {
		return this.versions;
	    }

	    @Override
	    public String getName() {
		return this.name;
	    }

	    public String getTooltip() {
		return this.tooltip;
	    }

	    public String getLatest() {
		return this.versions.last().toString();
	    }

	    public String getVersion() {
		return this.version;
	    }

	    public void setVersion(String version) {
		this.version = version;
	    }

	    @Override
	    public int compareTo(Library other) {
		return this.name.compareTo(other.name);
	    }

	    @Override
	    public boolean hasChildren() {
		return false;
	    }

	    @Override
	    public Object[] getChildren() {
		return null;
	    }

	    @Override
	    public Object getParent() {
		return this.category;
	    }
	}

	public LibraryTree() {
	    LibraryIndex libraryIndex = Manager.getLibraryIndex();

	    for (String categoryName : libraryIndex.getCategories()) {
		Category category = new Category(categoryName);
		for (io.sloeber.core.managers.Library library : libraryIndex.getLibraries(categoryName)) {
		    Library lib = category.libraries.get(library.getName());
		    if (lib == null) {
			StringBuilder builder = new StringBuilder("Architectures:") //$NON-NLS-1$
				.append(library.getArchitectures().toString()).append("\n\n") //$NON-NLS-1$
				.append(library.getSentence());
			lib = new Library(category, library.getName(), builder.toString());
			category.libraries.put(lib.getName(), lib);
		    }
		    lib.versions.add(new VersionNumber(library.getVersion()));
		    if (library.isInstalled()) {
			lib.version = library.getVersion();
		    }
		}

		this.categories.put(category.getName(), category);
	    }
	}

	public Collection<Category> getCategories() {
	    return this.categories.values();
	}

	public Collection<Library> getAllLibraries() {
	    Set<Library> all = new TreeSet<>();
	    for (Category category : this.categories.values()) {
		all.addAll(category.getLibraries());
	    }
	    return all;
	}

	public void reset() {
	    LibraryIndex libraryIndex = Manager.getLibraryIndex();
	    for (Library library : this.getAllLibraries()) {
		io.sloeber.core.managers.Library installed = libraryIndex.getInstalledLibrary(library.getName());
		library.setVersion(installed != null ? installed.getVersion() : null);
	    }
	}

    }

    public static IStatus setLibraryTree(LibraryTree libs, IProgressMonitor monitor, MultiStatus status) {
	for (LibraryTree.Library lib : libs.getAllLibraries()) {
	    io.sloeber.core.managers.Library toRemove = Manager.getLibraryIndex().getInstalledLibrary(lib.getName());
	    if (toRemove != null && !toRemove.getVersion().equals(lib.getVersion())) {
		status.add(toRemove.remove(monitor));
	    }
	    io.sloeber.core.managers.Library toInstall = Manager.getLibraryIndex().getLibrary(lib.getName(),
		    lib.getVersion());
	    if (toInstall != null && !toInstall.isInstalled()) {
		status.add(toInstall.install(monitor));
	    }
	}
	return status;
    }

}