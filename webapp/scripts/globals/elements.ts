export function getBlocker(): HTMLElement {
    const blocker = document.getElementById('blocker');
    if (!blocker) throw new Error('Blocker not found');
    return blocker;
}

export function getTreeContainer(): HTMLElement {
    const container = document.getElementById('tree-container');
    if (!container) throw new Error('Tree container not found');
    return container;
}

export function getTree(): HTMLDivElement {
    const foundTree = document.getElementById('tree');
    if (!(foundTree instanceof HTMLDivElement)) throw new Error('Could not find tree element');
    return foundTree;
}

export function getFirstSubtree(): HTMLDivElement {
    const rootSubtree = document.querySelector('.subtree[data-tree-path=""]');
    if (!rootSubtree) throw new Error('Root subtree not found');
    return rootSubtree as HTMLDivElement;
}

export function getZoomToFitButton(): HTMLButtonElement {
    const zoomToFitButton = document.getElementById('zoom-to-fit') as HTMLButtonElement;
    if (!zoomToFitButton) throw new Error('Zoom to fit button not found');
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
        console.debug(element.outerHTML);
        throw new Error("Element does not have a tree path");
    }
    return treePath;
}

export function getUndoButton(): HTMLButtonElement {
    return document.getElementById('undoButton') as HTMLButtonElement;
}

export function getRedoButton(): HTMLButtonElement {
    return document.getElementById('redoButton') as HTMLButtonElement;
}

export function getLangSelector(): HTMLSelectElement {
    return document.getElementById('lang-selector') as HTMLSelectElement;
}

export function getErrorDiv(): HTMLDivElement {
    return document.getElementById('error-message') as HTMLDivElement;
}

export function getToggleControlsButton(): HTMLButtonElement {
    return document.getElementById('toggle-controls-button') as HTMLButtonElement;
}

export function getControlsDiv(): HTMLDivElement {
    return document.getElementById('controls') as HTMLDivElement;
}

export function getShortcutsLink(): HTMLAnchorElement {
    return document.getElementById('shortcuts-link') as HTMLAnchorElement;
}
