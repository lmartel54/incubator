package com.lmartel54.easy;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileTypeDirectory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileOrganizer {

	private static final Logger logger = LoggerFactory.getLogger(FileOrganizer.class);

	public static void main(String[] args) throws Exception {

		final SimpleDateFormat SDF_PATTERN = new SimpleDateFormat("[yyyy-MM-dd][HH:mm:ss]");
		final Pattern GRP_PATTERN = Pattern.compile("\\[[^\\]\\[]*]");

		final AtomicLong found = new AtomicLong(0);
		final AtomicLong success = new AtomicLong(0);
		final AtomicLong moveError = new AtomicLong(0);
		final AtomicLong unexpectedError = new AtomicLong(0);

		final String root = "/home/user/devbox/projects/test";

		final List<Path> paths;

		final StopWatch watch = new StopWatch();
		watch.start();

		try (final Stream<Path> walk = Files.walk(Paths.get(root), 1)) {
			paths = walk.filter(Files::isRegularFile).collect(Collectors.toList());
			found.getAndSet(paths.size());
		}

		paths.stream().forEachOrdered(path -> {
			try {
				logger.info("\n\n@@@ file => {} @@@\n", path.toFile().getName());

				FileType fileType = FileTypeDetector
						.detectFileType(new DataInputStream(FileUtils.openInputStream(path.toFile())));

				if (fileType == FileType.Jpeg) {
					System.out.println("JPEG");
				} else if (fileType == FileType.Png) {
					System.out.println("PNG");
				}
				else {
					System.out.println(fileType);
				}

				// Extract file metadata

				final Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());

				dumpMetada(metadata);
				if (true) {
					return;
				}

				// final FileTypeDirectory fileType = metadata.getFirstDirectoryOfType(FileTypeDirectory.class);

				final ExifSubIFDDirectory subIfdDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				final Date date = subIfdDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL,
						TimeZone.getTimeZone("CET"));

				final ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
				final String manufacturer = StringUtils.trimToEmpty(ifd0Dir.getDescription(ExifIFD0Directory.TAG_MAKE));
				final String model = StringUtils.trimToEmpty(ifd0Dir.getDescription(ExifIFD0Directory.TAG_MODEL));

				String filename = SDF_PATTERN.format(date) + "[PICT0001][" + manufacturer + " " + model + "]";

				logger.info("[SUCCESS] " + path + " => " + filename);

				// Manage subdirectory

				final Path workingDirectory = Files.createDirectories(
						Paths.get(FilenameUtils.concat(root, String.valueOf(parse(date, Calendar.YEAR)))));

				// Manage duplicate file

				final Matcher matcher = GRP_PATTERN.matcher(filename);

				final List<String> groups = new LinkedList<>();
				while (matcher.find()) {
					groups.add(matcher.group());
				}

				final String fileUUID = groups.get(0) + groups.get(1);

				try (final Stream<Path> walk = Files.walk(workingDirectory, 1)) {
					final List<Path> duplicate = walk.filter(file -> file.getFileName().toString().startsWith(fileUUID))
							.collect(Collectors.toList());
					if (duplicate.size() > 0) {
						filename = SDF_PATTERN.format(date) + "[PICT" + String.format("%04d", (duplicate.size() + 1))
								+ "][" + manufacturer + " " + model + "]";
					}
				}

				// Archive

				Files.move(path, Paths.get(FilenameUtils.concat(workingDirectory.toString(), filename)));
				success.getAndIncrement();

			} catch (final FileAlreadyExistsException e) {
				moveError.getAndIncrement();
				logger.error("[MOVE.ERROR] " + path, e);
			} catch (final Exception e) {
				unexpectedError.getAndIncrement();
				logger.error("[UNEXPECTED.ERROR] " + path, e);
			}
		});

		watch.stop();

		logger.info("==========================");
		logger.info("{} fichiers trouvés sous : {}", found.get(), root);
		logger.info("temps de traitement: {}", watch.formatTime());
		logger.info("{} fichier(s) traité(s)", success.get());
		logger.info("{} fichier(s) non déplacé(s)", moveError.get());
		logger.info("{} fichier(s) en erreur", unexpectedError.get());
		logger.info("==========================");

	}

	private static int parse(final Date date, final int field) {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return calendar.get(field);
	}

	private static void dumpMetada(final Metadata metadata) throws ImageProcessingException, IOException {
		metadata.getDirectories().forEach(directory -> {
			logger.info("====== {} ======", directory.toString());
			directory.getTags().forEach(tag -> logger.info("{}", tag.toString()));
		});
	}
}
