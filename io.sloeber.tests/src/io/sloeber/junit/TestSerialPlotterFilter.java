package io.sloeber.junit;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import io.sloeber.ui.Activator;
import io.sloeber.ui.monitor.internal.SerialListener;

@SuppressWarnings({"nls","restriction"})
public class TestSerialPlotterFilter {
	private SerialListener serialListener= new SerialListener(null,0);


	@Test
	public void splitData() {
		SerialListener.setPlotterFilter(true);
		String input="abcdefgh";
		ByteBuffer input1= ByteBuffer.allocate(10);
		input1.order(ByteOrder.LITTLE_ENDIAN);
		input1.put(StandardCharsets.US_ASCII.encode( input.substring(0, 2)));
		input1.putShort(Activator.PLOTTER_START_DATA);
		input1.putShort((short) 4);
		input1.putShort((short) 255);
		input1.putShort((short) 110);
		input1.flip();
		ByteBuffer input2=StandardCharsets.US_ASCII.encode(input.substring(2, input.length()));
		String output= this.serialListener.internalMessage(input1.array());
		output= output+this.serialListener.internalMessage(input2.array());
		assertEquals("Data should be concattenated right",input,output);
	}

	@Test
	public void removeStartData() {
		SerialListener.setPlotterFilter(true);
		String input="abcdefgh";
		String output= this.serialListener.internalMessage(input.substring(0, 2).getBytes());
		output= output+this.serialListener.internalMessage(input.substring(2, input.length()).getBytes());
		assertEquals("Data should be concattenated right",input,output);
	}
}
