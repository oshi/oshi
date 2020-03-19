/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.demo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import oshi.SystemInfo;

/**
 * Demo class to vend OSHI JSON data via an HTTP Webserver
 * <p>
 * Code based on tutorial found on SSaurel's Blog :
 * https://www.ssaurel.com/blog/create-a-simple-http-web-server-in-java Each
 * Client Connection will be managed in a dedicated Thread
 */
public class OshiHTTPServer implements Runnable {
    // port to listen connection
    private static final int PORT = 8080;

    private static final Logger logger = LoggerFactory.getLogger(OshiHTTPServer.class);

    // Client Connection via Socket Class
    private Socket connect;

    public OshiHTTPServer(Socket c) {
        connect = c;
        logger.debug("Connecton opened.");
    }

    public static void main(String[] args) {
        try (ServerSocket serverConnect = new ServerSocket(PORT)) {
            logger.info("Server started. Listening for connections on port {}", PORT);

            // we listen until user halts server execution
            while (true) { // NOSONAR squid:S2189
                OshiHTTPServer myServer = new OshiHTTPServer(serverConnect.accept());

                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }
        } catch (IOException e) {
            logger.error("Server Connection error: {}", e.getMessage());
        }
    }

    @Override
    public void run() {
        try ( // read characters from the client via input stream on the socket
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(connect.getInputStream(), StandardCharsets.UTF_8));
                // get character output stream to client (for headers)
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(connect.getOutputStream(), StandardCharsets.UTF_8));
                // get binary output stream to client (for requested data)
                BufferedOutputStream dataOut = new BufferedOutputStream(connect.getOutputStream())) {

            // get first line of the request from the client
            String input = in.readLine();
            if (input == null) {
                throw new IOException("No characters read from input stream.");
            }
            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get fields requested
            String fileRequested = parse.nextToken().toLowerCase();

            // we support only GET and HEAD methods, we check
            if (!method.equals("GET") && !method.equals("HEAD")) {
                logger.debug("501 Not Implemented: {}", method);
                String contentMimeType = "text/html";

                // we send HTTP Headers with data to client
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: OSHI HTTP Server");
                out.println("Date: " + Instant.now());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + 0);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer

                // Could return other information here...
            } else {
                // Possibly could use the fileRequested value from user input to work down
                // OSHI's JSON tree and only return the relevant info instead of the entire
                // SystemInfo object.
                SystemInfo si = new SystemInfo();
                ObjectMapper mapper = new ObjectMapper();
                byte[] content = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(si)
                        .getBytes(StandardCharsets.UTF_8);

                if (method.equals("GET")) { // GET method so we return content
                    // send HTTP Headers
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: OSHI HTTP Server");
                    out.println("Date: " + Instant.now());
                    out.println("Content-type: application/json");
                    out.println("Content-length: " + content.length);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer

                    dataOut.write(content, 0, content.length);
                    dataOut.flush();
                }

                logger.debug("Data {} returned", fileRequested);
            }
        } catch (IOException ioe) {
            logger.error("Server error: {}", ioe.getMessage());
        } finally {
            try {
                // close socket connection, defined for this thread
                connect.close();
            } catch (Exception e) {
                logger.error("Error closing connection: {}", e.getMessage());
            }
            logger.debug("Connection closed.");
        }
    }
}
