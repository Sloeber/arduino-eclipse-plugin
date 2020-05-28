package io.sloeber.ui.monitor.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

/**
 * A plotter based on the scope.
 */
@SuppressWarnings("synthetic-access")
public class Plotter extends Canvas {
	private int height = DEFAULT_HEIGHT;
	private int width = DEFAULT_WIDTH;
	// Blocks painting if true
	private boolean paintBlock = true;
	private final Data[] chan;

	private int myRangeHighValue = 100;
	private int myRangeLowValue = -100;

	/**
	 * This class holds the data per channel.
	 *
	 * @author Wim Jongman
	 *
	 */
	private class Data {

		private int base;
		private int baseOffset = BASE_CENTER;
		private boolean connect;
		private int cursor = CURSOR_START_DEFAULT;
		private boolean fade;
		private Color fg;
		private int lineWidth = LINE_WIDTH_DEFAULT;
		private int originalSteadyPosition = STEADYPOSITION_75PERCENT;

		/**
		 * This contains the actual values that where input by the user before scaling.
		 * If the user resized we can calculate how the tail would have looked with the
		 * new window dimensions.
		 *
		 * @see Plotter#tail
		 */
		private int originalTailSize;

		private boolean percentage = false;

		private boolean steady;
		/**
		 * This contains the old or historical input and is used to paint the tail of
		 * the graph.
		 */
		private int[] tail;
		private int tailFade = TAILFADE_PERCENTAGE;
		private int tailSize = TAILSIZE_DEFAULT;

		private boolean antiAlias = false;
		private String name = new String();

	}

	/**
	 * The base of the line is positioned at the center of the widget.
	 *
	 * @see #setBaseOffset(int)
	 */
	public static final int BASE_CENTER = 50;

	/**
	 * The default cursor starting position.
	 */
	public static final int CURSOR_START_DEFAULT = 50;

	/**
	 * The default comfortable widget height.
	 */
	public static final int DEFAULT_HEIGHT = 100;

	/**
	 * The default comfortable widget width.
	 */
	public static final int DEFAULT_WIDTH = 180;

	/**
	 * The default line width.
	 */
	public static final int LINE_WIDTH_DEFAULT = 1;

	/**
	 * The default tail fade percentage
	 */
	public static final int PROGRESSION_DEFAULT = 1;

	/**
	 * Steady position @ 75% of graph.
	 */
	public static final int STEADYPOSITION_75PERCENT = -1;
	/**
	 * The default amount of tail fading in percentages (25).
	 */
	public static final int TAILFADE_DEFAULT = 25;

	/**
	 * No tailfade.
	 */
	public static final int TAILFADE_NONE = 0;

	/**
	 * The default tail fade percentage
	 */
	public static final int TAILFADE_PERCENTAGE = 25;

	/**
	 * The default tail size is 75% of the width.
	 */
	public static final int TAILSIZE_DEFAULT = -3;

	/**
	 * Will draw a tail from the left border but is only valid if the boolean in
	 * {@link #setSteady(boolean, int)} was set to true, will default to
	 * {@link #TAILSIZE_MAX} otherwise.
	 */
	public static final int TAILSIZE_FILL = -2;

	/**
	 * Will draw a maximum tail.
	 */
	public static final int TAILSIZE_MAX = -1;

	// van Sloeber

	/**
	 * Creates a new plotter with <code>channels</code> channels.
	 *
	 * @param channels
	 * @param parent
	 * @param style
	 */
	public Plotter(int channels, Composite parent, int style) {
		this(channels, parent, style, null, null);

	}

	/**
	 * Creates a new plotter with <code>channels</code> channels
	 *
	 * @param channels
	 * @param parent
	 * @param style
	 * @param backgroundColor if null use default background
	 * @param foregroundColor if null use default foreground
	 */

	public Plotter(int channels, Composite parent, int style, Color backgroundColor, Color foregroundColor) {
		super(parent, SWT.DOUBLE_BUFFERED | style);
		Color bg = backgroundColor;
		if (backgroundColor == null) {
			bg = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		}
		Color fg;
		if (foregroundColor == null) {
			fg = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
		} else {
			fg = foregroundColor;
		}

		setBackground(bg);

		this.chan = new Data[channels];
		for (int i = 0; i < this.chan.length; i++) {

			this.chan[i] = new Data();
			this.chan[i].fg = fg;

			setTailSize(i, TAILSIZE_DEFAULT);
		}

		addListener(SWT.Dispose, e -> Plotter.this.widgetDisposed(e));

		addListener(SWT.Paint, e -> {
			if (!Plotter.this.paintBlock) {
				Plotter.this.paintControl(e);
			}
			Plotter.this.paintBlock = false;
		});

		addListener(SWT.Move, e -> Plotter.this.controlMoved(e));
		addListener(SWT.Resize, e -> {
			Plotter.this.paintBlock = true;
			Plotter.this.controlResized(e);
		});
	}

	/**
	 * This method calculates the progression of the line.
	 *
	 * @return
	 */
	private Object[] calculate(int channel) {

		int c = channel;

		int[] line1 = null;
		int[] line2 = null;
		int splitPos = 0;

		splitPos = this.chan[c].tailSize * 4;

		if (!isSteady(c)) {
			this.chan[c].cursor++;
		}
		if (this.chan[c].cursor >= this.width) {
			this.chan[c].cursor = 0;
		}

		line1 = new int[this.chan[c].tailSize * 4];
		line2 = new int[this.chan[c].tailSize * 4];

		for (int i = 0; i < this.chan[c].tailSize; i++) {

			int posx = this.chan[c].cursor - this.chan[c].tailSize + i;
			int pos = i * 4;
			if (posx < 0) {
				posx += this.width;
				line1[pos] = posx - 1;

				line1[pos + 1] = getBase(c) + (isSteady(c) ? 0 : this.chan[c].tail[i]);
				line1[pos + 2] = posx;
				line1[pos + 3] = getBase(c) + (isSteady(c) ? 0 : this.chan[c].tail[i + 1]);
			}

			else {
				if (splitPos == this.chan[c].tailSize * 4) {
					splitPos = pos;
				}
				line2[pos] = posx - 1;
				line2[pos + 1] = getBase(c) + this.chan[c].tail[i];
				line2[pos + 2] = posx;
				line2[pos + 3] = (getBase(c) + this.chan[c].tail[i + 1]);
			}
			// chan[c].tail[tailIndex - 1] = chan[c].tail[tailIndex++];
		}
		// }

		int[] l1 = new int[splitPos];
		System.arraycopy(line1, 0, l1, 0, l1.length);
		int[] l2 = new int[(this.chan[c].tailSize * 4) - splitPos];
		System.arraycopy(line2, splitPos, l2, 0, l2.length);

		return new Object[] { l1, l2 };
	}

	/**
	 * calculate the base of the line
	 *
	 * @param channel
	 */
	private void calculateBase(int channel) {
		if ((this.myRangeLowValue != 0) || (this.myRangeHighValue != 0)) {
			this.chan[channel].base = 0;
		} else if (this.height > 2) {
			this.chan[channel].base = (this.height * +(100 - getBaseOffset(channel))) / 100;
		}
	}

	protected void paintControl(Event e) {

		for (int c = 0; c < this.chan.length; c++) {

			// Go calculate the line
			Object[] result = calculate(c);
			int[] l1 = (int[]) result[0];
			int[] l2 = (int[]) result[1];

			PositionPolyLine(l1);
			PositionPolyLine(l2);

			// Draw it
			GC gc = e.gc;
			gc.setForeground(getForeground(c));
			gc.setAdvanced(true);
			gc.setAntialias(this.chan[c].antiAlias ? SWT.ON : SWT.OFF);
			gc.setLineWidth(getLineWidth(c));

			// Fade tail
			if (isFade(c)) {
				gc.setAlpha(0);
				double fade = 0;
				double fadeOutStep = (double) 125 / (double) ((getTailSize(c) * (getTailFade(c)) / 100));
				for (int i = 0; i < l1.length - 4;) {
					fade += (fadeOutStep / 2);
					setAlpha(gc, fade);
					gc.drawLine(l1[i], l1[i + 1], l1[i + 2], l1[i + 3]);
					i += 2;
				}

				for (int i = 0; i < l2.length - 4;) {
					fade += (fadeOutStep / 2);
					setAlpha(gc, fade);
					gc.drawLine(l2[i], l2[i + 1], l2[i + 2], l2[i + 3]);
					i += 2;
				}

			} else {
				gc.drawPolyline(l1);
				gc.drawPolyline(l2);
			}

			// Connects the head with the tail
			if (isConnect(c) && !isFade(c) && this.chan[c].originalTailSize == TAILSIZE_MAX && l1.length > 0
					&& l2.length > 0) {
				gc.drawLine(l2[l2.length - 2], l2[l2.length - 1], l1[0], l1[1]);
			}
		}
	}

	/**
	 * Sets a value to be drawn relative to the center of the channel. Supply a
	 * positive or negative value. This method will only accept values if the width
	 * of the plotter > 0. The values will be stored in a stack and popped once a
	 * value is needed. The size of the stack is the width of the widget. If you
	 * resize the widget, the old stack will be copied into a new stack with the new
	 * capacity.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @param value   which is an absolute value or a percentage
	 *
	 * @see #isPercentage(int)
	 * @see #setBaseOffset(int, int)
	 */
	public void setValue(int channel, int value) {
		int copysize = this.chan[channel].tail.length;
		System.arraycopy(this.chan[channel].tail, 1, this.chan[channel].tail, 0, copysize - 1);
		this.chan[channel].tail[this.chan[channel].tailSize] = value;
	}

	/**
	 *
	 * @return the value represented by the bottom of the plotter
	 */
	public int getRangeLowValue() {
		return this.myRangeLowValue;
	}

	/**
	 *
	 * @return the value represented by the top of the plotter
	 */
	public int getRangeHighValue() {
		return this.myRangeHighValue;
	}

	private static void setAlpha(GC gc, double fade) {

		if (gc.getAlpha() == fade) {
			return;
		}
		if (fade >= 255) {
			gc.setAlpha(255);
		} else {
			gc.setAlpha((int) fade);
		}
	}

	protected void PositionPolyLine(int[] l1) {
		for (int i = 0; i < l1.length - 4; i += 4) {
			l1[i + 1] = ConvertValueToScreenPosition(l1[i + 1], getSize().y);
			l1[i + 3] = ConvertValueToScreenPosition(l1[i + 3], getSize().y);
		}
	}

	protected int ConvertValueToScreenPosition(int Value, int ScreenHeight) {
		if ((this.myRangeLowValue == 0) && (this.myRangeHighValue == 0)) {
			return ScreenHeight - Value;
		}
		float ret = ((float) (Value - this.myRangeLowValue) / (float) (this.myRangeHighValue - this.myRangeLowValue))
				* ScreenHeight;
		return ScreenHeight - (int) ret;
	}

	private void setTailSizeInternal(int channel) {

		if (this.chan[channel].originalTailSize == TAILSIZE_DEFAULT) {
			this.chan[channel].tailSize = (this.width / 4) * 3;
			this.chan[channel].tailSize--;
		} else if (this.chan[channel].originalTailSize == TAILSIZE_FILL) {
			if (isSteady(channel)) {
				this.chan[channel].tailSize = this.chan[channel].originalSteadyPosition - 1;
			} else {
				// act as if TAILSIZE_MAX
				this.chan[channel].tailSize = this.width - 2;
			}
		} else if (this.chan[channel].originalTailSize == TAILSIZE_MAX
				|| this.chan[channel].originalTailSize > this.width) {
			this.chan[channel].tailSize = this.width - 2;
		} else if (this.chan[channel].tailSize != this.chan[channel].originalTailSize) {
			this.chan[channel].tailSize = this.chan[channel].originalTailSize;
		}

		// Transform the old tail. This is we want to see sort of the same form
		// after resize.
		int[] oldTail = this.chan[channel].tail;
		if (oldTail == null) {
			this.chan[channel].tail = new int[this.chan[channel].tailSize + 1];
		} else {
			this.chan[channel].tail = new int[this.chan[channel].tailSize + 1];
			if (this.chan[channel].tail.length >= oldTail.length) {
				for (int i = 0; i < oldTail.length; i++) {
					this.chan[channel].tail[this.chan[channel].tail.length - 1 - i] = oldTail[oldTail.length - 1 - i];
				}
			} else {
				for (int i = 0; i < this.chan[channel].tail.length; i++) {
					this.chan[channel].tail[this.chan[channel].tail.length - 1 - i] = oldTail[oldTail.length - 1 - i];
				}
			}
		}
	}

	public void setRange(int lowValue, int highValue) {
		this.myRangeLowValue = lowValue;
		this.myRangeHighValue = highValue;
	}

	/**
	 * Returns the data in CSV format using the semicolon (;) as separator and
	 * delegating to {@link #getData(boolean, String)}.
	 *
	 * @param addHeader true to add the header
	 * @return the CSV string.
	 *
	 * @see #getData(boolean, String)
	 */
	public String getData(boolean addHeader) {
		return getData(addHeader, ";"); //$NON-NLS-1$
	}

	/**
	 * This method returns the data in csv format using the passed separator as
	 * field separator. if addHeader is true a header is added based on the names of
	 * the channels. Use {@link #SetChannelName(int, String)} to set the names.
	 */
	public String getData(boolean addHeader, String separator) {
		String ret = new String();
		String nl = System.lineSeparator();

		if (addHeader) {
			for (Data element : this.chan) {
				ret = ret + element.name + separator;
			}
			ret += nl;
		}
		for (int curvalue = 0; curvalue < this.chan[0].tail.length; curvalue++) {
			for (Data element : this.chan) {
				ret += element.tail[curvalue] + separator;
			}
			ret += nl;
		}
		return ret;
	}

	/**
	 * Set the descriptive name of the channel
	 *
	 * @param channel the channel to set the name for
	 * @param name    the descriptive name
	 */
	public void SetChannelName(int channel, String name) {
		this.chan[channel].name = name;
	}

	/**
	 * get the descriptive name of the channel
	 *
	 * @param channel the channel to get the name from returns the descriptive name
	 */
	public String getChannelName(int channel) {
		return this.chan[channel].name;
	}

	/**
	 * If steady is true the graph will draw on a steady position instead of
	 * advancing.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param steady
	 * @param steadyPosition
	 */
	public void setSteady(int channel, boolean steady, int steadyPosition) {
		this.chan[channel].steady = steady;
		this.chan[channel].originalSteadyPosition = steadyPosition;
		if (steady) {
			if (steadyPosition == STEADYPOSITION_75PERCENT) {
				this.chan[channel].cursor = (int) (this.width * 0.75);
			} else if (steadyPosition > 0 && steadyPosition < this.width) {
				this.chan[channel].cursor = steadyPosition;
			}
		}
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean steady indicator
	 * @see Oscilloscope#setSteady(boolean, int)
	 */
	public boolean isSteady(int channel) {
		return this.chan[channel].steady;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return the base of the line.
	 */
	public int getBase(int channel) {
		return this.chan[channel].base;
	}

	/**
	 * Gets the relative location where the line is drawn in the widget. This method
	 * can be called outside of the UI thread.
	 *
	 * @return baseOffset
	 */
	public int getBaseOffset(int channel) {
		return this.chan[channel].baseOffset;
	}

	/**
	 * Returns the number of channels on the plotter This method can be called
	 * outside of the UI thread.
	 *
	 * @return int, number of channels.
	 */
	public int getChannels() {
		return this.chan.length;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @return the foreground color associated with the supplied channel.
	 */
	public Color getForeground(int channel) {
		return this.chan[channel].fg;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return int, the width of the line.
	 * @see #setLineWidth(int)
	 */
	public int getLineWidth(int channel) {
		return this.chan[channel].lineWidth;
	}

	/**
	 * Gets the percentage of tail that must be faded out. This method can be called
	 * outside of the UI thread.
	 *
	 * @return int percentage
	 * @see #setFade(boolean)
	 */
	public int getTailFade(int channel) {
		return this.chan[channel].tailFade;
	}

	/**
	 * Returns the size of the tail. This method can be called outside of the UI
	 * thread.
	 *
	 * @return int
	 * @see #setTailSize(int)
	 * @see #TAILSIZE_DEFAULT
	 * @see #TAILSIZE_FILL
	 * @see #TAILSIZE_MAX
	 *
	 */
	public int getTailSize(int channel) {
		return this.chan[channel].tailSize;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean, true if the tail and the head of the graph must be connected
	 *         if tail size is {@link #TAILSIZE_MAX} no fading graph.
	 */
	public boolean isConnect(int channel) {
		return this.chan[channel].connect;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @see #setFade(boolean)
	 * @return boolean fade
	 */
	public boolean isFade(int channel) {
		return this.chan[channel].fade;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean
	 * @see #setPercentage(boolean)
	 */
	public boolean isPercentage(int channel) {
		return this.chan[channel].percentage;
	}

	/**
	 * This method can be called outside of the UI thread.
	 *
	 * @return boolean anti-alias indicator
	 * @see Oscilloscope#setAntialias(int, boolean)
	 */
	public boolean isAntiAlias(int channel) {
		return this.chan[channel].antiAlias;
	}

	/**
	 * The tail size defaults to TAILSIZE_DEFAULT which is 75% of the width. Setting
	 * it with TAILSIZE_MAX will leave one pixel between the tail and the head. All
	 * values are absolute except TAILSIZE*. If the width is smaller then the tail
	 * size then the tail size will behave like TAILSIZE_MAX.
	 *
	 * @param size the size of the tail
	 * @see #getTailSize()
	 * @see #TAILSIZE_DEFAULT
	 * @see #TAILSIZE_FILL
	 * @see #TAILSIZE_MAX
	 */
	public void setTailSize(int channel, int newSize) {
		int size = newSize;
		checkWidget();

		if (size == TAILSIZE_FILL && !isSteady(channel)) {
			size = TAILSIZE_MAX;
		}

		if (this.chan[channel].originalTailSize != size) {
			tailSizeCheck(size);
			this.chan[channel].originalTailSize = size;
			setTailSizeInternal(channel);
		}
	}

	private static void tailSizeCheck(int size) {
		if (size < -3 || size == 0) {
			throw new RuntimeException("Invalid tail size " + size); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unused")
	protected void widgetDisposed(Event e) {
		// nothing to do
	}

	@SuppressWarnings("unused")
	protected void controlMoved(Event e) {
		// nothing to do
	}

	@SuppressWarnings("unused")
	protected void controlResized(Event e) {

		this.width = getSize().x;
		this.height = getSize().y;
		for (int c = 0; c < this.chan.length; c++) {
			calculateBase(c);
		}
		if (getBounds().width > 0) {
			for (int channel = 0; channel < this.chan.length; channel++) {
				setSteady(channel, this.chan[channel].steady, this.chan[channel].originalSteadyPosition);
				setTailSizeInternal(channel);
			}
		}
	}

	/**
	 * Sets the percentage of tail that must be faded out. If you supply 100 then
	 * the tail is faded out all the way to the top. The effect will become
	 * increasingly less obvious.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param tailFade
	 */
	public void setTailFade(int channel, int newTailFade) {
		int tailFade = newTailFade;
		checkWidget();
		if (tailFade > 100) {
			tailFade = 100;
		}
		if (tailFade < 1) {
			tailFade = 1;
		}
		this.chan[channel].tailFade = tailFade;
	}

	/**
	 * Sets fade mode so that a percentage of the tail will be faded out at the
	 * costs of extra CPU utilization (no beauty without pain or as the Dutch say:
	 * "Wie mooi wil gaan moet pijn doorstaan"). The reason for this is that each
	 * pixel must be drawn separately with alpha faded in instead of the elegant
	 * {@link GC#drawPolygon(int[])} routine which does not support alpha blending.
	 * <p>
	 * In addition to this, set the percentage of tail that must be faded out
	 * {@link #setTailFade(int)}.
	 * <p>
	 * This method can be called outside of the UI thread.
	 *
	 * @param fade true or false
	 * @see #setTailFade(int)
	 */
	public void setFade(int channel, boolean fade) {
		this.chan[channel].fade = fade;
	}

	/**
	 * Sets the foreground color for the supplied channel.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param channel
	 * @param color
	 */
	public void setForeground(int channel, Color color) {
		this.chan[channel].fg = color;
	}

	/**
	 * Sets the line width. A value equal or below zero is ignored. The default
	 * width is 1. This method can be called outside of the UI thread.
	 *
	 * @param lineWidth
	 */
	public void setLineWidth(int channel, int lineWidth) {
		if (lineWidth > 0) {
			this.chan[channel].lineWidth = lineWidth;
		}
	}

	/**
	 * If set to true then the values are treated as percentages of the available
	 * space rather than absolute values. This will scale the amplitudes if the
	 * control is resized. Default is false.
	 * <p/>
	 * This method can be called outside of the UI thread.
	 *
	 * @param percentage true if percentages
	 */
	public void setPercentage(int channel, boolean percentage) {
		this.chan[channel].percentage = percentage;
	}

	/**
	 * Gets the relative location where the line is drawn in the widget, the default
	 * is <code>BASE_CENTER</code> which is in the middle of the plotter. This
	 * method can be called outside of the UI thread.
	 *
	 * @param baseOffset must be between 100 and -100, exceeding values are rounded
	 *                   to the closest allowable value.
	 */
	public void setBaseOffset(int channel, int newBaseOffset) {
		int baseOffset = newBaseOffset;
		if (baseOffset > 100) {
			baseOffset = 100;
		}

		if (baseOffset < -100) {
			baseOffset = -100;
		}

		this.chan[channel].baseOffset = baseOffset;

		calculateBase(channel);
	}

	/**
	 * Connects head and tail only if tail size is {@link #TAILSIZE_MAX} and no
	 * fading. This method can be called outside of the UI thread.
	 *
	 * @param connectHeadAndTail
	 */
	public void setConnect(int channel, boolean connectHeadAndTail) {
		this.chan[channel].connect = connectHeadAndTail;
	}

	/**
	 * Sets if the line must be anti-aliased which uses more processing power in
	 * return of a smoother image. The default value is false. This method can be
	 * called outside of the UI thread.
	 *
	 * @param channel
	 * @param antialias
	 */
	public void setAntialias(int channel, boolean antialias) {
		this.chan[channel].antiAlias = antialias;
	}

}
