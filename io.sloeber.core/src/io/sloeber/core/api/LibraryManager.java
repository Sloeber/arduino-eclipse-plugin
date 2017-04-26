package io.sloeber.core.api;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.common.InstancePreferences;
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
			private String indexName;
			private Category category;
			protected TreeSet<VersionNumber> versions = new TreeSet<>();
			protected String version;
			private String tooltip;

			public Library(Category category, String name, String indexName, String tooltip) {
				this.category = category;
				this.name = name;
				this.tooltip = tooltip;
				this.indexName = indexName;
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

			public String getIndexName() {
				return this.indexName;
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
			for (LibraryIndex libraryIndex : Manager.getLibraryIndices()) {
				for (String categoryName : libraryIndex.getCategories()) {
					Category category = this.categories.get(categoryName);
					if (category == null) {
						category = new Category(categoryName);
						this.categories.put(category.getName(), category);
					}
					for (io.sloeber.core.managers.Library library : libraryIndex.getLibraries(categoryName)) {
						Library lib = category.libraries.get(library.getName() + " (" + libraryIndex.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
						if (lib == null) {
							StringBuilder builder = new StringBuilder("Architectures:") //$NON-NLS-1$
									.append(library.getArchitectures().toString()).append("\n\n") //$NON-NLS-1$
									.append(library.getSentence());
							lib = new Library(category, library.getName(), libraryIndex.getName(), builder.toString());
							category.libraries.put(library.getName() + " (" + libraryIndex.getName() + ")", lib); //$NON-NLS-1$ //$NON-NLS-2$
						}
						lib.versions.add(new VersionNumber(library.getVersion()));
						if (library.isInstalled()) {
							lib.version = library.getVersion();
						}
					}
				}
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

		private static LibraryIndex findLibraryIndex(String name) {
			for (LibraryIndex libraryIndex : Manager.getLibraryIndices()) {
				if (libraryIndex.getName().equals(name))
					return libraryIndex;
			}
			return null;
		}

		public void reset() {
			for (Library library : this.getAllLibraries()) {
				LibraryIndex libraryIndex = findLibraryIndex(library.getIndexName());

				if (libraryIndex != null) {
					io.sloeber.core.managers.Library installed = libraryIndex.getInstalledLibrary(library.getName());
					library.setVersion(installed != null ? installed.getVersion() : null);
				}
			}
		}

	}

	public static IStatus setLibraryTree(LibraryTree libs, IProgressMonitor monitor, MultiStatus status) {
		for (LibraryTree.Library lib : libs.getAllLibraries()) {
			LibraryIndex libraryIndex = findLibraryIndex(lib.getIndexName());

			if (libraryIndex != null) {
				io.sloeber.core.managers.Library toRemove = libraryIndex.getInstalledLibrary(lib.getName());
				if (toRemove != null && !toRemove.getVersion().equals(lib.getVersion())) {
					status.add(toRemove.remove(monitor));
				}
				io.sloeber.core.managers.Library toInstall = libraryIndex.getLibrary(lib.getName(), lib.getVersion());
				if (toInstall != null && !toInstall.isInstalled()) {
					status.add(toInstall.install(monitor));
				}
			}

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
		}
		return status;
	}

	private static LibraryIndex findLibraryIndex(String indexName) {
		for (LibraryIndex libraryIndex : Manager.getLibraryIndices()) {
			if (libraryIndex.getName().equals(indexName))
				return libraryIndex;
		}
		return null;
	}

	public static String getPrivateLibraryPathsString() {
		return InstancePreferences.getPrivateLibraryPathsString();
	}

	public static void installAllLatestLibraries(String category) {
		Manager.installAllLatestLibraries(category);
	}

	public static void installAllLatestLibraries() {
		Set<String> allcategories = getAllCategories();
		for (String categorieName : allcategories) {
			Manager.installAllLatestLibraries(categorieName);
		}

	}

	public static Set<String> getAllCategories() {

		Set<String> ret = new TreeSet<>();

		for (LibraryIndex libraryIndex : Manager.getLibraryIndices()) {
			for (String categoryName : libraryIndex.getCategories()) {
				ret.add(categoryName);
			}
		}
		return ret;
	}
}