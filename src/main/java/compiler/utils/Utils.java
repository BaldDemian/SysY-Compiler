package compiler.utils;

import java.util.HashMap;

public class Utils {
    static HashMap<String, Integer> sgrMap = new HashMap<>();
    static {
        // 初始化srgMap
        sgrMap.put("White", 97);
        sgrMap.put("LightCyan", 96);
        sgrMap.put("LightMagenta", 95);
        sgrMap.put("LightBlue", 94);
        sgrMap.put("LightYellow", 93);
        sgrMap.put("LightGreen", 92);
        sgrMap.put("LightRed", 91);
        sgrMap.put("Magenta", 35);
        sgrMap.put("Underlined", 4);
        sgrMap.put("Reset", 0);
        sgrMap.put("ResetFore", 39);
    }
    // 根据给定的颜色名或者样式名返回转义序列，形如: \e[Xm
    public static String getSGR(String s) {
        return "\u001B[" + sgrMap.get(s) + "m";
    }
}
