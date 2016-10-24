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
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void stop() {
		controler.getQueue().stopAllConversions();
		controler.disconnectFileLogger();
	}
	
}
