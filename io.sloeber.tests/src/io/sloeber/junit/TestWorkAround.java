package io.sloeber.junit;

import static io.sloeber.core.txt.WorkAround.*;
import static org.junit.Assert.*;

import org.junit.Test;

@SuppressWarnings({ "nls", "static-method" })
public class TestWorkAround {

    @Test
    public void add_curly_braces() {
        String input = "tools.xmcprog.path={varName}\r\n"
                + "blabla{path}moreBla{jar_file}extraBla{upload.params}endBla\r\n";
        String expectedResult = "tools.xmcprog.path=${varName}\n"
                + "blabla${path}moreBla${jar_file}extraBla${upload.params}endBla\n";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("Add Curly Braces", expectedResult, result);
    }



    @Test
    public void ignoreUploadVerify() {
        String input = "tools.something.upload.verify=dummy\n" + "tools.something.else={upload.verify}";
        String expectedResult = "tools.something.upload.verify=dummy\n" + "tools.something.else=${upload.verify}";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("upload.verify should not be expanded", expectedResult, result);
    }

    @Test
    public void ignoreProgramVerify() {
        String input = "tools.something.program.verify=dummy\n" + "tools.something.else={program.verify}";
        String expectedResult = "tools.something.program.verify=dummy\n" + "tools.something.else=${program.verify}";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("upload.verify should not be expanded", expectedResult, result);
    }

    // "cmd", "path", "cmd.path", "config.path");
    @Test
    public void alwaysExpand_cmd() {
        String input = "tools.something.else={cmd}";
        String expectedResult = "tools.something.else=${tools.something.cmd}";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("cmd should always be expanded", expectedResult, result);
    }

    @Test
    public void alwaysExpand_path() {
        String input = "tools.something.else={path}";
        String expectedResult = "tools.something.else=${tools.something.path}";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("path should always be expanded", expectedResult, result);
    }

    @Test
    public void alwaysExpand_cmdPath() {
        String input = "tools.something.else={cmd.path}";
        String expectedResult = "tools.something.else=${tools.something.cmd.path}";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("cmd should always be expanded", expectedResult, result);
    }

    @Test
    public void alwaysExpand_ConfigPath() {
        String input = "tools.something.else={config.path}";
        String expectedResult = "tools.something.else=${tools.something.config.path}";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("path should always be expanded", expectedResult, result);
    }
    @Test
    public void HandleSpaceBeforeEqual() {
        String input = "\ntools.xmcprog.path={runtime.tools.XMCFlasher.path}\r\n"
                + "tools.xmcprog.jar_file=XMCFlasher.jar\r\n"
                + "tools.xmcprog.erase.params = -e -device \"XMC{build.board.version}-{build.board.v}\" \r\n"
                + "tools.xmcprog.erase.pattern=java -jar \"{path}/{jar_file}\" {erase.params}\r\n"
                + "tools.xmcprog.upload.protocol=\r\n" + "tools.xmcprog.upload.params.verbose=\r\n"
                + "tools.xmcprog.upload.params.quiet=\r\n"
                + "tools.xmcprog.upload.params =-p \"{build.path}/{build.project_name}.hex\"  -device \"XMC{build.board.version}-{build.board.v}\" \r\n"
                + "tools.xmcprog.upload.pattern=java -jar \"{path}/{jar_file}\" {upload.params}\r\n" + "";
        String expectedResult = "\ntools.xmcprog.path=${runtime.tools.XMCFlasher.path}\n"
                + "tools.xmcprog.jar_file=XMCFlasher.jar\n"
                + "tools.xmcprog.erase.params= -e -device \"XMC${build.board.version}-${build.board.v}\" \n"
                + "tools.xmcprog.erase.pattern=java -jar \"${tools.xmcprog.path}/${tools.xmcprog.jar_file}\" ${tools.xmcprog.erase.params}\n"
                + "tools.xmcprog.upload.protocol=\n" + "tools.xmcprog.upload.params.verbose=\n"
                + "tools.xmcprog.upload.params.quiet=\n"
                + "tools.xmcprog.upload.params=-p \"${build.path}/${build.project_name}.hex\"  -device \"XMC${build.board.version}-${build.board.v}\" \n"
                + "tools.xmcprog.upload.pattern=java -jar \"${tools.xmcprog.path}/${tools.xmcprog.jar_file}\" ${tools.xmcprog.upload.params}\n"
                + "";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("Handle space before =", expectedResult, result);
    }

    @Test
    public void replaceWithFirstLine() {
        String input = "tools.xmcprog.path={runtime.tools.XMCFlasher.path}\r\n"
                + "tools.xmcprog.upload.pattern=java -jar \"{path}/{jar_file}\" {upload.params}\r\n";
        String expectedResult = "tools.xmcprog.path=${runtime.tools.XMCFlasher.path}\n"
                + "tools.xmcprog.upload.pattern=java -jar \"${tools.xmcprog.path}/${jar_file}\" ${upload.params}\n";
        String result = platformApplyWorkArounds(input, null);

        assertEquals("Handle space before =", expectedResult, result);
    }
}
