package com.abhaythmaster.masternotes.screenshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScreenshotMeta {

    public String filename  = "";
    public String serverIp  = "";
    public String worldName = "";
    public long   timestamp = 0L;
    public int    customOrder = 0;
    public boolean favorite   = false;
    public String caption     = "";
    public List<String> tags  = new ArrayList<>();

    public ScreenshotMeta() {}

    /** Human-readable source string shown below the thumbnail */
    public String getDisplaySource() {
        if (worldName != null && !worldName.isEmpty()) return "World: " + worldName;
        if (serverIp  != null && !serverIp.isEmpty())  return serverIp;
        return "Unknown";
    }

    public String getDateString() {
        if (timestamp <= 0) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(timestamp));
    }
}
