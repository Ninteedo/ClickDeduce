import {getLangSelector} from "./globals/elements";
import {ClassDict} from "./globals/classDict";

let hasUsedLangSelector: boolean = false;
let hasCompletedFirstLangTasks: boolean = false;

export function setAttention(element: HTMLElement): void {
    element.classList.add(ClassDict.ATTENTION);
}

export function removeAttention(element: HTMLElement): void {
    element.classList.remove(ClassDict.ATTENTION);
}

export function attentionByCondition(condition: boolean, element: HTMLElement): void {
    if (condition) {
        setAttention(element);
    } else {
        removeAttention(element);
    }
}

export function markHasUsedLangSelector(): void {
    hasUsedLangSelector = true;
    updateLangSelectorAttention();
}

export function markHasCompletedFirstLangTasks(): void {
    hasCompletedFirstLangTasks = true;
    updateLangSelectorAttention();
}

function updateLangSelectorAttention(): void {
    attentionByCondition(!hasUsedLangSelector && hasCompletedFirstLangTasks, getLangSelector());
}
