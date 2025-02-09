import fs from "fs";
import path from "path";
import {handleLiteralChanged, startNodeBlank} from "../actions";
import {CustomExprSelector} from "../components/customExprSelector";
import {vitest} from "vitest";
import {DropdownOption} from "../components/baseDropdownSelector";
import {getLangSelector, getTreePathOfElement} from "../globals/elements";
import {getActiveContextMenu} from "../components/contextMenu/contextMenu";
import {SubtreeContextMenu} from "../components/contextMenu/SubtreeContextMenu";
import {getExprSelectors} from "../activeInputs";

export function slightDelay(delay: number = 10): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, delay));
}

export function loadHtmlTemplate(filename: string): string {
    const templatePath = path.resolve(__dirname, '..', '..', '..', `${filename}.html`);
    const readResult: string = fs.readFileSync(templatePath, 'utf8');
    return readResult.replace(/\r\n/g, '\n');
}

export function loadIndexHtmlTemplate(): string {
    return loadHtmlTemplate('index');
}

export function pressStartNodeButton() {
    startNodeBlank();
}

export function changeLanguage(langIndex: number): void {
    const langSelector = getLangSelector();
    langSelector.selectedIndex = langIndex;
    langSelector.dispatchEvent(new Event('change'));
}

export function getLeftmostExprDropdown(): CustomExprSelector {
    return getExprSelectors()[0];
}

export function getDropdownAt(treePath: string): CustomExprSelector | undefined {
    return getExprSelectors().find(selector => selector.getTreePath() === treePath);
}

export function getLiteralInputAt(treePath: string): HTMLInputElement {
    return document.querySelector(`input[data-tree-path="${treePath}"]`) as HTMLInputElement;
}

export function getExprDropdownOptions(selector: CustomExprSelector): DropdownOption[] {
    return selector.options;
}

export function selectExprOption(selector: CustomExprSelector | undefined, exprName: string): void {
    if (!selector) throw new Error('Selector is undefined');
    selector.enterValue(exprName);
    // input.dispatchEvent(new Event('input'));
    // input.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter'}));
}

export function doLiteralEdit(input: HTMLInputElement, newValue: string): void {
    input.value = newValue;
    handleLiteralChanged(input);
}

export function doBooleanLiteralEdit(input: HTMLInputElement, newValue: boolean): void {
    input.checked = newValue;
    handleLiteralChanged(input);
}

export function contextMenuSelect(element: HTMLElement | null): void {
    if (element === null) throw new Error('Element is null');
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

export function leftClickOn(element: HTMLElement | null): void {
    if (!element) throw new Error('Element is null');

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

export function removeWhitespace(str: string): string {
    return str.replace(/^\s+|\s+$/g, '');
}

export function getTabbableElements(allowSelect: boolean = false): HTMLElement[] {
    let selector = 'input[data-tree-path]:not([disabled])';
    if (allowSelect) {
        selector += ', select.expr-dropdown:not([disabled])';
    }
    const elements = Array.from(document.querySelectorAll(selector)) as HTMLElement[];
    elements.sort((a, b) => {
        const aPath = getTreePathOfElement(a);
        const bPath = getTreePathOfElement(b);
        return aPath.localeCompare(bPath, undefined, {numeric: true, sensitivity: 'base'});
    });
    return elements;
}

export function basicMocks(): void {
    Element.prototype.scrollIntoView = vitest.fn();
}

export function getActiveSubtreeContextMenu(): SubtreeContextMenu {
    const activeContextMenu = getActiveContextMenu();
    if (!(activeContextMenu instanceof SubtreeContextMenu)) throw new Error('Active context menu is not a SubtreeContextMenu');
    return activeContextMenu;
}
