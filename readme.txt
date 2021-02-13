
BibTeXManager

This is useful program that makes it easier to handle and expand BibTeX files, these are mainly used in combination with LaTeX.
What exactly is BibTeX: http://www.bibtex.org/

Key features:
    - management of entries inside various BibTeX files
    - detection and insertion of BibTeX entries from system clipboard
    - automatic formatting of BibTeX entries and files
    - script for automatic copy of BibTeX entries from acm.org

To deploy the app you can use maven and either run:

    - mvn clean javafx:jlink
(this will create BibTeXManager/target/output.zip, that includes all needed binaries for your specific platform  and the executable "launch" inside the bin folder to run the App, this version then works independent of your java installation)

    - mvn package
(note that this will create the BibTeXManager-version-shade.jar inside the target folder, it should work on windows, linuxand mac, but requires a valid java installation to run)

To use the script for acm.org:
Browser-Plugin to manage User-Scripts: https://www.tampermonkey.net/

After plugin installation this link should prompt you to install the script:
https://gist.github.com/WilhelmStu/cf1b994d934bd6f6d89dc07ab65b09d3/raw/64ce9084f2733e23f67326954edc00bb8c7a8380/BibTeXCopyAndSend.user.js

The script can also be found at: ../src/main/java/org/wst/helper/BibTeXCopyAndSend.user.js
