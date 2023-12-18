package com.lmartel54.easy;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.drew.imaging.FileType;
import com.drew.imaging.FileTypeDetector;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.imaging.bmp.BmpMetadataReader;
import com.drew.imaging.gif.GifMetadataReader;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.metadata.Metadata;
import com.lmartel54.easy.metadata.Device;
import com.lmartel54.easy.metadata.OriginalDate;

//2023 -> Saint-Vincent-sur-Jard (vendée)
//2022 -> Saint-Gilles-Croix-de-Vie (vendée)
//2019 -> bretagne
//2017 -> Notre-Dame-de-Monts (vendée)
//2015 -> Saint-Georges-de-Didonne (charente-maritime)

public class FileOrganizer {

	private static final boolean DUMP_METADATA = false;
	private static final String WORKING_FOLDER = "C:\\incubator";
//	private static final String WORKING_FOLDER = "\\\\home\\photos";

	private static final AtomicLong found = new AtomicLong(0);
	private static final AtomicLong success = new AtomicLong(0);
	private static final AtomicLong moveError = new AtomicLong(0);
	private static final AtomicLong unexpectedError = new AtomicLong(0);
	private static final Logger logger = LoggerFactory.getLogger(FileOrganizer.class);

	private static final String FAKE_DIRECTORY = "2006"; // "2005";
	private static final String FAKE_NAME = "2006-11"; // "2005-11-26";

	public static void main(String[] args) throws Exception {

		final List<Path> paths;

		final StopWatch watch = new StopWatch();
		watch.start();

		try (final Stream<Path> walk = Files.walk(Paths.get(WORKING_FOLDER), 1)) {
			paths = walk.filter(Files::isRegularFile).collect(Collectors.toList());
			found.getAndSet(paths.size());
		}

		paths.stream().forEachOrdered(path -> {
			try {
				logger.info("###### [file => {}] ######", path.toFile().getName());

				final Metadata metadata = getMetadata(path);

				if (DUMP_METADATA) {
					dumpMetada(metadata);
				}

				final Date date = OriginalDate.get(metadata);
				logger.info("[Original date] => {}", OriginalDate.format(date));

				final String device = Device.get(metadata);
				logger.info("[Device] => {}", device);

				String filename = getFileName(path, 1, date, device);
				logger.info("[SUCCESS] " + path + " => " + filename);

				// Manage subdirectory

				final Path workingDirectory;
				if (StringUtils.isNotEmpty(FAKE_DIRECTORY))
					workingDirectory = Files.createDirectories(Paths.get(FilenameUtils.concat(WORKING_FOLDER, FAKE_DIRECTORY)));
				else
					workingDirectory = Files.createDirectories(Paths.get(FilenameUtils.concat(WORKING_FOLDER, String.valueOf(parse(date, Calendar.YEAR)))));

				// Manage duplicate(s) file(s)
				final String fileUUID;
				if (StringUtils.isNotEmpty(FAKE_NAME))
					fileUUID = FAKE_NAME;
				else
					fileUUID = OriginalDate.format(date);

				try (final Stream<Path> walk = Files.walk(workingDirectory, 1)) {
					final List<Path> duplicate = walk.filter(file -> file.getFileName().toString().startsWith(fileUUID)).collect(Collectors.toList());
					if (duplicate.size() > 0) {
						filename = getFileName(path, (duplicate.size() + 1), date, device);
					}
				}

				// Archive

				Files.move(path, Paths.get(FilenameUtils.concat(workingDirectory.toString(), filename)));
				success.getAndIncrement();

			} catch (final FileAlreadyExistsException e) {
				moveError.getAndIncrement();
				logger.error("[MOVE.ERROR] {}", path, e);
			} catch (final Exception e) {
				unexpectedError.getAndIncrement();
				logger.error("[UNEXPECTED.ERROR] {}", path, e);
			}
		});

		watch.stop();

		logger.info("==========================");
		logger.info("{} fichiers trouvés sous : {}", found.get(), WORKING_FOLDER);
		logger.info("Temps de traitement: {}", watch.formatTime());
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
			logger.info("\t====== {} ======", directory.toString());
			directory.getTags().forEach(tag -> logger.info("\t{}", tag.toString()));
		});
		logger.info("\t==========================");
	}

	private static Metadata getMetadata(final Path path) throws IOException, ImageProcessingException {

		try (final BufferedInputStream inputStream = new BufferedInputStream(FileUtils.openInputStream(path.toFile()))) {
			final FileType fileType = FileTypeDetector.detectFileType(inputStream);
			switch (fileType) {
			case Jpeg:
				return JpegMetadataReader.readMetadata(path.toFile());
			case Bmp:
				return BmpMetadataReader.readMetadata(path.toFile());
			case Gif:
				return GifMetadataReader.readMetadata(path.toFile());
			case Mp4:
				return Mp4MetadataReader.readMetadata(path.toFile());
			case QuickTime:
//				return QuickTimeMetadataReader.readMetadata(path.toFile());
				return ImageMetadataReader.readMetadata(path.toFile());
			default:
				throw new FileNotFoundException("[" + fileType + "] unsupported file format ! ");
			}
			// if (fileType == FileType.Jpeg) {
			// System.out.println("JPEG");
			// } else if (fileType == FileType.Png) {
			// System.out.println("PNG");
			// } else {
			// System.out.println(fileType);
			// }

			// final Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
		}
	}

	private static String getFileName(final Path path, final int count, final Date date, final String device) {
		final StringBuilder builder = new StringBuilder();
		if (StringUtils.isNotEmpty(FAKE_NAME))
			builder.append(FAKE_NAME);
		else
			builder.append(OriginalDate.format(date));
//		builder.append(" [" + FilenameUtils.removeExtension(path.toFile().getName()) + "]");
		if (count > 1) {
			builder.append(" [" + String.format("%02d", count - 1) + "]");
		}
		if (!device.isEmpty()) {
			builder.append(" [" + device + "]");
		}
		builder.append("." + FilenameUtils.getExtension(path.toFile().getName()));
		return builder.toString();
	};
}
