package com.aesthetic.jduino;

import java.io.PipedInputStream;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.Vector;

public class BoardHandlerUI {
    private Scanner scanner;
    private BoardHandler boardHandler;
    private PipeListener pipeListener;
    private boolean isRunning;

    public BoardHandlerUI() {
        this.scanner = new Scanner(System.in);
        this.boardHandler = new BoardHandler();
        this.pipeListener = new PipeListener();
        this.isRunning = true;
        displayMenu();
    }

    class PipeListener extends Thread {
        private PipedInputStream pipeIn;
        private boolean pipeConnected;

        private PipeListener() {
            pipeConnected = false;
            pipeIn = new PipedInputStream();
            try {
                boardHandler.connectPipe(pipeIn);
            } catch (Exception ex) {
                return;
            }
            pipeConnected = true;
            start();
        }

        @Override
        public void run() {
            byte[] buffer;
            int bytesRead;

            while (pipeConnected)
                try {
                    if (pipeIn.available() > 0) {
                        buffer = new byte[pipeIn.available()];
                        bytesRead = pipeIn.read(buffer, 0, buffer.length);
                        String message = new String(buffer, 0, bytesRead);
                        System.out.println(message);
                    }
                } catch (Exception ex) {
                    pipeConnected = false;
                    Thread.currentThread().interrupt();
                    return;
                }
        }

        public void disconnect() {
            pipeConnected = false;
            try {
                pipeIn.close();
            } catch (Exception ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void connectBoard() {
        System.out.print("Enter COM port name for connection: ");
        String portName = scanner.nextLine();
        System.out.print("Enter handshake in message: ");
        String handshakeIn = scanner.nextLine();
        System.out.print("Enter handshake out message: ");
        String handshakeOut = scanner.nextLine();

        System.out.print("Enter baud rate: ");
        int baudRate = 9600;
        try {
            baudRate = scanner.nextInt();
        } catch (InputMismatchException ex) {
            System.out.println("Baud rate invalid. Using default (9600)");
        }
        scanner.nextLine();

        boardHandler.startListener(portName, baudRate, handshakeIn, handshakeOut);
    }

    private void closePort() {
        System.out.println("Select port: \n0. Back");
        Vector<String> openPorts = boardHandler.getOpenPorts();
        for (int i = 0; i < openPorts.size(); i++)
            System.out.println(i+1 + ". " + openPorts.get(i));
        int port = 0;
        String portName = null;
        try {
            port = scanner.nextInt()-1;
            if (port == -1) displayMenu();
            portName = openPorts.get(port);
        } catch (InputMismatchException | ArrayIndexOutOfBoundsException ex) {
            System.out.println("Incorrect port number.");
            closePort();
        }
        scanner.nextLine();

        boardHandler.closeListener(portName);
    }

    private void sendMessage() {
        System.out.println("Select port: \n0. Back");
        Vector<String> connectedPorts = boardHandler.getOpenPorts();
        for (int i = 0; i < connectedPorts.size(); i++)
            System.out.println(i+1 + ". " + connectedPorts.get(i));
        int port = 0;
        String portName = null;
        try {
            port = scanner.nextInt()-1;
            if (port == -1) displayMenu();
            portName = connectedPorts.get(port);
        } catch (InputMismatchException | ArrayIndexOutOfBoundsException ex) {
            System.out.println("Incorrect port number. ");
            sendMessage();
        }
        scanner.nextLine();

        System.out.print("Enter your message: ");
        String message = scanner.nextLine();
        byte[] messageBytes = message.getBytes();

        boardHandler.send(portName, messageBytes);
    }

    public void displayMenu() {
        while (isRunning) {
            System.out.println("\nMain Menu");
            System.out.println("1. Connect board");
            System.out.println("2. Close port");
            System.out.println("3. Send message");
            System.out.println("4. Exit");
            System.out.print("Select an option: \n");

            int choice;
            try {
                choice = scanner.nextInt();
            } catch (InputMismatchException ex) {
                choice = -1;
            }
            scanner.nextLine();

            switch (choice) {
                case 1:
                    connectBoard();
                    break;
                case 2:
                    closePort();
                    break;
                case 3:
                    sendMessage();
                    break;
                case 4:
                    System.out.println("Exiting...");
                    System.exit(0);
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    public static void main(String[] args) {
        BoardHandlerUI ui = new BoardHandlerUI();
    }
}