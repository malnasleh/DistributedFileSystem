package Client;

import Operator.RegistryOperations;
import Proxy.Proxy;
import Server.FileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.Instant;
import java.util.*;

public class Client {
    private static final Scanner myScanner = new Scanner(System.in);
    private static Map.Entry<String, Proxy> proxy;

    private static RegistryOperations operator;

    /**
     * Main method of client program. Handles setting proxy and user Input through menu based interface.
     *
     * @param args part of main java method. Not used in this case.
     */
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("192.168.183.70", 1099); //Put IP of machine hosting RMI here.
            operator = (RegistryOperations) registry.lookup("operator");
            proxy = getRandomProxy();
            if (proxy == null) {
                System.out.println("No proxies to connect to. Please try again.\n");
                return;
            }
            handleUserInput();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleUserInput() throws RemoteException {
        printMainMenu();
        while (true) {
            String input = myScanner.nextLine();
            if (input.equalsIgnoreCase("e") || input.equalsIgnoreCase("q")) {
                break;
            } else if (input.equalsIgnoreCase("d")) {
                getUserInputAndDownload();
            } else if (input.equalsIgnoreCase("u")) {
                getUserInputAndUpload();
            } else {
                System.out.println("That is not a valid input. Try again\n");
            }
            printMainMenu();
        }
    }

    /**
     * Gets the user input for file path to upload. Calls another method that handles actual uploading
     *
     * @throws RemoteException Required as part of java RMI. Used to catch failures for fault tolerance.
     */
    private static void getUserInputAndUpload() throws RemoteException {
        System.out.println("Please enter the full path of the file you would like to upload:");
        String filePathString = myScanner.nextLine();
        while (filePathString.isEmpty()) {
            filePathString = myScanner.nextLine();
        }
        System.out.println("\nUpload started.");
        performUpload(filePathString);
    }

    /**
     * Handles reading local file and calling proxy method for uploading file.
     *
     * @param filePath The absolute file path for the file to be uploaded. gotten from the user
     * @throws RemoteException Required as part of java RMI. Used to catch failures for fault tolerance.
     */
    private static void performUpload(String filePath) throws RemoteException {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                byte[] data = Files.readAllBytes(path);
                if (proxy == null) {
                    proxy = getRandomProxy();
                    if (proxy == null) {
                        System.out.println("No proxies available to fulfill your request. Please connect a proxy and try again.");
                        return;
                    }
                }
                FileData.StatusCodeEnum writeStatus = proxy.getValue().forwardWrite(path.getFileName().toString(), data, Instant.now()).status();
                if (writeStatus == FileData.StatusCodeEnum.NO_SERVERS) {
                    System.out.println("There are no servers to complete your request");
                } else if (writeStatus == FileData.StatusCodeEnum.OTHER) {
                    System.out.println("Upload failed, Please double check your file path");
                } else if (writeStatus == FileData.StatusCodeEnum.OVERWRITTEN) {
                    System.out.println("Your upload came in late so it was overwritten.");
                } else {
                    System.out.println("Upload completed through " + proxy.getKey() + "\n");
                }
            } else {
                System.out.println("\nThat is not a valid path please try again.\n");
            }
        } catch (RemoteException e) {
            operator.unBindRemoteObject(proxy.getKey());
            proxy = getRandomProxy();
            performUpload(filePath);
        } catch (IOException e) {
            System.out.println("Exception thrown while performing the write operation\n" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the user input for file path to download. Calls another method that handles actual downloading.
     *
     * @throws RemoteException Required as part of java RMI. Used to catch failures for fault tolerance.
     */
    private static void getUserInputAndDownload() throws RemoteException {
        System.out.println("Please enter the name (/w extension) of the file you would like to download:");
        String fileName = myScanner.nextLine();
        while (fileName.isEmpty()) {
            fileName = myScanner.nextLine();
        }
        System.out.println("\nDownload Started");
        performDownload(fileName);
    }

    /**
     * Handles calling proxy object download file method. Interprets return results.
     *
     * @param fileName The name of the file with the extension. gotten from the user
     * @throws RemoteException Required as part of java RMI. Used to catch failures for fault tolerance.
     */
    private static void performDownload(String fileName) throws RemoteException {
        try {
            if (proxy == null) {
                proxy = getRandomProxy();
                if (proxy == null) {
                    System.out.println("No proxies available to fulfill your request. Please connect a proxy and try again.\n");
                    return;
                }
            }
            FileData downloadedData = proxy.getValue().forwardRead(fileName);
            Path directoryPath = Paths.get("downloadedFiles");
            if (Files.notExists(directoryPath)) {
                Files.createDirectory(directoryPath);
            }
            if (downloadedData.fileData() != null) {
                Files.write(directoryPath.resolve(downloadedData.fileName()), downloadedData.fileData());
                System.out.println("Download Complete from " + proxy.getKey() + "\n");
            } else if (downloadedData.status() == FileData.StatusCodeEnum.NO_SERVERS) {
                System.out.println("There are no servers to complete your request");
            } else if (downloadedData.status() == FileData.StatusCodeEnum.OTHER) {
                System.out.println("Download failed, Please double check the file name.");
            }
        } catch (RemoteException e) {
            operator.unBindRemoteObject(proxy.getKey());
            proxy = getRandomProxy();
            performDownload(fileName);
        } catch (IOException e) {
            System.out.println("Exception thrown while performing the read operation\n" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints the main menu for the user to use our system.
     */
    private static void printMainMenu() {
        System.out.println("Welcome to your distributed file system. Please select one of the following options:");
        System.out.println("\t- Download a file (D)");
        System.out.println("\t- Upload a file (U)");
        System.out.println("\t- Exit (E)");
    }

    /**
     * This gets a random proxy to connect to from the proxies in the RMI registry.
     *
     * @return returns a proxy object along with its name in order to use for the operation.
     */
    private static Map.Entry<String, Proxy> getRandomProxy() {
        ArrayList<Map.Entry<String, Proxy>> proxies;
        try {
            proxies = operator.getAllProxies();
            if (proxies.isEmpty()) return null;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        Collections.shuffle(proxies);
        return proxies.get(0);
    }
}
