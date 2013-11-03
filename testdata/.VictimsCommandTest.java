package com.redhat.victims.plugin.ant;

import java.io.File;

import com.redhat.victims.VictimsException;
import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.database.VictimsDBInterface;
import com.redhat.victims.plugin.ant.FileStub;
import com.redhat.victims.plugin.ant.ExecutionContext;
import com.redhat.victims.plugin.ant.VictimsCommand;
import org.apache.tools.ant.types.resources.LogOutputResource;
import org.apache.tools.ant.BuildFileTest;
import org.junit.Test;

public class VictimsCommandTest extends BuildFileTest {

	private static VictimsDBInterface vdb;
	private VictimsCommand vc;
	
	public VictimsCommandTest(String s){
		super(s);
	}
	
	public void setUp(){
		File build = new File("testdata", "build.xml");
		
		configureProject(build.getAbsolutePath());
		ExecutionContext ctx = new ExecutionContext();
	//	LogOutputResource log = new LogOutputResource();
	//	ctx.setLog(l)
	//	vc = new VictimsCommand();
	}
	
	public void tearDown(){
		
	}
	
	public static void sync() throws VictimsException {
		vdb = VictimsDB.db();
		vdb.synchronize();
	}
}
