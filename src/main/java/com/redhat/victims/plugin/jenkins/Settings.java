package com.redhat.victims.plugin.jenkins;

import java.io.PrintStream;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.plaf.TextUI;

import com.redhat.victims.VictimsException;


/**
 * Configuration settings are defined and stored by this class. All settings
 * are validated and initialized with defaults from this class.
 *
 * @author gmurphy
 * @author isaacanderson
 * 
 */
public final class Settings {
    /*
     * Different modes of operation for the plugin
     */
    public static final String MODE_WARNING     = "warning";
    public static final String MODE_FATAL       = "fatal";
    public static final String MODE_DISABLED    = "disabled";
    
    public static String[] ListModes()
    {
    	String[] modes = new String[3];
    	modes[0] = MODE_WARNING;
    	modes[1] = MODE_FATAL;
    	modes[2] = MODE_DISABLED;
    	
    	return modes;
    }
    /*
     * Allow developers to disable automatic updates
     */
    public static final String UPDATES_DISABLED = "offline";
    public static final String UPDATES_DAILY    = "daily";
    public static final String UPDATES_AUTO     = "auto";
    
    public static String[] ListUpdates() {
    	String updates[] = new String[3];
    	updates[0] = UPDATES_AUTO;
    	updates[1] = UPDATES_DISABLED;
    	updates[2] = UPDATES_DAILY;
    	
    	return updates;
    }
    
	public static final String BASE_URL_DEFAULT = "http://www.victi.ms/";
	public static final String ENTRY_DEFAULT = "service/";
	public static final String DRIVER_DEFAULT = "org.h2.Driver";
	public static final String URL_DEFAULT = ".victims";
	public static final String USER_DEFAULT = "victims";
	public static final String PASS_DEFAULT = "victims";
    /*
     * The cache region to use with JCS 
     */
    public static final String CACHE_REGION_NAME = "default";
    
    /*
     * Different settings that can be configured. These need to map back to the
     * names of each of the private members in the rule definition in order to
     * be configurable in the pom.xml @see VictimsRule
     */

    public static final String METADATA         = "metadata";
    public static final String FINGERPRINT      = "fingerprint";
    public static final String UPDATE_DATABASE  = "updates";
    
    public static final String CACHE_SETTINGS   = "cacheConfig";

    /**
     * Reasonably sensible defaults
     */
    public static final Map<String, String> defaults;


    static {
        Map<String, String> mappings = new HashMap<String, String>();

        mappings.put(METADATA, MODE_WARNING);
        mappings.put(FINGERPRINT, MODE_FATAL);
        mappings.put(UPDATE_DATABASE, UPDATES_AUTO);
      
        defaults = Collections.unmodifiableMap(mappings);
    }
    /**
     * Map containing configuration values
     */
    private Map<String, String> settings;


    /**
     * Generic interface used validate settings in the configuration
     */
    private interface Validator {

        public void validate() throws VictimsException;
    }

    private Validator[] required = {

        (new Validator() {

            public void validate() throws VictimsException {

                List<String> modes = new ArrayList<String>();
                modes.add(MODE_FATAL);
                modes.add(MODE_WARNING);
                modes.add(MODE_DISABLED);

                List<String> modeSettings = new ArrayList<String>();
                modeSettings.add(METADATA);
                modeSettings.add(FINGERPRINT);

                for (String item : modeSettings) {
                    String value = settings.get(item);
                    if (value == null) {
                        throw new VictimsException("Missing setting: " + item);
                    }

                    if (!modes.contains(value)) {
                        throw new VictimsException("Invalid mode: " + item);
                    }
                }
            }
        })
    };

    /**
     * Creates a new empty settings instance
     */
    public Settings() {
        settings = new HashMap<String, String>();
    }

    /**
     * Add new setting for the specified key.
     * @param k The key to add to the settings.
     * @param v The value to associate with the supplied key.
     */
    public void set(String k, String v) {
        settings.put(k, v);
    }

    /**
     * Retrieve a setting via specified key.
     * @parma k The key to lookup in they configuration settings.
     * @return Value for setting.
     */
    public String get(String k) {
        return settings.get(k);
    }
    
    /**
     * Use the supplied log to display the current settings.
     * @param log Log to send output to.
     */
    public void show(PrintStream log)
    {
        log.println("[VICTIMS] ------- CONFIG -------");
        for (Entry<String, String> kv : settings.entrySet()){
          log.println("[VICTIMS] " + kv.getKey() + " : " + kv.getValue());
        }
    }

    /**
     * Validate the current settings against a list of
     * internal validation rules.
     * @throws VictimsException When validation fails.
     */
    public void validate() throws VictimsException {
        for (Validator v : required) {
            v.validate();
        }
    }

}
