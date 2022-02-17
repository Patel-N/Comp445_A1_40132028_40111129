/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package httpc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 *
 * @author neil
 */
public class Httpc {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws URISyntaxException {

        // TODO code application logic here

        /**
         * 1. Read args and split 2. If "help" alone: page 6 output get: page 6
         * output post: page 6/7 output
         *
         * 3. Get Valid command: -v: Prints detail ==> protocol, status &
         * headers -h: headers in 'key:value' format; there can be many Invalid
         * command: -d -f
         *
         * 4. Post Valid Command: -v: Prints detail ==> protocol, status &
         * headers -h: 'key:value' -d: inline data to body of POST -f: file
         * Invalid commands: -d & -f can't be there at the same time
         *
         */
        //No commands
        if (args.length == 0) {
            System.out.println("Type 'help' to get more information on how to use httpc.");
        } else {

            //Receiving "help"
            if (args[0].equals("help")) {
                handleHelpCall(args);
            } else if (args[0].equalsIgnoreCase("get")) {

                //Setup params
                //Apply param
                //Make Socket call
                //Output the stream
            } else if (args[0].equalsIgnoreCase("post")) {
                handlePostCall(args);
            }

        }

    }

    private static void handleHelpCall(String[] commands) {

        if (commands.length == 1 || commands[1].equals("help")) {
            //Only help
            System.out.println("httpc is a curl-like application but supports HTTP protocol only.\n"
                    + "Usage:\n\thttpc command [arguments]\nThe commands are:\n"
                    + "\tget\texecutes a HTTP GET request and prints the response.\n"
                    + "\tpost\texecutes a HTTP POST request and prints the response.\n"
                    + "\thelp\tprints this screen"
                    + "\n\nUse \"httpc help [command]\" for more information about a command.");
        } else if (commands.length == 2) {

            if (commands[1].equalsIgnoreCase("get")) {
                System.out.println("httpc help get");
                System.out.println(getHelpMessage);
            }
            if (commands[1].equalsIgnoreCase("post")) {
                System.out.println("httpc help post");
                System.out.println(postHelpMessage);
            }

        } else {
            //"help ____" invalid command
            System.out.println("Invalid command entered. Try using 'httpc help' for more information.");
        }

    }

    /**
     *
     *
     * mvn exec:java -Dexec.mainClass="ca.concordia.comp445_a1.httpc.httpc"
     * -Dexec.args="post -h Content-Type:application/json -d '{"Assignment":1}'
     * http://httpbin.org/post"
     *
     *
     */
    private static void handlePostCall(String[] commands) throws URISyntaxException {

        boolean isVerbose = false;
        boolean writeToFile = false;
        String outputFileName = "";
        String domain = "";
        String param = "";
        int port = 80;
        StringBuilder headerBuilder = new StringBuilder();
        StringBuilder bodyBuilder = new StringBuilder();

        //Iterate through argument to build request
        for (int i = 1; i < commands.length; i++) {
            //verbose
            if (commands[i].equals("-v")) {
                isVerbose = true;
            } else if (commands[i].equals("-h")) {

                //next argument should be a 'key:value'
                headerBuilder.append(commands[i + 1] + "\r\n");

            } else if (commands[i].equals("-o")) {

                if (i + 1 < commands.length) {
                    outputFileName = commands[i + 1];
                    writeToFile = true;
                    i++;
                } else {
                    System.out.println("Output file not specified.");
                    System.out.println(postHelpMessage);
                    break;
                }

            } else if (commands[i].equals("-d")) {
                //error if -f was already called
                if (bodyBuilder.length() != 0) {
                    System.out.println("-d and -f Can not be used at the same time.");
                    System.out.println(postHelpMessage);
                    break;
                }

                String s = commands[i + 1];
                boolean firstAlphaNumFound = false;
                for (int j = 0; j < s.length(); j++) {
                    if (s.substring(j, j + 1).matches("[a-zA-Z0-9]")) {
                        if (!firstAlphaNumFound) {
                            bodyBuilder.append('"');
                            bodyBuilder.append(s.substring(j, j + 1));
                            firstAlphaNumFound = true;
                        } else {
                            bodyBuilder.append(s.substring(j, j + 1));
                        }

                    } else if (s.substring(j, j + 1).equals(":")) {
                        //Close double quote and append the rest of the string
                        bodyBuilder.append('"');
                        bodyBuilder.append(s.substring(j));
                        break;
                    } else {
                        bodyBuilder.append(s.substring(j, j + 1));
                    }
                }

            } else if (commands[i].equals("-f")) {
                //error if -d was already called
                if (bodyBuilder.length() != 0) {
                    System.out.println("-d and -f Can not be used at the same time.");
                    System.out.println(postHelpMessage);
                    break;
                }

                //Append Body content
                try {
                    File file = new File(commands[i + 1]);
                    Scanner reader = new Scanner(file);
                    while (reader.hasNextLine()) {
                        bodyBuilder.append(reader.nextLine());
                    }
                } catch (FileNotFoundException ex) {
                    System.err.println(ex);
                    bodyBuilder.append("File not found");
                }
            } else if (isUrl(commands[i])) {
                //Get Domain and parameters
                domain = extractDomain(commands[i]);

                String domainParam = commands[i].substring(commands[i].indexOf(domain));
                param = (domainParam.contains("/") ? domainParam.substring(domainParam.indexOf("/")) : "");
            }
        }

        //Do the request
        try {
            Socket s = new Socket(domain, 80);
            PrintWriter pWriter = new PrintWriter(s.getOutputStream());
            Scanner inputS = new Scanner(s.getInputStream());

            //Setup request
            String req = "POST " + param + " HTTP/1.0\r\n"
                    + "Content-Length:" + bodyBuilder.toString().length() + "\r\n";
            if (!headerBuilder.toString().isEmpty()) {
                req += (headerBuilder.toString() + "\r\n");
            }
            if (!bodyBuilder.toString().isEmpty()) {
                req += (bodyBuilder.toString() + "\r\n");
            }

            //Send req
            pWriter.write(req);
            pWriter.flush();

            //Print Response
            printResponse(isVerbose, writeToFile, outputFileName, inputS);

            pWriter.close();
            inputS.close();
            s.close();

        } catch (IOException ex) {
            System.err.println(ex);
        }

    }

    private static String extractDomain(String url) throws URISyntaxException {
        System.out.println(url);
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private static boolean isUrl(String s) {
        Pattern regex = Pattern.compile("(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
        return regex.matcher(s).matches();
    }

    private static void printResponse(boolean isVerbose, boolean writeToFile, String outputFileName, Scanner inputS) throws FileNotFoundException, IOException {
        String result = "";
        boolean inBody = false;
        StringBuilder sb = new StringBuilder();

        while (inputS.hasNextLine()) {
            result = inputS.nextLine();
            if (isVerbose && !inBody) {
                System.out.println(result);
            }
            if (result.isBlank() && !inBody) {
                inBody = true;
                continue;
            } else if (inBody && (result != null)) {
                if (writeToFile) {
                    sb.append(result);
                    sb.append(System.lineSeparator());
                } else {
                    System.out.println(result);
                }
            }
        }

        if (writeToFile) {

            File file = new File(outputFileName);
            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();
            FileOutputStream fOS = new FileOutputStream(file);

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fOS));
            bw.write(sb.toString());
            bw.close();
        }

    }
    
    
    private static final String postHelpMessage = "Post executes a HTTP POST request for a given URL with inline data or from file.\n"
            + "Usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n"
            + "Get executes a HTTP GET request for a given URL.\n"
            + "\t-v\t\tPrints the detail of the response such as protocol, status, and headers.\n"
            + "\t-h key:value\tAssociates headers to HTTP Request with the format 'key:value'.\n"
            + "\t-d string\t\t Associates an inline data to the body HTTP POST request.\n"
            + "\t-f file\t\t Associates the content of a file to the body HTTP POST request.\n"
            + "\t -o string\t\t Flag to write to an output file. Body of the response will not be printed to the console.\n"
            + "INFORMATION: If [-v] and [-o] are used, the details will be displayed in console, but will not be written in the file.\n"
            + "WARNING: Either [-d] or [-f] can be used but not both.\n";

    private static final String getHelpMessage = "httpc is a curl-like application but supports HTTP protocol only.\n"
            + "Usage: httpc get [-v] [-h key:value] URL\n"
            + "Get executes a HTTP GET request for a given URL.\n"
            + "\t-v\t\tPrints the detail of the response such as protocol, status, and headers.\n"
            + "\t -o string\t\t Flag to write to an output file. Body of the response will not be printed to the console.\n"
            + "INFORMATION: If [-v] and [-o] are used, the details will be displayed in console, but will not be written in the file.\n"
            + "\t-h key:value\tAssociates headers to HTTP Request with the format 'key:value'.\n";

}
