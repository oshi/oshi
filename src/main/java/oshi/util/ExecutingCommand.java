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
			//p.waitFor();
		} catch (IOException e) {
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
		p.destroy();
		return sa;
	}
	
	public static String getFirstAnswer(String cmd2launch) {
		ArrayList<String> sa = ExecutingCommand
				.runNative(cmd2launch);

		if (sa != null)
			return sa.get(0);
		else
			return null;
	}
	
}
