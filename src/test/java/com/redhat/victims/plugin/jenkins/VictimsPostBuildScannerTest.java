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

import static org.junit.Assert.*;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

import com.redhat.victims.VictimsException;
import com.redhat.victims.fingerprint.Metadata;
import com.redhat.victims.plugin.jenkins.Settings;
import com.redhat.victims.plugin.jenkins.VictimsPostBuildScanner;

public class VictimsPostBuildScannerTest {

    @Test
    public void testFileStub()
    {
            File jar = new File("testdata", "spring-2.5.6.jar");
            File fakejar = new File("testdata/subfolder", "fake-jar_test-1.1.5.jar");
            try {
                    FileStub fs = new FileStub(jar);
                    assertTrue(fs.getId().contains("spring-2.5.6.jar"));
                    assertTrue(fs.getFile().equals(jar));
                    assertTrue(fs.getArtifactId().equals("spring"));
                    assertTrue(fs.getTitle().equals("Spring Framework"));
                    
                    /* Test artifact id creation */
                    FileStub fj = new FileStub(fakejar);
                    assertTrue(fj.getArtifactId().equals("fake-jar_test"));
                    assertTrue(fj.getVersion().equals("1.1.5"));
                    
            } catch (VictimsException e) {
                    fail("ERROR: " + e.getMessage());
            }
    }

    /**
     * Tests retrieval of Jar manifest info
     * 
     * @throws VictimsException
     *             if test data unavailable
     */
    @Test
    public void testMetadata() throws VictimsException {
            File jar = new File("testdata", "spring-2.5.6.jar");
            if (!jar.canRead()) {
                    throw new VictimsException(
                                    "Test data unavailable: spring-2.5.6.jar");
            }
            try {
                    Metadata meta = FileStub.getMeta(jar);
                    HashMap<String, String> gav = new HashMap<String, String>();
                    //fix naming
                    if (meta.containsKey("Manifest-Version"))
                            gav.put("groupId", meta.get("Manifest-Version"));
                    if (meta.containsKey("Implementation-Version"))
                            gav.put("artifactId", meta.get("Implementation-Version"));
                    if (meta.containsKey("Implementation-Title"))
                            gav.put("version", meta.get("Implementation-Title"));

                    assertTrue(gav.get("groupId").equals("1.0"));
                    assertTrue(gav.get("artifactId").equals("2.5.6"));
                    assertTrue(gav.get("version").equals("Spring Framework"));
            } catch (FileNotFoundException fn) {
                    //silently catch
            }
    }
    
    /**
     * Tests the directory search for jar files
     */   
    @Test
    public void testSearch() {
        VictimsPostBuildScanner vpbs = new VictimsPostBuildScanner("", "", Settings.MODE_DISABLED, Settings.MODE_DISABLED, "", "", "", "", "", "", false);
                
        File testDataFolder = new File("testdata");
        
        Collection files;
        
        try {
            files = vpbs.listFiles(testDataFolder.getAbsolutePath());
            File[] filesArray = (File[])files.toArray();
            assertTrue(filesArray[0].getAbsolutePath().equals(testDataFolder.getAbsolutePath() + "spring-2.5.6.jar"));
            assertTrue(filesArray[1].getAbsolutePath().equals(testDataFolder.getAbsolutePath() + "/subfolder/fake-jar_test-1.1.5.jar"));
        } catch (Exception e) {
            //shhh
        }
    }
}