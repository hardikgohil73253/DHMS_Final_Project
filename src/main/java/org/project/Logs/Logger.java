package org.project.Logs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.project.utils.VariableStore.*;

import static org.project.utils.VariableStore.LOG_TYPE_CLIENT;
import static org.project.utils.VariableStore.LOG_TYPE_SERVER;

public class Logger {

    public static void log(int logType, String serverOrClientID, String... logDetails) throws IOException {
        String fileName = getFileName(logType, serverOrClientID);
        FileWriter fileWriter = new FileWriter(fileName, true);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("DATE: " + getFormattedDate() + " " + String.join(" | ", logDetails));
        printWriter.close();
    }

    public static boolean deleteLogFile(String ID) {
        boolean isSuccess = false;
        try {

            String fileName = getFileName(LOG_TYPE_CLIENT, ID);
            File file = new File(fileName);
            isSuccess = file.delete();
        } catch (Exception e) {
            isSuccess = false;
            throw new RuntimeException(e);
        } finally {
            return isSuccess;
        }
    }

    private static String getFileName(int logType, String ID) {
        final String serverPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "Logs" + File.separator + "Server";
        final String clientPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "Logs" + File.separator + "Client";
        String fileName = "";
        if (logType == LOG_TYPE_SERVER) {
            fileName = serverPath + File.separator;
            if (ID.equalsIgnoreCase(SERVERS.MTL.toString())) {
                fileName += "Montreal.txt";
            } else if (ID.equalsIgnoreCase(SERVERS.SHE.toString())) {
                fileName += "Sherbrooke.txt";
            } else if (ID.equalsIgnoreCase(SERVERS.QUE.toString())) {
                fileName += "Quebec.txt";
            }
        } else {
            fileName = clientPath + File.separator;
            fileName += ID + ".txt";
        }
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileName;
    }

    private static String getFormattedDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
        return dateFormat.format(new Date());
    }

}
