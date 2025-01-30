import {hasClassOrParentHasClass, parseTreePath} from "../../utils";
import {getRootSubtree} from "../../treeManipulation";
import {SubtreeContextMenu} from "./SubtreeContextMenu";
import {AbstractContextMenu} from "./AbstractContextMenu";

let contextMenuSelectedElement: HTMLElement | null = null;

/**
 * Opens the context menu on the subtree that was right-clicked.
 * @param e the mouse event
 */
export function openContextMenu(e: MouseEvent): void {
    const highlightElement = getHighlightElementFromEvent(e);

    if (!highlightElement || hasClassOrParentHasClass(highlightElement, 'phantom')) {
        closeContextMenu();
    } else {
        const subtree = getRootSubtree()?.getChildFromPath(parseTreePath(highlightElement.getAttribute('data-tree-path')!));
        if (subtree) {
            e.preventDefault();
            closeContextMenu();
            activeContextMenu = new SubtreeContextMenu(e, subtree);
            contextMenuSelectedElement = highlightElement;
        }
    }
}

/**
 * Closes the context menu.
 */
export function closeContextMenu(): void {
    activeContextMenu?.close();
    activeContextMenu = null;
    clearContextMenuSelectedElement();
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

export function getContextMenuSelectedElement(): HTMLElement | null {
    return contextMenuSelectedElement;
}

export function clearContextMenuSelectedElement(): void {
    contextMenuSelectedElement = null;
}

let activeContextMenu: AbstractContextMenu | null = null;

export function getActiveContextMenu(): AbstractContextMenu | null {
    return activeContextMenu;
}
