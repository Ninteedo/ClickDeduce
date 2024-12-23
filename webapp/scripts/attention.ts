import {getLangSelector} from "./globals/elements";

const ATTENTION_CLASS = 'attention';

let hasUsedLangSelector: boolean = false;
let hasCompletedFirstLangTasks: boolean = false;

export function setAttention(element: HTMLElement): void {
    element.classList.add(ATTENTION_CLASS);
}

export function removeAttention(element: HTMLElement): void {
    element.classList.remove(ATTENTION_CLASS);
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
