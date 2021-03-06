package de.deadlocker8.budgetmasterclient.ui.cells;

import de.deadlocker8.budgetmaster.logic.category.Category;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import tools.ConvertTo;

public class ButtonCategoryCell extends ListCell<Category>
{			
	private Color color;
	
	public ButtonCategoryCell(Color color)
	{
		super();
		this.color = color;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
	}

	@Override
	protected void updateItem(Category item, boolean empty)
	{
		super.updateItem(item, empty);		

		if(!empty)
		{		
			HBox hbox = new HBox();
		
			Label labelName = new Label(item.getName());
			labelName.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: " + ConvertTo.toRGBHex(ConvertTo.getAppropriateTextColor(color)));
			labelName.setAlignment(Pos.CENTER);			
			hbox.getChildren().add(labelName);
				
			hbox.setPadding(new Insets(0));
			setStyle("-fx-background: transparent;");
			setGraphic(hbox);	
			setText(null);
			setAlignment(Pos.CENTER);
		}
		else
		{		
			setStyle("-fx-background: transparent");
			setText(null);
			setGraphic(null);
		}
	}
}