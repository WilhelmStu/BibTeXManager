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

    // NOTE: ^,{}\()%"'#~= not allowed in keyword, but '@' is allowed
    public final static String keywordRegEx = "[{][^,{}\\\\()%\"'#~=]*[,]";

    // todo? Advanced check for required/optional fields for each type above...
    // todo? second check for field validity...
    // todo? allow multiple entries?

    /**
     * @param raw input from system clipboard, that can contain a BibTeX entry
     * @return "invalid" if no BibTeX entry is found, else the first found valid entry
     */
    public static String basicBibTeXCheck(String raw) {

        String re = "[@]\\w{4,}\\s*";
        re += keywordRegEx;
        re += "[^@]+[}]";

        Pattern pt = Pattern.compile(re);
        Matcher mt = pt.matcher(raw);
        String firstEntry = "invalid";
        if (mt.find()) { // will check for the first entry matching above regex
            firstEntry = mt.group(0);
            String type = firstEntry.substring(firstEntry.indexOf("@") + 1, firstEntry.indexOf("{")).trim();
            if (Arrays.asList(types).contains(type.toLowerCase())) {
                //firstEntry = replaceQuotationMarks(firstEntry);
                firstEntry += "\r\n";
                return firstEntry;
            } else {
                return "invalid";
            }
        }
        return firstEntry;
    }

    // https://www.logicbig.com/tutorials/core-java-tutorial/java-regular-expressions/regex-lookahead.html
    // todo add config for this!

    /**
     * Will take a valid bib entry and replace the "" for each value after an tag
     * e.g.: title = "this is a title" -> title = {this is a title}
     * but e.g.: year = 2002 wont be changed, but year = "2002" will get year = {2002}
     * and e.g.: title = "{}" will become {{}}
     * <p>
     * Note: this might remove a single ',' if it is the last value of an entry (very unlikely)
     *
     * @param entry valid bib entry
     * @return same as input but "" -> {}
     */
    public static String replaceQuotationMarks(String entry) {
        String[] lines = entry.split("([,])(?=\\s*\\w+\\s*[=])");
        StringBuilder builder = new StringBuilder();

        builder.append(lines[0]).append(",");
        for (int i = 1; i < lines.length; i++) {
            String s = lines[i];
            String[] tagAndValue = s.split("(?<==)", 2);
            if (i == lines.length - 1) {
                tagAndValue[1] = tagAndValue[1].substring(0, tagAndValue[1].length() - 1).trim();
                if (tagAndValue[1].lastIndexOf(",") == tagAndValue[1].length() - 1) {
                    tagAndValue[1] = tagAndValue[1].substring(0, tagAndValue[1].length() - 1);
                }
            }
            char[] value = tagAndValue[1].trim().toCharArray();
            if (value[0] == '"' && value[value.length - 1] == '"') {
                value[0] = '{';
                value[value.length - 1] = '}';
            }
            builder.append("\r\n").append(tagAndValue[0].trim()).append(" ").append(value).append(",");
        }
        builder.setLength(builder.length() - 1);
        builder.append("\r\n");

        return builder.toString();
    }

    // todo add arrangement to config

    /**
     * Will return the first BibEntry-Keyword in a given string or "invalid" if none is found
     *
     * @param line a string that can contain a BibEntry-Head
     * @return first BibEntry-Keyword in a given String
     */
    public static String getBibEntryKeyword(String line) {
        String re = "[@]\\w{4,}\\s*";
        re += keywordRegEx;
        Pattern pt = Pattern.compile(re);
        Matcher mt = pt.matcher(line);
        if (mt.find()) {
            String entryHead = mt.group(0);
            String type = entryHead.substring(entryHead.indexOf("@") + 1, entryHead.indexOf("{")).trim().toUpperCase();
            if (Arrays.asList(types).contains(type.toLowerCase())) {
                return entryHead.substring(entryHead.indexOf("{") + 1, entryHead.indexOf(",")).trim();
            }
        }
        return null;
    }
}
