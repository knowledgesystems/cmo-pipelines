import java.io.*;
import java.nio.charset.*;
import java.net.*;

class StreamSink extends Thread {
    private InputStream in;
    private OutputStream out = null;
    private int buffersize = 60 * 1024;
    private byte[] bytebuffer = null;

    public StreamSink(InputStream inStream, OutputStream outStream, int aBuffersize) {
        in = inStream;
        out = outStream;
        buffersize = aBuffersize;
    }

    public StreamSink(InputStream inStream, int aBuffersize) {
        in = inStream;
        buffersize = aBuffersize;
    }

    public StreamSink(InputStream inStream, OutputStream outStream) {
        in = inStream;
        out = outStream;
    }

    public StreamSink(InputStream inStream) {
        in = inStream;
    }

    public void transfer() throws IOException {
        bytebuffer = new byte[buffersize];
        if (out == null) {
            while (true) {
                int bytes_read = in.read(bytebuffer,0,buffersize);
                if (bytes_read == -1) return;
            }
        }
        while (true) {
            int bytes_read = in.read(bytebuffer,0,buffersize);
            if (bytes_read == -1) return;
            out.write(bytebuffer,0,bytes_read);
        }
    }

    public void run() {
        try {
            transfer();
        } catch (IOException e) {
        }
    }
}

public class JKTester {
    static final int RETURN_CODE_TEST_SUCCESSFUL = 0;
    static final int RETURN_CODE_TEST_FAILED = 1;
    static final int RETURN_CODE_TEST_NOT_POSSIBLE_BAD_HOST = 2;
    static final int RETURN_CODE_TEST_NOT_POSSIBLE = 3;
    static final int RETURN_CODE_TEST_NOT_POSSIBLE_BAD_PORT = 4;
    static final byte[] cPingPacket = {0x12, 0x34, 0x00, 0x01, 0x0a};
    static final byte[] cPongPacket = {0x41, 0x42, 0x00, 0x01, 0x09};
    InetAddress ajpHost;
    int ajpPort;
    String tomcatLabel;
    int pingTimeout;
    String noticeEmailAddresses;
    boolean debugMode;
    String exceptionMessage;

    JKTester(InetAddress ajpHost, int ajpPort, String tomcatLabel, int pingTimeout, String noticeEmailAddresses, boolean debugMode) {
        this.ajpHost = ajpHost;
        this.ajpPort = ajpPort;
        this.tomcatLabel = tomcatLabel;
        this.pingTimeout = pingTimeout;
        this.noticeEmailAddresses = noticeEmailAddresses;
        this.debugMode = debugMode;
        this.exceptionMessage = null;
    }

    public void logDebugString(String s) {
        if (debugMode) System.err.print(s);
    }

    public void sendWarningEmail() {
        if (noticeEmailAddresses == null || noticeEmailAddresses.trim().length() == 0) return;
        String command = "mail -s 'tomcat_ajp_port_not_responding(" + tomcatLabel + ")'";
        if (exceptionMessage != null && exceptionMessage.trim().length() > 0) {
            command = command + "__" + exceptionMessage;
        }
        command = command + " " + noticeEmailAddresses.replace(',',' ');
        try {
            Process process = Runtime.getRuntime().exec(command);
            StreamSink stdoutSink = new StreamSink(process.getInputStream());
            StreamSink stderrSink = new StreamSink(process.getErrorStream());
            stdoutSink.start();
            stderrSink.start();
            OutputStreamWriter processIn = new OutputStreamWriter(process.getOutputStream(),Charset.forName("UTF-8").newEncoder());;
            String warningMessageBody = " a tomcat ajp port service did not respond in the expected time:\n" +
                    "\n    Host    : " + ajpHost.toString() +
                    "\n    Port    : " + Integer.toString(ajpPort) +
                    "\n    Tomcat  : " + tomcatLabel +
                    "\n    ms.max  : " + pingTimeout +
                    "\n\nA restart of the tomcat server may be necessary.\n";
            processIn.write(warningMessageBody, 0, warningMessageBody.length());
            processIn.write('\n');
            processIn.write('.');
            processIn.write('\n');
            processIn.flush();
            processIn.close();
            process.waitFor();
        } catch (InterruptedException e) {
        } catch (IOException e) {
        }
    }

    public boolean pingTestSuccess() {
        Socket probeSocket = new Socket();
        try {
            InetSocketAddress socketAddress = new InetSocketAddress(ajpHost, ajpPort);
            logDebugString("connecting..\n");
            probeSocket.connect(socketAddress, pingTimeout);
            OutputStream socketOut = probeSocket.getOutputStream();
            InputStream socketIn = probeSocket.getInputStream();
            probeSocket.setSoTimeout(pingTimeout);
            logDebugString("sending ping packet..\n");
            socketOut.write(cPingPacket);
            socketOut.flush();
            byte[] pongBuffer = new byte[256];
            int readCount = socketIn.read(pongBuffer);
            logDebugString("received " + Integer.toString(readCount) + "\n");
            logDebugString("comparing to pong..\n");
            boolean match = true;
            for (int i = 0; i < readCount && i < cPongPacket.length; i++) {
                if (cPongPacket[i] != pongBuffer[i]) {
                    logDebugString(Byte.toString(cPongPacket[i]));
                    logDebugString(" <--> ");
                    logDebugString(Byte.toString(pongBuffer[i]));
                    logDebugString("\n");
                    match = false;
                    break;
                }
            }
            if (match) {
                logDebugString("matched\n");
            } else {
                logDebugString("mismatched\n");
            }
            logDebugString("closing..\n");
            probeSocket.close();
            if (!match) return false;
        } catch (Throwable e) {
            exceptionMessage = e.getClass().getName();
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        // get configuration settings from environment
        final String DEFAULT_AJP_HOST = ""; //empty host = use localhost
        final int DEFAULT_AJP_PORT = 8009;
        final String DEFAULT_TOMCAT_LABEL = ""; //empty label = no label
        final int DEFAULT_PING_TIMEOUT = 2000;
        final String DEFAULT_NOTICE_EMAIL_ADDRESS = ""; //empty address = no mail sent
        final boolean DEFAULT_DEBUG_MODE = false;
        String ajpHostString = System.getProperty("tomcat.host");
        String ajpPortString = System.getProperty("tomcat.ajpport");
        String tomcatLabel = System.getProperty("tomcat.label");
        String pingTimeoutString = System.getProperty("ping.timeout");
        String noticeEmailAddresses = System.getProperty("notice.email");
        String debugModeString = System.getProperty("debug.mode");
        InetAddress ajpHost = null;
        try {
            if (ajpHostString == null || ajpHostString.trim().length() == 0 || ajpHostString.trim().equals("localhost")) {
                ajpHost = InetAddress.getLocalHost();
            } else {
                ajpHost = InetAddress.getByName(ajpHostString);
            }
        } catch (UnknownHostException e) {
            System.exit(RETURN_CODE_TEST_NOT_POSSIBLE_BAD_HOST);
        }
        int ajpPort = -1;
        try {
            if (ajpPortString == null || ajpPortString.trim().length() == 0 || ajpPortString.trim().equals("ajp13")) {
                ajpPort = DEFAULT_AJP_PORT;
            } else {
                ajpPort = Integer.parseInt(ajpPortString);
            }
            if (ajpPort < 0) {
                throw new NumberFormatException("Negative port number");
            }
        } catch (NumberFormatException e) {
            System.exit(RETURN_CODE_TEST_NOT_POSSIBLE_BAD_PORT);
        }
        if (tomcatLabel == null || tomcatLabel.trim().length() == 0) {
            tomcatLabel = DEFAULT_TOMCAT_LABEL;
        }
        int pingTimeout = -1;
        try {
            if (pingTimeoutString == null || pingTimeoutString.trim().length() == 0) {
                pingTimeout = DEFAULT_PING_TIMEOUT;
            } else {
                pingTimeout = Integer.parseInt(pingTimeoutString);
            }
            if (pingTimeout < 0) {
                pingTimeout = DEFAULT_PING_TIMEOUT;
            }
        } catch (NumberFormatException e) {
            pingTimeout = DEFAULT_PING_TIMEOUT;
        }
        if (noticeEmailAddresses == null || noticeEmailAddresses.trim().length() == 0) {
            noticeEmailAddresses = DEFAULT_NOTICE_EMAIL_ADDRESS;
        }
        boolean debugMode = DEFAULT_DEBUG_MODE;
        if (debugModeString != null && debugModeString.trim().length() > 0) {
            debugMode = debugModeString.trim().toLowerCase().equals("yes") ||
                    debugModeString.trim().toLowerCase().equals("y");
        }
        // create test object and run test
        JKTester tester = new JKTester(ajpHost, ajpPort, tomcatLabel, pingTimeout, noticeEmailAddresses, debugMode);
        if (tester.pingTestSuccess()) System.exit(RETURN_CODE_TEST_SUCCESSFUL);
        tester.sendWarningEmail();
        System.exit(RETURN_CODE_TEST_FAILED);
    }
}
