package io.sloeber.ui.monitor.views;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

public class MyPlotter extends org.eclipse.nebula.widgets.oscilloscope.multichannel.Plotter {

	private Color gridColor = null;
	private boolean ShowLabels = false;
	private String myStatus = new String();

	public MyPlotter(Composite parent, int style, Color backgroundColor, Color foregroundColor, Color gridColor) {
		this(1, parent, style, backgroundColor, foregroundColor, gridColor);
	}

	/**
	 * Creates a new plotter with <code>channels</code> channels and adds
	 * attaches
	 *
	 * @param channels
	 * @param dispatcher
	 *            may be null
	 * @param parent
	 * @param style
	 */
	public MyPlotter(int channels, Composite parent, int style, Color backgroundColor, Color foregroundColor,
			Color gridColor) {
		super(channels, parent, style, backgroundColor, foregroundColor);
		this.gridColor = gridColor;
	}

	/**
	 * Create a background image that shows the scale and channels as requested
	 *
	 * @return
	 */
	private Image CreateBackgroundImage(int numHorizontalLines, int numVerticalLines) {
		Rectangle rect = getBounds();
		if ((rect.width == 0) || (rect.height == 0))
			return null;
		Image TheImage = new Image(getDisplay(), rect);
		GC gc = new GC(TheImage);
		gc.setBackground(this.getBackground());
		int step = (getSize().y - 20) / (numHorizontalLines - 1);
		int width = getSize().x;
		int height = getSize().y;
		gc.fillRectangle(0, 0, width, getSize().y);
		gc.setForeground(this.gridColor);
		int Value = this.getRangeLowValue()
				+ (int) (10.0 * (this.getRangeHighValue() - this.getRangeLowValue()) / rect.height);
		int RangeStep = (int) ((float) (rect.height - 20) / (float) (numHorizontalLines - 1)
				* (this.getRangeHighValue() - this.getRangeLowValue()) / rect.height);
		for (int i = 0; i < numHorizontalLines; i++) {
			gc.drawLine(0, 10 + (step * i), width, 10 + (step * i));
			gc.drawString(Integer.toString(Value + RangeStep * (numHorizontalLines - (i + 1))), 10, 10 + (step * i));
		}
		step = (getSize().x - 20) / (numVerticalLines - 1);
		for (int i = 0; i < numVerticalLines; i++) {
			gc.drawLine(10 + (step * i), 0, 10 + (step * i), height);
		}
		step = (int) Math.round(gc.getFont().getFontData()[0].getHeight() * 1.5);
		if (this.ShowLabels) {
			for (int i = 0; i < this.getChannels(); i++) {
				gc.setForeground(this.getForeground(i));
				gc.drawString(this.getChannelName(i), 50, 10 + (step * i));
			}
		}
		gc.setForeground(this.getForeground(0));
		gc.drawString(this.myStatus, 200, 10);

		// gc.drawString(string, x, y)
		gc.dispose();
		return TheImage;
	}

	public boolean isShowLabels() {
		return this.ShowLabels;
	}

	public void setShowLabels(boolean showLabels) {
		this.ShowLabels = showLabels;
	}

	public void setnewBackgroundImage() {
		Image old = getBackgroundImage();
		if (null != old)
			old.dispose();
		setBackgroundImage(CreateBackgroundImage(5, 6));
	}

	public void saveData(String fileName) {

		try (PrintWriter writer = new PrintWriter(fileName, "UTF-8");) { //$NON-NLS-1$
			writer.println(this.getData(true)); // $NON-NLS-1$
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// ignore
			e.printStackTrace();
		}
	}

	public String getStatus() {
		return this.myStatus;
	}

	public void setStatus(String status) {
		this.myStatus = status;
	}

}
