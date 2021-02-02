// ==UserScript==
// @name        BibTeXCopyAndSend
// @version     1.0.4
// @grant       GM.setClipboard
// @include	    https://dl.acm.org/*
// @author      Wilhelm Stuhlpfarrer
// @description Script to copy bibTex from acm.org
// ==/UserScript==

/**
 * This is a simple userscript build to work with the Tampermonkey plugin.
 * It will work on: https://dl.acm.org/ and check for a bibTeX Citation within the
 * specified div container and then copy it to clipboard, to be used inside BibTeXManager
 * The check for content in the container happens every 500ms for a total of 10 times (5sec for data to load)
 */
// LINK plugin: https://www.tampermonkey.net/
// SCRIPT: https://gist.github.com/WilhelmStu/cf1b994d934bd6f6d89dc07ab65b09d3/raw/64ce9084f2733e23f67326954edc00bb8c7a8380/BibTeXCopyAndSend.user.js

if (window.location.origin === "https://dl.acm.org") {

    console.log("BibTeXCopyAndSend is active!")
    let retryCount = 0;
    let interval;
    let isRunning = false;
    let divWithCit = document.body.querySelector("#exportCitation");
    //console.log(divWithCit);

    let observer = new MutationObserver(function (mutations) {
        mutations.forEach(function () {

            console.log("BibTeXCopyAndSend: the div changed to: " + divWithCit.style.display);
            if (!isRunning && divWithCit.style.display !== "none") {
                isRunning = true;
                retryCount = 0;
                interval = setInterval(timeOutLoop, 500);
            }
        });
    });
    observer.observe(divWithCit, {attributes: true, attributeFilter: ["style"]});

    function timeOutLoop() {
        let bibDiv = document.body.querySelector("#exportCitation div.csl-right-inline");
        if (bibDiv != null) {
            let bibEntry = bibDiv.innerHTML;

            if (bibEntry.length > 20) {
                console.log("BibTeXCopyAndSend: found an entry, copy to clipboard and end loop");
                let randString = Math.random().toString(36).substring(2, 7);
                GM.setClipboard(bibEntry + randString);
                clearInterval(interval);
                isRunning = false;
                return;
            }
        }

        if (retryCount < 10) {
            console.log("BibTeXCopyAndSend: no entry found, retry in 500ms");
            retryCount++;
        } else {
            console.log("BibTeXCopyAndSend: 10 retries, with no bib entry, break loop");
            clearInterval(interval);
        }
    }
}