package com.aesthetic.jduino;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

public class BoardHandler {
    private Vector<PortHandler> portHandlers;
    private PipedOutputStream pipeOut;
    private boolean pipeConnected;

    public BoardHandler() {
        portHandlers = new Vector<PortHandler>();
        pipeOut = new PipedOutputStream();
        pipeConnected = false;
    }

    private class PortHandler extends Thread {
        private SerialPort comPort;
        private int baudRate;
        private boolean isListening;
        private boolean boardConnected;

        private String handshakeIn;
        private String handshakeOut;

        private PortHandler(String comPortIdent, int baudRate, String handshakeIn, String handshakeOut) {
            this.baudRate = baudRate;
            this.isListening = false;
            this.boardConnected = false;
            this.handshakeIn = handshakeIn;
            this.handshakeOut = handshakeOut;
            try {
                comPort = SerialPort.getCommPort(comPortIdent);
                comPort.setBaudRate(baudRate);
                if (!comPort.openPort()) {
                    pipeOut("[Port Handler] Port " + comPort.getDescriptivePortName() + " is already open or inaccessible.");
                    return;
                }
                pipeOut("[Port Handler] Port " + comPort.getDescriptivePortName() + " opened. Listening for handshake.");
                isListening = true;
                start();
            } catch (Exception ex) {
                pipeOut("[Port Handler] Couldn't open port. " + ex);
            }
        }

        @Override
        public void run() {
            byte[] readBuffer = new byte[0];
            String encodedBuffer = "";

            while (isListening) {
                if (comPort.bytesAvailable() > 0) {
                    encodedBuffer = readBytesToString(readBuffer).trim();

                    if (!boardConnected) {
                        if (encodedBuffer.trim().equals(handshakeIn))
                            comPort.writeBytes(handshakeOut.getBytes(), handshakeOut.getBytes().length);
                        else if (encodedBuffer.equals("$CONNECTION_START")) {
                            pipeOut("[Port Handler] Board connected on port " + comPort.getDescriptivePortName() + ".");
                            boardConnected = true;
                        }
                    } else {
                        if (encodedBuffer.equals("$CONNECTION_END")) {
                            boardConnected = false;
                            pipeOut("[Port Handler] Board disconnected on port " + comPort.getDescriptivePortName() + ".");
                            break;
                        } else if (encodedBuffer.equals("$BYTES_START")) {
                            String blockBuffer = "";
                            boolean readingBytes = true;
                            while (readingBytes) {
                                encodedBuffer = readBytesToString(readBuffer).trim();
                                if (encodedBuffer.equals("$BYTES_END")) {
                                    readingBytes = false;
                                    break;
                                }
                                blockBuffer = blockBuffer.concat(encodedBuffer);
                            }

                            pipeOut("[" + comPort.getSystemPortName() + "] " + blockBuffer);

                            // Add any other handling before the last else in case of custom commands.

                        } else {
                            pipeOut("[" + comPort.getSystemPortName() + "] " + encodedBuffer);
                        }
                    }
                }
            }
        }
        private String readBytesToString(byte[] readBuffer) {
            if (comPort.bytesAvailable() > 0) {
                readBuffer = new byte[comPort.bytesAvailable()];
                comPort.readBytes(readBuffer, readBuffer.length);
                return new String(readBuffer, StandardCharsets.UTF_8).trim();
            }
            return "";
        }
        private void sendBytes(byte[] content) {
            comPort.writeBytes(content, content.length);
        }
        private void closeListener() {
            try {
                isListening = false;
                boardConnected = false;
                comPort.closePort();
                pipeOut("[Port Handler] Port " + comPort.getDescriptivePortName() + " closed.");
            } catch (Exception ex) {
                pipeOut("[Port Handler] Couldn't close port. " + ex);
            }
        }
    }

    public Vector<String> getOpenPorts() {
        Vector<String> openPorts = new Vector<String>();
        for (PortHandler listener : portHandlers)
            openPorts.add(listener.comPort.getSystemPortName());
        return openPorts;
    }

    public void startListener(String comPort, int baudRate, String handshakeIn, String handshakeOut) {
        portHandlers.add(new PortHandler(comPort, baudRate, handshakeIn, handshakeOut));
    }
    public void send(String comPort, Object content) {
        if (content instanceof byte[]) {
            for (PortHandler listener : portHandlers)
                if (listener.comPort.getSystemPortName().equals(comPort) || listener.boardConnected)
                    listener.sendBytes((byte[]) content);
        }
    }
    public void closeListener(String comPort) {
        PortHandler listener;
        for (int i = 0; i < portHandlers.size(); i++) {
            listener = portHandlers.get(i);
            if (listener.comPort.getSystemPortName().equals(comPort)) listener.closeListener();
            portHandlers.remove(listener);
        }
    }
    public void close() {
        PortHandler listener;
        for (int i = 0; i < portHandlers.size(); i++) {
            listener = portHandlers.get(i);
            listener.closeListener();
            portHandlers.clear();
        }
    }

    public boolean connectPipe(PipedInputStream pipeIn) {
        try {
            pipeOut.connect(pipeIn);
            pipeConnected = true;
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
    public void pipeOut(String message) {
        if (pipeConnected)
            try {
                pipeOut.write(message.getBytes("UTF-8"));
                pipeOut.flush();
            } catch (IOException ex) {
                pipeConnected = false;
            }
    }
}
