/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for executing on the command line and returning the result of
 * execution.
 * 
 * @author alessandro[at]perucchi[dot]org
 */
public class ExecutingCommand {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutingCommand.class);

    /**
     * Executes a command on the native command line and returns the result.
     * 
     * @param cmdToRun
     *            Command to run
     * @return A list of Strings representing the result of the command
     */
    public static ArrayList<String> runNative(String cmdToRun) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmdToRun);
            p.waitFor();
        } catch (IOException e) {
            LOG.trace("", e);
            return null;
        } catch (InterruptedException e) {
            LOG.trace("", e);
            return null;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = "";
        ArrayList<String> sa = new ArrayList<>();
        try {
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
        } catch (IOException e) {
            LOG.trace("", e);
            return null;
        }
        return sa;
    }

    /**
     * Return first line of response for selected command.
     * 
     * @param cmd2launch
     *            String command to be launched
     * @return String or null
     */
    public static String getFirstAnswer(String cmd2launch) {
        return getAnswerAt(cmd2launch, 0);
    }

    /**
     * Return response on selected line index (0-based) after running selected
     * command.
     * 
     * @param cmd2launch
     *            String command to be launched
     * @param answerIdx
     *            int index of line in response of the command
     * @return String whole line in response or null if invalid index or running
     *         of command fails
     */
    public static String getAnswerAt(String cmd2launch, int answerIdx) {
        List<String> sa = ExecutingCommand.runNative(cmd2launch);

        if (sa != null && answerIdx >= 0 && answerIdx < sa.size()) {
            return sa.get(answerIdx);
        }
        return null;
    }

}
