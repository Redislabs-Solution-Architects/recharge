package com.redislabs.riot.test;

import com.redislabs.riot.RiotApp;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.charset.Charset;

public abstract class AbstractRiotTest {


    protected final static String LOCALHOST = "localhost";
    protected static final int REDIS_PORT = 6379;

    protected abstract String appName();

    private String removePreamble(String command) {
        if (command.startsWith(appName())) {
            return command.substring(appName().length());
        }
        return command;
    }

    protected abstract RiotApp app();

    protected Object command(String file) throws Exception {
        return command(app(), file);
    }

    protected Object command(RiotApp app, String file) throws Exception {
        CommandLine commandLine = app.commandLine();
        CommandLine.ParseResult parseResult = commandLine.parseArgs(args(file));
        return parseResult.subcommand().commandSpec().commandLine().getCommand();
    }

    private String[] args(String filename) throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/" + filename)) {
            String command = IOUtils.toString(inputStream, Charset.defaultCharset());
            return CommandLineUtils.translateCommandline(process(removePreamble(command)));
        }
    }

    protected int executeFile(String filename) throws Exception {
        RiotApp app = app();
        return app.execute(args(filename));
    }

    protected String process(String command) {
        return baseArgs() + " " + connectionArgs() + filter(command);
    }

    protected abstract String connectionArgs();

    private String baseArgs() {
        return "--info";
    }

    private String filter(String command) {
        String filtered = command.replace(String.format("-h %s -p %s", LOCALHOST, REDIS_PORT), "");
        return filtered.replaceAll("\b(import|export|replicate)\b", "$1 --no-progress");
    }

    protected void awaitTermination(JobExecution execution) throws InterruptedException {
        while (execution.isRunning()) {
            Thread.sleep(10);
        }
        for (StepExecution stepExecution : execution.getStepExecutions()) {
            Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecution.getExitStatus().getExitCode());
        }
    }


}