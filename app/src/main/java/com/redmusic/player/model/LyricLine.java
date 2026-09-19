package com.redmusic.player.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricLine {
    public long timeMs;
    public String text;

    public LyricLine(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text;
    }

    /** 解析 LRC 文本 */
    public static List<LyricLine> parse(String lrc) {
        List<LyricLine> lines = new ArrayList<>();
        if (lrc == null) return lines;
        Pattern timePattern = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?\\]");
        for (String raw : lrc.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher m = timePattern.matcher(line);
            List<Long> times = new ArrayList<>();
            while (m.find()) {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                int ms = 0;
                if (m.group(3) != null) {
                    String frac = m.group(3);
                    if (frac.length() == 1) ms = Integer.parseInt(frac) * 100;
                    else if (frac.length() == 2) ms = Integer.parseInt(frac) * 10;
                    else ms = Integer.parseInt(frac);
                }
                times.add(min * 60000L + sec * 1000L + ms);
            }
            if (times.isEmpty()) continue;
            String text = timePattern.matcher(line).replaceAll("").trim();
            if (text.isEmpty()) continue;
            for (long t : times) {
                lines.add(new LyricLine(t, text));
            }
        }
        Collections.sort(lines, (a, b) -> Long.compare(a.timeMs, b.timeMs));
        return lines;
    }
}
