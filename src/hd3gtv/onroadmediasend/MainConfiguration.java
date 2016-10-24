/*
 * This file is part of On Road Media Send.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.onroadmediasend;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.XMLBuilderParameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class MainConfiguration {
	
	private XMLConfiguration app_config;
	private File directory_conf;
	
	public MainConfiguration() throws IOException, ConfigurationException {
		List<String> classpath = Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)); //$NON-NLS-1$
		directory_conf = null;
		
		for (int pos = 0; pos < classpath.size(); pos++) {
			String path = classpath.get(pos);
			File item = new File(path);
			if (item.isDirectory() == false) {
				continue;
			}
			ArrayList<File> result = new ArrayList<>(FileUtils.listFiles(item, new IOFileFilter() {
				
				public boolean accept(File dir, String name) {
					return name.equals("log4j.xml"); //$NON-NLS-1$
				}
				
				public boolean accept(File file) {
					return file.getName().equals("log4j.xml"); //$NON-NLS-1$
				}
			}, null));
			
			if (result.isEmpty() == false) {
				directory_conf = item;
				break;
			}
		}
		
		if (directory_conf == null) {
			throw new FileNotFoundException("Can't found configuration directory"); //$NON-NLS-1$
		}
		
		Parameters params = new Parameters();
		
		FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class);
		builder.setAutoSave(true);
		
		XMLBuilderParameters x_parms = params.xml();
		x_parms.setEncoding("UTF-8"); //$NON-NLS-1$
		
		File xml_conf = new File(directory_conf.getAbsolutePath() + File.separator + "conf.xml"); //$NON-NLS-1$
		x_parms.setFile(xml_conf);
		/*boolean is_new =*/ initDefaultDocument(xml_conf);
		
		builder.configure(x_parms);
		app_config = builder.getConfiguration();
		/*if (is_new) {
			config.setProperty("_creation.hostname", "#000001");
			ArrayList<File> tests = new ArrayList<>();
			tests.add(new File("aa.b"));
			tests.add(new File("aa.c"));
			config.setProperty("_creation.tests", tests);
		}
		
		List<String> target = config.getList(String.class, "_creation.tests");
		System.out.println(target);
		
		*/
		/*HierarchicalConfiguration<ImmutableNode> conf = app_config.configurationAt("_creation");
		System.out.println(conf.configurationsAt("tests").get(0).getString("l"));
		
		System.exit(0);*/
	}
	
	public File getDirectoryConf() {
		return directory_conf;
	}
	
	private boolean initDefaultDocument(File xml_conf) throws IOException {
		if (xml_conf.exists() == false) {
			try {
				DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
				DocumentBuilder constructeur = fabrique.newDocumentBuilder();
				Document document = constructeur.newDocument();
				document.setXmlVersion("1.0"); //$NON-NLS-1$
				document.setXmlStandalone(true);
				document.createComment("Automatically generated the " + (new Date())); //$NON-NLS-1$
				Element root = document.createElement("configuration"); //$NON-NLS-1$
				document.appendChild(root);
				
				PrintWriter outFilePW = new PrintWriter(xml_conf, "UTF-8"); //$NON-NLS-1$
				DOMSource domSource = new DOMSource(document);
				StringWriter stringwriter = new StringWriter();
				StreamResult streamresult = new StreamResult(stringwriter);
				TransformerFactory tf = TransformerFactory.newInstance();
				Transformer transformer = tf.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
				transformer.transform(domSource, streamresult);
				outFilePW.print(stringwriter.toString());
				outFilePW.close();
				return true;
			} catch (ParserConfigurationException pce) {
				/**
				 * Specific error : can't to be thrown !
				 */
				pce.printStackTrace();
			} catch (UnsupportedEncodingException uee) {
				throw new IOException("Encoding XML is not supported", uee); //$NON-NLS-1$
			} catch (TransformerException tc) {
				throw new IOException("Converting error between XML and String", tc); //$NON-NLS-1$
			}
		}
		return false;
	}
	
	public XMLConfiguration get() {
		return app_config;
	}
	
}
