import {handleLiteralChanged, hasCopyCache} from "./actions";
import {getActiveInputs, lastNodeString} from "./treeManipulation";
import {getSelectedLanguage, getSelectedMode, hasClassOrParentHasClass} from "./utils";
import {panzoomInstance} from "./initialise";
import {selectorEnterPressed} from "./customExprSelector";
// @ts-ignore
import {convertToLaTeX} from "scalajs:main.js";

let errorDiv: HTMLDivElement;
export let nextFocusElement: HTMLElement | null = null;
export let contextMenuSelectedElement: HTMLElement | null = null;

/**
 * Resets the global variables used by the interface code.
 */
export function resetInterfaceGlobals(): void {
    contextMenuSelectedElement = null;
    nextFocusElement = null;
    errorDiv = document.getElementById('error-message') as HTMLDivElement;
    setupValueTypeColourHighlightingCheckbox();

    document.addEventListener('contextmenu', openContextMenu);
    document.addEventListener('click', closeContextMenu);

    getContextMenu().style.display = 'none';
}

/**
 * Handles the keydown event.
 *
 * On TAB, moves focus to the next input element.
 * On ENTER while focused on an input element, submits the literal change.
 *
 * @param e the keydown event
 */
export function handleKeyDown(e: KeyboardEvent): void {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        handleTabPressed(e);
    } else if (e.key === 'Enter' && e.target instanceof HTMLInputElement) {
        e.preventDefault();
        nextFocusElement = e.target;
        if (e.target.classList.contains('literal')) {
            handleLiteralChanged(e.target);
        } else if (e.target.classList.contains('expr-selector-input')) {
            const selector = e.target.parentElement;
            if (selector instanceof HTMLDivElement) {
                selectorEnterPressed(selector);
            }
        }
    }
}

/**
 * Changes the focus to the next input element when TAB is pressed.
 * @param e the keydown event
 */
export function handleTabPressed(e: KeyboardEvent): void {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        e.preventDefault();
        const activeInputPaths: string[] = getActiveInputs().map(getTreePathOfElement);
        const targetOuterPath: string = getTreePathOfElement(e.target);
        let activeElemIndex = activeInputPaths.indexOf(targetOuterPath);
        if (e.shiftKey) {
            activeElemIndex -= 1;
        } else {
            activeElemIndex += 1;
        }
        if (activeElemIndex < 0) {
            activeElemIndex = getActiveInputs().length - 1;
        } else if (activeElemIndex >= getActiveInputs().length) {
            activeElemIndex = 0;
        }
        e.target.dispatchEvent(new Event('blur'));
        nextFocusElement = getActiveInputs()[activeElemIndex];
        nextFocusElement.focus();
        if (nextFocusElement instanceof HTMLInputElement) {
            nextFocusElement.select();
        }
        nextFocusElement = null;
    }
}

/**
 * Clears the highlight from the currently highlighted element.
 *
 * Also clears the contextMenuSelectedElement.
 */
export function clearHighlight(): void {
    document.querySelector('.highlight')?.classList.remove('highlight');
    contextMenuSelectedElement = null;
}

/**
 * Opens the context menu on the subtree that was right-clicked.
 * @param e the mouse event
 */
function openContextMenu(e: MouseEvent): void {
    let target: EventTarget | null = e.target;
    if (contextMenuSelectedElement !== null) {
        // closes the context menu if it is already open
        target = null;
    }

    while (target instanceof HTMLElement && !target.classList.contains('highlight')) {
        target = target.parentElement;
    }

    if (target && target instanceof HTMLElement && !hasClassOrParentHasClass(target, 'phantom')) {
        e.preventDefault();

        contextMenuSelectedElement = target;

        const menu = getContextMenu();
        menu.style.display = 'block';
        menu.style.left = e.pageX + 'px';
        menu.style.top = e.pageY + 'px';

        const pasteButton = getPasteButton();
        if (hasCopyCache()) {
            pasteButton.removeAttribute('disabled');
        } else {
            pasteButton.setAttribute('disabled', 'true');
        }
    } else {
        closeContextMenu();
    }
}

/**
 * Closes the context menu.
 */
function closeContextMenu(): void {
    getContextMenu().style.display = 'none';
    if (contextMenuSelectedElement !== null) {
        clearHighlight();
    }
}

/**
 * Zooms the tree to fit the container.
 */
export async function zoomToFit(): Promise<void> {
    const container: HTMLElement = getTreeContainer();
    const firstSubtree: HTMLDivElement = document.querySelector('.subtree[data-tree-path=""]') as HTMLDivElement;

    const scaleWidth = container.clientWidth / firstSubtree.clientWidth;

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, scaleWidth);
}

let errorTimeoutId: number = 0;

function incrementErrorTimeout(): void {
    errorTimeoutId = (errorTimeoutId + 1) % 1000;
}

/**
 * Displays the given error message to the user.
 *
 * Disappears after 5 seconds.
 *
 * @param error the error to display, requires a 'message' property
 */
export function displayError(error: any): void {
    console.error(error);
    errorDiv.textContent = error.message;
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');

    incrementErrorTimeout();
    let myTimeoutId = errorTimeoutId;
    setTimeout(() => {
        if (myTimeoutId !== errorTimeoutId) return;
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}

function setupValueTypeColourHighlightingCheckbox(): void {
    const valueTypeColourCheckbox = document.getElementById('value-highlighting-toggle') as HTMLInputElement;
    valueTypeColourCheckbox.checked = true;
    valueTypeColourCheckbox.addEventListener('change', () => {
        toggleValueTypeColourHighlighting(valueTypeColourCheckbox.checked);
    });
    toggleValueTypeColourHighlighting(valueTypeColourCheckbox.checked);
}

function toggleValueTypeColourHighlighting(newState: boolean): void {
    const body = document.querySelector('body');
    const className = 'value-highlighting-enabled';
    if (!body) throw new Error('Body element not found');
    if (newState) {
        body.classList.add(className);
    } else {
        body.classList.remove(className);
    }
}

export function isAutoZoomEnabled(): boolean {
    const autoZoomCheckbox = document.getElementById('auto-zoom-toggle') as HTMLInputElement;
    return autoZoomCheckbox.checked;
}

export function exportLaTeX(): void {
    const langName = getSelectedLanguage();
    const modeName = getSelectedMode();
    const output: string = convertToLaTeX(langName, modeName, lastNodeString);
    showExportOutput("LaTeX Output", output, "Copy this LaTeX code and use the bussproofs package");
}

function showExportOutput(title: string, output: string, description: string | null): void {
    const outputDiv = document.getElementById('export-output-container') as HTMLDivElement;
    const outputTextArea = document.getElementById('export-output') as HTMLTextAreaElement;
    outputTextArea.value = output;
    const outputTitle = document.getElementById('export-output-title');
    if (outputTitle) outputTitle.textContent = title;
    const outputDescription = document.getElementById('export-output-desc');
    if (outputDescription) {
        if (description) {
            outputDescription.textContent = description;
            outputDescription.classList.add('visible');
        } else {
            outputDescription.classList.remove('visible');
        }
    }
    outputDiv.classList.add('visible');
    getBlocker().classList.add('visible');
}

export function copyExportOutput() {
    const outputTextArea = document.getElementById('export-output') as HTMLTextAreaElement;
    outputTextArea.select();
    navigator.clipboard.writeText(outputTextArea.value);
}

export function closeExportOutput() {
    const outputDiv = document.getElementById('export-output-container') as HTMLDivElement;
    outputDiv.classList.remove('visible');
    getBlocker().classList.remove('visible');
}

function getBlocker(): HTMLElement {
    const blocker = document.getElementById('blocker');
    if (!blocker) throw new Error('Blocker not found');
    return blocker;
}

function getContextMenu(): HTMLElement {
    const menu = document.getElementById('custom-context-menu');
    if (!menu) throw new Error('Context menu not found');
    return menu;
}

function getTreeContainer(): HTMLElement {
    const container = document.getElementById('tree-container');
    if (!container) throw new Error('Tree container not found');
    return container;
}

function getPasteButton(): HTMLButtonElement {
    const pasteButton = document.getElementById('paste-button') as HTMLButtonElement;
    if (!pasteButton) throw new Error('Paste button not found');
    return pasteButton;
}

export function getTreePathOfElement(element: HTMLElement | null): string {
    if (element === null) throw new Error("Cannot get tree path of null");
    const treePath = element.getAttribute("data-tree-path");
    if (treePath === null) throw new Error("Element does not have a tree path");
    return treePath;
}
