package de.deadlocker8.budgetmasterclient.ui.controller;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.deadlocker8.budgetmaster.logic.FilterSettings;
import de.deadlocker8.budgetmaster.logic.ServerType;
import de.deadlocker8.budgetmaster.logic.Settings;
import de.deadlocker8.budgetmaster.logic.category.CategoryBudget;
import de.deadlocker8.budgetmaster.logic.category.CategoryHandler;
import de.deadlocker8.budgetmaster.logic.payment.NormalPayment;
import de.deadlocker8.budgetmaster.logic.payment.Payment;
import de.deadlocker8.budgetmaster.logic.payment.PaymentHandler;
import de.deadlocker8.budgetmaster.logic.search.SearchPreferences;
import de.deadlocker8.budgetmaster.logic.serverconnection.ExceptionHandler;
import de.deadlocker8.budgetmaster.logic.serverconnection.ServerConnection;
import de.deadlocker8.budgetmaster.logic.tag.TagHandler;
import de.deadlocker8.budgetmaster.logic.updater.Updater;
import de.deadlocker8.budgetmaster.logic.updater.VersionInformation;
import de.deadlocker8.budgetmaster.logic.utils.Colors;
import de.deadlocker8.budgetmaster.logic.utils.Strings;
import de.deadlocker8.budgetmasterclient.ui.ShutdownHandler;
import de.deadlocker8.budgetmasterclient.ui.commandLine.CommandBundle;
import de.deadlocker8.budgetmasterclient.ui.commandLine.CommandLine;
import de.deadlocker8.budgetmasterclient.ui.controller.settings.LocalServerSettingsController;
import de.deadlocker8.budgetmasterclient.ui.controller.settings.SettingsController;
import de.deadlocker8.budgetmasterclient.utils.LoadingModal;
import fontAwesome.FontIcon;
import fontAwesome.FontIconType;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;
import logger.Logger;
import tools.AlertGenerator;
import tools.ConvertTo;
import tools.Localization;
import tools.Worker;

public class Controller extends BaseController
{
	@FXML private AnchorPane anchorPaneMain;
	@FXML private Button buttonDate;
	@FXML private Button buttonLeft;
	@FXML private Button buttonRight;
	@FXML private Button buttonToday;
	@FXML private Button buttonAbout;
	@FXML private TabPane tabPane;
	@FXML private Tab tabHome;
	@FXML private Tab tabPayments;
	@FXML private Tab tabCategories;
	@FXML private Tab tabCharts;
	@FXML private Tab tabReports;
	@FXML private Tab tabSettings;
	@FXML private Label labelNotification;

	private HomeController homeController;
	private PaymentController paymentController;
	private CategoryController categoryController;
	private ChartController chartController;
	private ReportController reportController;
	private SettingsController settingsController;

	private Image icon = new Image("de/deadlocker8/budgetmaster/icon.png");	
	private Settings settings;
	private ShutdownHandler shutdownHandler;
	private DateTime currentDate;
	private ArrayList<CategoryBudget> categoryBudgets;
	private PaymentHandler paymentHandler;
	private CategoryHandler categoryHandler;
	private FilterSettings filterSettings;
	private Updater updater;
	private Payment selectedPayment;
	private SearchPreferences searchPreferences;
	private CommandLine cmd;

	private boolean alertIsShowing = false;
	private static DateTimeFormatter DATE_FORMAT;
	
	public Controller(Settings settings, ShutdownHandler shutdownHandler)
	{
		this.settings = settings;
		this.shutdownHandler = shutdownHandler;
		DATE_FORMAT = DateTimeFormat.forPattern("MMMM yyyy").withLocale(this.settings.getLanguage().getLocale());
		load("/de/deadlocker8/budgetmaster/ui/fxml/GUI.fxml", Localization.getBundle());
		getStage().show();
	}
	
	@Override
	public void initStage(Stage stage)
	{
		stage.setTitle(Localization.getString(Strings.APP_NAME));
		stage.getIcons().add(icon);			
		stage.setResizable(true);
		stage.setWidth(660);
		stage.setHeight(725);
		stage.setMinWidth(660);
		stage.setMinHeight(650);
		stage.getScene().getStylesheets().add("/de/deadlocker8/budgetmaster/ui/style.css");
	}
	
	@Override
	public void init()
	{
		this.shutdownHandler.setController(this);
		
		getStage().setOnCloseRequest((event)->{
			Runtime.getRuntime().removeShutdownHook(shutdownHandler.getShutdownThread());
			shutdownHandler.shutdown();
		});
		
		try
		{
			Runtime.getRuntime().addShutdownHook(shutdownHandler.getShutdownThread());
		}
		catch(IllegalArgumentException e)
		{
		}
		
		if(settings.getServerType() == null)
		{
			settings.setServerType(ServerType.ONLINE);
		}
		Logger.info("Running with ServerType: " + settings.getServerType());
		
		currentDate = DateTime.now();
		buttonDate.setText(currentDate.toString(DATE_FORMAT));
		
		filterSettings = new FilterSettings();
		paymentHandler = new PaymentHandler();
		updater = new Updater();
		
		CommandBundle commandBundle = new CommandBundle(this);
		cmd = new CommandLine(getStage(), icon, ResourceBundle.getBundle("de/deadlocker8/budgetmaster/ui/commandLine/", Locale.ENGLISH), commandBundle);
		
		if(settings.isAutoUpdateCheckEnabled())
		{			
			checkForUpdates(false);
		}
		
		final KeyCombination keyCombinationSearch = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
		
		getStage().getScene().setOnKeyReleased((event)->{			
			if(event.getCode().toString().equals(Localization.getString(Strings.SHORTCUT_DEV_CONSOLE)))
			{
				try
				{
					 cmd.showCommandLine("Dev Console", 400, 250, 400, 200, -1, -1, true);
				}
				catch(IOException e)
				{
			       Logger.error("Error opening dev console");
			       Logger.error(e);
				}
			}
			else if(keyCombinationSearch.match(event))
			{
				if(!tabPayments.isDisabled())
				{
					tabPane.getSelectionModel().select(tabPayments);
					paymentController.search();
				}
			}
		});
				
		initUI();		
	}
	
	private <T> T loadTab(String fileName, Tab tab) throws IOException
	{
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(fileName));
		fxmlLoader.setResources(Localization.getBundle());
		Parent nodeTab = (Parent)fxmlLoader.load();
		tab.setContent(nodeTab);
		return fxmlLoader.getController();
	}
	
	private void initUI()
	{
		try
		{
			homeController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/HomeTab.fxml", tabHome);
			homeController.init(this);			
			
			paymentController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/PaymentTab.fxml", tabPayments);
			paymentController.init(this);
			
			categoryController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/CategoryTab.fxml", tabCategories);
			categoryController.init(this);
			
			chartController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/ChartTab.fxml", tabCharts);
			chartController.init(this);
			tabCharts.selectedProperty().addListener((a,b,c)->{
				if(c)
				{
					chartController.refresh();
				}
			});
			
			reportController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/ReportTab.fxml", tabReports);
			reportController.init(this);
			tabReports.selectedProperty().addListener((a,b,c)->{
				if(c)
				{
					reportController.refresh();
				}
			});
			
			loadSettingsTab();
		}
		catch(IOException e)
		{
			Logger.error(e);
			Platform.runLater(() -> {
				AlertGenerator.showAlert(AlertType.ERROR, Localization.getString(Strings.TITLE_ERROR), "", Localization.getString(Strings.ERROR_CREATE_UI), icon, getStage(), null, false);
			});			
		}
		
		buttonLeft.setGraphic(new FontIcon(FontIconType.CHEVRON_LEFT, 20, Colors.TEXT));
		buttonRight.setGraphic(new FontIcon(FontIconType.CHEVRON_RIGHT, 20, Colors.TEXT));		
		buttonToday.setGraphic(new FontIcon(FontIconType.CALENDAR_ALT, 20, Colors.TEXT));		
		buttonAbout.setGraphic(new FontIcon(FontIconType.INFO, 20, Colors.TEXT));

		// apply theme
		anchorPaneMain.setStyle("-fx-background-color: " + ConvertTo.toRGBHexWithoutOpacity(Colors.BACKGROUND_MAIN));
		labelNotification.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16; -fx-font-weight: bold; -fx-background-color: transparent;");
		
		buttonDate.setStyle("-fx-padding: 0; -fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 24; -fx-text-fill: " + ConvertTo.toRGBHexWithoutOpacity(Colors.TEXT));
		buttonDate.getStyleClass().add("button-hoverable");
		
		buttonLeft.setStyle("-fx-background-color: transparent;");
		buttonLeft.getStyleClass().add("button-hoverable");
		
		buttonRight.setStyle("-fx-background-color: transparent;");
		buttonRight.getStyleClass().add("button-hoverable");
		
		buttonToday.setStyle("-fx-background-color: transparent;");
		buttonToday.getStyleClass().add("button-hoverable");
		
		buttonAbout.setStyle("-fx-background-color: transparent;");
		buttonAbout.getStyleClass().add("button-hoverable");
	}
	
	public void loadSettingsTab()
	{
		try 
		{
			if(settings.getServerType().equals(ServerType.ONLINE))
			{
				settingsController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/SettingsTabOnlineServer.fxml", tabSettings);
				settingsController.init(this);
			}
			else
			{
				settingsController = loadTab("/de/deadlocker8/budgetmaster/ui/fxml/SettingsTabLocalServer.fxml", tabSettings);
				settingsController.init(this);
			}
			
		}
		catch(IOException e)
		{
			Logger.error(e);
			Platform.runLater(() -> {
				AlertGenerator.showAlert(AlertType.ERROR, Localization.getString(Strings.TITLE_ERROR), "", Localization.getString(Strings.ERROR_CREATE_UI), icon, getStage(), null, false);
			});			
		}
	}
	
	public boolean checkSettings()
	{
		if(!settings.isComplete())
		{			
			Platform.runLater(() -> {
				toggleAllTabsExceptSettings(true);
				tabPane.getSelectionModel().select(tabSettings);
				AlertGenerator.showAlert(AlertType.INFORMATION, Localization.getString(Strings.TITLE_INFO), "", Localization.getString(Strings.INFO_FIRST_START), icon, getStage(), null, false);
			});
			return false;
		}
		return true;
	}

	public Image getIcon()
	{
		return icon;
	}

	public Settings getSettings()
	{
		return settings;
	}

	public void setSettings(Settings settings)
	{
		this.settings = settings;
	}
	
	public ShutdownHandler getShutdownHandler()
	{
		return shutdownHandler;
	}

	public void showNotification(String text)
	{
		labelNotification.setText(text);
		labelNotification.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16; -fx-font-weight: bold; -fx-background-color: " + ConvertTo.toRGBHexWithoutOpacity(Colors.BACKGROUND_NOTIFICATION));
		FadeTransition fadeIn = new FadeTransition(Duration.millis(200), labelNotification);
		fadeIn.setFromValue(0.0);
		fadeIn.setToValue(1.0);

		FadeTransition fadeOut = new FadeTransition(Duration.millis(400), labelNotification);
		fadeOut.setFromValue(1.0);
		fadeOut.setToValue(0.0);
		fadeOut.setDelay(Duration.millis(3000));
		fadeOut.play();

		SequentialTransition seqT = new SequentialTransition(fadeIn, fadeOut);
		seqT.play();
		seqT.setOnFinished((a) -> {
			labelNotification.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16; -fx-font-weight: bold; -fx-background-color: transparent;");
		});
	}

	public void previousMonth()
	{
		currentDate = currentDate.minusMonths(1);
		buttonDate.setText(currentDate.toString(DATE_FORMAT));

		refresh(filterSettings);
	}

	public void nextMonth()
	{
		currentDate = currentDate.plusMonths(1);
		buttonDate.setText(currentDate.toString(DATE_FORMAT));

		refresh(filterSettings);
	}
	
	public void today()
	{
		currentDate = DateTime.now();
		buttonDate.setText(currentDate.toString(DATE_FORMAT));

		refresh(filterSettings);
	}
	
	public void setDate(DateTime newDate)
	{
		currentDate = newDate;
		buttonDate.setText(currentDate.toString(DATE_FORMAT));

		refresh(filterSettings);
	}

	public DateTime getCurrentDate()
	{
		return currentDate;
	}
	
	public void openDatePicker()
	{
		new DatePickerController(getStage(), this, currentDate);
	}
	
	public void forceSettingsTab()
	{
		toggleAllTabsExceptSettings(true);
		tabPane.getSelectionModel().select(tabSettings);
	}

	public void showConnectionErrorAlert(String errorMessage)
	{		
		if(!alertIsShowing)
		{
			alertIsShowing = true;
			Platform.runLater(() -> {
				toggleAllTabsExceptSettings(true);
				tabPane.getSelectionModel().select(tabSettings);
				
				alertIsShowing = true;
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle(Localization.getString(Strings.TITLE_ERROR));
				alert.setHeaderText("");
				if(errorMessage == null)
				{
					alert.setContentText(Localization.getString(Strings.ERROR_SERVER_CONNECTION));
				}
				else
				{
					alert.setContentText(Localization.getString(Strings.ERROR_SERVER_CONNECTION_WITH_DETAILS, errorMessage));
				}
				
				Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
				dialogStage.getIcons().add(icon);
				dialogStage.initOwner(getStage());
				alert.showAndWait();
				alertIsShowing = false;
			});
		}
	}

	public void refreshAllTabs()
	{
		homeController.refresh();
		paymentController.refresh();
		categoryController.refresh();		
		if(tabCharts.isSelected())
		{
			chartController.refresh();
		}
		if(tabReports.isSelected())
		{
			reportController.refresh();
		}
	}
	
	public ArrayList<CategoryBudget> getCategoryBudgets()
	{
		return categoryBudgets;
	}

	public PaymentHandler getPaymentHandler()
	{
		return paymentHandler;
	}

	public CategoryHandler getCategoryHandler()
	{
		return categoryHandler;
	}
	
	public FilterSettings getFilterSettings()
	{
		return filterSettings;
	}
	
	public Updater getUpdater()
	{
		return updater;
	}
	
	public SettingsController getSettingsController()
	{
		return settingsController;
	}

	public void setFilterSettings(FilterSettings filterSettings)
	{
		this.filterSettings = filterSettings;
	}
	
	public void toggleAllTabsExceptSettings(boolean disable)
	{
		tabHome.setDisable(disable);
		tabPayments.setDisable(disable);
		tabCategories.setDisable(disable);
		tabCharts.setDisable(disable);
		tabReports.setDisable(disable);
		buttonLeft.setDisable(disable);
		buttonRight.setDisable(disable);
		buttonToday.setDisable(disable);
		buttonDate.setDisable(disable);
	}
	
	public void setSelectedPayment(Payment payment)
	{
		selectedPayment = payment;
	}
	
	public Payment getSelectedPayment()
	{
		return selectedPayment;
	}	

	public SearchPreferences getSearchPreferences()
	{
		return searchPreferences;
	}

	public void setSearchPreferences(SearchPreferences searchPreferences)
	{
		this.searchPreferences = searchPreferences;
	}

	public PaymentController getPaymentController()
	{
		return paymentController;
	}

	public void checkForUpdates(boolean showNotification)
	{
		try
		{
			boolean updateAvailable = updater.isUpdateAvailable(Integer.parseInt(Localization.getString(Strings.VERSION_CODE)));			

			if(!updateAvailable)
			{
				if(showNotification)
				{
					showNotification(Localization.getString(Strings.NOTIFICATION_NO_UPDATE_AVAILABLE));
				}
				return;
			}
			
			Platform.runLater(()->{
				showUpdateAlert();
			});
		}		
		catch(Exception e)
		{
			Logger.error(e);
			AlertGenerator.showAlert(AlertType.ERROR, 
									Localization.getString(Strings.TITLE_ERROR),
									"", 
									Localization.getString(Strings.ERROR_UPDATER_GET_LATEST_VERSION), 
									icon, null, null, true);
		}
	}
	
	private void showUpdateAlert()
	{
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(Localization.getString(Strings.INFO_TITLE_UPDATE_AVAILABLE));
		alert.setHeaderText("");
		Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
		dialogStage.getIcons().add(icon);
		
		Hyperlink linkText =  new Hyperlink(Localization.getString(Strings.INFO_TEXT_UPDATE_AVAILABLE_SHOW_CHANGES));
		linkText.setOnAction((event)->{
			if(Desktop.isDesktopSupported())
			{
				try
				{
					Desktop.getDesktop().browse(new URI(Localization.getString(Strings.ROADMAP_URL)));
				}
				catch(IOException | URISyntaxException e)
				{
					Logger.error(e);
					AlertGenerator.showAlert(AlertType.ERROR, 
											Localization.getString(Strings.TITLE_ERROR),
											"",
											Localization.getString(Strings.ERROR_OPEN_BROWSER), 
											icon, getStage(), null, false);
				}
			}
		});
		
		Hyperlink detailedMilestones =  new Hyperlink(Localization.getString(Strings.INFO_TEXT_UPDATE_AVAILABLE_SHOW_CHANGES_DETAILED));
		detailedMilestones.setOnAction((event)->{
			if(Desktop.isDesktopSupported())
			{
				try
				{
					Desktop.getDesktop().browse(new URI(Localization.getString(Strings.GITHUB_URL)));
				}
				catch(IOException | URISyntaxException e)
				{
					Logger.error(e);
					AlertGenerator.showAlert(AlertType.ERROR, 
											Localization.getString(Strings.TITLE_ERROR),
											"",
											Localization.getString(Strings.ERROR_OPEN_BROWSER), 
											icon, getStage(), null, false);
				}
			}
		});
		
		TextFlow textFlow = new TextFlow(
		    new Text(Localization.getString(Strings.INFO_TEXT_UPDATE_AVAILABLE,
											updater.getLatestVersion().getVersionName())),
		    linkText,
		    new Text("\n\n"),
		    detailedMilestones
		);
		
		alert.getDialogPane().setContent(textFlow);
		
		ButtonType buttonTypeOne = new ButtonType(Localization.getString(Strings.INFO_TEXT_UPDATE_AVAILABLE_NOW));
		ButtonType buttonTypeTwo = new ButtonType(Localization.getString(Strings.CANCEL));							
		alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo);
		
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getButtonTypes().stream().map(dialogPane::lookupButton).forEach(button -> button.addEventHandler(KeyEvent.KEY_PRESSED, (event) -> {
			if(KeyCode.ENTER.equals(event.getCode()) && event.getTarget() instanceof Button)
			{
				((Button)event.getTarget()).fire();
			}
		}));
		
		Optional<ButtonType> result = alert.showAndWait();						
		if (result.get() == buttonTypeOne)
		{					
			LoadingModal.showModal(this, Localization.getString(Strings.TITLE_MODAL), Localization.getString(Strings.LOAD_UPDATE), getStage(), icon);
			
			Worker.runLater(() -> {
				try 
				{
					updater.downloadLatestVersion();
					Platform.runLater(() -> {
						LoadingModal.closeModal();						
					});
				}
				catch(Exception ex)
				{
					Logger.error(ex);
					Platform.runLater(() -> {
						LoadingModal.closeModal();
						AlertGenerator.showAlert(AlertType.ERROR, 
												Localization.getString(Strings.TITLE_ERROR),
												"", 
												Localization.getString(Strings.ERROR_UPDATER_DOWNLOAD_LATEST_VERSION, ex.getMessage()), 
												icon, getStage(), null, true);													
					});
				}
			});
		}
		else
		{
			alert.close();
		}
	}
	
	private Label getLabelForAboutColumns(String text, boolean bold)
	{
		Label label = new Label(text);
		if(bold)
		{
			label.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
		}
		else
		{			
			label.setStyle("-fx-font-size: 14;");
		}
		return label;
	}

	public void about()
	{
		Alert alert = new Alert(AlertType.NONE);
		alert.setTitle(Localization.getString(Strings.ABOUT, Localization.getString(Strings.APP_NAME)));
		alert.setHeaderText("");
		Stage dialogStage = (Stage)alert.getDialogPane().getScene().getWindow();
		dialogStage.getIcons().add(icon);
		
		Hyperlink roadmapLink =  new Hyperlink(Localization.getString(Strings.ABOUT_ROADMAP_LINK));
		roadmapLink.setFont(new Font(14));
		roadmapLink.setPadding(new Insets(0));
		roadmapLink.setOnAction((event)->{
			if(Desktop.isDesktopSupported())
			{
				try
				{
					Desktop.getDesktop().browse(new URI(Localization.getString(Strings.ROADMAP_URL)));
				}
				catch(IOException | URISyntaxException e)
				{
					Logger.error(e);
					AlertGenerator.showAlert(AlertType.ERROR, 
											Localization.getString(Strings.TITLE_ERROR),
											"",
											Localization.getString(Strings.ERROR_OPEN_BROWSER), 
											icon, getStage(), null, false);
				}
			}
		});
		
		Hyperlink githubLink =  new Hyperlink(Localization.getString(Strings.GITHUB_URL));
		githubLink.setFont(new Font(14));
		githubLink.setPadding(new Insets(0));
		githubLink.setOnAction((event)->{
			if(Desktop.isDesktopSupported())
			{
				try
				{
					Desktop.getDesktop().browse(new URI(Localization.getString(Strings.GITHUB_URL)));
				}
				catch(IOException | URISyntaxException e)
				{
					Logger.error(e);
					AlertGenerator.showAlert(AlertType.ERROR, 
											Localization.getString(Strings.TITLE_ERROR),
											"",
											Localization.getString(Strings.ERROR_OPEN_BROWSER), 
											icon, getStage(), null, false);
				}
			}
		});
		
		VBox vbox = new VBox();
		vbox.setSpacing(10);
		HBox hboxLogo = new HBox();
		hboxLogo.setSpacing(25);
		ImageView imageViewLogo = new ImageView(icon);
		imageViewLogo.setFitHeight(75);
		imageViewLogo.setFitWidth(75);
		hboxLogo.getChildren().add(imageViewLogo);
		
		Label labelName = new Label(Localization.getString(Strings.ABOUT, Localization.getString(Strings.APP_NAME)));
		labelName.setStyle("-fx-font-weight: bold; -fx-font-size: 22;");
		labelName.setMaxWidth(Double.MAX_VALUE);
		hboxLogo.getChildren().add(labelName);
		HBox.setHgrow(labelName, Priority.ALWAYS);
		hboxLogo.setAlignment(Pos.CENTER);
		vbox.getChildren().add(hboxLogo);
		
		HBox hboxColumns = new HBox();
		VBox vboxLeft = new VBox();
		vboxLeft.setSpacing(7);
		VBox vboxRight = new VBox();
		vboxRight.setSpacing(7);
		
		vboxLeft.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.ABOUT_VERSION), true));
		vboxLeft.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.ABOUT_DATE), true));
		vboxLeft.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.ABOUT_AUTHOR), true));
		vboxLeft.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.ABOUT_ROADMAP), true));
		Label labelSourceCode = getLabelForAboutColumns(Localization.getString(Strings.ABOUT_SOURCECODE), true);
		vboxLeft.getChildren().add(labelSourceCode);
		VBox.setMargin(labelSourceCode, new Insets(2, 0, 0, 0));
		vboxLeft.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.ABOUT_CREDITS), true));		
		
		vboxRight.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.VERSION_NAME) + " (" + Localization.getString(Strings.VERSION_CODE) + ")", false));
		vboxRight.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.VERSION_DATE), false));
		vboxRight.getChildren().add(getLabelForAboutColumns(Localization.getString(Strings.AUTHOR), false));
		vboxRight.getChildren().add(roadmapLink);
		vboxRight.getChildren().add(githubLink);
		
		VBox vboxCredits = new VBox();
		for(String line : Localization.getString(Strings.CREDITS).split("\n"))
		{
			vboxCredits.getChildren().add(getLabelForAboutColumns(line, false));
		}
		vboxRight.getChildren().add(vboxCredits);
		
		vboxLeft.setMinWidth(100);
		hboxColumns.getChildren().addAll(vboxLeft, vboxRight);
		HBox.setHgrow(vboxLeft, Priority.ALWAYS);
		HBox.setHgrow(vboxRight, Priority.ALWAYS);
		
		vbox.getChildren().add(hboxColumns);		
		alert.getDialogPane().setContent(vbox);		
		
		alert.getButtonTypes().setAll(new ButtonType(Localization.getString(Strings.OK)));
		
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getButtonTypes().stream().map(dialogPane::lookupButton).forEach(button -> button.addEventHandler(KeyEvent.KEY_PRESSED, (event) -> {
			if(KeyCode.ENTER.equals(event.getCode()) && event.getTarget() instanceof Button)
			{
				((Button)event.getTarget()).fire();
			}
		}));
		
		alert.showAndWait();						
	}	
	
	public void refresh(FilterSettings newFilterSettings)
	{
		Logger.debug("Starting main refresh...");
		Platform.runLater(()->{
			LoadingModal.showModal(this, Localization.getString(Strings.TITLE_MODAL), Localization.getString(Strings.LOAD_DATA), getStage(), icon);
		});

		Worker.runLater(() -> {
			try
			{
				ServerConnection connection = new ServerConnection(settings);
				
				//check if server is compatible with client
				try
				{
					Logger.debug("Checking server compatibility...");
					VersionInformation serverVersion = connection.getServerVersion();
					if(serverVersion.getVersionCode() < Integer.parseInt(Localization.getString(Strings.VERSION_CODE)))
					{
						Logger.debug("Server (versionCode: " + serverVersion.getVersionCode() + ") is incompatible with client (versionCode: " + Localization.getString(Strings.VERSION_CODE) + ")");
						if(settings.getServerType().equals(ServerType.ONLINE))
						{					
							Platform.runLater(()->{
								AlertGenerator.showAlert(AlertType.WARNING,
														Localization.getString(Strings.TITLE_WARNING), 
														"",
														Localization.getString(Strings.WARNING_SERVER_VERSION, serverVersion.getVersionName(), Localization.getString(Strings.VERSION_NAME)), 
														icon, getStage(), null, false);				
							
								LoadingModal.closeModal();
								categoryHandler = new CategoryHandler(null);			
								toggleAllTabsExceptSettings(true);
								tabPane.getSelectionModel().select(tabSettings);	
							});
						}
						else
						{
							Platform.runLater(()->{
								LoadingModal.closeModal();
								categoryHandler = new CategoryHandler(null);			
								toggleAllTabsExceptSettings(true);
								tabPane.getSelectionModel().select(tabSettings);
								((LocalServerSettingsController)settingsController).handleIncompatibleServer();
							});
						}
						return;
					}
					else
					{
						Logger.debug("Found compatible server (versionCode: " + serverVersion.getVersionCode() + ")");
					}
				}
				catch(Exception e1)
				{
					Logger.error(e1);
					Platform.runLater(()->{
						LoadingModal.closeModal();
					});
					
					if(e1.getMessage().contains("404"))
					{
						Logger.debug("Server version is incompatible with current client version (" + Localization.getString(Strings.VERSION_CODE) + ")");
						//old server
						Platform.runLater(()->{
							AlertGenerator.showAlert(AlertType.WARNING,
							Localization.getString(Strings.TITLE_WARNING), 
							"",
							Localization.getString(Strings.WARNING_SERVER_VERSION, Localization.getString(Strings.UNDEFINED), Localization.getString(Strings.VERSION_NAME)), 
							icon, getStage(), null, false);			
							
							categoryHandler = new CategoryHandler(null);					
							toggleAllTabsExceptSettings(true);
							tabPane.getSelectionModel().select(tabSettings);
						});
					}
					else
					{
						//normal connection error (e.g. server not running)
						showConnectionErrorAlert(ExceptionHandler.getMessageForException(e1));
					}
					return;
				}
				
				Logger.debug("Connected");
				
				paymentHandler = new PaymentHandler();
				paymentHandler.getPayments().addAll(connection.getPayments(currentDate.getYear(), currentDate.getMonthOfYear()));
				paymentHandler.getPayments().addAll(connection.getRepeatingPayments(currentDate.getYear(), currentDate.getMonthOfYear()));			
				paymentHandler.sort();
				if(settings.isRestActivated())
				{
					int rest = connection.getRestForAllPreviousMonths(currentDate.getYear(), currentDate.getMonthOfYear());
					//categoryID 2 = Rest
					paymentHandler.getPayments().add(new NormalPayment(-1, rest, currentDate.withDayOfMonth(1).toString("yyyy-MM-dd"), 2, Localization.getString(Strings.CATEGORY_REST), ""));				
				}
				
				categoryHandler = new CategoryHandler(connection.getCategories());
				
				categoryBudgets = connection.getCategoryBudgets(currentDate.getYear(), currentDate.getMonthOfYear());	
				paymentHandler.filter(newFilterSettings, new TagHandler(settings));
				Logger.debug("Main refresh done");

				Platform.runLater(() -> {
					LoadingModal.closeModal();
					toggleAllTabsExceptSettings(false);
					refreshAllTabs();
				});
			}
			catch(Exception e)
			{
				Logger.error(e);
				Platform.runLater(() -> {
					LoadingModal.closeModal();
					categoryHandler = new CategoryHandler(null);	
					showConnectionErrorAlert(ExceptionHandler.getMessageForException(e));
					refreshAllTabs();
				});
			}
		});	
	}
}