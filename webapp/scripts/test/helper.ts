import fs from "fs";
import path from "path";
import {handleDropdownChange, handleSubmit} from "../actions";

export function slightDelay(delay: number = 10): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, delay));
}

export function loadHtmlTemplate(filename: string): string {
    const readResult: string = fs.readFileSync(path.resolve(__dirname, '../../test_resources', `${filename}.html`), 'utf8');
    return readResult.replace(/\r\n/g, '\n');
}

export function getTree() {
    return document.getElementById('tree');
}

export function getStartNodeButton() {
    return document.getElementById('start-node-button');
}

export async function pressStartNodeButton() {
    await handleSubmit(new Event(""), '/start-node-blank')
}

export function getLangSelector() {
    return document.getElementById('lang-selector') as HTMLSelectElement;
}

export async function changeLanguage(langIndex: number): Promise<void> {
    const langSelector = getLangSelector();
    langSelector.selectedIndex = langIndex;
    langSelector.dispatchEvent(new Event('change'));
    await new Promise(resolve => setTimeout(resolve, 50));
}

export function getLeftmostExprDropdown(): HTMLSelectElement {
    return document.querySelector('select.expr-dropdown:not([disabled])') as HTMLSelectElement;
}

export async function selectExprOption(dropdown: HTMLSelectElement, optionIndex: number, manual: boolean = false): Promise<void> {
    dropdown.selectedIndex = optionIndex;
    if (manual) {
        dropdown.dispatchEvent(new Event('change'));
        await slightDelay(50);
    } else {
        await handleDropdownChange(dropdown, 'expr');
    }
}

export async function doLiteralEdit(input: HTMLInputElement, newValue: string): Promise<void> {
    input.value = newValue;
    input.dispatchEvent(new Event('change'));
    await new Promise(resolve => setTimeout(resolve, 100));
}

export function contextMenuSelect(element: HTMLElement): void {
    element.dispatchEvent(new MouseEvent('mouseover', {
        bubbles: true,
        cancelable: true,
    }));

    element.dispatchEvent(new MouseEvent('contextmenu', {
        bubbles: true,
        cancelable: true,
        clientX: 0,
        clientY: 0,
        button: 2
    }));
}

export function leftClickOn(element: HTMLElement): void {
    element.dispatchEvent(new MouseEvent('mouseover', {
        bubbles: true,
        cancelable: true,
    }));

    element.dispatchEvent(new MouseEvent('click', {
        bubbles: true,
        cancelable: true,
        clientX: 0,
        clientY: 0,
        button: 0
    }));
}

export function getContextMenuElement(): HTMLElement {
    return document.getElementById('custom-context-menu');
}

export function removeWhitespace(str: string): string {
    return str.replace(/^\s+|\s+$/g, '');
}

export function getUndoButton(): HTMLButtonElement {
    return document.getElementById('undoButton') as HTMLButtonElement;
}

export function getRedoButton(): HTMLButtonElement {
    return document.getElementById('redoButton') as HTMLButtonElement;
}

export function getTabbableElements(allowSelect: boolean = false): HTMLElement[] {
    let selector = 'input[data-tree-path]:not([disabled])';
    if (allowSelect) {
        selector += ', select.expr-dropdown:not([disabled])';
    }
    const elements = Array.from(document.querySelectorAll(selector)) as HTMLElement[];
    elements.sort((a, b) => {
        const aPath = a.getAttribute("data-tree-path");
        const bPath = b.getAttribute("data-tree-path");
        return aPath.localeCompare(bPath, undefined, {numeric: true, sensitivity: 'base'});
    });
    return elements;
}

export function getErrorDiv(): HTMLElement {
    return document.getElementById('error-message');
}
