// @ts-ignore
import DemoGif from "../images/demo.gif";
import {getCurrentLanguage} from "./langSelector";
import {IdDict} from "./globals/idDict";

export function setupGuide(): void {
    console.debug("Setting up guide");
    addDummyLangSelector();
    if (getCurrentLanguage() !== "LArith") {
        throw new Error("Expected current language to be LArith");
    }
    (document.getElementById(IdDict.GUIDE_DEMO_GIF) as HTMLImageElement).src = DemoGif;
}

function addDummyLangSelector(): void {
    const langSelectorDiv = document.createElement('div');
    langSelectorDiv.id = IdDict.LANG_SELECTOR_DIV;
    langSelectorDiv.style.display = "none";
    const langSelector = document.createElement('select');
    langSelector.id = IdDict.LANG_SELECTOR;
    langSelectorDiv.appendChild(langSelector);
    const lArithOption = document.createElement('option');
    lArithOption.value = "LArith";
    langSelector.appendChild(lArithOption);
    langSelector.selectedIndex = 0;
    document.body.appendChild(langSelectorDiv);
}
