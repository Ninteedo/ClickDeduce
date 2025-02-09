import {copyTreeNode, deleteTreeNode, pasteTreeNode} from "./actions";
import {redo, undo} from "./treeManipulation";
import {compareTreePaths, parseTreePath} from "./utils";
import {
    getControlsDiv,
    getToggleControlsButton,
    getTreePathOfElement,
    getTreePathOfElementOptional
} from "./globals/elements";
import {
    clearContextMenuSelectedElement,
    closeContextMenu,
    getHighlightElement,
    openContextMenu
} from "./components/contextMenu/contextMenu";
import {setNextFocusTreePath} from "./focus";
import {getActiveInputs} from "./activeInputs";

/**
 * Resets the global variables used by the interface code.
 */
export function resetInterfaceGlobals(): void {
    clearContextMenuSelectedElement();
    setNextFocusTreePath(null);
    setupValueTypeColourHighlightingCheckbox();

    document.addEventListener('contextmenu', openContextMenu);
    document.addEventListener('click', closeContextMenu);
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
    let focusedElement = getActiveInputs().find(input => compareTreePaths(path, input.getTreePath()) <= 0);

    if (!focusedElement) {
        const parsedPath = parseTreePath(path);

        function isBefore(a: number[], b: number[]): boolean {
            let i = 0;
            while (i < a.length && i < b.length) {
                if (a[i] < b[i]) {
                    return true;
                } else if (a[i] > b[i]) {
                    return false;
                }
                i++;
            }
            return true;
        }

        function findLast<T>(arr: T[], pred: (elem: T) => boolean): T | undefined {
            for (let i = arr.length - 1; i >= 0; i--) {
                if (pred(arr[i])) return arr[i];
            }
            return undefined;
        }

        focusedElement = findLast(getActiveInputs(), input => isBefore(parseTreePath(input.getTreePath()), parsedPath));
    }

    focusedElement?.focus();
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

export function toggleControls(): void {
    const button = getToggleControlsButton();
    const controls = getControlsDiv();

    const hiddenClass = 'hidden';

    if (controls.classList.contains(hiddenClass)) {
        controls.classList.remove(hiddenClass);
        button.innerHTML = '&#9650;';
    } else {
        controls.classList.add(hiddenClass);
        button.innerHTML = '&#9660;';
    }
}

export function findTreePathFromElement(element: HTMLElement): string | null {
    let curr: HTMLElement | null = element;
    while (curr) {
        const treePath = getTreePathOfElementOptional(curr);
        if (treePath !== null) return treePath;
        curr = curr.parentElement;
    }
    return null;
}

// export function findInputFromElement(element: HTMLElement): AbstractTreeInput | null {
//     if (!hasClassOrParentHasClass(element, 'subtree')) return null;
//
//     let curr: HTMLElement | null = element;
//     while (curr && !(curr.classList.contains('literal') || curr.classList.contains('dropdown-selector-container') || curr.classList.contains('subtree'))) {
//         curr = curr.parentElement;
//     }
//     if (!curr) return null;
//
//     if (curr.classList.contains('literal')) {
//         const treePath = curr.getAttribute('data-tree-path');
//         if (!treePath) return null;
//         return getRootSubtree()?.getAllInputs().find(input => input instanceof LiteralInput && input.getTreePath() === treePath) ?? null;
//     } else if (curr.classList.contains('dropdown-selector-container')) {
//         const treePath = curr.getAttribute('data-tree-path');
//         if (!treePath) return null;
//         return getRootSubtree()?.getAllInputs().find(input => input instanceof CustomExprSelector && input.getTreePath() === treePath) ?? null;
//     } else if (curr.classList.contains('subtree')) {
//         const treePath = curr.getAttribute('data-tree-path');
//         if (!treePath) return null;
//         return getRootSubtree()?.getChildFromPath(parseTreePath(treePath))?.getPrimaryInput() ?? null;
//     } else {
//         return null;
//     }
// }
