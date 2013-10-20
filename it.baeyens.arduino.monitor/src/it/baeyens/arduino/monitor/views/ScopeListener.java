package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.arduino.MessageConsumer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.nebula.widgets.oscilloscope.multichannel.Oscilloscope;
import org.eclipse.nebula.widgets.oscilloscope.multichannel.OscilloscopeDispatcher;

public class ScopeListener implements MessageConsumer {

	private OscilloscopeDispatcher dispatcher;
	private Integer fDelayLoop = 10;
	private boolean fTailFade = false;
	private Pattern fCommandPattern = Pattern.compile(".*?\\\"(setscope\\s.*?)\\\".*");

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
		dispatcher.dispatch();
	}

	@Override
	public void message(String s) {

		Integer value = null;
		try {

			if (s.contains(">>")) {
				setCommand(s);
				return;
			}

			StringBuilder builder = new StringBuilder();
			for (Character c : s.toCharArray()) {
				if ((c >= '0' && c <= '9') || c == '-') {
					builder.append(c);
				}
			}
			value = Integer.valueOf(builder.toString());
			dispatcher.getOscilloscope().setValue(dispatcher.getChannel(), value);
		} catch (Exception e) {
			System.out.println("Invalid value " + s);
		}
	}

	private void setCommand(String s) {

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
			fDelayLoop = Integer.valueOf(command.replaceAll("setscope delayLoop ", ""));
		}

		if (command.startsWith("setscope toggleTailFade")) {
			fTailFade = !fTailFade;
		}
	}
}
