package org.wst;

import org.wst.helper.FormatChecker;

/**
 * Small test class to test some basic strings that will be
 * processed through the FormatChecker...
 */
public class testMain {
    public static void main(String[] args) {
        String test = "ANY TEXT THAT IS NOT BIBTEX " +
                " @misc     {   patashnik-123bibtexing ,\n" +
                "       author = \"Oren Patashnik\",\n" +
                "       title = \"BIBTEXing\",\n" +
                "       year = \"1988\" }   " +
                " MORE COPIED TEXT THAT IS NOT BIBTEX        ";

        String test2 = "@article      {nash51,\n" +
                "      author  = \"Nash, John\",\n" +
                "      title   = \"Non-c,,, , , ,oo,  ; ::; === == 0perative Games\",\n" +
                "      journal = \"Annals of Mathematics\",\n" +
                "      year    = 1951,\n" +
                "      volume  = \"54\",\n" +
                "      number  = \"2\",\n" +
                "      pages   = \"286--295\"\n" +
                "    }";

        String test3 = "ANY TEXT THAT IS NOT BIBTEX " +
                " @misc     {   patashnik-bibtexing ," +
                "       author = \"Oren Patashnik\"," +
                "       title = \"BIBTEXing\"," +
                "       year = \"1988\" }   " +
                " MORE COPIED TEXT THAT IS NOT BIBTEX        ";


       // System.out.println(FormatChecker.replaceValueClosures(test2, true));


/*
        System.out.println(FormatChecker.basicBibTeXCheck(test));
        System.out.println(FormatChecker.basicBibTeXCheck(test2));
        System.out.println(FormatChecker.basicBibTeXCheck(test3));

        System.out.println(FormatChecker.getBibEntryHead(test));
        System.out.println(FormatChecker.getBibEntryHead(test2));
        System.out.println(FormatChecker.getBibEntryHead(test3));

 */

    }
}
