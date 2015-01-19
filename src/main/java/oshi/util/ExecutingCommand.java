/**
 * 
 */
package oshi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author alessandro[at]perucchi[dot]org
 */

public class ExecutingCommand {
	public static ArrayList<String> runNative(String cmdToRun) {
		Process p = null;
		try {
			p = Runtime.getRuntime().exec(cmdToRun);
			p.waitFor();
		} catch (IOException e) {
			return null;
		} catch (InterruptedException e) {
			return null;
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String line = "";
		ArrayList<String> sa = new ArrayList<String>();
		try {
			while ((line = reader.readLine()) != null) {
				sa.add(line);
			}
		} catch (IOException e) {
			return null;
		}
		return sa;
	}

	/**
	 * Return first line of response for selected command
	 * @param cmd2launch String command to be launched
	 * @return String or null
	 */
	public static String getFirstAnswer(String cmd2launch) {
		return getAnswerAt(cmd2launch, 0);
	}

	/**
	 * Return response on selected line index (0-based) after running selected command
	 * @param cmd2launch String command to be launched
	 * @param answerIdx int index of line in response of the command
	 * @return String whole line in response or null if invalid index or running of command fails
	 */
	public static String getAnswerAt(String cmd2launch, int answerIdx) {
		ArrayList<String> sa = ExecutingCommand.runNative(cmd2launch);

		if (sa != null && answerIdx >= 0 && answerIdx < sa.size())
			return sa.get(answerIdx);
		else
			return null;
	}
	
}
