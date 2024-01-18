import {handleLiteralChanged} from "./actions";
import {activeInputs} from "./treeManipulation";
import {hasClassOrParentHasClass} from "./utils";
import {panzoomInstance, tree} from "./initialise";

let errorDiv: HTMLDivElement;
export let nextFocusElement: HTMLElement = null;
export let contextMenuSelectedElement: HTMLElement = null;

export function resetInterfaceGlobals(): void {
    contextMenuSelectedElement = null;
    nextFocusElement = null;
    errorDiv = document.getElementById('error-message') as HTMLDivElement;

    document.addEventListener('contextmenu', openContextMenu);
    document.addEventListener('click', closeContextMenu);
    document.getElementById('custom-context-menu').style.display = 'none';
}

export async function handleKeyDown(e: KeyboardEvent): Promise<void> {
    if (e.key === 'Tab' && (e.target instanceof HTMLInputElement || e.target instanceof HTMLSelectElement)) {
        await handleTabPressed(e);
    } else if (e.key === 'Enter' && e.target instanceof HTMLInputElement) {
        e.preventDefault();
        nextFocusElement = e.target;
        await handleLiteralChanged(e.target);
    }
}

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

export function clearHighlight(): void {
    document.querySelector('.highlight')?.classList.remove('highlight');
    contextMenuSelectedElement = null;
}

function openContextMenu(e: MouseEvent): void {
    let target: EventTarget = e.target;
    if (contextMenuSelectedElement !== null) {
        // closes context menu if it is already open
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

function closeContextMenu(e: MouseEvent) {
    document.getElementById('custom-context-menu').style.display = 'none';
    if (contextMenuSelectedElement !== null) {
        clearHighlight();
    }
}

export function zoomToFit(): void {
    const container: HTMLElement = document.getElementById('tree-container');
    const firstSubtree: Element = tree.children[0];

    const widthScale: number = container.clientWidth / firstSubtree.clientWidth;
    const heightScale: number = container.clientHeight / firstSubtree.clientHeight;

    const newScale: number = Math.min(widthScale, heightScale);

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, newScale);
}

export function displayError(error: any): void {
    errorDiv.textContent = error.message;
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');
    setTimeout(() => {
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}
