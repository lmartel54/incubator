package com.lmartel54.easy.metadata;

import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.lmartel54.easy.tag.CustomTag;

public final class Device {

	private static LinkedList<CustomTag> tags = new LinkedList<CustomTag>();

	private static final Logger logger = LoggerFactory.getLogger(Device.class);

	static {
		tags.add(new CustomTag(ExifIFD0Directory.class, ExifIFD0Directory.TAG_MAKE, "DEVICE.IFD.TAG_MAKE"));
		tags.add(new CustomTag(ExifIFD0Directory.class, ExifIFD0Directory.TAG_MODEL, "DEVICE.IFD.TAG_MODEL"));
	}

	public static String get(final Metadata metadata) throws MetadataException {

		LinkedList<String> descriptions = new LinkedList<String>();

		for (final CustomTag tag : tags) {
			descriptions.add(getDescriptionFromMetadata(metadata, tag));
		}

		descriptions.removeIf(Objects::isNull);

//		if (descriptions.isEmpty()) {
//			throw new MetadataException("Can't specify device type !");
//		}

		return descriptions.stream().collect(Collectors.joining(" "));
	}

	private static String getDescriptionFromMetadata(final Metadata metadata, final CustomTag tag) {
		final Directory directory = metadata.getFirstDirectoryOfType(tag.getDirectory());
		final String description = getDescriptionFromDirectory(directory, tag.getTagType());
		logger.info("\t[{}] => {}", tag.getLabel(), description);
		return description;
	}

	private static String getDescriptionFromDirectory(final Directory directory, final int tagType) {
		String description = null;
		if (directory != null) {
			description = StringUtils.trim(directory.getDescription(tagType));
		}
		return description;
	}
}
