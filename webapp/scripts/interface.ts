import {copyTreeNode, deleteTreeNode, pasteTreeNode} from "./actions";
import {getActiveInputs, redo, undo} from "./treeManipulation";
import {compareTreePaths} from "./utils";
import {AbstractTreeInput} from "./components/abstractTreeInput";
import {getContextMenu, getTreePathOfElement} from "./globals/elements";
import {
    clearContextMenuSelectedElement,
    closeContextMenu,
    getHighlightElement,
    openContextMenu
} from "./components/contextMenu";

export let nextFocusElement: AbstractTreeInput | null = null;

/**
 * Resets the global variables used by the interface code.
 */
export function resetInterfaceGlobals(): void {
    clearContextMenuSelectedElement();
    nextFocusElement = null;
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

export function setNextFocusElement(input: AbstractTreeInput): void {
    nextFocusElement = input;
}

/**
 * Changes the focus to the next input element when TAB is pressed.
 * @param e the keydown event
 */
export function handleTabPressed(e: KeyboardEvent): void {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        e.preventDefault();
        const activeInputPaths: string[] = getActiveInputs().map(input => input.getTreePath());
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
    clearContextMenuSelectedElement();
}

/**
 * Sets the focus to the input element with the given tree path.
 * If that element is not found, will instead try to focus on the first input element in a subtree of that path.
 * If there is no input element in the subtree, will do nothing.
 */
export function setFocusElement(path: string): void {
    const focusedElement = getActiveInputs().find(input => compareTreePaths(path!, input.getTreePath()) <= 0);
    if (focusedElement) {
        focusedElement.focus();
    }
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
