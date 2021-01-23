package org.wst.helper;


import org.wst.PrimaryController;
import org.wst.model.TableEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://www.openoffice.org/bibliographic/bibtex-defs.html
// https://www.verbosus.com/bibtex-style-examples.html
// https://dl.acm.org/action/doSearch?AllField=ui&expand=all&ConceptID=118230
public abstract class FormatChecker {
    public final static String[] types = {"article", "book", "booklet", "conference", "inbook", "incollection", "inproceedings",
            "manual", "mastersthesis", "misc", "phdthesis", "proceedings", "techreport", "unpublished"};

    public final static String[] fields = {"address", "annote", "author", "booktitle", "chapter", "crossref", "edition",
            "editor", "howpublished", "institution", "journal", "key", "month", "note", "number", "organization",
            "pages", "publisher", "school", "series", "title", "type", "volume", "year",};

    // NOTE: ^,{}\()%"'#~= not allowed in keyword, but '@' is allowed
    public final static String keywordRegEx = "[{][^,{}\\\\()%\"'#~=]*[,]";


    /**
     * Will search for the larges possible bib entry inside the raw input from clipboard
     * If one is found and if the curly braces are correct return the exact entry
     *
     * @param raw input from system clipboard, that can contain a BibTeX entry
     * @return "" if no BibTeX entry is found, else the first found valid entry
     */
    public static String basicBibTeXCheck(String raw) {

        String re = "[@]\\w{4,}\\s*";
        re += keywordRegEx;
        re += "[^@]+[}]";

        Pattern pt = Pattern.compile(re);
        Matcher mt = pt.matcher(raw);
        String firstEntry = "";
        if (mt.find()) { // will check for the first entry matching above regex
            firstEntry = mt.group(0);
            int index;
            while (!areCurlyBracesOkay(firstEntry)) {
                index = firstEntry.lastIndexOf('}') + 1;
                if (index == 0) return "";
                else if (index == firstEntry.length()) index--;
                firstEntry = firstEntry.substring(0, index);
            }
            index = firstEntry.lastIndexOf('}') + 1;
            if (index == 0) return "";
            else firstEntry = firstEntry.substring(0, index);

            String type = firstEntry.substring(firstEntry.indexOf("@") + 1, firstEntry.indexOf("{")).trim();
            if (Arrays.asList(types).contains(type.toLowerCase())) {
                //firstEntry = replaceQuotationMarks(firstEntry);
                firstEntry += "\r\n";
                return firstEntry;
            } else {
                return "";
            }
        }
        return firstEntry;
    }

    /**
     * Will check if the placement of curly braces for this entry is valid
     * Invalid if:  - unequal amount,
     * - closing } before opening {,
     * - opening after stack is already empty
     *
     * @param entry entry to check
     * @return if valid
     */
    private static boolean areCurlyBracesOkay(String entry) {
        Stack<Character> stack = new Stack<>();

        char c;
        boolean foundOneBrace = false;
        for (int i = 0; i < entry.length(); i++) {
            c = entry.charAt(i);
            if (c == '{') {
                if (stack.isEmpty() && foundOneBrace) return false;
                stack.push(c);
                foundOneBrace = true;
            } else if (c == '}') {
                if (stack.empty()) return false;
                else if (stack.peek() == '{') stack.pop();
                else return false;
            }
        }
        return (stack.empty() && foundOneBrace);
    }

    // https://www.logicbig.com/tutorials/core-java-tutorial/java-regular-expressions/regex-lookahead.html
    // todo add config for this!

    /**
     * Will take a valid bib entry and replace the ""/{} for each value after an tag
     * e.g.: title = "this is a title" <-> title = {this is a title}
     * but e.g.: year = 2002 wont be changed, but year = "2002" <-> year = {2002}
     * and e.g.: title = "{}" will become {{}} or """"
     *
     * @param entry valid bib entry
     * @return same as input but "" <-> {}
     */
    public static String replaceValueClosures(String entry, boolean toCurlyBraces) {
        String[] lines = entry.split("([,])(?=\\s*\\w+\\s*[=])");
        StringBuilder builder = new StringBuilder();
        boolean hadCommaAtEnd = false;

        if (lines.length < 2) return entry;
        builder.append(lines[0]).append(",");
        for (int i = 1; i < lines.length; i++) {
            String[] tagAndValue = lines[i].split("(?<==)", 2);
            if (i == lines.length - 1) {
                String tmp = tagAndValue[1].trim();
                tmp = tmp.substring(0, tmp.length() - 1).trim();
                tagAndValue[1] = tmp;

                if (tmp.lastIndexOf(",") == tmp.length() - 1) {
                    tmp = tmp.substring(0, tmp.length() - 1).trim();
                    if (tmp.endsWith("\"") || tmp.endsWith("}")) {
                        hadCommaAtEnd = true;
                        tagAndValue[1] = tmp;
                    }
                }
            }
            char[] value = tagAndValue[1].trim().toCharArray();
            if (toCurlyBraces) {
                if (value[0] == '"' && value[value.length - 1] == '"') {
                    value[0] = '{';
                    value[value.length - 1] = '}';
                }
            } else {
                if (value[0] == '{' && value[value.length - 1] == '}') {
                    value[0] = '"';
                    value[value.length - 1] = '"';
                }
            }

            builder.append("\r\n    ").append(tagAndValue[0].trim()).append(" ").append(value).append(",");
        }
        if (!hadCommaAtEnd) builder.setLength(builder.length() - 1);
        builder.append("\r\n}");

        return builder.toString();
    }

    /**
     * Will return the first BibEntry-Keyword in a given string or null if none is found
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

    /**
     * Creates a new TableEntry to store important values from the given Bib-Entry
     * If a value is not given "none" is stored
     *
     * @param entry valid Bib-Entry
     * @return tableEntry for the TableView in the App
     */
    public static TableEntry getBibTableEntry(String entry) {
        String keyword = getBibEntryKeyword(entry);
        if (keyword != null) {
            String type = entry.substring(entry.indexOf("@") + 1, entry.indexOf("{")).trim().toUpperCase();
            String[] lines = entry.split("([,])(?=\\s*\\w+\\s*[=])");
            String author = "none", title = "none", year = "none", url = "none", doi = "none";

            for (int i = 1; i < lines.length; i++) {
                String[] tagAndValue = lines[i].split("=", 2);
                switch (tagAndValue[0].toUpperCase().trim()) {
                    case "AUTHOR":
                        author = removeClosure(tagAndValue[1]);
                        break;
                    case "TITLE":
                        title = removeClosure(tagAndValue[1]);
                        break;
                    case "YEAR":
                        year = tagAndValue[1].trim().replaceAll("[\n\r]*(\\s+)", " ")
                                .replaceAll("[\"{},]", "").trim();
                        break;
                    case "URL":
                    case "OPTURL":
                        url = removeClosure(tagAndValue[1]);
                        break;
                    case "DOI":
                    case "OPTDOI":
                        doi = removeClosure(tagAndValue[1]);
                }
            }
            return new TableEntry(keyword, type, title, author, year, url, doi);
        } else return null;
    }

    /**
     * Removes the closure from the given string
     * Each value in a Bib-Entry can have 0 to multiple closures,
     * that are either "" or {} or combinations of those
     *
     * @param str string with closure
     * @return string without closure
     */
    private static String removeClosure(String str) {
        str = str.trim().replaceAll("[\n\r]*(\\s+)", " ");
        while ((str.startsWith("{") && str.endsWith("}")) ||
                (str.startsWith("\"") && str.endsWith("\""))) {
            str = str.substring(1, str.length() - 1);
        }
        return str.trim();
    }

    /**
     * Will go through the given text block and search for bibEntries
     * and add all to the list of entries
     *
     * @param text ram text from textArea
     * @return all bib entries in the given text block (no complete duplicates)
     */
    public static ArrayList<String> getBibEntries(String text) {
        ArrayList<String> entries = new ArrayList<>();

        String entry;
        while (!(entry = basicBibTeXCheck(text)).isEmpty()) {
            String correctClosure = replaceValueClosures(entry, PrimaryController.isToCurlyMode());
            entries.add(correctClosure + "\r\n");
            if (text.length() > entry.length()) {
                text = text.replace(entry.substring(0, entry.length() - 2), "");
            } else break;
        }
        return entries;
    }
}
