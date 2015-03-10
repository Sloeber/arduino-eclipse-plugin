package multichannel;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * Animated widget that tries to mimic an Oscilloscope.
 * 
 * <i>An oscilloscope (also known as a scope, CRO or, an O-scope) is a type of electronic test instrument that allows observation of constantly
 * varying signal voltages, usually as a two-dimensional graph of one or more electrical potential differences using the vertical or 'Y' axis, plotted
 * as a function of time, (horizontal or 'x' axis).</i>
 * <p/>
 * <a href="http://en.wikipedia.org/wiki/Oscilloscope">http://en.wikipedia.org/ wiki/Oscilloscope<a/>
 * 
 * @author Wim.Jongman (@remainsoftware.com)
 * 
 */
public class Oscilloscope extends Canvas {

    /**
     * The default comfortable widget width.
     */
    public static final int DEFAULT_WIDTH = 180;

    /**
     * The default comfortable widget height.
     */
    public static final int DEFAULT_HEIGHT = 100;

    /**
     * The default amount of tail fading in percentages (25).
     */
    public static final int TAILFADE_DEFAULT = 25;

    /**
     * No tailfade.
     */
    public static final int TAILFADE_NONE = 0;

    private Color bg;
    private Color mGridColor;

    public class Data {

	Color fg;
	private int cursor = 50;
	int width = DEFAULT_WIDTH;
	int height = DEFAULT_HEIGHT;
	private int base;
	IntegerFiFoCircularStack stack;
	int tailSize;
	private int lineWidth = 1;
	private boolean percentage = false;
	OscilloscopeDispatcher dispatcher;

	/**
	 * This contains the old or historical input and is used to paint the tail of the graph.
	 */
	private int[] tail;

	/**
	 * This contains the actual values that where input by the user before scaling. If the user resized we can calculate how the tail would have
	 * looked with the new window dimensions.
	 * 
	 * @see Oscilloscope#tail
	 */
	// private int[] originalTailInput;
	private int originalTailSize;
	boolean steady;
	private int tailFade = 25;
	private boolean fade;
	private boolean connect;
	int originalSteadyPosition = STEADYPOSITION_75PERCENT;
	int baseOffset = BASE_CENTER;
	private ArrayList<OscilloscopeStackAdapter> stackListeners;
	private int progression = 0;
	String name = "";

    }

    // Blocks painting if true
    boolean paintBlock;

    private Data[] chan;

    private int myLowRangeValue = 0;

    private int myhighValue = 0;

    private boolean ShowLabels = false;
    private String myStatus = "";

    public String getStatus() {
	return myStatus;
    }

    public void setStatus(String status) {
	myStatus = status;
    }

    public boolean isShowLabels() {
	return ShowLabels;
    }

    public void setShowLabels(boolean showLabels) {
	ShowLabels = showLabels;
    }

    public void SetChannelName(int channel, String name) {
	chan[channel].name = name;
    }

    /**
     * This set of values will draw a figure that is similar to the heart beat that you see on hospital monitors.
     */
    public static final int[] HEARTBEAT = new int[] { 2, 10, 2, -16, 16, 44, 49, 44, 32, 14, -16, -38, -49, -47, -32, -10, 8, 6, 6, -2, 6, 4, 2, 0,
	    0, 6, 8, 6 };
    /**
     * Will draw a maximum tail.
     */
    public static final int TAILSIZE_MAX = -1;

    /**
     * Will draw a tail from the left border but is only valid if the boolean in {@link #setSteady(boolean, int)} was set to true, will default to
     * {@link #TAILSIZE_MAX} otherwise.
     */
    public static final int TAILSIZE_FILL = -2;

    /**
     * The default tail size is 75% of the width.
     */
    public static final int TAILSIZE_DEFAULT = -3;

    /**
     * Steady position @ 75% of graph.
     */
    public static final int STEADYPOSITION_75PERCENT = -1;

    /**
     * The base of the line is positioned at the center of the widget.
     * 
     * @see #setBaseOffset(int)
     */
    public static final int BASE_CENTER = 50;

    /**
     * The stack will not overflow if you push too many values into it but it will rotate and overwrite the older values. Think of the stack as a
     * closed ring with one hole to push values in and one that lets them out.
     * 
     */
    public class IntegerFiFoCircularStack {
	final private int[] stack;
	private int top;
	private int bottom;
	private final int capacity;
	private int storedValues;

	/**
	 * Creates a stack with the indicated capacity.
	 * 
	 * @param capacity
	 */
	public IntegerFiFoCircularStack(int capacity) {
	    if (capacity <= 1)
		throw new RuntimeException("Stack capacity must be > 1");
	    this.capacity = capacity;
	    stack = new int[capacity];
	    top = 0;
	    bottom = 0;
	}

	/**
	 * Creates stack with the indicated capacity and copies the old stack into the new stack and the old stack will be empty after this action.
	 * 
	 * @param capacity
	 * @param oldStack
	 */
	public IntegerFiFoCircularStack(int capacity, IntegerFiFoCircularStack oldStack) {
	    this(capacity);
	    while (!oldStack.isEmpty())
		push(oldStack.pop(0));
	}

	/**
	 * Clears the stack.
	 */
	public void clear() {
	    synchronized (stack) {
		for (int i = 0; i < stack.length; i++) {
		    stack[i] = 0;
		}
		top = 0;
		bottom = 0;
	    }
	}

	/**
	 * Puts a value on the stack.
	 * 
	 * @param value
	 */
	public void push(int value) {
	    if (storedValues == capacity) {
		top = bottom;
		bottom++;
		if (bottom == capacity) {
		    bottom = 0;
		}
	    } else
		storedValues++;

	    if (top == capacity)
		top = 0;

	    stack[top++] = value;
	}

	/**
	 * Returns the oldest value from the stack. Returns the supplied entry if the stack is empty.
	 * 
	 * @param valueIfEmpty
	 * @return int
	 */
	public int pop(int valueIfEmpty) {
	    if (isEmpty())
		return valueIfEmpty;

	    storedValues--;
	    int result = stack[bottom++];

	    if (bottom == capacity)
		bottom = 0;

	    return result;
	}

	/**
	 * Returns the oldest value from the stack and negates the value. Returns the supplied entry if the stack is empty.
	 * 
	 * @param valueIfEmpty
	 * @return int
	 */
	public int popNegate(int valueIfEmpty) {
	    return pop(valueIfEmpty) * -1;
	}

	/**
	 * Returns the oldest value from the stack without removing the value from the stack. Returns the supplied entry if the stack is empty.
	 * 
	 * @param valueIfEmpty
	 * @return int
	 */
	public int peek(int valueIfEmpty) {
	    if (storedValues > 0)
		return stack[bottom];
	    return valueIfEmpty;
	}

	/**
	 * 
	 * @return boolean
	 */
	public boolean isEmpty() {
	    return storedValues == 0;
	}

	/**
	 * 
	 * @return boolean
	 */
	public boolean isFull() {
	    return storedValues == capacity;
	}

	/**
	 * 
	 * @return boolean
	 */
	public int getLoad() {
	    return storedValues;
	}

	public int getCapacity() {
	    return capacity;
	}
    }

    public Oscilloscope(Composite parent, int style, Color backgroundColor, Color foregroundColor, Color gridColor) {
	this(1, null, parent, style, backgroundColor, foregroundColor, gridColor);
    }

    public Oscilloscope(int channels, Composite parent, int style, Color backgroundColor, Color foregroundColor, Color gridColor) {
	this(channels, null, parent, style, backgroundColor, foregroundColor, gridColor);
    }

    /**
     * Creates a new Oscilloscope.
     * 
     * @param parent
     * @param style
     */
    public Oscilloscope(int channels, OscilloscopeDispatcher dispatcher, Composite parent, int style, Color backgroundColor, Color foregroundColor,
	    Color gridColor) {
	super(parent, SWT.DOUBLE_BUFFERED | style);
	bg = backgroundColor;
	mGridColor = gridColor;
	chan = new Data[channels];
	for (int i = 0; i < chan.length; i++) {

	    chan[i] = new Data();

	    if (dispatcher == null)
		chan[i].dispatcher = new OscilloscopeDispatcher(i, this);
	    else {
		chan[i].dispatcher = dispatcher;
		dispatcher.setOscilloscope(this);
	    }

	    setBackground(bg);

	    chan[i].fg = foregroundColor;

	    setTailSize(i, TAILSIZE_DEFAULT);
	}

	addDisposeListener(new DisposeListener() {
	    @Override
	    public void widgetDisposed(DisposeEvent e) {
		Oscilloscope.this.widgetDisposed(e);
	    }
	});

	addPaintListener(new PaintListener() {
	    @Override
	    public void paintControl(PaintEvent e) {
		if (!paintBlock)
		    Oscilloscope.this.paintControl(e);
		paintBlock = false;
	    }
	});

	addControlListener(new ControlListener() {
	    @Override
	    public void controlResized(ControlEvent e) {
		paintBlock = true;
		Oscilloscope.this.controlResized(e);

	    }

	    @Override
	    public void controlMoved(ControlEvent e) {
		Oscilloscope.this.controlMoved(e);
	    }
	});

    }

    /**
     * @param e
     *            no need to use e
     */
    protected void controlMoved(ControlEvent e) {
	// not interested; I guess
    }

    /**
     * @param e
     *            no need to use e
     */
    protected void controlResized(ControlEvent e) {
	setSizeInternal(getSize().x, getSize().y);

	if (getBounds().width > 0) {
	    for (int channel = 0; channel < chan.length; channel++) {

		setSteady(channel, chan[channel].steady, chan[channel].originalSteadyPosition);
		setTailSizeInternal(channel);
	    }
	}
    }

    /**
     * Returns the size of the tail.
     * 
     * @return int
     * @see #setTailSize(int)
     * @see #TAILSIZE_DEFAULT
     * @see #TAILSIZE_FILL
     * @see #TAILSIZE_MAX
     * 
     */
    public int getTailSize(int channel) {
	checkWidget();
	return chan[channel].tailSize;
    }

    private void setSizeInternal(int width, int height) {

	for (int c = 0; c < chan.length; c++) {

	    chan[c].width = width;
	    chan[c].height = height;

	    // calculate the base of the line
	    calculateBase(c);

	    if (width > 1)
		if (chan[c].stack == null)
		    chan[c].stack = new IntegerFiFoCircularStack(width);
		else
		    chan[c].stack = new IntegerFiFoCircularStack(width, chan[c].stack);
	}
    }

    /**
     * Gets the relative location where the line is drawn in the widget.
     * 
     * @return baseOffset
     */
    public int getBaseOffset(int channel) {
	return chan[channel].baseOffset;
    }

    /**
     * Gets the relative location where the line is drawn in the widget, the default is <code>BASE_CENTER</code> which is in the middle of the scope.
     * 
     * @param baseOffset
     *            must be between 100 and -100, exceeding values are rounded to the closest allowable value.
     */
    public void setBaseOffset(int channel, int baseOffset) {

	if (baseOffset > 100)
	    chan[channel].baseOffset = 100;

	else if (baseOffset < -100)
	    chan[channel].baseOffset = -100;
	else
	    chan[channel].baseOffset = baseOffset;

	calculateBase(channel);
    }

    private void calculateBase(int channel) {
	if ((myLowRangeValue != 0) || (myhighValue != 0))
	    chan[channel].base = 0;
	else if (chan[channel].height > 2)
	    chan[channel].base = (chan[channel].height * +(100 - getBaseOffset(channel))) / 100;
    }

    protected void widgetDisposed(DisposeEvent e) {
	// bg.dispose();
	// for (int channel = 0; channel < chan.length; channel++) {
	// chan[channel].fg.dispose();
	// }
    }

    protected int ConvertValueToScreenPosition(int Value, int ScreenHeight) {
	if ((myLowRangeValue == 0) && (myhighValue == 0))
	    return ScreenHeight - Value;
	float ret = ((float) (Value - myLowRangeValue) / (float) (myhighValue - myLowRangeValue)) * (float) ScreenHeight;
	return ScreenHeight - (int) ret;
    }

    protected int ConvertScreenPositionToValue(int screenYPosition) {
	// todo write this stuff
	return 0;
    }

    protected void PositionPolyLine(int[] l1) {
	for (int i = 0; i < l1.length - 4; i += 4) {
	    l1[i + 1] = ConvertValueToScreenPosition(l1[i + 1], getSize().y);
	    l1[i + 3] = ConvertValueToScreenPosition(l1[i + 3], getSize().y);
	}
    }

    protected void paintControl(PaintEvent e) {

	long start = System.currentTimeMillis();

	for (int c = 0; c < chan.length; c++) {

	    // if (chan[c].tailSize <= 0) {
	    // chan[c].stack.popNegate(0);
	    // continue;
	    // }

	    // Go calculate the line
	    Object[] result = calculate(c);
	    int[] l1 = (int[]) result[0];
	    int[] l2 = (int[]) result[1];

	    PositionPolyLine(l1);
	    PositionPolyLine(l2);
	    // System.out.print(System.currentTimeMillis() - start + "-");

	    // Draw it
	    GC gc = e.gc;
	    gc.setForeground(getForeground(c));
	    gc.setAdvanced(true);
	    gc.setAntialias(SWT.ON);
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
		long time = System.nanoTime();
		gc.drawPolyline(l1);
		gc.drawPolyline(l2);
		// System.out.println(System.nanoTime() - time + " nanoseconds");
	    }

	    // Connects the head with the tail
	    if (isConnect(c) && !isFade(c) && chan[c].originalTailSize == TAILSIZE_MAX && l1.length > 0 && l2.length > 0) {
		gc.drawLine(l2[l2.length - 2], l2[l2.length - 1], l1[0], l1[1]);
	    }
	}

	// System.out.println(System.currentTimeMillis() - start + " milliseconds for all channels");

    }

    public Color getForeground(int channel) {
	checkWidget();
	return chan[channel].fg;
    }

    public void setForeground(int channel, Color color) {
	chan[channel].fg = color;
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

	// for (int progress = 0; progress < getProgression(c); progress++) {

	// if (chan[c].stack.isEmpty() && chan[c].stackListeners != null)
	// notifyListeners(c);

	splitPos = chan[c].tailSize * 4;

	if (!isSteady(c))
	    chan[c].cursor++;
	if (chan[c].cursor >= chan[c].width)
	    chan[c].cursor = 0;

	line1 = new int[chan[c].tailSize * 4];
	line2 = new int[chan[c].tailSize * 4];

	// chan[c].tail[chan[c].tailSize] = transform(c, chan[c].width,
	// chan[c].height, chan[c].stack.popNegate(0));

	for (int i = 0; i < chan[c].tailSize; i++) {

	    int posx = chan[c].cursor - chan[c].tailSize + i;
	    int pos = i * 4;
	    if (posx < 0) {
		posx += chan[c].width;
		line1[pos] = posx - 1;

		line1[pos + 1] = getBase(c) + (isSteady(c) ? 0 : chan[c].tail[i]);
		line1[pos + 2] = posx;
		line1[pos + 3] = getBase(c) + (isSteady(c) ? 0 : chan[c].tail[i + 1]);
	    }

	    else {
		if (splitPos == chan[c].tailSize * 4)
		    splitPos = pos;
		line2[pos] = posx - 1;
		line2[pos + 1] = getBase(c) + chan[c].tail[i];
		line2[pos + 2] = posx;
		line2[pos + 3] = (getBase(c) + chan[c].tail[i + 1]);
	    }
	    // chan[c].tail[tailIndex - 1] = chan[c].tail[tailIndex++];
	}
	// }

	int[] l1 = new int[splitPos];
	System.arraycopy(line1, 0, l1, 0, l1.length);
	int[] l2 = new int[(chan[c].tailSize * 4) - splitPos];
	System.arraycopy(line2, splitPos, l2, 0, l2.length);

	return new Object[] { l1, l2 };
    }

    // private void printTails() {
    // if (originalTailInput != null) {
    // System.out.print("x " + tail[0]);
    // for (int i = 1; i < tail.length; i++) {
    // System.out.print(", " + tail[i]);
    // }
    // System.out.println();
    // System.out.print("o " + originalTailInput[0]);
    // for (int i = 1; i < tail.length; i++) {
    // System.out.print(", " + originalTailInput[i]);
    // }
    // System.out.println();
    // System.out.println("----");
    // System.out.println();
    // }
    // }

    /**
     * Transforms the value before it is drawn.
     * 
     * @param value
     *            the next value to be processed
     * @return the transformed value
     */
    private int transform(int channel, int vWidth, int vHeight, int value) {
	if (isPercentage(channel))
	    return (((vHeight / 2) * value) / 100);
	return value;
    }

    /**
     * This method sets the view of the monitor This range is used for all channels which are not set on percentage To go back to default set lowValue
     * and highValue back to 0 This method invalidates the widget so it gets redrawn.
     * 
     * @param lowValue
     *            The value at the bottom of the monitor
     * @param highValue
     *            The value at the top of the monitor
     */
    public void setRange(int lowValue, int highValue) {
	myLowRangeValue = lowValue;
	myhighValue = highValue;
	setnewBackgroundImage();

    }

    public void setnewBackgroundImage() {
	Image old = getBackgroundImage();
	if (null != old)
	    old.dispose();
	setBackgroundImage(CreateBackgroundImage(5, 6));
    }

    /**
     * Create a background image that shows the scale and channels as requested
     * 
     * @return
     */
    private Image CreateBackgroundImage(int numHorizontalLines, int numVerticalLines) {
	// * Image i = new Image(device, boundsRectangle);
	// * GC gc = new GC(i);
	// * gc.drawRectangle(0, 0, 50, 50);
	// * gc.dispose();
	Rectangle rect = getBounds();
	if ((rect.width == 0) || (rect.height == 0))
	    return null;
	Image TheImage = new Image(getDisplay(), rect);
	GC gc = new GC(TheImage);
	gc.setBackground(bg);
	int step = (getSize().y - 20) / (numHorizontalLines - 1);
	int width = getSize().x;
	int height = getSize().y;
	gc.fillRectangle(0, 0, width, getSize().y);
	gc.setForeground(mGridColor);
	int Value = myLowRangeValue + (int) (10.0 * (float) (myhighValue - myLowRangeValue) / (float) rect.height);
	int RangeStep = (int) ((float) (rect.height - 20) / (float) (numHorizontalLines - 1) * (float) (myhighValue - myLowRangeValue) / (float) rect.height);
	for (int i = 0; i < numHorizontalLines; i++) {
	    gc.drawLine(0, 10 + (step * i), width, 10 + (step * i));
	    gc.drawString(Integer.toString(Value + RangeStep * (numHorizontalLines - (i + 1))), 10, 10 + (step * i));
	}
	step = (getSize().x - 20) / (numVerticalLines - 1);
	for (int i = 0; i < numVerticalLines; i++) {
	    gc.drawLine(10 + (step * i), 0, 10 + (step * i), height);
	}
	step = (int) Math.round(gc.getFont().getFontData()[0].height * 1.5);
	if (ShowLabels) {
	    for (int i = 0; i < chan.length; i++) {
		gc.setForeground(chan[i].fg);
		gc.drawString(chan[i].name, 50, 10 + (step * i));
	    }
	}
	gc.setForeground(chan[0].fg);
	gc.drawString(myStatus, 200, 10);

	// gc.drawString(string, x, y)
	gc.dispose();
	return TheImage;
    }

    /**
     * 
     * @return the value represented by the bottom of the oscilloscope
     */
    public int getLowRangeValue() {
	return myLowRangeValue;
    }

    /**
     * 
     * @return the value represented by the top of the oscilloscope
     */
    public int getHighRangeValue() {
	return myhighValue;
    }

    /**
     * @return the base of the line
     */
    public int getBase(int channel) {
	return chan[channel].base;
    }

    /**
     * @return the number of internal calculation steps at each draw request.
     * @see #setProgression(int)
     */
    public int getProgression(int channel) {
	return chan[channel].progression;
    }

    /**
     * The number of internal steps that must be made before drawing. Normally this will slide the graph one pixel. Setting this to a higher value
     * will speed up the animation at the cost of a more jerky motion.
     * 
     * @param progression
     */
    public void setProgression(int channel, int progression) {
	if (progression > 0)
	    chan[channel].progression = progression;

    }

    private void notifyListeners(int channel) {
	if (chan[channel].stackListeners == null || chan[channel].stackListeners.size() == 0)
	    return;
	for (int i = 0; i < chan[channel].stackListeners.size(); i++) {
	    ((OscilloscopeStackAdapter) chan[channel].stackListeners.get(i)).stackEmpty(this, channel);
	}
    }

    /**
     * @return boolean, true if the tail and the head of the graph must be connected if tail size is {@link #TAILSIZE_MAX} no fading graph.
     */
    public boolean isConnect(int channel) {
	checkWidget();
	return chan[channel].connect;
    }

    /**
     * @return int, number of channels.
     */
    public int getChannels() {
	checkWidget();
	return chan.length;
    }

    /**
     * Connects head and tail only if tail size is {@link #TAILSIZE_MAX} and no fading.
     * 
     * @param connectHeadAndTail
     */
    public void setConnect(int channel, boolean connectHeadAndTail) {
	checkWidget();
	chan[channel].connect = connectHeadAndTail;
    }

    /**
     * @see #setFade(boolean)
     * @return boolean fade
     */
    public boolean isFade(int channel) {
	checkWidget();
	return chan[channel].fade;
    }

    /**
     * Sets fade mode so that a percentage of the tail will be faded out at the costs of extra CPU utilization (no beauty without pain or as the Dutch
     * say: "Wie mooi wil gaan moet pijn doorstaan"). The reason for this is that each pixel must be drawn separately with alpha faded in instead of
     * the elegant {@link GC#drawPolygon(int[])} routine which does not support alpha blending.
     * <p>
     * In addition to this, set the percentage of tail that must be faded out {@link #setTailFade(int)}.
     * 
     * @param fade
     *            true or false
     * @see #setTailFade(int)
     */
    public void setFade(int channel, boolean fade) {
	checkWidget();
	chan[channel].fade = fade;
    }

    private void setAlpha(GC gc, double fade) {

	if (gc.getAlpha() == fade)
	    return;
	if (fade >= 255)
	    gc.setAlpha(255);
	else
	    gc.setAlpha((int) fade);
    }

    /**
     * gets the percentage of tail that must be faded out.
     * 
     * @return int percentage
     * @see #setFade(boolean)
     */
    public int getTailFade(int channel) {
	checkWidget();
	return chan[channel].tailFade;
    }

    /**
     * @return boolean steady indicator
     * @see Oscilloscope#setSteady(boolean, int)
     */
    public boolean isSteady(int channel) {
	checkWidget();
	return chan[channel].steady;
    }

    /**
     * Set a bunch of values that will be drawn. The values will be stored in a stack and popped once a value is needed. The size of the stack is the
     * width of the widget. If you resize the widget, the old stack will be copied into a new stack with the new capacity.
     * 
     * @param values
     */
    public synchronized void setValues(int channel, int[] values) {
	checkWidget();

	if (getBounds().width <= 0)
	    return;

	if (!super.isVisible())
	    return;

	if (chan[channel].stack == null)
	    chan[channel].stack = new IntegerFiFoCircularStack(chan[channel].width);

	for (int i = 0; i < values.length; i++) {
	    chan[channel].stack.push(values[i]);
	}
    }

    /**
     * Sets a value to be drawn relative to the middle of the widget. Supply a positive or negative value.
     * 
     * @param value
     */
    public void setValueOrg(int channel, int value) {
	checkWidget();
	if (getBounds().width <= 0)
	    return;

	if (!super.isVisible())
	    return;

	if (chan[channel].stack.capacity > 0)
	    chan[channel].stack.push(value);
    }

    public void setValue(int channel, int value) {
	// chan[c].tail[tailIndex - 1] = chan[c].tail[tailIndex++];
	int copysize = chan[channel].tail.length;
	System.arraycopy(chan[channel].tail, 1, chan[channel].tail, 0, copysize - 1);
	chan[channel].tail[chan[channel].tailSize] = value;
    }

    /**
     * The tail size defaults to TAILSIZE_DEFAULT which is 75% of the width. Setting it with TAILSIZE_MAX will leave one pixel between the tail and
     * the head. All values are absolute except TAILSIZE*. If the width is smaller then the tail size then the tail size will behave like
     * TAILSIZE_MAX.
     * 
     * @param size
     *            the size of the tail
     * @see #getTailSize()
     * @see #TAILSIZE_DEFAULT
     * @see #TAILSIZE_FILL
     * @see #TAILSIZE_MAX
     */
    public void setTailSize(int channel, int size) {
	checkWidget();

	if (size == TAILSIZE_FILL && !isSteady(channel))
	    size = TAILSIZE_MAX;

	if (chan[channel].originalTailSize != size) {
	    tailSizeCheck(size);
	    chan[channel].originalTailSize = size;
	    setTailSizeInternal(channel);
	}
    }

    private void tailSizeCheck(int size) {
	if (size < -3 || size == 0)
	    throw new RuntimeException("Invalid tail size " + size);
    }

    private void setTailSizeInternal(int channel) {

	if (chan[channel].originalTailSize == TAILSIZE_DEFAULT) {
	    // tail = new int[(width / 4) * 3];
	    chan[channel].tailSize = (chan[channel].width / 4) * 3;
	    chan[channel].tailSize--;
	} else if (chan[channel].originalTailSize == TAILSIZE_FILL) {
	    if (isSteady(channel)) {
		chan[channel].tailSize = chan[channel].originalSteadyPosition - 1;
	    } else { // act as if TAILSIZE_MAX
		     // tail = new int[width - 2 + 1];
		chan[channel].tailSize = chan[channel].width - 2;
	    }
	} else if (chan[channel].originalTailSize == TAILSIZE_MAX || chan[channel].originalTailSize > chan[channel].width) {
	    // tail = new int[width - 2 + 1];
	    chan[channel].tailSize = chan[channel].width - 2;
	} else if (chan[channel].tailSize != chan[channel].originalTailSize) {
	    // tail = new int[originalTailSize + 1];
	    chan[channel].tailSize = chan[channel].originalTailSize;
	}

	// Transform the old tail. This is we want to see sort of the same form
	// after resize.
	int[] oldTail = chan[channel].tail;
	if (oldTail == null) {
	    chan[channel].tail = new int[chan[channel].tailSize + 1];
	} else {
	    chan[channel].tail = new int[chan[channel].tailSize + 1];
	    if (chan[channel].tail.length >= oldTail.length) {
		for (int i = 0; i < oldTail.length; i++) {
		    chan[channel].tail[chan[channel].tail.length - 1 - i] = oldTail[oldTail.length - 1 - i];
		}
	    } else {
		for (int i = 0; i < chan[channel].tail.length; i++) {
		    chan[channel].tail[chan[channel].tail.length - 1 - i] = oldTail[oldTail.length - 1 - i];
		}
	    }
	}

	// System.out.println(Arrays.toString(tail));
	// System.out.println(Arrays.toString(oldTail));
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
	checkWidget();

	int width;
	int height;

	if (wHint != SWT.DEFAULT)
	    width = wHint;
	else
	    width = DEFAULT_WIDTH;

	if (hHint != SWT.DEFAULT)
	    height = hHint;
	else
	    height = DEFAULT_HEIGHT;

	return new Point(width + 2, height + 2);
    }

    public boolean needsRedraw() {
	checkWidget();
	return isDisposed() ? false : true;
    }

    /**
     * Sets the line width. A value equal or below zero is ignored. The default width is 1.
     * 
     * @param lineWidth
     */
    public void setLineWidth(int channel, int lineWidth) {
	checkWidget();
	if (lineWidth > 0)
	    chan[channel].lineWidth = lineWidth;
    }

    /**
     * @return int, the width of the line.
     * @see #setLineWidth(int)
     */
    public int getLineWidth(int channel) {
	checkWidget();
	return chan[channel].lineWidth;
    }

    /**
     * If set to true then the values are treated as percentages of the available space rather than absolute values. This will scale the amplitudes if
     * the control is resized. Default is false.
     * 
     * @param percentage
     *            true if percentages
     */
    public void setPercentage(int channel, boolean percentage) {
	checkWidget();
	chan[channel].percentage = percentage;
    }

    /**
     * @return boolean
     * @see #setPercentage(boolean)
     */
    public boolean isPercentage(int channel) {
	checkWidget();
	return chan[channel].percentage;
    }

    /**
     * If steady is true the graph will draw on a steady position instead of advancing.
     * 
     * @param steady
     * @param steadyPosition
     */
    public void setSteady(int channel, boolean steady, int steadyPosition) {
	checkWidget();
	chan[channel].steady = steady;
	chan[channel].originalSteadyPosition = steadyPosition;
	if (steady)
	    if (steadyPosition == STEADYPOSITION_75PERCENT)
		chan[channel].cursor = (int) ((double) chan[channel].width * (double) 0.75);
	    else if (steadyPosition > 0 && steadyPosition < chan[channel].width)
		chan[channel].cursor = steadyPosition;
	// setTailSizeInternal();
    }

    /**
     * Sets the percentage of tail that must be faded out. If you supply 100 then the tail is faded out all the way to the top. The effect will become
     * increasingly less obvious.
     * 
     * @param tailFade
     */
    public void setTailFade(int channel, int tailFade) {
	checkWidget();
	if (tailFade > 100)
	    tailFade = 100;
	if (tailFade < 1)
	    tailFade = 1;
	chan[channel].tailFade = tailFade;
    }

    /**
     * Adds a new stack listener to the collection of stack listeners. Adding the same listener twice will have no effect.
     * 
     * @param listener
     */
    public synchronized void addStackListener(int channel, OscilloscopeStackAdapter listener) {
	checkWidget();
	if (chan[channel].stackListeners == null)
	    chan[channel].stackListeners = new ArrayList<OscilloscopeStackAdapter>();
	if (!chan[channel].stackListeners.contains(listener))
	    chan[channel].stackListeners.add(listener);
    }

    /**
     * Removes a stack listener from the collection of stack listeners.
     * 
     * @param listener
     */
    public void removeStackListener(int channel, OscilloscopeStackAdapter listener) {
	checkWidget();
	if (chan[channel].stackListeners != null) {
	    chan[channel].stackListeners.remove(listener);
	    if (chan[channel].stackListeners.size() == 0)
		synchronized (chan[channel].stackListeners) {
		    chan[channel].stackListeners = null;
		}
	}
    }

    public OscilloscopeDispatcher getDispatcher(int channel) {
	return chan[channel].dispatcher;
    }

    public void setDispatcher(int channel, OscilloscopeDispatcher dispatcher) {
	chan[channel].dispatcher = dispatcher;
    }

    @Override
    public void dispose() {
	// TODO Auto-generated method stub
	super.dispose();
    }

}
