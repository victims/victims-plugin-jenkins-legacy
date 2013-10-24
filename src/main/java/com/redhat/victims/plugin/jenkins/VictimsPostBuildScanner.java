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
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.redhat.victims.VictimsConfig;
import com.redhat.victims.VictimsException;
import com.redhat.victims.VictimsResultCache;
import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.database.VictimsDBInterface;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class VictimsPostBuildScanner extends Recorder {
    
    private String baseUrl = Settings.BASE_URL_DEFAULT;
    private String entryPoint = Settings.ENTRY_DEFAULT;
    private String metadata = Settings.MODE_WARNING;
    private String fingerprint = Settings.MODE_WARNING;
    private String updates = Settings.UPDATES_AUTO;
    private String jdbcDriver = VictimsDB.defaultDriver();
    private String jdbcUrl = VictimsDB.defaultURL();
    private String jdbcUsername = Settings.USER_DEFAULT;
    private String jdbcPassword = Settings.PASS_DEFAULT;
    private String outputDir = "";
    private Boolean printCheckedFiles = false;

    public ExecutionContext ctx;

    @DataBoundConstructor
    public VictimsPostBuildScanner(final String baseUrl,
            final String entryPoint, final String metadata,
            final String fingerprint, final String updates,
            final String jdbcDriver, final String jdbcUrl,
            final String jdbcUsername, final String jdbcPassword,
            final String outputDir, final Boolean printCheckedFiles) {
        setBaseUrl(baseUrl);
        setEntryPoint(entryPoint);
        setMetadata(metadata);
        setFingerprint(fingerprint);
        setUpdates(updates);
        setJdbcDriver(jdbcDriver);
        setJdbcUrl(jdbcUrl);
        setJdbcUsername(jdbcUsername);
        setJdbcPassword(jdbcPassword);
        setOutputDir(outputDir);
        setPrintCheckedFiles(printCheckedFiles);
    }

    // Descriptor Implementation
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Scan Project Dependencies for Vulnerabilities";
        }

        public ListBoxModel doFillMetadataItems() {
            ListBoxModel items = new ListBoxModel();
            for (String wt : Settings.ListModes()) {
                items.add(wt.substring(0, 1).toUpperCase() + wt.substring(1),
                        wt);
            }
            return items;
        }

        public ListBoxModel doFillFingerprintItems() {
            return doFillMetadataItems();
        }

        public ListBoxModel doFillUpdatesItems() {
            ListBoxModel items = new ListBoxModel();
            for (String up : Settings.ListUpdates()) {
                items.add(up.substring(0, 1).toUpperCase() + up.substring(1),
                        up);
            }
            return items;
        }

        public String getDefaultBaseUrl() {
            return Settings.BASE_URL_DEFAULT;
        }

        public String getDefaultEntryPoint() {
            return Settings.ENTRY_DEFAULT;
        }

        public String getDefaultJdbcDriver() {
            return VictimsDB.defaultDriver();
        }

        /* Currently not working (dependency clash?) */
        public String getDefaultJdbcUrl() {
            return VictimsDB.defaultURL();
        }

        public String getDefaultJdbcUsername() {
            return Settings.USER_DEFAULT;
        }

        public String getDefaultJdbcPassword() {
            return Settings.PASS_DEFAULT;
        }
        
        // Jenkins form validation methods
        public FormValidation doCheckBaseUrl(@QueryParameter final String baseUrl) {
            FormValidation fv;
            
            if(!baseUrl.equals("")) {
                fv = FormValidation.ok();
                return fv;
            }
            fv = FormValidation.error("Base Url cannot be empty");
            return fv;
        }
        
        public FormValidation doCheckEntryPoint(@QueryParameter final String entryPoint) {
            FormValidation fv;
            
            if(!entryPoint.equals("")) {
                fv = FormValidation.ok();
                return fv;
            }
            fv = FormValidation.error("Entry Point cannot be empty");
            return fv;
        }
        
        public FormValidation doCheckJdbcDriver(@QueryParameter final String jdbcDriver) {
            FormValidation fv;
            
            if(!jdbcDriver.equals("")) {
                fv = FormValidation.ok();
                return fv;
            }
            fv = FormValidation.error("No jdbc driver supplied");
            return fv;
        }
        
        public FormValidation doCheckJdbcUrl(@QueryParameter final String jdbcUrl) {
            FormValidation fv;
            
            if(!jdbcUrl.equals("")) {
                fv = FormValidation.ok();
                return fv;
            }
            fv = FormValidation.error("No jdbc url supplied");
            return fv;
        }
        
        public FormValidation doCheckOutputDir(@QueryParameter final String outputDir) {
            FormValidation fv;
            
            if(!outputDir.equals("")) {
                fv = FormValidation.ok();
                return fv;
            }
            fv = FormValidation.error("Output location cannot be empty");
            return fv;
        }
    }

    @Extension
    public static final DescriptorImpl Descriptor = new DescriptorImpl();

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return Descriptor;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        // No synchronisation necessary between concurrent builds
        return BuildStepMonitor.NONE;
    }

    // Function that is run when project is built
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws IOException, AbortException {
        PrintStream log = listener.getLogger();
        
        log.println(TextUI.box("STARTING VICTIMS SCAN"));
        
        setupExecutionContext(listener.getLogger());
        execute();

        listener.getLogger().println(TextUI.box("FINISHED VICTIMS SCAN"));
        
        return true;
    }

    private void setupExecutionContext(PrintStream log) throws AbortException {
        ctx = new ExecutionContext();
        ctx.setSettings(new Settings());
        ctx.setLog(log);

        ctx.getSettings().set(VictimsConfig.Key.URI, baseUrl);
        ctx.getSettings().set(VictimsConfig.Key.DB_DRIVER, jdbcDriver);
        ctx.getSettings().set(VictimsConfig.Key.DB_URL, jdbcUrl);
        ctx.getSettings().set(Settings.METADATA, metadata);
        ctx.getSettings().set(Settings.FINGERPRINT, fingerprint);
        ctx.getSettings().set(VictimsConfig.Key.ENTRY, entryPoint);
        ctx.getSettings().set(VictimsConfig.Key.DB_USER, jdbcUsername);
        ctx.getSettings().set(VictimsConfig.Key.DB_PASS, jdbcPassword);
        ctx.getSettings().set(Settings.UPDATE_DATABASE, updates);

        System.setProperty(VictimsConfig.Key.ALGORITHMS, "SHA512");

        try {
            VictimsResultCache cache = new VictimsResultCache();
            ctx.setCache(cache);

            VictimsDBInterface db = VictimsDB.db();
            ctx.setDatabase(db);

            ctx.getSettings().validate();
            ctx.getSettings().show(ctx.getLog());
        } catch (VictimsException e) {
            log.println("[VICTIMS] ERROR:");
            log.println(e.getMessage());
            throw new AbortException();
        }
    }

    /**
     * Creates and synchronises the database then checks supplied dependencies
     * against the vulnerability database.
     */
    private void execute() throws AbortException {
        VictimsResultCache cache = ctx.getCache();
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = null;
        List<Future<FileStub>> jobs = null;
        PrintStream log = ctx.getLog();
        boolean buildFailure = false;
        ArrayList<VulnerableDependencyException> vulnerabilities = new ArrayList<VulnerableDependencyException>();

        try {
            // All files will be scanned for vulnerabilities and reported on at the end
            // rather than fail at the first one
            
            // Sync database
            updateDatabase(ctx);
            // Concurrency, yay!
            executor = Executors.newFixedThreadPool(cores);
            jobs = new ArrayList<Future<FileStub>>();

            // Find all files under supplied path
            Collection<File> sources = listFiles(this.outputDir);
            log.println("Scanning Files:");
            for (File f : sources) {
                if (printCheckedFiles) {
                  log.println("\t- " + f.getAbsolutePath());    
                }
                FileStub fs;
                try {
                    fs = new FileStub(f);
                } catch (Exception e) {
                    log.println("ERROR : unable to generate filestub for file: " + f.getAbsolutePath());
                    continue;
                }
                String fsid = fs.getId();
                // Check the cache
                if (cache.exists(fsid)) {
                    HashSet<String> cves = cache.get(fsid);
                    
                    if(printCheckedFiles) {
                        log.println("Cached: " + fsid);
                    }
                    
                    /* Report vulnerabilities */
                    if (!cves.isEmpty()) {
                        VulnerableDependencyException err = new VulnerableDependencyException(
                                fs, Settings.FINGERPRINT, cves);
                        vulnerabilities.add(err);
                        log.println(err.getLogMessage());
                        if (err.isFatal(ctx)) {
                            buildFailure = true;
                        }
                    }
                    continue;
                }

                // Process dependencies that haven't been cached
                Callable<FileStub> worker = new VictimsCommand(ctx, fs);
                jobs.add(executor.submit(worker));
            }
            executor.shutdown();
            
            // Check the results
            for (Future<FileStub> future : jobs) {
                try {
                    FileStub checked = future.get();
                    if (checked != null) {
                        cache.add(checked.getId(), null);
                    }
                } catch (InterruptedException ie) {
                    log.println(ie.getMessage());
                } catch (ExecutionException e) {

                    Throwable cause = e.getCause();
                    if (cause instanceof VulnerableDependencyException) {
                        VulnerableDependencyException vbe = (VulnerableDependencyException) cause;
                        cache.add(vbe.getId(), vbe.getVulnerabilites());

                        // Add exception to list for logging as group
                        vulnerabilities.add(vbe);
                        log.println(vbe.getLogMessage());

                        if (vbe.isFatal(ctx)) {
                            buildFailure = true;
                        }
                    } else {
                        throw new VictimsBuildException(e.getCause().getMessage());
                    }
                }
            }
        } catch (VictimsException ve) {
            log.println("vic exception found: " + ve.getMessage());
            throw new VictimsBuildException(ve.getMessage());

        } finally {
            if (executor != null) {
                executor.shutdown();
            }
        }
        
        if (!vulnerabilities.isEmpty()) {
            for(VulnerableDependencyException ex : vulnerabilities) {
                log.println(ex.getErrorMessage());
            }
        }
        
        if (buildFailure) {
            throw new AbortException("Vulnerable jar found");
        }
    }

    /**
     * Updates the database according to the given configuration
     * 
     * @param ctx
     * @throws VictimsException
     */
    private void updateDatabase(ExecutionContext context)
            throws VictimsException {
        VictimsDBInterface db = context.getDatabase();
        PrintStream log = context.getLog();

        Date updated = db.lastUpdated();

        // update: auto
        if (context.updateAlways()) {
            log.println("Updating database");
            db.synchronize();
        } else if (context.updateDaily()) { // update: daily
            Date today = new Date();
            SimpleDateFormat cmp = new SimpleDateFormat("yyyyMMdd");
            boolean updatedToday = cmp.format(today)
                    .equals(cmp.format(updated));

            if (!updatedToday) {
                log.println("Updating database");
                db.synchronize();
            } else {
                log.println("Database last updated: "
                        + updated.toString());
            }
        } else { // update: disabled
            log.println("Database synchronization disabled.");
        }
    }
    
    /**
     * Return a list of all jars in the output directory or the specified
     * jar as a list.  As these files/directory might not prior to the
     * build we are checking their existence here rather than during the
     * configuration of the VictimsPostBuildScanner.
     * 
     * @return a list of jars to scan for vulnerabilities
     * @throws AbortException
     */
    public Collection<File> listFiles(String outputDirectory) throws AbortException
    {  
        File outputFile = new File(outputDirectory);
        if (!outputFile.exists()) {
            // Output file/dir should exist by now.
            throw new AbortException("Output directory/file does not exist");
        }
        
        if (outputFile.isFile())
        {
            Collection<File> file = new ArrayList<File>();
            file.add(outputFile);
            return file;
        }
        
        if (outputFile.isDirectory()) {
            Collection<File> files = FileUtils.listFiles(outputFile, new RegexFileFilter("^(.*?)\\.jar"), DirectoryFileFilter.DIRECTORY);
            return files;
        }
        
        // Something has gone horribly wrong
        throw new AbortException("Supplied output location is neither a file nor directory");
    }
    
    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(final String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(final String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(final String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getUpdates() {
        return updates;
    }

    public void setUpdates(final String updates) {
        this.updates = updates;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(final String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(final String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(final String outputDir) {
        this.outputDir = outputDir;
    }
    
    public Boolean getPrintCheckedFiles() {
        return printCheckedFiles;
    }
    
    public void setPrintCheckedFiles(Boolean b) {
        this.printCheckedFiles = b;
    }

}
