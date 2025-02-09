import {runAction} from "./actions";
import {updateTaskList} from "./components/tasks/taskManager";
import {resumeFileDragAndDrop, setupFileDragAndDrop, setupFileInput} from "./saveLoad";
import TreeHistoryManager from "./components/TreeHistoryManager";
import {getFirstSubtree, getRedoButton, getTree, getUndoButton} from "./globals/elements";
import {isAutoZoomEnabled, unlockPanZoom, zoomToFit} from "./components/panzoom";
import {Subtree} from "./components/subtree";
import {loadLangSelector, setCurrentLanguage} from "./langSelector";
import {modeRadios, setModeRadios, updateActiveInputsList} from "./activeInputs";

let treeHistoryManager: TreeHistoryManager;

let rootSubtree: Subtree | null = null;

export let lastNodeString: string | null = null;

/**
 * Resets the global variables used by the tree manipulation code.
 */
export function resetTreeManipulation(): void {
    treeHistoryManager = new TreeHistoryManager(getUndoButton(), getRedoButton());

    lastNodeString = null;
    rootSubtree = null;

    setModeRadios(Array.from(document.querySelectorAll('input[name="mode"]')));
    for (const radio of modeRadios) {
        radio.addEventListener('change', () => {
            runAction("IdentityAction", "");
        });
    }

    loadLangSelector();

    setupFileInput();
    setupFileDragAndDrop();
}


/**
 * Updates the contents of the tree.
 *
 * Also updates the state of the undo/redo buttons, which mode is selected, and which language is selected.
 *
 * @param newTreeHtml the new HTML to use for the tree
 * @param newNodeString the new node string to use for the tree
 * @param modeName the name of the mode to select
 * @param lang the language to select
 * @param addToHistory whether to add this change to the history
 */
export function updateTree(newTreeHtml: string, newNodeString: string, modeName: string, lang: string, addToHistory: boolean = false): void {
    getTree().innerHTML = newTreeHtml;
    lastNodeString = newNodeString;
    rootSubtree = new Subtree(getFirstSubtree(), null, newNodeString);
    if (addToHistory) {
        treeHistoryManager.addRecord({
            html: newTreeHtml,
            nodeString: newNodeString,
            mode: modeName,
            lang,
        });
    }
    treeHistoryManager.updateButtons();
    makeOrphanedInputsReadOnly();
    updateActiveInputsList();
    setSelectedMode(modeName);
    setCurrentLanguage(lang);
    updateTaskList(lang, lastNodeString);

    resumeFileDragAndDrop();
    unlockPanZoom();

    if (isAutoZoomEnabled()) zoomToFit();
}

export function reloadCurrentTree(): void {
    treeHistoryManager.reloadCurrentTree();
}

/**
 * Makes all inputs without a data-tree-path attribute read-only.
 */
function makeOrphanedInputsReadOnly(): void {
    document.querySelectorAll('#tree select:not([data-tree-path]), #tree input.literal:not([data-tree-path])').forEach(el => {
        el.setAttribute('readonly', "true");
        el.setAttribute('disabled', "true");
    });
}

/**
 * Undoes the last change to the tree.
 */
export function undo(): void {
    treeHistoryManager.undo();
}

/**
 * Redoes an undone change to the tree.
 */
export function redo(): void {
    treeHistoryManager.redo();
}

/**
 * The text input width is updated to match the text width
 *
 * Requires the font to be monospace
 *
 * @param textInput the text input to update
 */
export function updateTextInputWidth(textInput: HTMLInputElement): void {
    const minWidth: number = 2;
    textInput.style.width = Math.max(minWidth, textInput.value.length) + "ch";
}

export function setSelectedMode(mode: string): void {
    modeRadios.forEach(radio => {
        radio.checked = radio.value === mode;
    });
}

export function loadTreeFromString(nodeString: string): void {
    lastNodeString = nodeString;
    runAction("IdentityAction", "");
    zoomToFit();
}

export function getCurrentNodeString(): string | null {
    return lastNodeString;
}

export function getRootSubtree(): Subtree | null {
    return rootSubtree;
}
