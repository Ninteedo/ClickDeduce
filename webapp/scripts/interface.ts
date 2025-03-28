import {copyTreeNode, deleteTreeNode, pasteTreeNode} from "./actions";
import {redo, undo} from "./treeManipulation";
import {getControlsDiv, getToggleControlsButton, getTreePathOfElementOptional} from "./globals/elements";
import {
    clearContextMenuSelectedElement,
    closeContextMenu,
    getHighlightElement,
    openContextMenu
} from "./components/contextMenu/contextMenu";
import {getFocusedSubtreePath, setNextFocusTreePath} from "./focus";
import {ClassDict} from "./globals/classDict";
import {IdDict} from "./globals/idDict";

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
    const activePath: string | null = getFocusedSubtreePath() ?? getTreePathOfElementOptional(getHighlightElement());

    const ctrl: boolean = e.ctrlKey;
    const shift: boolean = e.shiftKey;
    const key: string = e.key.toUpperCase();

    if (ctrl && key === 'Z') {
        if (shift) {
            redo();
        } else {
            undo();
        }
    } else if (activePath !== null) {
        if (ctrl && key === 'C') {
            copyTreeNode(activePath);
        } else if (ctrl && key === 'V') {
            e.preventDefault();
            pasteTreeNode(activePath);
        } else if (ctrl && key === 'X') {
            copyTreeNode(activePath);
            deleteTreeNode(activePath);
        }
    }
}

document.addEventListener('keydown', globalHandleKeyDown);

function setupValueTypeColourHighlightingCheckbox(): void {
    const valueTypeColourCheckbox = document.getElementById(IdDict.VALUE_HIGHLIGHTING_TOGGLE) as HTMLInputElement;
    valueTypeColourCheckbox.checked = true;
    valueTypeColourCheckbox.addEventListener('change', () => {
        toggleValueTypeColourHighlighting(valueTypeColourCheckbox.checked);
    });
    toggleValueTypeColourHighlighting(valueTypeColourCheckbox.checked);
}

function toggleValueTypeColourHighlighting(newState: boolean): void {
    const body = document.querySelector('body');
    if (!body) throw new Error('Body element not found');
    if (newState) {
        body.classList.add(ClassDict.VALUE_HIGHLIGHTING_ENABLED);
    } else {
        body.classList.remove(ClassDict.VALUE_HIGHLIGHTING_ENABLED);
    }
}

export function toggleControls(): void {
    const button = getToggleControlsButton();
    const controls = getControlsDiv();

    if (controls.classList.contains(ClassDict.HIDDEN)) {
        controls.classList.remove(ClassDict.HIDDEN);
        button.innerHTML = '&#9650;';
    } else {
        controls.classList.add(ClassDict.HIDDEN);
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
