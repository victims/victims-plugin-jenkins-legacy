package com.redhat.victims.plugin.jenkins;

import java.io.PrintStream;

import org.apache.tools.ant.types.resources.LogOutputResource;

import com.redhat.victims.VictimsResultCache;
import com.redhat.victims.database.VictimsDBInterface;

/**
 * Context to pass to each command.
 * 
 * @author gmurphy
 */
public final class ExecutionContext {

	private VictimsResultCache cache;
	private VictimsDBInterface database;
	private PrintStream log;
	private Settings settings;

	/**
	 * @return The log to use within this execution context.
	 */
	public PrintStream getLog() {
		return this.log;
	}

	/**
	 * @return The cache to store artifacts in
	 */
	public VictimsResultCache getCache() {
		return this.cache;
	}

	/**
	 * @return Configuration to apply to this execution context.
	 */
	public Settings getSettings() {
		return settings;
	}

	/**
	 * @return victims DB
	 */
	public VictimsDBInterface getDatabase() {
		return database;
	}
	
	/**
	 * Send all messages to this log.
	 * 
	 * @param l
	 *            The log to associate with this execution context.
	 */
	public void setLog(PrintStream l) {
		this.log = l;
	}

	/**
	 * Set victims database
	 * 
	 * @param database
	 *            Victims database
	 */
	public void setDatabase(VictimsDBInterface database) {
		this.database = database;
	}

	/**
	 * Set victims cache
	 * 
	 * @param victimsResultCache
	 *            Result cache for scanned dependencies
	 */
	public void setCache(VictimsResultCache victimsResultCache) {
		this.cache = victimsResultCache;

	}

	/**
	 * Applies the given settings to the execution context.
	 */
	public void setSettings(final Settings setup) {
		this.settings = setup;
	}

	/**
	 * Returns true if the setting is in fatal mode. Used when determining if
	 * the rule should fail a build.
	 * 
	 * @param mode
	 *            The configuration item to check if in fatal mode.
	 * @return True when the mode is in fatal mode.
	 */
	public boolean inFatalMode(String mode) {
		String val = settings.get(mode);
		return val != null && val.equalsIgnoreCase(Settings.MODE_FATAL);
	}

	/**
	 * Returns true if the value associated with the supplied key isn't set to
	 * disabled.
	 * 
	 * @param setting
	 *            The setting to check if is disabled.
	 * @return True if the setting is enabled.
	 */
	public boolean isEnabled(String setting) {
		String val = settings.get(setting);
		return val != null && !val.equalsIgnoreCase(Settings.MODE_DISABLED);
	}

	/**
	 * Returns true if automatic updates are enabled.
	 * 
	 * @return True if automatic updates of database are enabled.
	 */
	public boolean updatesEnabled() {
		String val = settings.get(Settings.UPDATE_DATABASE);
		return val != null
				&& (val.equalsIgnoreCase(Settings.UPDATES_AUTO) || val
						.equalsIgnoreCase(Settings.UPDATES_DAILY));
	}

	/**
	 * @return True if daily updates are enabled
	 */
	public boolean updateDaily() {
		String val = settings.get(Settings.UPDATE_DATABASE);
		return val != null && val.equalsIgnoreCase(Settings.UPDATES_DAILY);
	}

	/**
	 * @return True if automatic updates are enabled and run for each build
	 */
	public boolean updateAlways() {
		String val = settings.get(Settings.UPDATE_DATABASE);
		return val != null && val.equalsIgnoreCase(Settings.UPDATES_AUTO);
	}

}