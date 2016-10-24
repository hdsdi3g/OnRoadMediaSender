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
package hd3gtv.onroadmediasend.logpanel;

import java.util.Enumeration;
import java.util.function.Predicate;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import hd3gtv.onroadmediasend.MainConfiguration;
import hd3gtv.onroadmediasend.MainControler;
import hd3gtv.onroadmediasend.Messages;
import hd3gtv.onroadmediasend.logpanel.LogAppenderHandler.TextedEvent;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class LWControler {
	
	private final static Logger log = Logger.getLogger(LWControler.class);
	private LogAppenderHandler log_appender_handler;
	private Font font;
	
	public void startApp(Stage stage, Button btn_open, MainConfiguration configuration, LogAppenderHandler log_appender_handler, MainControler main_controler) {
		this.stage = stage;
		this.log_appender_handler = log_appender_handler;
		
		btn_open.setDisable(true);
		
		stage.setTitle(Messages.getString("LWControler.title")); //$NON-NLS-1$
		
		to_text_cb = new ToTextCb();
		
		String font_name = Font.getFamilies().stream().filter((Predicate<? super String>) predicate -> {
			if (predicate.equalsIgnoreCase("consolas")) {//$NON-NLS-1$
				return true;
			} else if (predicate.equalsIgnoreCase("monaco")) {//$NON-NLS-1$
				return true;
			} else if (predicate.equalsIgnoreCase("deja vu sans mono")) {//$NON-NLS-1$
				return true;
			} else {
				return false;
			}
		}).findFirst().orElse("Courier New"); //$NON-NLS-1$
		font = Font.font(font_name, 14);
		
		stage.setOnHidden(event -> {
			btn_open.setDisable(false);
			log_appender_handler.removeRegisterCallback(to_text_cb);
		});
		
		stage.setOnShown(l -> {
			textpnl = new TextFlow();
			scrollpnl = new ScrollPane();
			scrollpnl.setContent(textpnl);
			scrollpnl.setFitToHeight(true);
			scrollpnl.setFitToWidth(true);
			scrollpnl.setPadding(new Insets(5));
			
			textpnl.getChildren().addListener((ListChangeListener<Node>) change -> {
				textpnl.layout();
				scrollpnl.layout();
				scrollpnl.setVvalue(1.0f);
				scrollpnl.setHvalue(0f);
			});
			brdpane.centerProperty().set(scrollpnl);
			
			log_appender_handler.getAll().forEach(t_event -> {
				to_text_cb.onTextedEvent(t_event);
			});
			
			log_appender_handler.registerCallback(to_text_cb);
			
			cb_level.getItems().add(Level.OFF);
			cb_level.getItems().add(Level.FATAL);
			cb_level.getItems().add(Level.ERROR);
			cb_level.getItems().add(Level.WARN);
			cb_level.getItems().add(Level.INFO);
			cb_level.getItems().add(Level.DEBUG);
			cb_level.getItems().add(Level.TRACE);
			cb_level.getItems().add(Level.ALL);
			
			cb_level.getSelectionModel().select(Logger.getRootLogger().getLevel());
			cb_level.setOnAction(event -> {
				Level level = cb_level.getSelectionModel().getSelectedItem();
				log.info("Change log level " + level); //$NON-NLS-1$
				
				for (Enumeration<?> iterator = LogManager.getLoggerRepository().getCurrentLoggers(); iterator.hasMoreElements();) {
					((Logger) iterator.nextElement()).setLevel(level);
				}
				Logger.getRootLogger().setLevel(level);
			});
		});
		
		btnopentempdir.setOnAction(event -> {
			main_controler.openTempDirectory();
		});
		btnopenconfdir.setOnAction(event -> {
			main_controler.openApplicationConfDirectory();
		});
		
	}
	
	private Stage stage;
	private ToTextCb to_text_cb;
	private TextFlow textpnl;
	private ScrollPane scrollpnl;
	
	@FXML
	private Button btnopentempdir;
	@FXML
	private Button btnopenconfdir;
	
	@FXML
	private BorderPane brdpane;
	
	@FXML
	private Button btnclose;
	@FXML
	private Button btnclean;
	@FXML
	private ChoiceBox<Level> cb_level;
	
	@FXML
	void cleanwindow(ActionEvent event) {
		textpnl.getChildren().clear();
		log_appender_handler.getAll().clear();
	}
	
	@FXML
	void closewindow(ActionEvent event) {
		stage.close();
	}
	
	private class ToTextCb implements TextedEventCallbacks {
		
		@Override
		public void onTextedEvent(TextedEvent t_event) {
			Platform.runLater(() -> {
				Text t = new Text(t_event.getContent());
				t.setFont(font);
				if (t_event.isRed()) {
					t.setFill(Color.RED);
				}
				textpnl.getChildren().add(t);
			});
		}
	}
	
}
