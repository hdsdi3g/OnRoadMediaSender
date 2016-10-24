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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import hd3gtv.onroadmediasend.engine.Destination;
import hd3gtv.onroadmediasend.engine.FileToSend;
import hd3gtv.onroadmediasend.engine.Format;
import hd3gtv.onroadmediasend.engine.OperationState;
import hd3gtv.onroadmediasend.engine.Progression;
import hd3gtv.onroadmediasend.engine.Quality;
import hd3gtv.onroadmediasend.engine.SizeDuration;
import hd3gtv.onroadmediasend.ffmpeg.FFmpeg;
import hd3gtv.onroadmediasend.logpanel.LWControler;
import hd3gtv.onroadmediasend.logpanel.LogAppenderHandler;
import hd3gtv.tools.FileValidation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainControler {
	
	private final static Logger log = Logger.getLogger(MainControler.class);
	
	private Stage stage;
	private LogAppenderHandler log_appender_handler;
	
	private Queue queue;
	private ObservableList<FileToSend> list_rows_files_to_send;
	private ObservableList<Quality> list_qualities;
	private ObservableList<Destination> list_destinations;
	
	private MainConfiguration configuration;
	private Quality selected_quality;
	private Destination selected_destination;
	private File temp_directory;
	private RollingFileAppender appender_to_file;
	
	void startApp(Stage stage) throws IOException, ConfigurationException {
		this.stage = stage;
		if (stage == null) {
			throw new NullPointerException("\"stage\" can't to be null"); //$NON-NLS-1$
		}
		
		list_rows_files_to_send = FXCollections.observableArrayList();
		list_qualities = FXCollections.observableArrayList();
		list_destinations = FXCollections.observableArrayList();
		
		configuration = new MainConfiguration();
		
		log_appender_handler = new LogAppenderHandler(configuration);
		
		prepareLogToSessionFile();
		Logger.getRootLogger().addAppender(appender_to_file);
		
		stage.setTitle(configuration.get().getString("vendor.title", "OnRoadMediaSender")); //$NON-NLS-1$ //$NON-NLS-2$
		
		temp_directory = new File(configuration.get().getString("tempdir", System.getProperty("java.io.tmpdir"))); //$NON-NLS-1$ //$NON-NLS-2$
		if (temp_directory.exists() == false) {
			if (temp_directory.mkdirs() == false) {
				temp_directory = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			}
		}
		
		List<HierarchicalConfiguration<ImmutableNode>> all_qualities = configuration.get().configurationsAt("quality"); //$NON-NLS-1$
		if (all_qualities.isEmpty() == false) {
			list_qualities.addAll(Quality.getConfiguredQualities(all_qualities));
		} else {
			new AppAlert(Messages.getString("MainControler.noqual_inconf"), e -> { //$NON-NLS-1$
				System.exit(1);
			}).showAndWait();
		}
		
		List<HierarchicalConfiguration<ImmutableNode>> all_destinations = configuration.get().configurationsAt("destination"); //$NON-NLS-1$
		if (all_destinations.isEmpty() == false) {
			list_destinations.addAll(Destination.getConfiguredDestinations(all_destinations));
		} else {
			new AppAlert(Messages.getString("MainControler.nodest_inconf"), e -> { //$NON-NLS-1$
				System.exit(1);
			}).showAndWait();
		}
		
		prepareControls();
		
		File external_logo = new File(configuration.get().getString("vendor.logo")); //$NON-NLS-1$
		if (external_logo != null) {
			if (external_logo.exists() == false) {
				external_logo = new File(configuration.getDirectoryConf().getAbsolutePath() + File.separator + external_logo.getName());
			}
			if (external_logo.exists() && external_logo.canRead() && external_logo.isFile()) {
				log.debug("Load logo file: " + external_logo.getPath()); //$NON-NLS-1$
				Image logo = new Image(external_logo.toURI().toString());
				if (logo.getHeight() > image_logo.getFitHeight() || logo.getWidth() > image_logo.getFitWidth()) {
					File new_converted_logo = new File(external_logo.getParentFile().getAbsolutePath() + File.separator + FilenameUtils.getBaseName(external_logo.getName()) + "-converted.png"); //$NON-NLS-1$
					try {
						FFmpeg.convertImage(new File(configuration.get().getString("ffmpeg", "ffmpeg")), external_logo, new_converted_logo, (int) Math.round(image_logo.getFitWidth()), //$NON-NLS-1$ //$NON-NLS-2$
								(int) Math.round(image_logo.getFitHeight()));
						logo = new Image(new_converted_logo.toURI().toString());
						configuration.get().setProperty("vendor.logo", new_converted_logo.getAbsolutePath()); //$NON-NLS-1$
					} catch (Exception e) {
						log.error("Can't convert logo", e); //$NON-NLS-1$
					}
				}
				
				image_logo.setImage(logo);
			} else {
				log.warn("Can't found logo file: " + external_logo.getPath()); //$NON-NLS-1$
			}
		}
		
		queue = new Queue(this);
	}
	
	void disconnectFileLogger() {
		Logger.getRootLogger().removeAppender(appender_to_file);
	}
	
	private void prepareLogToSessionFile() throws IOException {
		/**
		 * Windows's user conf files
		 */
		local_user_dir = new File(System.getenv().getOrDefault("APPDATA", "")); //$NON-NLS-1$ //$NON-NLS-2$
		if (local_user_dir.getName().equals("")) { //$NON-NLS-1$
			/**
			 * OSX's user conf files
			 */
			local_user_dir = new File(System.getenv().getOrDefault("HOME", "") + "/Library/Application Support"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (local_user_dir.getName().startsWith("/Library/Application Support")) { //$NON-NLS-1$
				/**
				 * Other
				 */
				local_user_dir = new File(System.getProperty("user.home"));//$NON-NLS-1$
			}
		}
		FileValidation.checkExistsCanRead(local_user_dir);
		FileValidation.checkIsDirectory(local_user_dir);
		FileValidation.checkIsWritable(local_user_dir);
		
		local_user_dir = new File(local_user_dir.getAbsolutePath() + File.separator + "OnRoadMediaSend"); //$NON-NLS-1$
		FileUtils.forceMkdir(local_user_dir);
		
		appender_to_file = new RollingFileAppender();
		appender_to_file.setLayout(new PatternLayout(configuration.get().getString("messagepatternlayout.file", "%m%n"))); //$NON-NLS-1$ //$NON-NLS-2$
		appender_to_file.setEncoding("UTF-8"); //$NON-NLS-1$
		appender_to_file.setFile(local_user_dir.getAbsolutePath() + File.separator + "application.log"); //$NON-NLS-1$
		appender_to_file.setMaxBackupIndex(10);
		appender_to_file.setMaximumFileSize(1000000);
		appender_to_file.setName("Log to user's local settings file"); //$NON-NLS-1$
		appender_to_file.activateOptions();
	}
	
	private File local_user_dir;
	
	public Progression createTotalProgressConvertion() {
		return new Progression(0l, 0, progress_conversion.progressProperty(), label_possize_conversion.textProperty(), label_eta_conversion.textProperty(), label_percent_conversion.textProperty());
	}
	
	public Progression createTotalProgressUpload() {
		return new Progression(0l, 0, progress_upload.progressProperty(), label_possize_upload.textProperty(), label_eta_upload.textProperty(), label_percent_upload.textProperty());
	}
	
	public void addFileToSend(FileToSend file) {
		list_rows_files_to_send.add(file);
	}
	
	public void removeFileToSend(FileToSend file) {
		list_rows_files_to_send.remove(file);
	}
	
	public Queue getQueue() {
		return queue;
	}
	
	public File getTempDirectory() {
		return temp_directory;
	}
	
	public MainConfiguration getConfiguration() {
		return configuration;
	}
	
	public Destination getSelectedDestination() {
		return selected_destination;
	}
	
	public Quality getSelectedQuality() {
		return selected_quality;
	}
	
	public void showFileChooser() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(Messages.getString("MainControler.selectfiles")); //$NON-NLS-1$
		addFiles(fileChooser.showOpenMultipleDialog(stage));
	}
	
	public void addFiles(List<File> selected_files) {
		if (selected_files == null) {
			return;
		}
		if (selected_files.isEmpty()) {
			return;
		}
		HashSet<String> actual_paths = new HashSet<>(list_rows_files_to_send.size() + 1);
		
		list_rows_files_to_send.forEach(fts -> {
			actual_paths.add(fts.getSource().get().getAbsolutePath());
		});
		
		selected_files.forEach(f -> {
			if (actual_paths.contains(f.getAbsolutePath())) {
				return;
			}
			if (f.isDirectory()) {
				return;
			}
			if (f.canRead() == false) {
				return;
			}
			
			try {
				new FileToSend(f.getCanonicalFile(), this).publish();
			} catch (Exception e) {
				log.error("Can't add file to list", e); //$NON-NLS-1$
			}
		});
	}
	
	@FXML
	private ResourceBundle resources;
	@FXML
	private URL location;
	@FXML
	private TableView<FileToSend> table_file_to_send;
	@FXML
	private TableColumn<FileToSend, File> col_file_name;
	@FXML
	private TableColumn<FileToSend, Format> col_format;
	@FXML
	private TableColumn<FileToSend, String> col_convert;
	@FXML
	private TableColumn<FileToSend, String> col_upload;
	@FXML
	private TableColumn<FileToSend, SizeDuration> col_sizedur;
	@FXML
	private TableColumn<FileToSend, OperationState> col_state;
	@FXML
	private Button btn_add_file;
	@FXML
	private Button btn_remove_file;
	@FXML
	private ComboBox<Quality> cmb_quality;
	@FXML
	private ComboBox<Destination> cmb_destination;
	@FXML
	private ProgressBar progress_conversion;
	@FXML
	private ProgressBar progress_upload;
	@FXML
	private Label label_possize_conversion;
	@FXML
	private Label label_possize_upload;
	@FXML
	private Label label_eta_conversion;
	@FXML
	private Label label_eta_upload;
	@FXML
	private Label label_percent_conversion;
	@FXML
	private Label label_percent_upload;
	@FXML
	private ImageView image_logo;
	@FXML
	private Button btn_showlogs;
	@FXML
	private Hyperlink url_copyr;
	
	private void prepareControls() {
		/**
		 * Init Cell Value Factories mapping
		 */
		col_file_name.setCellValueFactory(cellData -> cellData.getValue().getSource());
		col_format.setCellValueFactory(cellData -> cellData.getValue().getFormat());
		col_convert.setCellValueFactory(cellData -> cellData.getValue().getConvertProgress());
		col_upload.setCellValueFactory(cellData -> cellData.getValue().getUploadProgress());
		col_sizedur.setCellValueFactory(cellData -> cellData.getValue().getSizeDuration());
		col_state.setCellValueFactory(cellData -> cellData.getValue().getState());
		
		/**
		 * Init specific Cell Factories
		 */
		col_file_name.setCellFactory(column -> {
			return new TableCell<FileToSend, File>() {
				protected void updateItem(File item, boolean empty) {
					super.updateItem(item, empty);
					
					if (item == null || empty) {
						setText(null);
						return;
					}
					
					setText(item.getName() + " • " + item.getParent()); //$NON-NLS-1$
				}
			};
		});
		
		/**
		 * Init file table
		 */
		table_file_to_send.setItems(list_rows_files_to_send);
		
		/**
		 * Table placeholder
		 */
		VBox placeholder = new VBox(50);
		placeholder.setAlignment(Pos.CENTER);
		Text welcomeLabel = new Text(Messages.getString("MainControler.placeholder")); //$NON-NLS-1$
		welcomeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 32;"); //$NON-NLS-1$
		welcomeLabel.setStrokeType(StrokeType.INSIDE);
		welcomeLabel.setTextAlignment(TextAlignment.CENTER);
		welcomeLabel.setFill(Color.web("#ccc")); //$NON-NLS-1$
		placeholder.getChildren().add(welcomeLabel);
		table_file_to_send.setPlaceholder(placeholder);
		
		/**
		 * Table selection behavior
		 */
		TableViewSelectionModel<FileToSend> select_model = table_file_to_send.getSelectionModel();
		select_model.setSelectionMode(SelectionMode.MULTIPLE);
		select_model.selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			btn_remove_file.setDisable(newSelection == null);
		});
		
		/**
		 * Drag&drop table
		 */
		table_file_to_send.setOnDragOver(e -> {
			e.acceptTransferModes(TransferMode.LINK);
			e.consume();
		});
		table_file_to_send.setOnDragDropped(e -> {
			List<File> dragged_files = e.getDragboard().getFiles();
			if (dragged_files == null) {
				e.consume();
				return;
			}
			addFiles(dragged_files);
			e.consume();
		});
		
		/**
		 * Buttons
		 */
		btn_add_file.setOnAction(event -> {
			showFileChooser();
			event.consume();
		});
		
		btn_remove_file.setOnAction(event -> {
			ArrayList<FileToSend> to_remove = new ArrayList<>();
			select_model.getSelectedItems().forEach(selected -> {
				if (selected != null) {
					to_remove.add(selected);
				}
			});
			to_remove.forEach(selected -> {
				selected.removed();
			});
			
			event.consume();
		});
		
		/**
		 * Plug qualities
		 */
		cmb_quality.setItems(list_qualities);
		cmb_quality.setOnAction(event -> {
			if (cmb_quality.getSelectionModel().isEmpty() == false) {
				selected_quality = cmb_quality.getSelectionModel().getSelectedItem();
				log.info("User change selected quality: " + selected_quality); //$NON-NLS-1$
			}
		});
		if (list_qualities.isEmpty() == false) {
			cmb_quality.getSelectionModel().selectFirst();
			selected_quality = cmb_quality.getSelectionModel().getSelectedItem();
		}
		
		/**
		 * Plug destinations
		 */
		cmb_destination.setItems(list_destinations);
		cmb_destination.setOnAction(event -> {
			if (cmb_destination.getSelectionModel().isEmpty() == false) {
				selected_destination = cmb_destination.getSelectionModel().getSelectedItem();
				log.info("User change selected destination: " + selected_destination); //$NON-NLS-1$
			}
		});
		if (list_destinations.isEmpty() == false) {
			cmb_destination.getSelectionModel().selectFirst();
			selected_destination = cmb_destination.getSelectionModel().getSelectedItem();
		}
		
		/**
		 * Reset progress
		 */
		progress_conversion.setProgress(0);
		progress_upload.setProgress(0);
		label_possize_conversion.setText(""); //$NON-NLS-1$
		label_possize_upload.setText(""); //$NON-NLS-1$
		label_eta_conversion.setText(""); //$NON-NLS-1$
		label_eta_upload.setText(""); //$NON-NLS-1$
		label_percent_conversion.setText(""); //$NON-NLS-1$
		label_percent_upload.setText(""); //$NON-NLS-1$
		
		btn_showlogs.setOnAction(event -> {
			try {
				Stage lw_stage = new Stage();
				
				FXMLLoader d = new FXMLLoader();
				d.setResources(ResourceBundle.getBundle(Main.class.getPackage().getName() + ".messages")); //$NON-NLS-1$
				BorderPane root = (BorderPane) d.load(LWControler.class.getResource("LogPanel.fxml").openStream());//$NON-NLS-1$
				
				LWControler log_panel_controler = d.getController();
				log_panel_controler.startApp(lw_stage, btn_showlogs, configuration, log_appender_handler, this);
				
				lw_stage.setScene(new Scene(root));
				lw_stage.getIcons().addAll(stage.getIcons());
				lw_stage.show();
				
				log.info("Open log panel"); //$NON-NLS-1$
			} catch (IOException e) {
				log.error("Can't open Log panel", e); //$NON-NLS-1$
			}
			
		});
	}
	
	@FXML
	void onurlcopyrclick(ActionEvent event) {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(new URI("https://github.com/hdsdi3g/OnRoadMediaSender")); //$NON-NLS-1$
			} catch (Exception e) {
			}
		}
	}
	
	public void openApplicationConfDirectory() {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.open(this.local_user_dir);
			} catch (Exception e) {
			}
		}
	}
	
	public void openTempDirectory() {
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.open(this.temp_directory);
			} catch (Exception e) {
			}
		}
	}
	
}
