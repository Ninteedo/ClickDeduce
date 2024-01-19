import {handleLiteralChanged} from "./actions";
import {activeInputs} from "./treeManipulation";
import {hasClassOrParentHasClass} from "./utils";
import {panzoomInstance, tree} from "./initialise";

let errorDiv: HTMLDivElement;
export let nextFocusElement: HTMLElement = null;
export let contextMenuSelectedElement: HTMLElement = null;

/**
 * Resets the global variables used by the interface code.
 */
export function resetInterfaceGlobals(): void {
    contextMenuSelectedElement = null;
    nextFocusElement = null;
    errorDiv = document.getElementById('error-message') as HTMLDivElement;

    document.addEventListener('contextmenu', openContextMenu);
    document.addEventListener('click', closeContextMenu);
    document.getElementById('custom-context-menu').style.display = 'none';
}

/**
 * Handles the keydown event.
 *
 * On TAB, moves focus to the next input element.
 * On ENTER while focused on an input element, submits the literal change.
 *
 * @param e the keydown event
 */
export async function handleKeyDown(e: KeyboardEvent): Promise<void> {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        await handleTabPressed(e);
    } else if (e.key === 'Enter' && e.target instanceof HTMLInputElement) {
        e.preventDefault();
        nextFocusElement = e.target;
        await handleLiteralChanged(e.target);
    }
}

/**
 * Changes the focus to the next input element when TAB is pressed.
 * @param e the keydown event
 */
export async function handleTabPressed(e: KeyboardEvent): Promise<void> {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        e.preventDefault();
        let activeElemIndex = activeInputs.indexOf(e.target);
        if (e.shiftKey) {
            activeElemIndex -= 1;
        } else {
            activeElemIndex += 1;
        }
        if (activeElemIndex < 0) {
            activeElemIndex = activeInputs.length - 1;
        } else if (activeElemIndex >= activeInputs.length) {
            activeElemIndex = 0;
        }
        nextFocusElement = activeInputs[activeElemIndex];
        nextFocusElement.focus();
        if (nextFocusElement instanceof HTMLInputElement) {
            nextFocusElement.select();
        }
        // nextFocusElement = null;
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
    let target: EventTarget = e.target;
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

        const menu = document.getElementById('custom-context-menu');
        menu.style.display = 'block';
        menu.style.left = e.pageX + 'px';
        menu.style.top = e.pageY + 'px';
    } else {
        closeContextMenu(e);
    }
}

/**
 * Closes the context menu.
 * @param e the mouse event
 */
function closeContextMenu(e: MouseEvent): void {
    document.getElementById('custom-context-menu').style.display = 'none';
    if (contextMenuSelectedElement !== null) {
        clearHighlight();
    }
}

/**
 * Zooms the tree to fit the container.
 */
export function zoomToFit(): void {
    const container: HTMLElement = document.getElementById('tree-container');
    const firstSubtree: Element = tree.children[0];

    const widthScale: number = container.clientWidth / firstSubtree.clientWidth;
    const heightScale: number = container.clientHeight / firstSubtree.clientHeight;

    const newScale: number = Math.min(widthScale, heightScale);

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, newScale);
}

/**
 * Displays the given error message to the user.
 *
 * Disappears after 5 seconds.
 *
 * @param error the error to display, requires a 'message' property
 */
export function displayError(error: any): void {
    errorDiv.textContent = error.message;
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');
    setTimeout(() => {
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}
