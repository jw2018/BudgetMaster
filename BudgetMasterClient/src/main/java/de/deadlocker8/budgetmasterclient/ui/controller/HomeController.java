package de.deadlocker8.budgetmasterclient.ui.controller;

import java.util.ArrayList;

import de.deadlocker8.budgetmaster.logic.Budget;
import de.deadlocker8.budgetmaster.logic.category.CategoryBudget;
import de.deadlocker8.budgetmaster.logic.utils.Colors;
import de.deadlocker8.budgetmaster.logic.utils.Helpers;
import de.deadlocker8.budgetmaster.logic.utils.Strings;
import de.deadlocker8.budgetmasterclient.ui.Refreshable;
import de.deadlocker8.budgetmasterclient.ui.Styleable;
import de.deadlocker8.budgetmasterclient.ui.cells.CategoryBudgetCell;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import tools.ConvertTo;
import tools.Localization;

public class HomeController implements Refreshable, Styleable
{
	@FXML private AnchorPane anchorPaneMain;
	@FXML private Label labelBudget;
	@FXML private Label labelStartBudget;
	@FXML private ProgressBar progressBar;
	@FXML private ListView<CategoryBudget> listView;

	private Controller controller;

	public void init(Controller controller)
	{
		this.controller = controller;

		HomeController thisController = this;
		listView.setCellFactory(param -> new CategoryBudgetCell(thisController));
		
		Label labelPlaceholder = new Label(Localization.getString(Strings.HOME_PLACEHOLDER));          
        labelPlaceholder.setStyle("-fx-font-size: 16");
        listView.setPlaceholder(labelPlaceholder);

		listView.getSelectionModel().selectedIndexProperty().addListener((ChangeListener<Number>)(observable, oldValue, newValue) -> Platform.runLater(() -> listView.getSelectionModel().select(-1)));
		
		applyStyle();
	}
	
	private void refreshListView()
	{		
		listView.getItems().clear();
	
		ArrayList<CategoryBudget> categoryBudgets = controller.getCategoryBudgets();
		if(categoryBudgets != null)
		{				
			listView.getItems().setAll(categoryBudgets);
		}		
	}
	
	private void refreshCounter()
	{
		if(controller.getPaymentHandler().getPayments() != null)
		{
			Budget budget = new Budget(controller.getPaymentHandler().getPayments());	
			double remaining = budget.getIncomeSum() + budget.getPaymentSum();
			String currency = "€";
			if(controller.getSettings() != null)
			{
				currency = controller.getSettings().getCurrency();
			}
			labelBudget.setText(Helpers.getCurrencyString(remaining, currency));
			if(remaining <= 0)
			{
				labelBudget.setStyle("-fx-text-fill: " + ConvertTo.toRGBHexWithoutOpacity(Colors.TEXT_RED));
			}
			else
			{
				labelBudget.setStyle("-fx-text-fill: " + ConvertTo.toRGBHexWithoutOpacity(Colors.TEXT));
			}
			labelStartBudget.setText(Localization.getString(Strings.HOME_BUDGET, Helpers.getCurrencyString(budget.getIncomeSum(), currency)));
			
			double factor = remaining / budget.getIncomeSum();
			if(factor < 0)
			{
				factor = 0;
			}
			progressBar.setProgress(factor);
		}
	}
	
	public Controller getController()
	{
		return controller;
	}

	@Override
	public void refresh()
	{
		refreshListView();	
		refreshCounter();
	}

	@Override
	public void applyStyle()
	{
		anchorPaneMain.setStyle("-fx-background-color: " + ConvertTo.toRGBHexWithoutOpacity(Colors.BACKGROUND));
	}
}