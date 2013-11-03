package com.redhat.victims.plugin.jenkins;

/*
 * #%L
 * This file is part of victims-plugin-jenkins.
 * %%
 * Copyright (C) 2013 The Victims Project
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.apache.commons.io.IOUtils;

import com.redhat.victims.VictimsException;
import com.redhat.victims.fingerprint.Metadata;

/**
 * Holds dependency metadata for caching
 * 
 * @author kurt
 */
public class FileStub {
	private Date cached;
	private String filename;
	private String id;
	private String artifactId;
	private File file;
	private Metadata meta;

	/**
	 * Holds metadata for file, if unreachable file we can't cache it.
	 * 
	 * @param file
	 *            file to cache
	 * @throws VictimsException
	 *             if can't be hashed
	 */
	public FileStub(File file) throws VictimsException {
		try {
			filename = file.getName();
			id = hashFile(file, filename);
			this.file = file;
			meta = getMeta(file);
			artifactId = createArtifactId(getFileName());
		} catch (IOException io) {
			filename = null;
			id = null;
		}
		cached = new Date();
	}

	/**
	 * Hash the file to get a "unique" key for caching
	 * 
	 * @param file
	 *            file to hash
	 * @param name
	 *            canonical file name
	 * @return name + md5 hash of file
	 * @throws VictimsException
	 */
	private static String hashFile(File file, String name)
			throws VictimsException {
		InputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] buffer = new byte[1024];

			MessageDigest mda = MessageDigest
					.getInstance(MessageDigestAlgorithms.MD5);
			int numRead;
			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					mda.update(buffer, 0, numRead);
				}
			} while (numRead != -1);

			return name + Hex.encodeHexString(mda.digest());

		} catch (NoSuchAlgorithmException e) {
			throw new VictimsException(String.format("Could not hash file: %s",
					name), e);
		} catch (IOException io) {
			throw new VictimsException(String.format("Could not open file: %s",
					name), io);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	/**
	 * Creates metadata from a given jar file.
	 * 
	 * @param jar
	 *            file containing a manifest
	 * @return Metadata containing extracted information from manifest file.
	 * @throws FileNotFoundException
	 * @throws VictimsException
	 */
	public static Metadata getMeta(File jar) throws FileNotFoundException,
			VictimsException {
		if (!jar.getAbsolutePath().endsWith(".jar"))
			return null;
		JarInputStream jis = null;
		try {
			jis = new JarInputStream(new FileInputStream(jar));
			Manifest mf = jis.getManifest();
			jis.close();
			if (mf != null)
				return Metadata.fromManifest(mf);
		} catch (IOException io) {
			throw new VictimsException(String.format("Could not open file: %s",
					jar.getName()), io);
		} finally {
			IOUtils.closeQuietly(jis);
		}
		return null;
	}

	/**
	 * Creates an artifact ID for a java library. Strips off version and any non
	 * alphanumeric characters
	 * 
	 * @param name
	 * @return
	 */
	public String createArtifactId(String name) {
		// Strip version
		if (getVersion() == null){
			name = filename;
		} else {
			name = name.split(getVersion())[0];
		}
		// Strip non alphanumeric characters
		return name.replaceAll("[^\\p{L}\\p{Nd}]$", "");
	}

	/**
	 * @return Name of library hopefully in the form of a maven artifact id
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @return Implementation title as defined in library manifest
	 */
	public String getTitle() {
		return meta.get(Attributes.Name.IMPLEMENTATION_TITLE.toString());
	}

	/**
	 * @return Implementation version as defined in library manifest
	 */
	public String getVersion() {
		return meta.get(Attributes.Name.IMPLEMENTATION_VERSION.toString());
	}

	/**
	 * @return File for this Stub
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return unique file identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return Canonical file name
	 */
	public String getFileName() {
		return filename;
	}

	/**
	 * @return Date when file was cached
	 */
	public Date getCachedDate() {
		return cached;
	}

	/**
	 * Returns string representation of the FileStub in the form of id:
	 * "filename + hash of file", file: "filename", created on: "date"
	 */
	public String toString() {
		return String.format("id: %s, file: %s, created on: %s", id, filename,
				cached.toString());
	}
}
