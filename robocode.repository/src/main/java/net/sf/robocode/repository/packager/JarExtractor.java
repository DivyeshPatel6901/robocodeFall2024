package net.sf.robocode.repository.packager;

import net.sf.robocode.io.FileUtil;
import net.sf.robocode.io.Logger;
import net.sf.robocode.io.URLJarCollector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.jar.JarInputStream;
import java.util.jar.JarEntry;
import java.net.URLConnection;
import java.net.URL;

public class JarExtractor {

	// Helper method to create parent directories
	private static void ensureParentDirectoryExists(File file) {
		File parentDirectory = new File(file.getParent());
		if (!parentDirectory.exists() && !parentDirectory.mkdirs()) {
			Logger.logError("Cannot create parent dir: " + parentDirectory);
		}
	}

	// Helper method to validate and resolve paths
	private static File validateAndResolve(File destDir, String entryName) throws IOException {
		File file = new File(destDir, entryName);
		String destDirPath = destDir.getCanonicalPath();
		String filePath = file.getCanonicalPath();

		// Ensure the resulting path is within the target directory
		if (!filePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Invalid entry detected: " + entryName);
		}
		return file;
	}

	public static void extractJar(URL url) {
		File dest = FileUtil.getRobotsDir();
		InputStream is = null;
		BufferedInputStream bis = null;
		JarInputStream jarIS = null;

		try {
			final URLConnection con = URLJarCollector.openConnection(url);

			is = con.getInputStream();
			bis = new BufferedInputStream(is);
			jarIS = new JarInputStream(bis);

			JarEntry entry = jarIS.getNextJarEntry();

			while (entry != null) {
				if (entry.isDirectory()) {
					File dir = validateAndResolve(dest, entry.getName()); // Validate directory path
					// Use the helper method to create the parent directory
					ensureParentDirectoryExists(dir);
					if (!dir.exists() && !dir.mkdirs()) {
						throw new IOException("Failed to create directory: " + dir);
					}
				} else {
					extractFile(dest, jarIS, entry); // Process files using the existing extractFile method
				}
				entry = jarIS.getNextJarEntry();
			}
		} catch (IOException e) {
			Logger.logError(e);
		} finally {
			FileUtil.cleanupStream(jarIS);
			FileUtil.cleanupStream(bis);
			FileUtil.cleanupStream(is);
		}
	}

	public static void extractFile(File dest, JarInputStream jarIS, JarEntry entry) throws IOException {
		File out = validateAndResolve(dest, entry.getName()); // Validate file path

		// Use the helper method to create the parent directory
		ensureParentDirectoryExists(out);

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		byte[] buf = new byte[2048];

		try {
			fos = new FileOutputStream(out);
			bos = new BufferedOutputStream(fos);

			int num;
			while ((num = jarIS.read(buf, 0, 2048)) != -1) {
				bos.write(buf, 0, num);
			}
		} finally {
			FileUtil.cleanupStream(bos);
			FileUtil.cleanupStream(fos);
		}
	}
}
