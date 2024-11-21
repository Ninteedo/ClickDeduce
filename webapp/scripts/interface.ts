import {copyTreeNode, deleteTreeNode, handleLiteralChanged, hasCopyCache, pasteTreeNode} from "./actions";
import {getActiveInputs, lastNodeString, redo, undo} from "./treeManipulation";
import {compareTreePaths, getSelectedLanguage, getSelectedMode, hasClassOrParentHasClass} from "./utils";
// @ts-ignore
import {convertToLaTeX} from "scalajs:main.js";
import {PanZoom} from "panzoom";

let errorDiv: HTMLDivElement;
export let nextFocusElement: HTMLElement | null = null;
export let contextMenuSelectedElement: HTMLElement | null = null;
let panzoomInstance: PanZoom;

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
 * Handles any keydown event on the document.
 */
function globalHandleKeyDown(e: KeyboardEvent): void {
    const highlightElement = getHighlightElement();
    const highlightPath = highlightElement && highlightElement.hasAttribute('data-tree-path') ? getTreePathOfElement(highlightElement) : null;

    const ctrl = e.ctrlKey || e.metaKey;
    const shift = e.shiftKey;
    const key = e.key.toUpperCase();

    if (ctrl && key === 'Z') {
        if (shift) {
            redo();
        } else {
            undo();
        }
    } else if (highlightPath !== null) {
        if (ctrl && key === 'C') {
            copyTreeNode(highlightPath);
        } else if (ctrl && key === 'V') {
            e.preventDefault();
            pasteTreeNode(highlightPath);
        } else if (ctrl && key === 'X') {
            copyTreeNode(highlightPath);
            deleteTreeNode(highlightPath);
        }
    }
}

document.addEventListener('keydown', globalHandleKeyDown);

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
        nextFocusElement = e.target;
        if (e.target.classList.contains('literal')) {
            e.preventDefault();
            handleLiteralChanged(e.target);
        } else if (e.target.classList.contains('expr-selector-input')) {
            const selector = e.target.parentElement;
            if (selector instanceof HTMLDivElement) {
                // selectorEnterPressed(selector);
            }
        }
    }
}

export function setNextFocusElement(element: HTMLElement): void {
    nextFocusElement = element;
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
 * Returns the currently highlighted tree element.
 *
 * If the element is a phantom element, or no element is highlighted, returns null.
 */
export function getHighlightElement(): HTMLElement | null {
    const res = document.querySelector('.highlight');
    if (res instanceof HTMLElement && !hasClassOrParentHasClass(res, 'phantom')) {
        return res;
    }
    return null;
}

function getHighlightElementFromEvent(e: Event): HTMLElement | null {
    let target: EventTarget | null = e.target;
    if (contextMenuSelectedElement !== null) {
        target = null;
    }

    while (target instanceof HTMLElement && !target.classList.contains('highlight')) {
        target = target.parentElement;
    }

    return target instanceof HTMLElement ? target : null;
}

/**
 * Sets the focus to the input element with the given tree path.
 * If that element is not found, will instead try to focus on the first input element in a subtree of that path.
 * If there is no input element in the subtree, will do nothing.
 */
export function setFocusElement(path: string): void {
    const focusedElement = getActiveInputs().find(input => compareTreePaths(path!, getTreePathOfElement(input)) <= 0);
    if (focusedElement && focusedElement instanceof HTMLElement) {
        focusedElement.focus();
        if (focusedElement instanceof HTMLInputElement) {
            focusedElement.select();
        }
    }
}

/**
 * Opens the context menu on the subtree that was right-clicked.
 * @param e the mouse event
 */
function openContextMenu(e: MouseEvent): void {
    const highlightElement = getHighlightElementFromEvent(e);

    if (highlightElement && !hasClassOrParentHasClass(highlightElement, 'phantom')) {
        e.preventDefault();

        contextMenuSelectedElement = highlightElement;

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

function incrementErrorTimeoutId(): void {
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

    incrementErrorTimeoutId();
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

/**
 * Returns whether the auto-zoom checkbox is checked.
 */
export function isAutoZoomEnabled(): boolean {
    const autoZoomCheckbox = document.getElementById('auto-zoom-toggle') as HTMLInputElement;
    return autoZoomCheckbox.checked;
}

/**
 * Displays the export LaTeX output modal.
 */
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

/**
 * Copies the LaTeX output to the clipboard.
 */
export function copyExportOutput() {
    const outputTextArea = document.getElementById('export-output') as HTMLTextAreaElement;
    outputTextArea.select();
    navigator.clipboard.writeText(outputTextArea.value);
}

/**
 * Closes the export output modal.
 */
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

export function getPasteButton(): HTMLButtonElement {
    const pasteButton = document.getElementById('paste-button') as HTMLButtonElement;
    if (!pasteButton) throw new Error('Paste button not found');
    return pasteButton;
}

export function getCopyButton(): HTMLButtonElement {
    const copyButton = document.getElementById('copy-button') as HTMLButtonElement;
    if (!copyButton) throw new Error('Copy button not found');
    return copyButton;
}

export function getDeleteButton(): HTMLButtonElement {
    const deleteButton = document.getElementById('delete-button') as HTMLButtonElement;
    if (!deleteButton) throw new Error('Delete button not found');
    return deleteButton;
}

export function getZoomToFitButton(): HTMLButtonElement {
    const zoomToFitButton = document.getElementById('zoom-to-fit') as HTMLButtonElement;
    if (!zoomToFitButton) throw new Error('Zoom to fit button not found');
    return zoomToFitButton;
}

export function getContextMenuZoomToFitButton(): HTMLButtonElement {
    const zoomToFitButton = document.getElementById('zoom-button') as HTMLButtonElement;
    if (!zoomToFitButton) throw new Error('Context menu zoom to fit button not found');
    return zoomToFitButton;
}

export function getSaveButton(): HTMLButtonElement {
    const saveButton = document.getElementById('saveButton') as HTMLButtonElement;
    if (!saveButton) throw new Error('Save button not found');
    return saveButton;
}

export function getLoadButton(): HTMLButtonElement {
    const loadButton = document.getElementById('loadButton') as HTMLButtonElement;
    if (!loadButton) throw new Error('Load button not found');
    return loadButton;
}

export function getExportLatexButton(): HTMLButtonElement {
    const exportLatexButton = document.getElementById('exportLatexButton') as HTMLButtonElement;
    if (!exportLatexButton) throw new Error('Export LaTeX button not found');
    return exportLatexButton;
}

export function getExportCopyButton(): HTMLButtonElement {
    const exportCopyButton = document.getElementById('export-copy-button') as HTMLButtonElement;
    if (!exportCopyButton) throw new Error('Export copy button not found');
    return exportCopyButton;
}

export function getExportCloseButton(): HTMLButtonElement {
    const exportCloseButton = document.getElementById('export-close-button') as HTMLButtonElement;
    if (!exportCloseButton) throw new Error('Export close button not found');
    return exportCloseButton;
}

/**
 * Returns the tree path string of the given tree element.
 */
export function getTreePathOfElement(element: HTMLElement | null): string {
    if (element === null) throw new Error("Cannot get tree path of null");
    const treePath = element.getAttribute("data-tree-path");
    if (treePath === null) {
        console.log(element.outerHTML);
        throw new Error("Element does not have a tree path");
    }
    return treePath;
}

export function setPanZoomInstance(instance: PanZoom): void {
    panzoomInstance = instance;
}
