package org.wst;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.openoffice.org/bibliographic/bibtex-defs.html
public abstract class FormatChecker {
    public final static String[] types = {"article", "book", "booklet", "conference", "inbook", "incollection", "inproceedings",
            "manual", "mastersthesis", "misc", "phdthesis", "proceedings", "techreport", "unpublished",};

    public static String basicBibTeXCheck(String raw) {
        String re = "[@](?s).*";

        Pattern pt = Pattern.compile(re);
        Matcher mt = pt.matcher(raw);
        String firstEntry = "invalid";
        if (mt.find()) { // will check for the first entry matching above regex
            firstEntry = mt.group(0);
            String type = firstEntry.substring(firstEntry.indexOf("@") + 1, firstEntry.indexOf("{"));
            if (Arrays.asList(types).contains(type)){
                return firstEntry;
            }else{
                return "invalid";
            }
        }


        return firstEntry;
    }
}
