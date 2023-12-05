package com.lmartel54.easy.metadata;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import com.drew.metadata.mp4.media.Mp4SoundDirectory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;
import com.lmartel54.easy.tag.CustomTag;

public final class OriginalDate {

	private static LinkedList<CustomTag> tags = new LinkedList<CustomTag>();
	private static LinkedList<CustomTag> excluded_tags = new LinkedList<CustomTag>();

	private static Calendar INVALID_DATE = Calendar.getInstance();
	private static final SimpleDateFormat SDF_PATTERN = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");

	private static final Logger logger = LoggerFactory.getLogger(OriginalDate.class);

	static {

		INVALID_DATE.set(Calendar.MONTH, 21);
		INVALID_DATE.set(Calendar.DATE, 06);
		INVALID_DATE.set(Calendar.YEAR, 1972);

		tags.add(new CustomTag(ExifIFD0Directory.class, ExifIFD0Directory.TAG_DATETIME, "[Exif IFD] [TAG_DATETIME]"));
		tags.add(new CustomTag(ExifIFD0Directory.class, ExifIFD0Directory.TAG_DATETIME_ORIGINAL, "[Exif IFD] [TAG_DATETIME_ORIGINAL]"));
		tags.add(new CustomTag(ExifIFD0Directory.class, ExifIFD0Directory.TAG_DATETIME_DIGITIZED, "[ExifIFD] [TAG_DATETIME_DIGITIZED]"));
		tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifIFD0Directory.TAG_DATETIME, "[Exif SubIFD] [TAG_DATETIME]"));
		tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifIFD0Directory.TAG_DATETIME_ORIGINAL, "[Exif SubIFD] [TAG_DATETIME_ORIGINAL]"));
		tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifIFD0Directory.TAG_DATETIME_DIGITIZED, "[Exif SubIFD] [TAG_DATETIME_DIGITIZED]"));
//		tags.add(new CustomTag(IccDirectory.class, IccDirectory.TAG_PROFILE_DATETIME, "DATE.ICC.PROFILE_DATETIME"));
		tags.add(new CustomTag(FileSystemDirectory.class, FileSystemDirectory.TAG_FILE_MODIFIED_DATE, "[File] [TAG_FILE_MODIFIED_DATE]"));
		// [MP4]
		tags.add(new CustomTag(Mp4Directory.class, Mp4Directory.TAG_CREATION_TIME, "[MP4] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(Mp4Directory.class, Mp4Directory.TAG_MODIFICATION_TIME, "[MP4] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(Mp4VideoDirectory.class, Mp4VideoDirectory.TAG_CREATION_TIME, "[MP4 Video] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(Mp4VideoDirectory.class, Mp4VideoDirectory.TAG_MODIFICATION_TIME, "[MP4 Video] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(Mp4SoundDirectory.class, Mp4SoundDirectory.TAG_CREATION_TIME, "[MP4 Sound] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(Mp4SoundDirectory.class, Mp4SoundDirectory.TAG_MODIFICATION_TIME, "[MP4 Sound] [TAG_MODIFICATION_TIME]"));

		excluded_tags.add(new CustomTag(FileSystemDirectory.class, FileSystemDirectory.TAG_FILE_NAME, "DATE.FILE.NAME"));
		excluded_tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifSubIFDDirectory.TAG_SUBSECOND_TIME, "DATE.DATE.SUBIFD.SUBSECOND_TIME"));
		excluded_tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifSubIFDDirectory.TAG_SUBSECOND_TIME_ORIGINAL, "DATE.SUBIFD.SUBSECOND_TIME_ORIGINAL"));
		excluded_tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifSubIFDDirectory.TAG_SUBSECOND_TIME_DIGITIZED, "DATE.SUBIFD.SUBSECOND_TIME_DIGITIZED"));
		excluded_tags.add(new CustomTag(GpsDirectory.class, GpsDirectory.TAG_DATE_STAMP, "DATE.GPS.STAMP"));
	}

	public static Date get(final Metadata metadata) throws MetadataException {

		checkDateFromMetadata(metadata);

		LinkedList<Date> dates = new LinkedList<Date>();

		for (final CustomTag tag : tags) {
			dates.add(getDateFromMetadata(metadata, tag));
		}

		dates.removeIf(Objects::isNull);

		if (dates.isEmpty()) {
			throw new MetadataException("Can't specify file creation date !");
		}

		return Collections.min(dates);
	}

	public static String format(final Date date) {
		return SDF_PATTERN.format(date);
	}

	private static void checkDateFromMetadata(final Metadata metadata) throws MetadataException {

		for (final Directory directory : metadata.getDirectories()) {
			for (final Tag tag : directory.getTags()) {
				final Date date = getDateFromDirectory(directory, tag.getTagType());
				if (date != null) {
					boolean found = false;
					for (final CustomTag customTag : tags) {
						if (customTag.equals(directory, tag.getTagType())) {
							found = true;
						}
					}
					if (!found) {
						throw new MetadataException("Metadata contains valid date not used ! [" + directory.getClass().getSimpleName() + "][" + tag.getTagName() + "][" + tag.getDescription() + "]");
					}
				}
			}
		}
	}

	private static Date getDateFromMetadata(final Metadata metadata, final CustomTag tag) {
		final Directory directory = metadata.getFirstDirectoryOfType(tag.getDirectory());
		final Date date = getDateFromDirectory(directory, tag.getTagType());
		logger.info("\t[Original date] {} => {}", tag.getLabel(), (date != null) ? SDF_PATTERN.format(date) : null);
		return date;
	}

	private static Date getDateFromDirectory(final Directory directory, final int tagType) {

		Date date = null;
		if (directory != null) {

			boolean found = false;
			for (final CustomTag customTag : excluded_tags) {
				if (customTag.equals(directory, tagType)) {
					found = true;
				}
			}

			if (!found && StringUtils.compare("0000:00:00 00:00:00", directory.getString(tagType)) != 0) {
				date = directory.getDate(tagType, TimeZone.getTimeZone("CET"));
				if (date != null && date.before(INVALID_DATE.getTime())) {
					date = null;
				}
			}
		}
		return date;
	}
}
