package io.sloeber.autoBuilld.jUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.autoBuild.extensionPoint.providers.InternalBuildRunner;

@SuppressWarnings({ "nls", "static-method" })
public class TestCommands {

	@ParameterizedTest
	@MethodSource("windowsCommandSplitterData")
	public void testWindowsCommandSplitter(String command, String[] args) {
		String[] calcedArgs = InternalBuildRunner.argumentsToArrayWindowsStyle(command);
		assertArrayEquals(args, calcedArgs);
	}

	public static Stream<Arguments> windowsCommandSplitterData() throws Exception {
		List<Arguments> ret = new LinkedList<>();
		ret.add(Arguments.of("cmd -a -b", new String[] { "cmd", "-a", "-b" }));
		ret.add(Arguments.of("\"cmd\" '-a' \"-b\"", new String[] { "\"cmd\"", "-a", "\"-b\"" }));
		ret.add(Arguments.of("cmd \"-a=f f\" -b", new String[] { "cmd", "\"-a=f f\"", "-b" }));
		ret.add(Arguments.of("cmd '\"-a=f f\"' -b", new String[] { "cmd", "\"-a=f f\"", "-b" }));
		ret.add(Arguments.of("cmd \"-a=\\\"f f\\\"\" -b", new String[] { "cmd", "\"-a=\\\"f f\\\"\"", "-b" }));
		ret.add(Arguments.of("C:/MinGW64/x86_64-8.1.0-release-posix-seh-rt_v6-rev0/bin/gcc -c -fmessage-length=0 -O3 -Wall -MMD -MP -MF\"src/libraries/c with space/cExtra.d\" -MT\"src/libraries/c with space/cExtra.o\" -o  \"src/libraries/c with space/cExtra.o\" \"../../src/libraries/c with space/cExtra.c\" ", new String[] { "C:/MinGW64/x86_64-8.1.0-release-posix-seh-rt_v6-rev0/bin/gcc", "-c", "-fmessage-length=0", "-O3" ,"-Wall", "-MMD" ,"-MP", "-MF\"src/libraries/c with space/cExtra.d\"" ,"-MT\"src/libraries/c with space/cExtra.o\"" ,"-o"  ,"\"src/libraries/c with space/cExtra.o\"" ,"\"../../src/libraries/c with space/cExtra.c\""  }));
		return ret.stream();

	}

}
