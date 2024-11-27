import {hasClassOrParentHasClass} from "./utils";
import {runAction} from "./actions";
import {clearHighlight, contextMenuSelectedElement, isAutoZoomEnabled, zoomToFit} from "./interface";
import {getLangSelectorNew} from "./serverRequest";
import {CustomExprSelector, replaceSelectInputs} from "./components/customExprSelector";
import {updateTaskList} from "./tasks";
import {createLiteralInputs, LiteralInput} from "./components/literalInput";
import {setupFileDragAndDrop, setupFileInput} from "./saveLoad";
import {AbstractTreeInput} from "./components/abstractTreeInput";

let treeHistory: { mode: string; html: string; nodeString: string; lang: string }[] = [];
export let treeHistoryIndex: number = 0;
let undoButton: HTMLButtonElement
let redoButton: HTMLButtonElement;

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
    treeHistory = [];
    treeHistoryIndex = 0;
    undoButton = document.getElementById('undoButton') as HTMLButtonElement;
    redoButton = document.getElementById('redoButton') as HTMLButtonElement;
    activeInputs = [];
    lastNodeString = null;

    updateUndoRedoButtons();

    modeRadios = Array.from(document.querySelectorAll('input[name="mode"]'));
    for (const radio of modeRadios) {
        radio.addEventListener('change', () => {
            runAction("IdentityAction", "", []);
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
        runAction("IdentityAction", "", []);
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
    if (addToHistory && (treeHistory.length === 0 ||
        (newTreeHtml !== treeHistory[treeHistoryIndex].html || newNodeString !== treeHistory[treeHistoryIndex].nodeString))) {
        if (treeHistoryIndex < treeHistory.length - 1) {
            treeHistory = treeHistory.slice(0, treeHistoryIndex + 1);
        }
        const newEntry = {
            html: newTreeHtml,
            nodeString: newNodeString,
            mode: modeName,
            lang,
        };
        treeHistoryIndex = treeHistory.push(newEntry) - 1;
    }
    updateUndoRedoButtons();
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

/**
 * Updates the tree to the state it was in at the given history index.
 * @param newHistoryIndex the index of the tree history entry to use
 */
export function useTreeFromHistory(newHistoryIndex: number): void {
    if (newHistoryIndex >= 0 && newHistoryIndex < treeHistory.length) {
        treeHistoryIndex = newHistoryIndex;
        const entry = treeHistory[newHistoryIndex];
        updateTree(entry.html, entry.nodeString, entry.mode, entry.lang, false);
    }
}

/**
 * Updates whether the undo/redo buttons are disabled.
 */
export function updateUndoRedoButtons(): void {
    undoButton.disabled = treeHistoryIndex <= 0;
    redoButton.disabled = treeHistoryIndex >= treeHistory.length - 1;
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
            if (contextMenuSelectedElement === null) {
                document.querySelectorAll('.subtree').forEach(el => el.classList.remove('highlight'));
                if (target instanceof HTMLElement) {
                    target.classList.add('highlight');  // Add the highlight to the subtree currently hovered over
                }
            }
        });
        div.addEventListener('mouseout', (event) => {
            event.stopPropagation();  // Stop the event from bubbling up to parent subtree elements
            if (contextMenuSelectedElement === null) {
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
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length) {
        useTreeFromHistory(treeHistoryIndex - 1);
    }
}

/**
 * Redoes an undone change to the tree.
 */
export function redo(): void {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length - 1) {
        useTreeFromHistory(treeHistoryIndex + 1);
    }
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
    runAction("IdentityAction", "", [])
}

/**
 * Finds the substring of the node string at the given path.
 * @param path the tree path to the node, integers separated by dashes
 */
export function getNodeStringFromPath(path: string): string {
    /**
     * Parses the node string and returns the arguments of the given node.
     * <p>
     * The node string is the name followed by a comma-separated list of arguments in parentheses.
     * The arguments can themselves be nodes, so a recursive approach is used.
     * </p>
     * <p>
     * There can also be string literals in the arguments
     * which can contain commas, and parentheses, and escape characters.
     * </p>
     * <p>
     * For example, the node string <code>'Plus(Num("1"), Times(Num(""), Num("mess(\")\\")))'</code>
     * would return <code>['Num("1")', 'Times(Num(""), Num("mess(\")\\"))']</code>.
     * </p>
     *
     * @param node
     */
    function nodeArgs(node: string): string[] {
        let current: string = '';
        let nodes: string[] = [];
        let depth: number = 0;
        let escaped: boolean = false;
        let inString: boolean = false;
        for (let char of node) {
            if (escaped) {
                current += "\\" + char;
                escaped = false;
            } else if (char === '\\') {
                escaped = true;
            } else if (char === '(' && !inString) {
                if (depth === 0) {
                    current = '';
                } else {
                    current += char;
                }
                depth += 1;
            } else if (char === ')' && !inString) {
                depth -= 1;
                if (depth === 0) {
                    nodes.push(current);
                } else {
                    current += char;
                }
            } else if (char === ',' && depth === 1 && !inString) {
                nodes.push(current);
                current = '';
            } else if (char === '"' && !escaped) {
                inString = !inString;
                current += char;
            } else {
                current += char;
            }
        }
        return nodes;
    }

    function recurse(curr: string, remaining: number[]): string {
        if (remaining.length === 0) {
            return curr;
        }

        const next = remaining.shift();
        if (next === undefined) {
            throw new Error('Unexpected undefined value');
        }

        const nodeArgsList = nodeArgs(curr)[1];
        const innerNode = nodeArgs(nodeArgsList)[next];
        const nextNodeString = nodeArgs(innerNode)[0];
        return recurse(nextNodeString, remaining);
    }

    if (!lastNodeString) {
        throw new Error('No node string to get path from');
    }
    return recurse(lastNodeString, path.split('-').map(s => parseInt(s)).filter(n => !isNaN(n)));
}

export function getTree(): HTMLDivElement {
    const foundTree = document.getElementById('tree');
    if (foundTree instanceof HTMLDivElement) {
        return foundTree;
    } else {
        throw new Error('Could not find tree element');
    }
}

export function getCurrentLanguage(): string {
    return langSelector.value;
}

export function setCurrentLanguage(lang: string): void {
    langSelector.value = lang;
}
