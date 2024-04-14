package org.project.replica3;

import org.project.utils.VariableStore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Logger {

    public static void log(int logType, String serverOrClientID, String... logDetails) throws IOException {
        String fileName = getFileName(logType, serverOrClientID);
        FileWriter fileWriter = new FileWriter(fileName, true);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.println("DATE: " + getFormattedDate() + " " + String.join(" | ", logDetails));
        printWriter.close();
    }

    private static String getFileName(int logType, String ID) {
        final String serverPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "Logs" + File.separator + "Replica3_Server";
        final String clientPath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "Logs" + File.separator + "Replica3_Client";
        String fileName = "";
        if (logType == VariableStore.LOG_TYPE_SERVER) {
            fileName = serverPath + File.separator;
            if (ID.equalsIgnoreCase(VariableStore.SERVERS.MTL.toString())) {
                fileName += "Replica3_Montreal.txt";
            } else if (ID.equalsIgnoreCase(VariableStore.SERVERS.SHE.toString())) {
                fileName += "Replica3_Sherbrooke.txt";
            } else if (ID.equalsIgnoreCase(VariableStore.SERVERS.QUE.toString())) {
                fileName += "Replica3_Quebec.txt";
            }
        } else {
            fileName = clientPath + File.separator;
            fileName += "Replica3_"+ID + ".txt";
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
