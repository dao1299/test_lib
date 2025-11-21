package com.vtnet.netat.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateEdgeHelper {
    private static final Logger log = LoggerFactory.getLogger(UpdateEdgeHelper.class);
    private final String PATH_PROJECT = System.getProperty("user.dir");

    /**
     * Gets the full version of the currently installed Microsoft Edge.
     * @return The version string of Edge.
     */
    public String getCurrentEdgeVersion() {
        // Command to get the Edge version
        String versionEdgeCommand = "wmic datafile where name=\"C:\\\\Program Files (x86)\\\\Microsoft\\\\Edge\\\\Application\\\\msedge.exe\" get Version /value";
        try {
            Process process = Runtime.getRuntime().exec(versionEdgeCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Version=")) {
                    return line.substring(8);
                }
            }
        } catch (IOException e) {
            log.error("Could not retrieve Microsoft Edge version.", e);
            return null;
        }
        return null;
    }

    /**
     * Deletes the downloaded zip file.
     * @param pathEdgeDriverZipFile Path to the downloaded zip file.
     */
    private void deleteDownloadedFile(String pathEdgeDriverZipFile) {
        File edgeDriverZipFile = new File(pathEdgeDriverZipFile);
        if (edgeDriverZipFile.exists()) {
            edgeDriverZipFile.delete();
        }
    }

    /**
     * Checks if the Edge driver for the corresponding version already exists.
     * @param edgeDriverVersion The version of the Edge driver.
     * @return true if it exists, false otherwise.
     */
    private boolean checkExistingEdgeDriver(String edgeDriverVersion) {
        File edgeDriverFile = new File(Paths.get(PATH_PROJECT, "driver", "msedgedriver" + edgeDriverVersion + ".exe").toString());
        return edgeDriverFile.exists();
    }

    /**
     * Unzips the msedgedriver.exe file from the downloaded zip archive.
     * @param zipFilePath Path to the zip file.
     * @param edgeDriverVersion The version of the Edge driver.
     */
    private void unzipCurrentEdgeDriverVersion(String zipFilePath, String edgeDriverVersion) {
        File parentDir = new File(Paths.get(PATH_PROJECT, "driver").toString());
        if (!parentDir.exists()) parentDir.mkdirs();

        String filePath = Paths.get(parentDir.getAbsolutePath(), "msedgedriver" + edgeDriverVersion + ".exe").toString();

        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zipInputStream = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith("msedgedriver.exe")) {
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    zipInputStream.closeEntry();
                    break; // Found and unzipped, exit the loop
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            log.error("Error while unzipping EdgeDriver file", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads the zip file of the current Edge driver version.
     * @param urlEdgeDriverVersion URL to download from.
     * @return Path to the downloaded zip file.
     */
    private String downloadCurrentEdgeDriverVersionZipFile(String urlEdgeDriverVersion) {
        String proxyHost = ConfigReader.getProperty("proxy.host","10.207.163.162");
        String proxyPort = ConfigReader.getProperty("proxy.port","3128");
        String proxyString = "";

        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort != null && !proxyPort.isEmpty()) {
            proxyString = String.format("-x %s:%s --proxy-ntlm", proxyHost, proxyPort);
        }

        String zipFilePath = Paths.get(PATH_PROJECT, "msedgedriver.zip").toString();
        String command = String.format("curl %s --ssl-no-revoke -L -o %s %s", // Added -L to follow redirects
                proxyString, zipFilePath, urlEdgeDriverVersion);
        try {
            Process process = Runtime.getRuntime().exec(command);
            int code = process.waitFor();
            if (code == 0) {
                log.info("EdgeDriver zip file downloaded successfully.");
                return zipFilePath;
            } else {
                log.error("Error downloading EdgeDriver zip file. Exit code: {}", code);
                return null;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error downloading EdgeDriver: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes old, unused versions of the Edge driver.
     */
    public void deleteOldEdgeDriver() {
        File folder = new File(Paths.get(PATH_PROJECT, "driver").toString());
        if (folder.isDirectory()) {
            File[] listFiles = folder.listFiles();
            if (listFiles != null) {
                for (File file : listFiles) {
                    if (file.isFile() && file.getName().startsWith("msedgedriver")) {
                        file.delete();
                    }
                }
            }
        }
    }

    /**
     * The main method to automatically update the Edge driver.
     * @return The version of the current Edge driver.
     */
    public String updateAutomaticallyEdgeDriver() {
        String edgeVersion = getCurrentEdgeVersion();
        if (edgeVersion != null) {
            if (!checkExistingEdgeDriver(edgeVersion)) {
                log.info("EdgeDriver version {} does not exist. Starting download process...", edgeVersion);

                String urlEdgeDriverVersion = "https://msedgedriver.microsoft.com/" + edgeVersion + "/edgedriver_win64.zip";

                String pathEdgeDriverZipFile = downloadCurrentEdgeDriverVersionZipFile(urlEdgeDriverVersion);
                if (pathEdgeDriverZipFile != null) {
                    deleteOldEdgeDriver();
                    unzipCurrentEdgeDriverVersion(pathEdgeDriverZipFile, edgeVersion);
                    deleteDownloadedFile(pathEdgeDriverZipFile);
                }
            } else {
                log.info("EdgeDriver version {} already exists!", edgeVersion);
            }
        }else{
            log.info("Cannot get edge version! Error updateAutomaticallyEdgeDriver");
            return null;
        }
        return edgeVersion;
    }
}