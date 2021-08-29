package com.urrecliner.blackbox;

import static com.urrecliner.blackbox.Vars.ChronoLog;
import static com.urrecliner.blackbox.Vars.utils;

import java.util.ArrayList;

class ShowKmLogs {
    void show(ArrayList<ChronoLog> logs){
        if (logs.size() == 0)
            return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < logs.size(); i++) {
            Vars.ChronoLog log = logs.get(i);
            sb.append((i%2 == 0) ? "\n":", ");
            sb.append(log.chroDate).append(" = ")
                    .append((log.todayKilo>9)?log.todayKilo:"0"+log.todayKilo).append("Km");
        }
        utils.logBoth("Kilo Log", sb.toString());
    }

}
