/* 
 * jackaroo in CAT'11
 */

package cat11.agent.jackaroo.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implements a log to store plain-text output.
 * 
 * @author jackaroo Team
 * @version $Revision: 1.88 $
 */

public class DataRecord {
	private BufferedWriter record;

	private final String passName;

	public DataRecord(final String fileName) {
		try {
			record = new BufferedWriter(new FileWriter(new File(fileName)));
		} catch (final Exception e) {
			System.out.println("Fail to creat file.");
		}
		passName = fileName;
	}

	/**
	 * write a line of text into the log file.
	 * 
	 * @param msg
	 */
	public void putIn(final Object msg) {
		try {
			record.write((String) msg);
			// System.out.println(msg);
			record.newLine();
			record.flush();
		} catch (final Exception e) {
			System.out.println("Fail to put in a record.");
		}
	}

	public void writeIn(final Object msg) {
		putIn(msg);
	}

	/**
	 * not used.
	 */
	@SuppressWarnings("resource")
	public void readOut() {
		final byte[] buff = new byte[80];
		try {
			final InputStream fileIn = new FileInputStream(passName);
			fileIn.read(buff);
		} catch (final FileNotFoundException e) {
			System.out.println("Fail to find file.");
		} catch (final IOException e) {
		}
		final String s = new String(buff);
		System.out.println(s);
	}

	public void closeFile() {
		try {
			record.close();
		} catch (final IOException e) {
			System.out.println("Fail to close the file.");
		}
	}
}