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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

public class AppAlert extends Alert {
	
	public AppAlert(String message, EventHandler<ActionEvent> on_close) {
		super(AlertType.ERROR);
		initModality(Modality.APPLICATION_MODAL);
		// Icon
		/*ImageView icon = new ImageView(stage.getIcons().get(0));
		icon.maxHeight(80);
		icon.setFitHeight(80);
		icon.setPreserveRatio(true);
		icon.setCache(true);
		getDialogPane().setGraphic(icon);*/
		// Content
		setTitle(Messages.getString("AppAlert.title")); //$NON-NLS-1$
		setHeaderText(Messages.getString("AppAlert.header")); //$NON-NLS-1$
		setContentText(message);
		
		Button ok_button = (Button) getDialogPane().lookupButton(ButtonType.OK);
		ok_button.setOnAction(e -> {
			close();
			on_close.handle(e);
		});
		setOnCloseRequest(e -> {
			e.consume();
		});
	}
	
}
