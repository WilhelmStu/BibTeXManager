package org.wst.helper;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.openoffice.org/bibliographic/bibtex-defs.html
// https://www.verbosus.com/bibtex-style-examples.html
public abstract class FormatChecker {
    public final static String[] types = {"article", "book", "booklet", "conference", "inbook", "incollection", "inproceedings",
            "manual", "mastersthesis", "misc", "phdthesis", "proceedings", "techreport", "unpublished"};

    public final static String[] fields = {"address", "annote", "author", "booktitle", "chapter", "crossref", "edition",
            "editor", "howpublished", "institution", "journal", "key", "month", "note", "number", "organization",
            "pages", "publisher", "school", "series", "title", "type", "volume", "year",};

    // todo: Advanced check for required/optional fields for each type above...
    // todo? second check for field validity...
    // todo? allow multiple entries?

    /**
     * @param raw input from system clipboard, that can contain a BibTeX entry
     * @return "invalid" if no BibTeX entry is found, else the first found valid entry
     */
    public static String basicBibTeXCheck(String raw) {

        String re = "[@]\\w{4,}\\s*";
        re += "[{]\\s*((?s)[\\w-\\s])*[,]";
        re += "[^@]+[}]";

        Pattern pt = Pattern.compile(re);
        Matcher mt = pt.matcher(raw);
        String firstEntry = "invalid";
        if (mt.find()) { // will check for the first entry matching above regex
            firstEntry = mt.group(0);
            String type = firstEntry.substring(firstEntry.indexOf("@") + 1, firstEntry.indexOf("{")).trim();
            if (Arrays.asList(types).contains(type.toLowerCase())) {
                return firstEntry;
            } else {
                return "invalid";
            }
        }
        return firstEntry;
    }

    // todo add arrangement to config

    /**
     * Will return the first BibEntry-Head in a given string or "invalid" if none is found
     * @param line a string that can contain a BibEntry-Head
     * @return first BibEntry-Head in the form "TYPE, keyword"
     */
    public static String getBibEntryHead(String line) {
        String re = "[@]\\w{4,}\\s*";
        re += "[{]\\s*((?s)[\\w-\\s])*[,]";

        Pattern pt = Pattern.compile(re);
        Matcher mt = pt.matcher(line);
        if (mt.find()) {
            String entryHead = mt.group(0);
            String type = entryHead.substring(entryHead.indexOf("@") + 1, entryHead.indexOf("{")).trim();
            if (Arrays.asList(types).contains(type.toLowerCase())) {
                String tmp = type.toUpperCase() + ", "; // todo make this configurable
                tmp += entryHead.substring(entryHead.indexOf("{") + 1, entryHead.indexOf(",")).trim();
                return tmp;
            }
        }
        return "invalid";
    }
}
