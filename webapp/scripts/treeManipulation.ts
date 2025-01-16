import {hasClassOrParentHasClass} from "./utils";
import {runAction} from "./actions";
import {clearHighlight} from "./interface";
import {getLangSelectorNew} from "./serverRequest";
import {CustomExprSelector, replaceSelectInputs} from "./components/customExprSelector";
import {updateTaskList} from "./tasks";
import {createLiteralInputs, LiteralInput} from "./components/literalInput";
import {setupFileDragAndDrop, setupFileInput} from "./saveLoad";
import {AbstractTreeInput} from "./components/abstractTreeInput";
import {markHasUsedLangSelector} from "./attention";
import TreeHistoryManager from "./components/TreeHistoryManager";
import {getRedoButton, getTree, getUndoButton} from "./globals/elements";
import {isAutoZoomEnabled, zoomToFit} from "./components/panzoom";
import {getContextMenuSelectedElement} from "./components/contextMenu";

let treeHistoryManager: TreeHistoryManager;

let modeRadios: HTMLInputElement[];
let langSelector: HTMLSelectElement;

let activeInputs: AbstractTreeInput[] = [];
let literalInputs: LiteralInput[] = [];
let exprSelectors: CustomExprSelector[] = [];

export let lastNodeString: string | null = null;

/**
 * Resets the global variables used by the tree manipulation code.
 */
export function resetTreeManipulation(): void {
    treeHistoryManager = new TreeHistoryManager(getUndoButton(), getRedoButton());

    activeInputs = [];
    lastNodeString = null;

    modeRadios = Array.from(document.querySelectorAll('input[name="mode"]'));
    for (const radio of modeRadios) {
        radio.addEventListener('change', () => {
            runAction("IdentityAction", "");
        });
    }

    loadLangSelector();
    langSelector = document.getElementById('lang-selector') as HTMLSelectElement;

    setupFileInput();
    setupFileDragAndDrop();
}

/**
 * Loads the language selector HTML from the server and adds it to the DOM.
 */
function loadLangSelector(): void {
    const langSelectorContainer: HTMLDivElement = document.getElementById('lang-selector-div') as HTMLDivElement;

    langSelectorContainer.innerHTML = getLangSelectorNew();
    const langSelector: HTMLElement | null = document.getElementById('lang-selector');
    if (!langSelector) throw new Error('Language selector not found');
    langSelector.addEventListener('change', () => {
        markHasUsedLangSelector();
        runAction("IdentityAction", "");
    })
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
    treeCleanup();
    if (addToHistory) {
        treeHistoryManager.addRecord({
            html: newTreeHtml,
            nodeString: newNodeString,
            mode: modeName,
            lang,
        });
    }
    treeHistoryManager.updateButtons();
    literalInputs = createLiteralInputs();
    updateActiveInputsList();
    setSelectedMode(modeName);
    setCurrentLanguage(lang);
    updateTaskList(lang, lastNodeString);

    if (isAutoZoomEnabled()) zoomToFit();
}

/**
 * Updates the state of the tree after it has been changed.
 *
 * Adds hover listeners to the tree, makes orphaned inputs read-only,
 * and updates the stored initial values of literal inputs.
 */
function treeCleanup(): void {
    exprSelectors = replaceSelectInputs();
    addHoverListeners();
    makeOrphanedInputsReadOnly();
    makePhantomInputsReadOnly();
    makeDisabledInputsFocusOriginal();
    addClickListeners();
}

export function reloadCurrentTree(): void {
    treeHistoryManager.reloadCurrentTree();
}

/**
 * Adds hover listeners to the tree.
 *
 * These automatically add and remove the highlight class to the subtree elements.
 */
function addHoverListeners(): void {
    document.querySelectorAll('.subtree').forEach(div => {
        div.addEventListener('mouseover', (event) => {
            event.stopPropagation();  // Stop the event from bubbling up to parent subtree elements
            const target: EventTarget | null = event.currentTarget;

            // Remove the highlight from any other subtree elements
            if (!getContextMenuSelectedElement()) {
                document.querySelectorAll('.subtree').forEach(el => el.classList.remove('highlight'));
                if (target instanceof HTMLElement) {
                    target.classList.add('highlight');  // Add the highlight to the subtree currently hovered over
                }
            }
        });
        div.addEventListener('mouseout', (event) => {
            event.stopPropagation();  // Stop the event from bubbling up to parent subtree elements
            if (!getContextMenuSelectedElement()) {
                clearHighlight();  // Remove the highlight from currently hovered over subtree
            }
        });
    });
}

/**
 * Add left-click listeners to nodes that focus the node's input when clicked (if it exists).
 */
function addClickListeners(): void {
    document.querySelectorAll('.subtree').forEach(subtree => {
        if (subtree instanceof HTMLElement) {
            const input = subtree.querySelector(':scope > .node input:not([disabled])');
            if (input instanceof HTMLInputElement) {
                subtree.addEventListener('click', (evt) => {
                    if (!subtree.classList.contains('highlight')) return;
                    evt.preventDefault();
                    input.focus();
                    input.select();
                });
                subtree.querySelectorAll('input').forEach(input => {
                    input.addEventListener('click', (evt) => {
                        evt.stopPropagation();
                    });
                })
            }
        }
    });
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
 * Makes all inputs with the phantom class or part of a phantom subtree read-only.
 */
function makePhantomInputsReadOnly(): void {
    document.querySelectorAll('#tree select, #tree input').forEach(el => {
        if (el instanceof HTMLElement && hasClassOrParentHasClass(el, 'phantom')) {
            el.setAttribute('readonly', "true");
            el.setAttribute('disabled', "true");

            if (el.classList.contains('identifier-lookup')) {
                el.parentElement?.classList.add('dropdown-selector-container')
            }
        }
    })
}

function makeDisabledInputsFocusOriginal(): void {
    document.querySelectorAll('div.expr-selector-placeholder, div.type-dropdown-placeholder').forEach(input => {
        const treePath = input.getAttribute('data-tree-path');
        if (treePath === null) return;

        const origin = getTree().querySelector(`input:not([disabled])[data-tree-path="${treePath}"]`) as HTMLInputElement;
        input.addEventListener('mouseover', () => {
            origin.parentElement?.classList.add('guide-highlight');
        });
        input.addEventListener('mouseout', () => {
            origin.parentElement?.classList.remove('guide-highlight');
        });
    });
}

/**
 * Updates the list of inputs which the user can use.
 *
 * Also adds event listeners to the inputs.
 */
function updateActiveInputsList(): void {
    activeInputs = (literalInputs as AbstractTreeInput[]).concat(exprSelectors as AbstractTreeInput[]);
    activeInputs.sort((a, b) => {
        return a.getTreePath().localeCompare(b.getTreePath(), undefined, {numeric: true, sensitivity: 'base'});
    });
}

export function getActiveInputs(): AbstractTreeInput[] {
    return activeInputs;
}

export function getLiteralInputs(): LiteralInput[] {
    return literalInputs;
}

export function getExprSelectors(): CustomExprSelector[] {
    return exprSelectors;
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
