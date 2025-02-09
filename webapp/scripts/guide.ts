import {getCurrentLanguage} from "./treeManipulation";
// @ts-ignore
import DemoGif from "../images/demo.gif";

export function setupGuide(): void {
    console.debug("Setting up guide");
    addDummyLangSelector();
    if (getCurrentLanguage() !== "LArith") {
        throw new Error("Expected current language to be LArith");
    }
    (document.getElementById("guide-demo-gif") as HTMLImageElement).src = DemoGif;
}

function addDummyLangSelector(): void {
    const langSelectorDiv = document.createElement('div');
    langSelectorDiv.id = "lang-selector-div";
    langSelectorDiv.style.display = "none";
    const langSelector = document.createElement('select');
    langSelector.id = "lang-selector";
    langSelectorDiv.appendChild(langSelector);
    const lArithOption = document.createElement('option');
    lArithOption.value = "LArith";
    langSelector.appendChild(lArithOption);
    langSelector.selectedIndex = 0;
    document.body.appendChild(langSelectorDiv);
}
