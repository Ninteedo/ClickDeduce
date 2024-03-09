import {tree} from "./initialise";
import {hasClassOrParentHasClass} from "./utils";
import {handleLiteralChanged, runAction} from "./actions";
import {
    clearHighlight,
    contextMenuSelectedElement,
    displayError,
    handleKeyDown,
    isAutoZoomEnabled,
    zoomToFit
} from "./interface";
import {getLangSelectorNew} from "./serverRequest";
import {replaceSelectInputs} from "./customExprSelector";

let treeHistory: { mode: string; html: string; nodeString: string; lang: string }[] = [];
export let treeHistoryIndex: number = 0;
let undoButton: HTMLButtonElement
let redoButton: HTMLButtonElement;

let modeRadios: HTMLInputElement[];
let langSelector: HTMLSelectElement;

export let activeInputs: HTMLElement[] = [];
export let initialValues: [string, string][] = [];

export let lastNodeString: string = null;

const fileInput: HTMLInputElement = document.createElement('input');

/**
 * Resets the global variables used by the tree manipulation code.
 */
export function resetTreeManipulation(): void {
    treeHistory = [];
    treeHistoryIndex = 0;
    undoButton = document.getElementById('undoButton') as HTMLButtonElement;
    redoButton = document.getElementById('redoButton') as HTMLButtonElement;
    activeInputs = [];
    initialValues = [];
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
    const langSelector: HTMLElement = document.getElementById('lang-selector');
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
    tree.innerHTML = newTreeHtml;
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
    updateActiveInputsList();
    setSelectedMode(modeName);
    langSelector.value = lang;

    if (isAutoZoomEnabled()) zoomToFit();
}

/**
 * Updates the state of the tree after it has been changed.
 *
 * Adds hover listeners to the tree, makes orphaned inputs read-only,
 * and updates the stored initial values of literal inputs.
 */
function treeCleanup(): void {
    replaceSelectInputs();
    addHoverListeners();
    makeOrphanedInputsReadOnly();
    makePhantomInputsReadOnly();
    makeDisabledInputsFocusOriginal();
    setLiteralInitialValues();
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
            // Stop the event from bubbling up to parent subtree elements
            event.stopPropagation();
            const target: EventTarget = event.currentTarget;

            // Remove the highlight from any other subtree elements
            if (contextMenuSelectedElement === null) {
                document.querySelectorAll('.subtree').forEach(el => el.classList.remove('highlight'));
                if (target instanceof HTMLElement) {
                    // Add the highlight to the subtree currently hovered over
                    target.classList.add('highlight');
                }
            }
        });
        div.addEventListener('mouseout', (event) => {
            // Stop the event from bubbling up to parent subtree elements
            event.stopPropagation();
            // Remove the highlight from the currently hovered over subtree
            if (contextMenuSelectedElement === null) {
                clearHighlight();
            }
        });
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
        }
    })
}

function makeDisabledInputsFocusOriginal(): void {
    document.querySelectorAll('div.expr-selector-placeholder, div.type-dropdown-placeholder').forEach(input => {
        const treePath = input.getAttribute('data-tree-path');
        if (treePath === null) return;

        const origin = tree.querySelector(`input:not([disabled])[data-tree-path="${treePath}"]`) as HTMLInputElement;
        input.addEventListener('mouseover', () => {
            origin.parentElement.classList.add('guide-highlight');
        });
        input.addEventListener('mouseout', () => {
            origin.parentElement.classList.remove('guide-highlight');
        });
    });
}

/**
 * Updates the list of inputs which the user can use.
 *
 * Also adds event listeners to the inputs.
 */
function updateActiveInputsList(): void {
    activeInputs = Array.from(document.querySelectorAll(
        'input.literal[data-tree-path]:not([disabled]), input.expr-selector-input:not([disabled])'
    ));
    activeInputs.sort((a, b) => {
        const aPath = a.getAttribute("data-tree-path");
        const bPath = b.getAttribute("data-tree-path");
        return aPath.localeCompare(bPath, undefined, {numeric: true, sensitivity: 'base'});
    });
    activeInputs.forEach(input => {
        input.addEventListener('keydown', handleKeyDown);
        if (input instanceof HTMLInputElement && input.classList.contains('literal')) {
            input.addEventListener('blur', () => handleLiteralChanged(input));
            input.addEventListener('change', () => handleLiteralChanged(input));
            input.addEventListener('input', () => {
                updateTextInputWidth(input);
                updateLinkedInputPlaceholders(input);
            });
        }
    })
}

export function getActiveInputs(): HTMLElement[] {
    return activeInputs;
}

/**
 * Updates the list of initial values for literal inputs.
 */
function setLiteralInitialValues() {
    initialValues = [];
    document.querySelectorAll('input[data-tree-path]').forEach(input => {
        if (input instanceof HTMLInputElement) {
            initialValues.push([input.getAttribute('data-tree-path'), input.value]);
        }
    });
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

function updateLinkedInputPlaceholders(input: HTMLInputElement): void {
    const treePath: string = input.getAttribute('data-tree-path');
    const selector = `input[data-origin="${treePath}"]`;
    tree.querySelectorAll(selector).forEach((el: HTMLInputElement) => {
        el.value = input.value;
        updateTextInputWidth(el);
    });
}

function setSelectedMode(mode: string): void {
    modeRadios.forEach(radio => {
        radio.checked = radio.value === mode;
    });
}

let reEnableInputsId: number = 0;

function incrementReEnableInputsId(): void {
    reEnableInputsId = (reEnableInputsId + 1) % 1000;
}

export function disableInputs(): void {
    activeInputs.forEach(input => {
        input.setAttribute('readonly', "true");
        input.setAttribute('disabled', "true");
    });
    modeRadios.forEach(radio => radio.setAttribute('disabled', "true"));
    langSelector.setAttribute('disabled', "true");
    tree.querySelectorAll('.expr-selector-button').forEach(button => button.setAttribute('disabled', "true"));

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
    activeInputs.forEach(input => {
        input.removeAttribute('readonly');
        input.removeAttribute('disabled');
    });
    modeRadios.forEach(radio => {
        radio.removeAttribute('disabled');
    });
    langSelector.removeAttribute('disabled');
    tree.querySelectorAll('.expr-selector-button').forEach(button => button.removeAttribute('disabled'));
}

export function saveTree(): void {
    const contents = JSON.stringify({
        nodeString: lastNodeString,
        lang: langSelector.value,
        mode: modeRadios.find(radio => radio.checked).value,
    })
    const blob = new Blob([contents], {type: 'text/plain'});
    const url = window.URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = 'tree.cdtree';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);

    window.URL.revokeObjectURL(url);
}

function setupFileInput(): void {
    fileInput.type = 'file';
    fileInput.accept = '.cdtree';
    fileInput.onchange = (event) => {
        const file = (event.target as HTMLInputElement).files[0];
        const reader = new FileReader();
        reader.onload = () => {
            loadFromFile(reader);
        };
        reader.readAsText(file);
    };
}

function setupFileDragAndDrop(): void {
    const treeContainer = document.getElementById('tree-container');
    if (!treeContainer) {
        console.error('Tree container not found');
        return;
    }

    const highlightClass: string = 'file-drag-highlight';
    const addHighlight = () => treeContainer.classList.add(highlightClass);
    const removeHighlight = () => treeContainer.classList.remove(highlightClass);

    treeContainer.addEventListener('dragover', (event) => {
        event.preventDefault();
        addHighlight();
    });

    treeContainer.addEventListener('dragleave', removeHighlight);

    treeContainer.addEventListener('drop', (event) => {
        event.preventDefault();
        removeHighlight();

        const file: File = event.dataTransfer?.files[0];
        if (!file) {
            displayError('No file dropped');
            return;
        }

        const reader = new FileReader();
        reader.onload = () => loadFromFile(reader);
        reader.readAsText(file);
    });
}

function loadFromFile(reader: FileReader): void {
    reader.onerror = () => displayError(new Error('Error occurred while attempting to read file'))
    try {
        const contents: string = reader.result as string;
        const json = JSON.parse(contents);
        if (!json.nodeString || !json.lang || !json.mode) {
            throw new Error('Provided file did not contain required tree data');
        }
        langSelector.value = json.lang;
        setSelectedMode(json.mode);
        loadTreeFromString(json.nodeString);
    } catch (e) {
        if (e instanceof SyntaxError) {
            e = new SyntaxError('Provided file was not valid JSON');
        }
        displayError(e);
    }
}

export function loadTree(): void {
    fileInput.click();
}

function loadTreeFromString(nodeString: string): void {
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

    return recurse(lastNodeString, path.split('-').map(s => parseInt(s)).filter(n => !isNaN(n)));
}
