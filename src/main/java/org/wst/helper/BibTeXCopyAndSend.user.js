// ==UserScript==
// @name     BibTeXCopyAndSend
// @version  1.0.1
// @grant    GM.setClipboard
// @include	 https://dl.acm.org/*
// @author   Wilhelm Stuhlpfarrer
// ==/UserScript==

/**
 * This is a simple userscript build to work with the Greasemonkey plugin.
 * It will work on: https://dl.acm.org/ and detect when a BibTeX Citation is within the
 * specified div container and then copy it to clipboard, to be used inside BibTeXManager
 */
// LINK plugin: https://addons.mozilla.org/de/firefox/addon/greasemonkey/
// SCRIPT: https://gist.github.com/WilhelmStu/cf1b994d934bd6f6d89dc07ab65b09d3/raw/b439585cdee97d74fc8286afe0c7fd9160e0b44c/BibTeXCopyAndSend.user.js
if (window.location.origin === "https://dl.acm.org") {

    let divWithCit = document.body.querySelector("#exportCitation");
    console.log(divWithCit);

    let observer = new MutationObserver(function (mutations) {
        mutations.forEach(function () {

            console.log("the div changed to: " + divWithCit.style.display);
            if (divWithCit.style.display !== "none") {

                setTimeout(() => {

                    let bibDiv = document.body.querySelector("#exportCitation div.csl-right-inline");
                    let bibEntry = bibDiv.innerHTML;

                    if (bibEntry.length > 20) {
                        GM.setClipboard(bibEntry);
                    }
                }, 500);
            }
        });
    });

    observer.observe(divWithCit, {attributes: true, attributeFilter: ["style"]});
}
