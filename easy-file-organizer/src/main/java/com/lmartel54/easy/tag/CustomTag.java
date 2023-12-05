package com.lmartel54.easy.tag;

import com.drew.metadata.Directory;

public class CustomTag {

	private Class<? extends Directory> directory;
	private int tagType;
	private String label;

	public CustomTag(final Class<? extends Directory> directory, final int tagType, final String label) {
		this.directory = directory;
		this.tagType = tagType;
		this.label = label;
	}

	public Class<? extends Directory> getDirectory() {
		return this.directory;
	}

	public int getTagType() {
		return this.tagType;
	}

	public String getLabel() {
		return this.label;
	}

	public boolean equals(final Directory directory, final int tagType) {
		return (this.directory.isAssignableFrom(directory.getClass())) && (this.tagType == tagType);
	}
}
