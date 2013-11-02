package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.MessageConsumer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.nebula.widgets.oscilloscope.multichannel.Oscilloscope;
import org.eclipse.nebula.widgets.oscilloscope.multichannel.OscilloscopeDispatcher;
import org.eclipse.nebula.widgets.oscilloscope.multichannel.OscilloscopeStackAdapter;

public class ScopeListener implements MessageConsumer {

    OscilloscopeDispatcher dispatcher;
    Queue<Integer> fStack = new LinkedList<Integer>();
    int fDelayLoop = 10;
    boolean fTailFade = false;
    private Pattern fCommandPattern = Pattern.compile(".*?\\\"(setscope\\s.*?)\\\".*");
    StringBuilder fSaveString = new StringBuilder();
    private OscilloscopeStackAdapter stackAdapter;

    public ScopeListener(Oscilloscope oscilloscope) {
	dispatcher = new OscilloscopeDispatcher(0, oscilloscope) {
	    @Override
	    public int getDelayLoop() {
		return fDelayLoop;
	    }

	    @Override
	    public boolean getFade() {
		return fTailFade;
	    }
	};

	stackAdapter = new OscilloscopeStackAdapter() {
	    private int oldValue = 0;

	    @Override
	    public void stackEmpty(Oscilloscope scope, int channel) {
		if (!fStack.isEmpty()) {
		    oldValue = fStack.remove().intValue();
		}
		dispatcher.getOscilloscope().setValue(0, oldValue);
	    }
	};

	oscilloscope.addStackListener(0, stackAdapter);
	dispatcher.dispatch();
    }

    @Override
    public synchronized void message(String s) {

	s = fSaveString.toString() + s;
	fSaveString = new StringBuilder();

	if (!s.contains("\r\n")) {
	    fSaveString.append(s);
	    return;
	}

	String[] split = s.split("\r\n");
	List<String> result = Arrays.asList(split);
	if (!s.endsWith("\r\n")) {
	    fSaveString.append(result.get(result.size() - 1));
	    result = result.subList(0, result.size() - 1);
	}

	for (String message : result) {
	    if (message.length() > 0) {
		Integer value = null;
		try {

		    if (message.startsWith(">>")) {
			parseCommand(message);
			return;
		    }

		    StringBuilder stringBuilder = new StringBuilder();
		    for (char c : message.toCharArray()) {
			if ((c >= '0' && c <= '9') || c == '-') {
			    stringBuilder.append(c);
			}
		    }

		    String buildString = stringBuilder.toString();
		    // System.out.println(buildString);
		    value = Integer.valueOf(buildString);
		    fStack.add(value);

		} catch (Exception e) {
		    // System.out.println("Invalid value " + s);
		}
	    }
	}
    }

    private void parseCommand(String s) {

	String command = null;
	s = s.replaceAll("\r\n", "");
	Matcher matcher = fCommandPattern.matcher(s);
	if (matcher.matches()) {
	    command = matcher.group(1);
	}

	if (command == null) {
	    return;
	}

	if (command.startsWith("setscope delayLoop ")) {
	    fDelayLoop = Integer.valueOf(command.replaceAll("setscope delayLoop ", "")).intValue();
	}

	if (command.startsWith("setscope toggleTailFade")) {
	    fTailFade = !fTailFade;
	}
    }

    @Override
    public void dispose() {
	dispatcher.getOscilloscope().removeStackListener(0, stackAdapter);
	dispatcher.stop();
    }
}
