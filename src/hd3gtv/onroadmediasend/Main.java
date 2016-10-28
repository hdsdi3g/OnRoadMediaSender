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

import java.util.ResourceBundle;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import hd3gtv.tools.FreehandClass;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

@SuppressWarnings("nls")
public class Main extends Application {
	
	private MainControler controler;
	
	@Override
	public void start(Stage primary_stage) {
		try {
			if (SystemUtils.IS_OS_MAC_OSX) {
				setOSXDockIcon(getClass(), "icon.png");
			}
			
			FXMLLoader d = new FXMLLoader();
			d.setResources(ResourceBundle.getBundle(Main.class.getPackage().getName() + ".messages"));
			BorderPane root = (BorderPane) d.load(getClass().getResource("MainApp.fxml").openStream());
			
			controler = d.getController();
			controler.startApp(primary_stage);
			
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			primary_stage.getIcons().add(new Image(getClass().getResourceAsStream("icon.png")));
			
			primary_stage.setScene(scene);
			primary_stage.show();
			
			primary_stage.setOnCloseRequest(event -> {
				controler.getQueue().stopAllConversions();
				System.exit(0);
			});
		} catch (
		
		Exception e) {
			e.printStackTrace();
		}
	}
	
	private static FreehandClass fc_Application;
	
	private static void setOSXDockIcon(Class<?> class_ref, String ressource_name) {
		try {
			java.awt.Image i = new javax.swing.ImageIcon(Main.class.getResource("icon.png")).getImage();
			FreehandClass fc_image = new FreehandClass(i, java.awt.Image.class);
			fc_Application = FreehandClass.staticCall("com.apple.eawt.Application", "getApplication");
			fc_Application.call("setDockIconImage", fc_image);
			// com.apple.eawt.Application.getApplication().setDockIconImage(i);
		} catch (Exception e) {
			Logger.getLogger(class_ref).warn("Can't set OSX icon", e);
		}
	}
	
	public static void setOSXDockBadge(String value) {
		try {
			fc_Application.call("setDockIconBadge", value);
			// com.apple.eawt.Application.getApplication().setDockIconBadge("2");
		} catch (Exception e) {
			if (Logger.getLogger(Main.class).isDebugEnabled()) {
				Logger.getLogger(Main.class).debug("Can't set OSX badge", e);
			}
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void stop() {
		controler.getQueue().stopAllConversions();
		controler.disconnectFileLogger();
	}
	
}
