/*******************************************************************************
 *  Copyright (c) 2010 Weltevree Beheer BV, Remain Software & Industrial-TSI
 * 
 * All rights reserved. 
 * This program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Wim S. Jongman - initial API and implementation
 ******************************************************************************/
package multichannel;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * This class keeps the oscilloscope animation running and is used to set various attributes of the scope.
 * 
 * <p/>
 * You need to provide an {@link Oscilloscope} widget by overriding the {@link #getOscilloscope()} method. The {@link Oscilloscope#redraw()} method
 * will be called every {@link #getDelayLoop()} milliseconds. The higher the value, the slower the animation will run.
 * </p>
 * Just before the redraw and just after the redraw the <code>hook..Draw</code> methods are called. Don't do any expensive calculations there because
 * this will slow down the animation.
 * <p/>
 * Then a counter is incremented and if the counter reaches the {@link #getPulse()} value, then the {@link #hookSetValues(int)} method is called. This
 * is you opportunity to provide a value to the scope by calling its setValue or setValues method. The hookSetValues method is only called if the
 * {@link #getPulse()} value is greater than {@link #NO_PULSE}
 * <p/>
 * You can also be called back by the widget if it runs out of values by setting a listener in the
 * {@link Oscilloscope#addStackListener(OscilloscopeStackAdapter)} method.
 * <p/>
 * If you want to speed up the scope, try overriding the {@link #getProgression()} method. This will draw the scope this number of times before
 * actually painting.
 * 
 * @author Wim Jongman
 * 
 */
public class OscilloscopeDispatcher {

    /**
     * Plays a sound clip.
     * 
     */
    public class SoundClip {
	Clip clip = null;
	String oldFile = "";

	/**
	 * Returns the clip so you can control it.
	 * 
	 * @return the Clip
	 */
	public Clip getClip() {
	    return this.clip;
	}

	/**
	 * Creates a clip from the passed sound file and plays it. If the clip is currently playing then the method returns, get the clip with
	 * {@link #getClip()} to control it.
	 * 
	 * @param file
	 * @param loopCount
	 */
	public void playClip(File file, int loopCount) {

	    if (file == null)
		return;

	    try {

		if ((this.clip == null) || !file.getAbsolutePath().equals(this.oldFile)) {
		    this.oldFile = file.getAbsolutePath();
		    this.clip = AudioSystem.getClip();
		    this.clip.open(AudioSystem.getAudioInputStream(file));
		}
		if (this.clip.isActive())
		    return;
		// clip.stop(); << Alternative

		this.clip.setFramePosition(0);
		this.clip.loop(loopCount);

	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    private int channel;

    private final SoundClip clipper = new SoundClip();

    /**
     * Contains a small image that can serve as the background of the scope.
     */
    final public static int[] BACKGROUND_MONITOR = new int[] { 255, 216, 255, 224, 0, 16, 74, 70, 73, 70, 0, 1, 1, 1, 0, 72, 0, 72, 0, 0, 255, 254,
	    0, 19, 67, 114, 101, 97, 116, 101, 100, 32, 119, 105, 116, 104, 32, 71, 73, 77, 80, 255, 219, 0, 67, 0, 5, 3, 4, 4, 4, 3, 5, 4, 4, 4, 5,
	    5, 5, 6, 7, 12, 8, 7, 7, 7, 7, 15, 11, 11, 9, 12, 17, 15, 18, 18, 17, 15, 17, 17, 19, 22, 28, 23, 19, 20, 26, 21, 17, 17, 24, 33, 24, 26,
	    29, 29, 31, 31, 31, 19, 23, 34, 36, 34, 30, 36, 28, 30, 31, 30, 255, 219, 0, 67, 1, 5, 5, 5, 7, 6, 7, 14, 8, 8, 14, 30, 20, 17, 20, 30,
	    30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
	    30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 255, 192, 0, 17, 8, 0, 20, 0, 20, 3, 1, 34, 0, 2, 17, 1, 3, 17, 1, 255, 196,
	    0, 23, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 7, 255, 196, 0, 34, 16, 0, 2, 2, 0, 5, 5, 1, 0, 0, 0, 0, 0, 0, 0, 0,
	    0, 0, 1, 3, 4, 2, 20, 33, 52, 84, 17, 115, 145, 178, 209, 97, 255, 196, 0, 23, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0,
	    1, 3, 255, 196, 0, 27, 17, 0, 2, 2, 3, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 17, 33, 49, 81, 18, 255, 218, 0, 12, 3, 1, 0, 2, 17,
	    3, 17, 0, 63, 0, 225, 37, 87, 118, 245, 59, 79, 217, 140, 212, 60, 24, 60, 226, 250, 83, 110, 196, 74, 10, 205, 211, 133, 245, 141, 180,
	    155, 122, 106, 255, 0, 78, 77, 187, 88, 4, 164, 237, 96, 204, 5, 89, 168, 120, 48, 121, 197, 244, 10, 223, 5, 233, 240, 148, 170, 238,
	    222, 167, 105, 251, 48, 9, 237, 20, 182, 137, 64, 6, 140, 255, 217 };

    /**
     * Contains a small image that can serve as the background of the scope.
     */
    final public static int[] BACKGROUND_MONITOR_SMALL = new int[] { 255, 216, 255, 224, 0, 16, 74, 70, 73, 70, 0, 1, 1, 1, 0, 72, 0, 72, 0, 0, 255,
	    254, 0, 20, 67, 114, 101, 97, 116, 101, 100, 32, 119, 105, 116, 104, 32, 71, 73, 77, 80, 0, 255, 219, 0, 67, 0, 2, 1, 1, 2, 1, 1, 2, 2,
	    2, 2, 2, 2, 2, 2, 3, 5, 3, 3, 3, 3, 3, 6, 4, 4, 3, 5, 7, 6, 7, 7, 7, 6, 7, 7, 8, 9, 11, 9, 8, 8, 10, 8, 7, 7, 10, 13, 10, 10, 11, 12, 12,
	    12, 12, 7, 9, 14, 15, 13, 12, 14, 11, 12, 12, 12, 255, 219, 0, 67, 1, 2, 2, 2, 3, 3, 3, 6, 3, 3, 6, 12, 8, 7, 8, 12, 12, 12, 12, 12, 12,
	    12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
	    12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 255, 192, 0, 17, 8, 0, 10, 0, 10, 3, 1, 34, 0, 2, 17, 1, 3, 17, 1, 255, 196, 0, 31, 0, 0, 1, 5,
	    1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 255, 196, 0, 181, 16, 0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0,
	    0, 1, 125, 1, 2, 3, 0, 4, 17, 5, 18, 33, 49, 65, 6, 19, 81, 97, 7, 34, 113, 20, 50, 129, 145, 161, 8, 35, 66, 177, 193, 21, 82, 209, 240,
	    36, 51, 98, 114, 130, 9, 10, 22, 23, 24, 25, 26, 37, 38, 39, 40, 41, 42, 52, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73, 74, 83,
	    84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104, 105, 106, 115, 116, 117, 118, 119, 120, 121, 122, 131, 132, 133, 134, 135, 136,
	    137, 138, 146, 147, 148, 149, 150, 151, 152, 153, 154, 162, 163, 164, 165, 166, 167, 168, 169, 170, 178, 179, 180, 181, 182, 183, 184,
	    185, 186, 194, 195, 196, 197, 198, 199, 200, 201, 202, 210, 211, 212, 213, 214, 215, 216, 217, 218, 225, 226, 227, 228, 229, 230, 231,
	    232, 233, 234, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 255, 196, 0, 31, 1, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0,
	    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 255, 196, 0, 181, 17, 0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119, 0, 1, 2, 3, 17, 4, 5, 33, 49,
	    6, 18, 65, 81, 7, 97, 113, 19, 34, 50, 129, 8, 20, 66, 145, 161, 177, 193, 9, 35, 51, 82, 240, 21, 98, 114, 209, 10, 22, 36, 52, 225, 37,
	    241, 23, 24, 25, 26, 38, 39, 40, 41, 42, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73, 74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100,
	    101, 102, 103, 104, 105, 106, 115, 116, 117, 118, 119, 120, 121, 122, 130, 131, 132, 133, 134, 135, 136, 137, 138, 146, 147, 148, 149,
	    150, 151, 152, 153, 154, 162, 163, 164, 165, 166, 167, 168, 169, 170, 178, 179, 180, 181, 182, 183, 184, 185, 186, 194, 195, 196, 197,
	    198, 199, 200, 201, 202, 210, 211, 212, 213, 214, 215, 216, 217, 218, 226, 227, 228, 229, 230, 231, 232, 233, 234, 242, 243, 244, 245,
	    246, 247, 248, 249, 250, 255, 218, 0, 12, 3, 1, 0, 2, 17, 3, 17, 0, 63, 0, 248, 10, 210, 13, 31, 254, 17, 29, 65, 22, 238, 252, 192, 110,
	    160, 44, 198, 221, 119, 3, 178, 92, 12, 110, 233, 215, 244, 245, 227, 48, 89, 104, 35, 143, 183, 106, 60, 127, 211, 170, 127, 241, 84,
	    182, 31, 242, 35, 106, 63, 245, 247, 111, 255, 0, 160, 203, 88, 213, 143, 41, 162, 143, 153, 255, 217 };

    /**
     * This is a special value indicating there is no pulse which means that {@link #hookPulse(Oscilloscope, int)} will never be called.
     * 
     * @see #getPulse()
     */
    public static final int NO_PULSE = 0;

    private Image backgroundImage;

    private Oscilloscope scope;

    public OscilloscopeDispatcher(int channel, Oscilloscope scope) {
	this(channel);
	this.scope = scope;
    }

    public OscilloscopeDispatcher(int channel) {
	this.channel = channel;
    }

    /**
     * This method will get the animation going. It will create a runnable that will be dispatched to the user interface thread. The runnable
     * increments a counter that leads to the {@link #getPulse()} value and if this is reached then the counter is reset. The counter is passed to the
     * hook methods so that they can prepare for the next pulse.
     * <p/>
     * After the hook methods are called, the runnable is placed in the user interface thread with a timer of {@link #getDelayLoop()} milliseconds.
     * However, if the delay loop is set to 1, it will dispatch using {@link Display#asyncExec(Runnable)} for maximum speed.
     * <p/>
     * This method is not meant to be overridden, override {@link #init()} {@link #hookBeforeDraw(Oscilloscope, int)},
     * {@link #hookAfterDraw(Oscilloscope, int)} and {@link #hookPulse(Oscilloscope, int)}.
     * 
     * 
     */
    public void dispatch() {

	init();

	Runnable runnable = new Runnable() {

	    private int pulse;

	    public void run() {
		if (getOscilloscope().isDisposed())
		    return;

		hookBeforeDraw(getOscilloscope(), this.pulse);
		getOscilloscope().redraw();
		hookAfterDraw(getOscilloscope(), this.pulse);
		this.pulse++;

		if (this.pulse >= getPulse()) {
		    if (getPulse() != OscilloscopeDispatcher.NO_PULSE) {
			hookPulse(getOscilloscope(), this.pulse);
		    }
		    this.pulse = 0;
		}

		if (getDelayLoop() > 1) {
		    getOscilloscope().getDisplay().timerExec(getDelayLoop(), this);
		} else {
		    getOscilloscope().getDisplay().asyncExec(this);
		}

	    }
	};

	getOscilloscope().getDisplay().syncExec(runnable);

    }

    @Override
    protected void finalize() throws Throwable {
	if ((this.backgroundImage != null) && !this.backgroundImage.isDisposed()) {
	    this.backgroundImage.dispose();
	}
    }

    /**
     * Is used to get the color of the foreground when the thing that the scope is measuring is still alive. The aliveness of the thing that is being
     * measured is returned by the {@link #isServiceActive()} method. The result of this method will be used in the
     * {@link Oscilloscope#setForeground(Color)} method.
     * 
     * @return the system color green. Override if you want to control the foreground color.
     * 
     * @see #getInactiveForegoundColor()
     * @see Oscilloscope#setForeground(Color)
     */
    public Color getActiveForegoundColor() {
	return getOscilloscope().getDisplay().getSystemColor(SWT.COLOR_GREEN);
    }

    /**
     * Override this to return a soundfile that will be played by the dispatcher in the {@link #hookPulse(Oscilloscope, int)} method if the
     * {@link #isSoundRequired()} method returns true.
     * 
     * @return <code>null</code>. Override to return a file that can be played by your sound hardware.
     */
    public File getActiveSoundfile() {
	return null;
    }

    /**
     * Override this to return the background image for the scope.
     * 
     * @return the image stored in {@link #BACKGROUND_MONITOR}. Override to supply your own Image.
     */
    public Image getBackgroundImage() {

	if (this.backgroundImage == null) {
	    byte[] bytes = new byte[OscilloscopeDispatcher.BACKGROUND_MONITOR.length];
	    for (int i = 0; i < OscilloscopeDispatcher.BACKGROUND_MONITOR.length; i++) {
		bytes[i] = (byte) OscilloscopeDispatcher.BACKGROUND_MONITOR[i];
	    }
	    this.backgroundImage = new Image(null, new ByteArrayInputStream(bytes));
	}
	return this.backgroundImage;
    }

    /**
     * Override this to set the offset of the scope line in percentages where 100 is the top of the widget and 0 is the bottom.
     * 
     * @return {@link Oscilloscope#BASE_CENTER} which positions in the center. Override for other values.
     */
    public int getBaseOffset() {
	return Oscilloscope.BASE_CENTER;
    }

    /**
     * Override this to return the Clip player.
     * <p/>
     * Overriding this method is not expected.
     * 
     * @return the PlayClip object
     */
    public SoundClip getSoundClip() {
	return this.clipper;
    }

    /**
     * Override this to return a draw delay in milliseconds. The scope will progress {@link #getProgression()} steps.
     * 
     * @return 30 milliseconds. Override with a smaller value for more speed.
     */
    public int getDelayLoop() {
	return 30;
    }

    public boolean getFade() {
	return true;

    }

    /**
     * Is used to get the color of the foreground when the thing that the scope is measuring is not active. The aliveness of the thing that is being
     * measured is returned by the {@link #isServiceActive()} method. The result of this method will be used in the
     * {@link Oscilloscope#setForeground(Color)} method.
     * 
     * @return the system color red, override if you want to control the inactive foreground color
     * 
     * @see #getActiveForegoundColor()
     * @see Oscilloscope#setForeground(Color)
     */
    public Color getInactiveForegoundColor() {
	return getOscilloscope().getDisplay().getSystemColor(SWT.COLOR_RED);

    }

    public File getInactiveSoundfile() {
	return null;
    }

    public int getLineWidth() {
	return 1;
    }

    /**
     * This method returns the {@link Oscilloscope}.
     * 
     * @return the oscilloscope
     */
    public Oscilloscope getOscilloscope() {
	return scope;
    }

    /**
     * This method sets the {@link Oscilloscope}.
     * 
     * @param scope
     */
    public void setOscilloscope(Oscilloscope scope) {
	this.scope = scope;
    }

    /**
     * Override this to set the number of steps that is calculated before it is actually drawn on the display. This will make the graphic look more
     * jumpy for slower/higher delay rates but you can win speed at faster/lower delay rates. There will be {@link #getProgression()} values consumed
     * so make sure that the value stack contains enough entries.
     * <p/>
     * If the {@link #getDelayLoop()} is 10 and the {@link #getPulse()} is 1 and the {@link #getProgression()} is 5 then every 10 milliseconds the
     * graph will have progressed 5 pixels. If you want to avoid gaps in your graph, you need to input 5 values every time you reach
     * {@link #hookSetValues(int)}. If the {@link #getPulse()} is 3, you need to input 15 values for a gapless graph. Alternatively, you can implement
     * a stack listener in the scope to let it call you in case it runs out of values.
     * 
     * @return 1. Override and increment for more speed. Must be higher then zero.
     */
    public int getProgression() {
	return 1;
    }

    public int getPulse() {
	return 1;
    }

    public int getSteadyPosition() {
	return 200;
    }

    public int getTailFade() {
	return Oscilloscope.TAILFADE_DEFAULT;
    }

    public int getTailSize() {
	return Oscilloscope.TAILSIZE_FILL;
    }

    /**
     * Is called just after the widget is redrawn every {@link #getDelayLoop()} milliseconds. The pulse counter will be set to zero when it reaches
     * {@link #getPulse()}.
     * 
     * @param oscilloscope
     * @param counter
     */
    public void hookAfterDraw(Oscilloscope oscilloscope, int counter) {

    }

    /**
     * Is called just before the widget is redrawn every {@link #getDelayLoop()} milliseconds. It will also call the {@link #hookChangeAttributes()}
     * method if the number of times this method is called matches the {@link #getPulse()} value. The pulse counter will be set to zero when it
     * reaches {@link #getPulse()}.
     * <p/>
     * If you override this method, don't forget to call {@link #hookChangeAttributes()} every now and then.
     * 
     * @param oscilloscope
     * @param counter
     */
    public void hookBeforeDraw(Oscilloscope oscilloscope, int counter) {

	if (counter == getPulse() - 1) {
	    hookChangeAttributes();
	}

    }

    /**
     * This method sets the values in the scope by calling the individual value methods in the dispatcher. Be aware that this method actually calls
     * some candy so you might want to call super.
     */
    public void hookChangeAttributes() {
	//
	getOscilloscope().setBackgroundImage(getBackgroundImage());

	for (int i = 0; i < getOscilloscope().getChannels(); i++) {

	    getOscilloscope().setPercentage(i, isPercentage());
	    getOscilloscope().setTailSize(i, isTailSizeMax() ? Oscilloscope.TAILSIZE_MAX : getTailSize());
	    getOscilloscope().setSteady(i, isSteady(), getSteadyPosition());
	    getOscilloscope().setFade(i, getFade());
	    getOscilloscope().setTailFade(i, getTailFade());
	    getOscilloscope().setConnect(i, mustConnect());
	    getOscilloscope().setLineWidth(i, getLineWidth());
	    getOscilloscope().setBaseOffset(i, getBaseOffset());
	}

    }

    /**
     * This method is called every time the dispatcher reaches the getPulse() counter. This method plays the active or inactive sound if so required.
     * If you do not want the sounds to play, either disable sounds by not overriding the {@link #isSoundRequired()} method or override this method.
     * 
     * @param oscilloscope
     * @param pulse
     */
    public void hookPulse(Oscilloscope oscilloscope, int pulse) {

	// Set the color
	if (isServiceActive()) {
	    getOscilloscope().setForeground(getActiveForegoundColor());

	    // Set a v
	    hookSetValues(pulse);
	    if (isSoundRequired()) {
		getSoundClip().playClip(getActiveSoundfile(), 0);
	    }
	} else {
	    if (isSoundRequired()) {
		getSoundClip().playClip(getInactiveSoundfile(), 0);
	    }
	    getOscilloscope().setForeground(getInactiveForegoundColor());
	}

    }

    /**
     * This method will be called every {@link #getPulse()} times the scope is redrawn which will occur every {@link #getDelayLoop()} milliseconds (if
     * your hardware is capable of doing so). The scope will progress one pixel every {@link #getDelayLoop()} milliseconds and will draw the next
     * value from the queue of the scope. If the scope is out of values it will progress one pixel without a value (draw a pixel at his center).
     * <p/>
     * If the delay loop is 10 and the pulse is 20, you have an opportunity to set a value in the scope every 200 milliseconds. In this time the scope
     * will have progressed 20 pixels. If you supply 10 values by calling the setValue(int) 10 times or if you call the setValues(int[]) with 10 ints
     * then you will see 10 pixels of movement and a straight line of 10 pixels.
     * <p/>
     * If the setPulse method is not overridden or if you supply {@link #NO_PULSE} then this method will not be called unless you override the
     * dispatch method (not recommended). To still set values in the scope you can set a stack listener in the widget that will be called when there
     * are no more values in the stack. Alternatively you can set the return value of {@link #getPulse()} to 1 so you have the opportunity to provide
     * a value every cycle.
     * 
     * @param pulse
     * @see Oscilloscope#setValue(int)
     * @see Oscilloscope#setValues(int[])
     * @see Oscilloscope#addStackListener(OscilloscopeStackAdapter)
     */
    public void hookSetValues(int pulse) {

    }

    /**
     * Will be called only once.
     */
    public void init() {
    }

    public boolean isPercentage() {
	return true;
    }

    public boolean isServiceActive() {
	return true;
    }

    public boolean isSoundRequired() {
	return false;
    }

    public boolean isSteady() {
	return false;
    }

    public boolean isTailSizeMax() {
	return false;
    }

    public boolean mustConnect() {
	return false;
    }

    public void stop() {
	// TODO Auto-generated method stub

    }
}
