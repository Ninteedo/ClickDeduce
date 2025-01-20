import {runAction} from "./actions";
import {getLangSelectorNew} from "./serverRequest";
import {CustomExprSelector} from "./components/customExprSelector";
import {updateTaskList} from "./tasks";
import {LiteralInput} from "./components/literalInput";
import {setupFileDragAndDrop, setupFileInput} from "./saveLoad";
import {AbstractTreeInput} from "./components/abstractTreeInput";
import {markHasUsedLangSelector} from "./attention";
import TreeHistoryManager from "./components/TreeHistoryManager";
import {getFirstSubtree, getRedoButton, getTree, getUndoButton} from "./globals/elements";
import {isAutoZoomEnabled, zoomToFit} from "./components/panzoom";
import {Subtree} from "./components/subtree";

let treeHistoryManager: TreeHistoryManager;

let modeRadios: HTMLInputElement[];
let langSelector: HTMLSelectElement;

let activeInputs: AbstractTreeInput[] = [];

let rootSubtree: Subtree | null = null;

export let lastNodeString: string | null = null;

/**
 * Resets the global variables used by the tree manipulation code.
 */
export function resetTreeManipulation(): void {
    treeHistoryManager = new TreeHistoryManager(getUndoButton(), getRedoButton());

    activeInputs = [];
    lastNodeString = null;
    rootSubtree = null;

    modeRadios = Array.from(document.querySelectorAll('input[name="mode"]'));
    for (const radio of modeRadios) {
        radio.addEventListener('change', () => {
            runAction("IdentityAction", "");
        });
    }

    langSelector = loadLangSelector();

    setupFileInput();
    setupFileDragAndDrop();
}

/**
 * Loads the language selector HTML from the server and adds it to the DOM.
 */
function loadLangSelector(): HTMLSelectElement {
    const langSelectorContainer: HTMLDivElement = document.getElementById('lang-selector-div') as HTMLDivElement;

    langSelectorContainer.innerHTML = getLangSelectorNew();
    const langSelector: HTMLElement | null = document.getElementById('lang-selector');
    if (!(langSelector instanceof HTMLSelectElement)) throw new Error('Language selector not found');
    langSelector.selectedIndex = 0;
    langSelector.addEventListener('change', () => {
        markHasUsedLangSelector();
        runAction("IdentityAction", "");
    });

    return langSelector;
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
    rootSubtree = new Subtree(getFirstSubtree(), null);
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
 * Updates the list of inputs which the user can use.
 *
 * Also adds event listeners to the inputs.
 */
function updateActiveInputsList(): void {
    activeInputs = getRootSubtree()!.getAllInputs();
    activeInputs.sort((a, b) => {
        return a.getTreePath().localeCompare(b.getTreePath(), undefined, {numeric: true, sensitivity: 'base'});
    });
}

export function getActiveInputs(): AbstractTreeInput[] {
    return activeInputs;
}

export function getLiteralInputs(): LiteralInput[] {
    return getActiveInputs().filter(input => input instanceof LiteralInput) as LiteralInput[];
}

export function getExprSelectors(): CustomExprSelector[] {
    return getActiveInputs().filter(input => input instanceof CustomExprSelector) as CustomExprSelector[];
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

let reEnableInputsId: number = 0;

function incrementReEnableInputsId(): void {
    reEnableInputsId = (reEnableInputsId + 1) % 1000;
}

export function disableInputs(): void {
    activeInputs.forEach(input => input.disable());
    modeRadios.forEach(radio => radio.setAttribute('disabled', "true"));
    langSelector.setAttribute('disabled', "true");
    getTree().querySelectorAll('.expr-selector-button').forEach(button => button.setAttribute('disabled', "true"));

    // re-enable inputs after 5 seconds
    incrementReEnableInputsId();
    const currentId = reEnableInputsId;
    setTimeout(() => {
        if (currentId === reEnableInputsId) {
            enableInputs();
        }
    }, 5000);
}

export function enableInputs(): void {
    incrementReEnableInputsId();
    activeInputs.forEach(input => input.enable());
    modeRadios.forEach(radio => {
        radio.removeAttribute('disabled');
    });
    langSelector.removeAttribute('disabled');
    getTree().querySelectorAll('.expr-selector-button').forEach(button => button.removeAttribute('disabled'));
}

export function loadTreeFromString(nodeString: string): void {
    lastNodeString = nodeString;
    runAction("IdentityAction", "");
    zoomToFit();
}

export function getCurrentLanguage(): string {
    return langSelector.value;
}

export function setCurrentLanguage(lang: string): void {
    langSelector.value = lang;
}

export function getCurrentNodeString(): string | null {
    return lastNodeString;
}

export function getRootSubtree(): Subtree | null {
    return rootSubtree;
}
