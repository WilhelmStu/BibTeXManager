package org.wst;

public class testMain {
    public static void main(String[] args) {
        String test = "  @article{ patashnik-bibtexing,\n" +
                "       author = \"Oren Patashnik\",\n" +
                "       title = \"BIBTEXing\",\n" +
                "       year = \"1988\" }           ";


        System.out.println(FormatChecker.basicBibTeXCheck(test));
    }
}
