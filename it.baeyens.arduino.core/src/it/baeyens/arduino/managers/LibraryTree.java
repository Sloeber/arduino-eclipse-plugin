package it.baeyens.arduino.managers;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class LibraryTree {
	private TreeMap<String, Category> categories = new TreeMap<>();
	
	public static interface Node {
		boolean hasChildren();
		Object[] getChildren();
		Object getParent();
		String getName();
	}
	
	public class Category implements Comparable<Category>, Node {
		private String name;
		private TreeMap<String, Library> libraries = new TreeMap<>();

		public Category(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		
		public Collection<Library> getLibraries() {
			return libraries.values();
		}
		
		@Override
		public int compareTo(Category other) {
			return name.compareTo(other.name);
		}
		@Override
		public boolean hasChildren() {
			return !libraries.isEmpty();
		}
		@Override
		public Object[] getChildren() {
			return libraries.values().toArray();
		}
		@Override
		public Object getParent() {
			return LibraryTree.this;
		}
	}
	
	public class Library implements Comparable<Library>, Node {
		private String name;
		private Category category;
		private TreeSet<Version> versions = new TreeSet<>();
		private String installed;

		public Library(Category category, String name) {
			this.category = category;
			this.name = name;
		}
		
		public Collection<Version> getVersions() {
			return versions;
		}

		public String getName() {
			return name;
		}
		
		public Category getCategory() {
			return category;
		}

		public String getInstalled() {
			return installed;
		}
		
		public String getLatest() {
			return versions.last().toString();
		}

		@Override
		public int compareTo(Library other) {
			return name.compareTo(other.name);
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
			return category;
		}
	}
	
	public class Version implements Comparable<Object> {
		private String[] parts;
		public Version(String version) {
			parts = version.split("\\.");
		}
		
		@Override
		public int compareTo(Object other) {
			if (other instanceof String) {
				return this.compareTo(new Version((String)other));
			} else if (other instanceof Version) {
				return this.compareParts(((Version)other).parts, 0);
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		private int compareParts(String[] other, int level) {
			if (parts.length > level && other.length > level) {
				if (parts[level].compareTo(other[level]) == 0) {
					return this.compareParts(other, ++level);
				} else {
					try {
						return new Integer(parts[level]).compareTo(Integer.parseInt(other[level]));
					} catch (Exception e) {
						return parts[level].compareTo(other[level]);
					}
				}
			} else {
				return parts.length > other.length ? 1 : -1;
			}
		}
		
		public String toString() {
			return String.join(".", parts);
		}
	}
	
	public LibraryTree() {
		LibraryIndex libraryIndex = Manager.getLibraryIndex();
		
		for (String categoryName : libraryIndex.getCategories()) {
			Category category = new Category(categoryName);
			for (it.baeyens.arduino.managers.Library library : libraryIndex.getLibraries(categoryName)) {
				Library lib = category.libraries.get(library.getName());
				if (lib == null) {
					lib = new Library(category, library.getName());
					category.libraries.put(lib.getName(), lib);
				}
				lib.versions.add(new Version(library.getVersion()));
				if (library.isInstalled()) {
					lib.installed = library.getVersion();
				}
			}
			
			categories.put(category.getName(), category);
		}
	}

	public Collection<Category> getCategories() {
		return categories.values();
	}
	
	public Collection<Library> getAllLibraries() {
		Set<Library> all = new TreeSet<>();
		for (Category category : categories.values()) {
			all.addAll(category.getLibraries());
		}
		return all;
	}	
}
