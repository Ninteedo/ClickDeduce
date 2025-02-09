import {ClassDict} from "./classDict";
import {IdDict} from "./idDict";

export function getBlocker(): HTMLElement {
    const blocker = document.getElementById(IdDict.BLOCKER);
    if (!blocker) throw new Error('Blocker not found');
    return blocker;
}

export function getTreeContainer(): HTMLElement {
    const container = document.getElementById(IdDict.TREE_CONTAINER);
    if (!container) throw new Error('Tree container not found');
    return container;
}

export function getTree(): HTMLDivElement {
    const foundTree = document.getElementById(IdDict.TREE);
    if (!(foundTree instanceof HTMLDivElement)) throw new Error('Could not find tree element');
    return foundTree;
}

export function getFirstSubtree(): HTMLDivElement {
    const rootSubtree = document.querySelector(`.${ClassDict.SUBTREE}[data-tree-path=""]`);
    if (!rootSubtree) throw new Error('Root subtree not found');
    return rootSubtree as HTMLDivElement;
}

export function getZoomToFitButton(): HTMLButtonElement {
    const zoomToFitButton = document.getElementById(IdDict.ZOOM_TO_FIT) as HTMLButtonElement;
    if (!zoomToFitButton) throw new Error('Zoom to fit button not found');
    return zoomToFitButton;
}

export function getSaveButton(): HTMLButtonElement {
    const saveButton = document.getElementById(IdDict.SAVE_BUTTON) as HTMLButtonElement;
    if (!saveButton) throw new Error('Save button not found');
    return saveButton;
}

export function getLoadButton(): HTMLButtonElement {
    const loadButton = document.getElementById(IdDict.LOAD_BUTTON) as HTMLButtonElement;
    if (!loadButton) throw new Error('Load button not found');
    return loadButton;
}

export function getExportLatexButton(): HTMLButtonElement {
    const exportLatexButton = document.getElementById(IdDict.EXPORT_LATEX_BUTTON) as HTMLButtonElement;
    if (!exportLatexButton) throw new Error('Export LaTeX button not found');
    return exportLatexButton;
}

export function getExportCopyButton(): HTMLButtonElement {
    const exportCopyButton = document.getElementById(IdDict.EXPORT_COPY_BUTTON) as HTMLButtonElement;
    if (!exportCopyButton) throw new Error('Export copy button not found');
    return exportCopyButton;
}

export function getExportCloseButton(): HTMLButtonElement {
    const exportCloseButton = document.getElementById(IdDict.EXPORT_CLOSE_BUTTON) as HTMLButtonElement;
    if (!exportCloseButton) throw new Error('Export close button not found');
    return exportCloseButton;
}

/**
 * Returns the tree path string of the given tree element.
 */
export function getTreePathOfElement(element: HTMLElement | null): string {
    if (element === null) throw new Error("Cannot get tree path of null");
    const treePath = getTreePathOfElementOptional(element);
    if (treePath === null) {
        console.debug(element.outerHTML);
        throw new Error("Element does not have a tree path");
    }
    return treePath;
}

export function getTreePathOfElementOptional(element: HTMLElement | null): string | null {
    if (element === null) return null;
    return element.getAttribute("data-tree-path");
}

export function getUndoButton(): HTMLButtonElement {
    return document.getElementById(IdDict.UNDO_BUTTON) as HTMLButtonElement;
}

export function getRedoButton(): HTMLButtonElement {
    return document.getElementById(IdDict.REDO_BUTTON) as HTMLButtonElement;
}

export function getLangSelector(): HTMLSelectElement {
    return document.getElementById(IdDict.LANG_SELECTOR) as HTMLSelectElement;
}

export function getErrorDiv(): HTMLDivElement {
    return document.getElementById(IdDict.ERROR_MESSAGE) as HTMLDivElement;
}

export function getToggleControlsButton(): HTMLButtonElement {
    return document.getElementById(IdDict.TOGGLE_CONTROLS_BUTTON) as HTMLButtonElement;
}

export function getControlsDiv(): HTMLDivElement {
    return document.getElementById(IdDict.CONTROLS) as HTMLDivElement;
}

export function getShortcutsLink(): HTMLAnchorElement {
    return document.getElementById(IdDict.SHORTCUTS_LINK) as HTMLAnchorElement;
}
