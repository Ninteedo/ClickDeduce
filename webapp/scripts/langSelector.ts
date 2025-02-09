import {getLangSelectorNew} from "./serverRequest";
import {markHasUsedLangSelector} from "./attention";
import {runAction} from "./actions";
import {IdDict} from "./globals/idDict";

let langSelector: HTMLSelectElement;

/**
 * Loads the language selector HTML from the server and adds it to the DOM.
 */
export function loadLangSelector(): void {
    const langSelectorContainer: HTMLElement | null = document.getElementById(IdDict.LANG_SELECTOR_DIV);

    if (!(langSelectorContainer instanceof HTMLDivElement)) {
        throw new Error("Could not find lang-selector-div");
    }

    langSelectorContainer.innerHTML = getLangSelectorNew();
    const langSelectorElement: HTMLElement | null = document.getElementById(IdDict.LANG_SELECTOR);
    if (!(langSelectorElement instanceof HTMLSelectElement)) throw new Error('Language selector not found');
    langSelectorElement.selectedIndex = 0;
    langSelectorElement.addEventListener('change', () => {
        markHasUsedLangSelector();
        runAction("IdentityAction", "");
    });

    langSelector = langSelectorElement;
}

export function getCurrentLanguage(): string {
    if (!langSelector) {
        langSelector = document.getElementById(IdDict.LANG_SELECTOR) as HTMLSelectElement;
    }
    return langSelector.value;
}

export function setCurrentLanguage(lang: string | number): void {
    if (typeof lang === 'number') {
        langSelector.selectedIndex = lang;
    } else {
        langSelector.value = lang;
    }
}

export function disableLangSelector(): void {
    langSelector.disabled = true;
}

export function enableLangSelector(): void {
    langSelector.disabled = false;
}
