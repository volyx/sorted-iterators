package io.github.volyx;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DiffUtils {
	private static final Logger logger = LoggerFactory.getLogger(DiffUtils.class);
	private DiffUtils(){}

	/**
	 * See {@link com.google.common.collect.Sets#difference(Set, Set)}
	 * @param source iterator from which values are excluded
	 * @param target values to exclude
	 * @param addConsumer
	 */
	public static void difference(Iterator<String> source, Iterator<String> target, Consumer<String> addConsumer) {
		try {

			if (!source.hasNext()) {
				return;
			}

			if (!target.hasNext()) {
				while (source.hasNext()) {
					addConsumer.accept(source.next());
				}
				return;
			}

			String line1;
			String line2 = target.next();

			while (source.hasNext()) {
				line1 = source.next();
				int compare = line1.compareTo(line2);
				if (compare == 0) {
					//nothing
				} else if (compare < 0) {
					addConsumer.accept(line1);
				} else {
					while (target.hasNext()) {
						line2 = target.next();
						if (line2.compareTo(line1) < 0) {

						} else {
							break;
						}
					}
					compare = line1.compareTo(line2);
					if (compare < 0) {
						addConsumer.accept(line1);
					} else if (!target.hasNext() && compare > 0) {
						addConsumer.accept(line1);
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * See {@link com.google.common.collect.Sets#symmetricDifference(Set, Set)}
	 * @param source
	 * @param target
	 * @param addConsumer
	 */
	public static void symmetricDifference(File source, File target, Consumer<String> addConsumer) {

		try (BufferedReader reader1 = new BufferedReader(new FileReader(source));
			 BufferedReader reader2 = new BufferedReader(new FileReader(target))) {

			String line1 = reader1.readLine();
			String line2 = reader2.readLine();

			String prevLine1 = "";
			String prevLine2 = "";

			while (line1 != null || line2 != null) {
				if (line1 == null) {
					addConsumer.accept(line2);
					prevLine2 = line2;
					line2 = reader2.readLine();
					continue;
				} else if (line2 == null) {
					addConsumer.accept(line1);
					prevLine1 = line1;
					line1 = reader1.readLine();
					continue;
				}

				// remove 01
				//
				// 01234_
				// __2_45

				line1 = line1.toUpperCase(Locale.ENGLISH);
				line2 = line2.toUpperCase(Locale.ENGLISH);

				Preconditions.checkArgument(line1.compareTo(prevLine1) >= 0, "sourceBufferedReader is not sorted");
				Preconditions.checkArgument(line2.compareTo(prevLine2) >= 0, "targetBufferedReader is not sorted");

				prevLine1 = line1;
				prevLine2 = line2;

				final int compare = line1.compareTo(line2);
				if (compare == 0) {
					line1 = reader1.readLine();
					line2 = reader2.readLine();
				} else if (compare < 0) {
					addConsumer.accept(line1);
					prevLine1 = line1;
					line1 = reader1.readLine();
				} else {
					addConsumer.accept(line2);
					prevLine2 = line2;
					line2 = reader2.readLine();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}


	public static DiffStatistics diff(BufferedReader sourceBufferedReader, BufferedReader targetBufferedReader, Consumer<String> addConsumer, Consumer<String> removeConsumer) {
		final AtomicLong addCounter = new AtomicLong();
		final AtomicLong removeCounter = new AtomicLong();

		String prevLine1;
		String prevLine2;

		try {

			String line1 = sourceBufferedReader.readLine();
			prevLine1 = line1;


			String line2 = targetBufferedReader.readLine();
			prevLine2 = line2;

			// 13
			// 012

			while (line1 != null || line2 != null) {
				if (line1 == null) {
					addConsumer.accept(line2.toUpperCase(Locale.ENGLISH));
					addCounter.incrementAndGet();
					line2 = targetBufferedReader.readLine();
					continue;
				} else if (line2 == null) {
					removeConsumer.accept(line1.toUpperCase(Locale.ENGLISH));
					removeCounter.incrementAndGet();
					line1 = sourceBufferedReader.readLine();
					continue;
				}

				// remove 01
				//
				// 01234_
				// __2_45

				line1 = line1.toUpperCase(Locale.ENGLISH);
				line2 = line2.toUpperCase(Locale.ENGLISH);

				Preconditions.checkArgument(line1.compareTo(prevLine1) >= 0, "sourceBufferedReader is not sorted");
				Preconditions.checkArgument(line2.compareTo(prevLine2) >= 0, "targetBufferedReader is not sorted");

				prevLine1 = line1;
				prevLine2 = line2;

				final int compare = line1.compareTo(line2);
				if (compare == 0) {
					line1 = sourceBufferedReader.readLine();
					line2 = targetBufferedReader.readLine();
				} else if (compare < 0) {
					removeConsumer.accept(line1);
					removeCounter.incrementAndGet();
					line1 = sourceBufferedReader.readLine();
				} else {
					addConsumer.accept(line2);
					addCounter.incrementAndGet();
					line2 = targetBufferedReader.readLine();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}

		final DiffStatistics diffStatistics = new DiffStatistics();
		diffStatistics.addCounter = addCounter.get();
		diffStatistics.removeCounter = removeCounter.get();
		return diffStatistics;
	}

	public static class DiffStatistics {
		public long addCounter;
		public long removeCounter;
	}
}
