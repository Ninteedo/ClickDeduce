import {getContextMenu, getPasteButton} from "../globals/elements";
import {hasClassOrParentHasClass} from "../utils";
import {hasCopyCache} from "../actions";
import {clearHighlight} from "../interface";

let contextMenuSelectedElement: HTMLElement | null = null;

/**
 * Opens the context menu on the subtree that was right-clicked.
 * @param e the mouse event
 */
export function openContextMenu(e: MouseEvent): void {
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
export function closeContextMenu(): void {
    getContextMenu().style.display = 'none';
    if (contextMenuSelectedElement !== null) {
        clearHighlight();
    }
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
