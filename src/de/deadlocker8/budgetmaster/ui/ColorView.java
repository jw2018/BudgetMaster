package de.deadlocker8.budgetmaster.ui;

import java.util.ArrayList;

import fontAwesome.FontIcon;
import fontAwesome.FontIconType;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ColorView extends GridPane
{
	public ColorView(Color startColor, ArrayList<Color> colors, NewCategoryController controller)
	{
		System.out.println(startColor);
		colors.add(Color.TRANSPARENT);

		double size = Math.sqrt(colors.size());
		int iSize = (int)size;
		if(size != iSize)
		{
			iSize++;
		}

		setVgap(5);
		setHgap(5);

		setPadding(new Insets(5));

		int index = 0;
		for(int y = 0; y < iSize; y++)
		{
			for(int x = 0; x < iSize; x++)
			{
				if(index < colors.size())
				{
					Color color = colors.get(index++);

					if(color == Color.TRANSPARENT)
					{
						StackPane stackPane = new StackPane();						

						Rectangle rectangle;
						if(colors.contains(startColor))
						{
							rectangle = createRectangle(startColor, color);
						}
						else
						{
							rectangle = createRectangle(startColor, startColor);
						}
						stackPane.getChildren().add(rectangle);
						
						FontIcon icon = new FontIcon(FontIconType.PLUS);
						icon.setSize(20);
						stackPane.getChildren().add(icon);
						
						stackPane.getChildren().add(createRectangle(startColor, Color.TRANSPARENT));

						// EventHandler
						stackPane.setOnMouseReleased(event -> {
							// TODO
							ColorPicker picker = new ColorPicker();
							Stage stage = new Stage();
							stage.setScene(new Scene(picker));
							stage.show();
							picker.show();
						});

						add(stackPane, x, y);
					}
					else
					{
						Rectangle rectangle = createRectangle(startColor, color);

						// EventHandler
						rectangle.setOnMouseReleased(event -> {
							controller.setColor(color);
						});

						add(rectangle, x, y);
					}
				}
			}
		}
	}

	private Rectangle createRectangle(Color startColor, Color color)
	{
		Rectangle rectangle = new Rectangle(40, 40);
		rectangle.setFill(color);
		rectangle.getStyleClass().add("color-view-item");

		// dotted border
		if(color == startColor)
		{
			rectangle.getStrokeDashArray().addAll(3.0);
		}

		return rectangle;
	}
}