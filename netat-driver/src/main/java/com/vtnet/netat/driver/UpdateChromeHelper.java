package com.vtnet.netat.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateChromeHelper {
    private static final Logger log = LoggerFactory.getLogger(LocalDriverFactory.class);
    private final String PATH_PROJECT = System.getProperty("user.dir");
    /**
     * This method aims to get the full version of Chrome currently
     *
     * @return Version of Chrome currently
     */
    public String getCurrentChromeDriverVersion() {
        // Chrome version retrieval command
        String versionChromeCommand = "wmic datafile where name=\"C:\\\\Program Files\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe\" get Version /value";
        try {
            //Run command
            Process process = Runtime.getRuntime().exec(versionChromeCommand);

            //Read result
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // find line have "Version"
                if (line.contains("Version=")) {
                    //The result returned from the command will be in the form "Version=xxx.xx.xx" -> use method substring from 8th to the last dot
                    return line.substring(8);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * This method aims to delete all of downloaded file when update chrome driver
     *
     * @param pathChromeDriverZipFile     path to the downloaded chrome driver archive
     * @param nameListChromeDriverVersion path to file list of chrome driver versions
     */
    private void deleteDownloadedFile(String pathChromeDriverZipFile, String nameListChromeDriverVersion) {
        File chromeDriverZipFile = new File(pathChromeDriverZipFile);
        if (chromeDriverZipFile.exists()) chromeDriverZipFile.delete();
        File nameListChromeDriverVersionFile = new File(nameListChromeDriverVersion);
        if (nameListChromeDriverVersionFile.exists()) nameListChromeDriverVersionFile.delete();
    }

    /**
     * This method is to check if the chrome driver exists or not
     *
     * @param chromeDriverVersion path to chrome driver file
     * @return True: ChromeDriver already exists
     * False: ChromeDriver does not exist yet
     */
    private boolean checkExistingChromeDriver(String chromeDriverVersion) {
        // The path to the chromedriver file will look like: ./driver/chromedriverXXX.exe (XXX is version of chromedriver)
        File chromeDriverFile = new File(PATH_PROJECT + "/driver/chromedriver" + chromeDriverVersion + ".exe");
        return chromeDriverFile.exists();
    }

    /**
     * This method is to extract the current chrome driver that has just been downloaded
     *
     * @param zipFilePath:         path to chrome driver zip file
     * @param chromeDriverVersion: version of current chrome driver
     */
//    private void unzipCurrentChromeDriverVersion(String zipFilePath, String chromeDriverVersion) {
//        FileInputStream fis;
//        try {
//            fis = new FileInputStream(zipFilePath);
//            ZipInputStream zipInputStream = new ZipInputStream(fis);
//            ZipEntry entry;
//            while ((entry = zipInputStream.getNextEntry()) != null) {
//                String entryName = entry.getName();
//                if (entryName.contains(".exe")) {
//                    System.out.println(entryName);
//                    File parentDir = new File(PATH_PROJECT + "/driver");
//                    // Check the existence of the driver folder, if it does not exist yet, a new folder will be created
//                    if (!parentDir.exists()) parentDir.mkdir();
//
//                    String filePath = PATH_PROJECT + "/driver/chromedriver" + chromeDriverVersion + ".exe";
//                    if (!entry.isDirectory()) {
//                        FileOutputStream fos = new FileOutputStream(filePath);
//                        byte[] buffer = new byte[1024];
//                        int bytesRead;
//                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
//                            fos.write(buffer, 0, bytesRead);
//                        }
//                        fos.close();
//                    }
//                }
//                zipInputStream.closeEntry();
//            }
//            fis.close();
//            zipInputStream.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private void unzipCurrentChromeDriverVersion(String zipFilePath, String chromeDriverVersion) {
        File parentDir = new File(Paths.get(PATH_PROJECT, "driver").toString());
        if (!parentDir.exists()) parentDir.mkdirs();

        String filePath = Paths.get(parentDir.getAbsolutePath(), "chromedriver" + chromeDriverVersion + ".exe").toString();

        // Sử dụng try-with-resources để tự động đóng các stream
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zipInputStream = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith("chromedriver.exe")) {
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    zipInputStream.closeEntry();
                    break; // Đã tìm thấy và giải nén xong, thoát vòng lặp
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            log.error("Lỗi khi giải nén file ChromeDriver", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is responsible for downloading the current chrome driver version zip file
     *
     * @param urlChromeDriverVersion: URL to download chrome driver
     * @param chromeDriverVersion:    version of chrome driver
     * @return Path to the downloaded zip file
     */
    private String downloadCurrentChromeDriverVersionZipFile(String urlChromeDriverVersion, String chromeDriverVersion) {
        String proxyHost = ConfigReader.getProperty("proxy.host","10.207.163.162");
        String proxyPort = ConfigReader.getProperty("proxy.port","3128");
        String proxyString = "";

        if (proxyHost != null && !proxyHost.isEmpty()) {
            proxyString = String.format("-x %s:%s --proxy-ntlm", proxyHost, proxyPort);
        }

        String zipFilePath = Paths.get(PATH_PROJECT, "chromedriver.zip").toString();
        String command = String.format("curl %s --ssl-no-revoke -o %s %s",
                proxyString, zipFilePath, urlChromeDriverVersion);
        try {
            int code = Runtime.getRuntime().exec(command).waitFor();
            if (code == 0) {
                log.info("Download file zip success");
                return zipFilePath;
            } else {
                log.error("Error when download file zip chrome driver");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Lỗi khi download Chromedriver {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * This method is responsible for automatically deleting old unused versions
     */
    public void deleteOldChromeDriver() {
        String pathDriverFolder = PATH_PROJECT + "\\driver";
        File folder = new File(pathDriverFolder);

        if (folder.isDirectory()) {
            File[] listFiles = folder.listFiles();
            if (listFiles != null) {
                for (File tep : listFiles) {
                    if (tep.isFile() && tep.getName().contains("chromedriver")) tep.delete();
                }
            }
        }
    }

    /**
     * This method is responsible for automatically update chrome driver
     *
     * @return Current version chromedriver
     */
    public String updateAutomaticallyChromeDriver() {
        String chromeDriverVersion = getCurrentChromeDriverVersion();
        if (chromeDriverVersion != null) {
            if (!checkExistingChromeDriver(chromeDriverVersion)) {
                String urlChromeDriverVersion = "https://storage.googleapis.com/chrome-for-testing-public/" + chromeDriverVersion + "/win64/chromedriver-win64.zip";
                String pathChromeDriverZipFile = downloadCurrentChromeDriverVersionZipFile(urlChromeDriverVersion, chromeDriverVersion);
                deleteOldChromeDriver();
                unzipCurrentChromeDriverVersion(pathChromeDriverZipFile, chromeDriverVersion);
                deleteDownloadedFile(pathChromeDriverZipFile, "");
            } else {
                System.out.println("Chrome driver existed!");
            }
        }
        return chromeDriverVersion;
    }
}
