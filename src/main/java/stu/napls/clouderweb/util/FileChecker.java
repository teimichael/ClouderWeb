package stu.napls.clouderweb.util;

import stu.napls.clouderweb.core.dictionary.ItemType;

import java.util.HashMap;
import java.util.Map;

public class FileChecker {

    private static Map<String, Integer> suffix2TypeMap;

    static {
        suffix2TypeMap = new HashMap<>();
        // image
        suffix2TypeMap.put("jpg", ItemType.IMAGE);
        suffix2TypeMap.put("png", ItemType.IMAGE);
        suffix2TypeMap.put("gif", ItemType.IMAGE);
        suffix2TypeMap.put("ico", ItemType.IMAGE);

        // video
        suffix2TypeMap.put("mp4", ItemType.VIDEO);
        suffix2TypeMap.put("mpeg", ItemType.VIDEO);
        suffix2TypeMap.put("mpg", ItemType.VIDEO);
        suffix2TypeMap.put("mov", ItemType.VIDEO);
        suffix2TypeMap.put("flv", ItemType.VIDEO);
        suffix2TypeMap.put("avi", ItemType.VIDEO);

        // audio
        suffix2TypeMap.put("mp3", ItemType.AUDIO);
        suffix2TypeMap.put("wma", ItemType.AUDIO);
        suffix2TypeMap.put("wav", ItemType.AUDIO);

        // document
        suffix2TypeMap.put("pdf", ItemType.DOCUMENT);
        suffix2TypeMap.put("doc", ItemType.DOCUMENT);
        suffix2TypeMap.put("docx", ItemType.DOCUMENT);
        suffix2TypeMap.put("txt", ItemType.DOCUMENT);

        // compressed
        suffix2TypeMap.put("rar", ItemType.COMPRESSED);
        suffix2TypeMap.put("zip", ItemType.COMPRESSED);

    }

    public static int getItemType(String suffix) {
        Integer itemType = suffix2TypeMap.get(suffix);
        return itemType != null ? itemType : ItemType.NO_TYPE;
    }

}
