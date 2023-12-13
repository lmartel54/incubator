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
import com.drew.metadata.exif.makernotes.AppleMakernoteDirectory;
import com.drew.metadata.exif.makernotes.PanasonicMakernoteDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mov.media.QuickTimeSoundDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
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
		tags.add(new CustomTag(IccDirectory.class, IccDirectory.TAG_PROFILE_DATETIME, "DATE.ICC.PROFILE_DATETIME"));
		tags.add(new CustomTag(FileSystemDirectory.class, FileSystemDirectory.TAG_FILE_MODIFIED_DATE, "[File] [TAG_FILE_MODIFIED_DATE]"));
		// [MP4]
		tags.add(new CustomTag(Mp4Directory.class, Mp4Directory.TAG_CREATION_TIME, "[MP4] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(Mp4Directory.class, Mp4Directory.TAG_MODIFICATION_TIME, "[MP4] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(Mp4VideoDirectory.class, Mp4VideoDirectory.TAG_CREATION_TIME, "[MP4 Video] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(Mp4VideoDirectory.class, Mp4VideoDirectory.TAG_MODIFICATION_TIME, "[MP4 Video] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(Mp4SoundDirectory.class, Mp4SoundDirectory.TAG_CREATION_TIME, "[MP4 Sound] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(Mp4SoundDirectory.class, Mp4SoundDirectory.TAG_MODIFICATION_TIME, "[MP4 Sound] [TAG_MODIFICATION_TIME]"));
		// [MOV]
		tags.add(new CustomTag(QuickTimeDirectory.class, QuickTimeDirectory.TAG_CREATION_TIME, "[QuickTime] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(QuickTimeDirectory.class, QuickTimeDirectory.TAG_MODIFICATION_TIME, "[QuickTime] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(QuickTimeVideoDirectory.class, QuickTimeVideoDirectory.TAG_CREATION_TIME, "[QuickTime Video] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(QuickTimeVideoDirectory.class, QuickTimeVideoDirectory.TAG_MODIFICATION_TIME, "[QuickTime Video] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(QuickTimeMetadataDirectory.class, 1286, "[QuickTime Metadata] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(QuickTimeMetadataDirectory.class, QuickTimeMetadataDirectory.TAG_MODIFICATION_TIME, "[QuickTime Metadata] [TAG_MODIFICATION_TIME]"));
		tags.add(new CustomTag(QuickTimeMetadataDirectory.class, QuickTimeMetadataDirectory.TAG_CONTENT_IDENTIFIER, "[QuickTime Metadata] [TAG_CONTENT_IDENTIFIER]"));
		tags.add(new CustomTag(QuickTimeSoundDirectory.class, 20481, "[QuickTime Sound] [TAG_CREATION_TIME]"));
		tags.add(new CustomTag(QuickTimeSoundDirectory.class, 20482, "[QuickTime Sound] [TAG_MODIFICATION_TIME]"));
		// [PANASONIC]
		tags.add(new CustomTag(PanasonicMakernoteDirectory.class, PanasonicMakernoteDirectory.TAG_BABY_AGE, "[Panasonic] [TAG_BABY_AGE]"));
		tags.add(new CustomTag(PanasonicMakernoteDirectory.class, PanasonicMakernoteDirectory.TAG_BABY_AGE_1, "[Panasonic] [TAG_BABY_AGE_1]"));
		// [APPLE]
		tags.add(new CustomTag(AppleMakernoteDirectory.class, 0x002b, "[AppleMakeNote] [[Unknown tag]"));
		tags.add(new CustomTag(AppleMakernoteDirectory.class, 0x0020, "[AppleMakeNote] [[Unknown tag]"));
		tags.add(new CustomTag(AppleMakernoteDirectory.class, AppleMakernoteDirectory.TAG_IMAGE_UNIQUE_ID, "[AppleMakeNote] [[TAG_IMAGE_UNIQUE_ID]"));
		tags.add(new CustomTag(AppleMakernoteDirectory.class, AppleMakernoteDirectory.TAG_CONTENT_IDENTIFIER, "[AppleMakeNote] [[TAG_CONTENT_IDENTIFIER]"));
		tags.add(new CustomTag(AppleMakernoteDirectory.class, AppleMakernoteDirectory.TAG_BURST_UUID, "[AppleMakeNote] [[TAG_BURST_UUID]"));
		// [IPTC]
//		tags.add(new CustomTag(IptcDirectory.class, IptcDirectory.TAG_DATE_CREATED, "[Iptc] [[TAG_DATE_CREATED]"));
//		tags.add(new CustomTag(IptcDirectory.class, IptcDirectory.TAG_TIME_CREATED, "[Iptc] [[TAG_TIME_CREATED]"));
//		tags.add(new CustomTag(IptcDirectory.class, IptcDirectory.TAG_DIGITAL_DATE_CREATED, "[Iptc] [[TAG_DIGITAL_DATE_CREATED]"));
//		tags.add(new CustomTag(IptcDirectory.class, IptcDirectory.TAG_DIGITAL_TIME_CREATED, "[Iptc] [[TAG_DIGITAL_TIME_CREATED]"));
		
		
		excluded_tags.add(new CustomTag(GpsDirectory.class, GpsDirectory.TAG_DATE_STAMP, "DATE.GPS.STAMP"));
		excluded_tags.add(new CustomTag(FileSystemDirectory.class, FileSystemDirectory.TAG_FILE_NAME, "DATE.FILE.NAME"));
		excluded_tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifSubIFDDirectory.TAG_SUBSECOND_TIME, "DATE.DATE.SUBIFD.SUBSECOND_TIME"));
		excluded_tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifSubIFDDirectory.TAG_SUBSECOND_TIME_ORIGINAL, "DATE.SUBIFD.SUBSECOND_TIME_ORIGINAL"));
		excluded_tags.add(new CustomTag(ExifSubIFDDirectory.class, ExifSubIFDDirectory.TAG_SUBSECOND_TIME_DIGITIZED, "DATE.SUBIFD.SUBSECOND_TIME_DIGITIZED"));
//		excluded_tags.add(new CustomTag(QuickTimeDirectory.class, QuickTimeDirectory.TAG_CREATION_TIME, "[QuickTime] [TAG_CREATION_TIME]"));
//		excluded_tags.add(new CustomTag(QuickTimeDirectory.class, QuickTimeDirectory.TAG_MODIFICATION_TIME, "[QuickTime] [TAG_MODIFICATION_TIME]"));
//		excluded_tags.add(new CustomTag(QuickTimeVideoDirectory.class, QuickTimeVideoDirectory.TAG_CREATION_TIME, "[QuickTime Video] [TAG_CREATION_TIME]"));
//		excluded_tags.add(new CustomTag(QuickTimeVideoDirectory.class, QuickTimeVideoDirectory.TAG_MODIFICATION_TIME, "[QuickTime Video] [TAG_MODIFICATION_TIME]"));
//		excluded_tags.add(new CustomTag(QuickTimeSoundDirectory.class, QuickTimeSoundDirectory.TAG_CREATION_TIME, "[QuickTime Sound] [TAG_CREATION_TIME]"));
//		excluded_tags.add(new CustomTag(QuickTimeSoundDirectory.class, QuickTimeSoundDirectory.TAG_MODIFICATION_TIME, "[QuickTime Sound] [TAG_MODIFICATION_TIME]"));
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
