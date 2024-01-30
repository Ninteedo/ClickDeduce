import {tree} from "./initialise";
import {hasClassOrParentHasClass} from "./utils";
import {handleDropdownChange, handleExprSelectorChoice, handleLiteralChanged, runAction} from "./actions";
import {clearHighlight, contextMenuSelectedElement, displayError, handleKeyDown} from "./interface";

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
export async function resetTreeManipulation(): Promise<void> {
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

    await loadLangSelector().then(() => {
        langSelector = document.getElementById('lang-selector') as HTMLSelectElement;
    });

    setupFileInput();
    setupDragAndDrop();
}

/**
 * Loads the language selector HTML from the server and adds it to the DOM.
 */
async function loadLangSelector(): Promise<void> {
    const langSelectorContainer: HTMLDivElement = document.getElementById('lang-selector-div') as HTMLDivElement;

    await fetch('get-lang-selector', {
        method: 'GET'
    }).then(response => response.json()).then(langSelector => {
        langSelectorContainer.innerHTML = langSelector.langSelectorHtml;
    }).then(() => {
        const langSelector: HTMLElement = document.getElementById('lang-selector');
        langSelector.addEventListener('change', () => {
            runAction("IdentityAction", "", []);
        })
    });
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
        const newEntry: { mode: string; html: string; nodeString: string; lang: string } = {
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
    addDropdownListeners();
    makeOrphanedInputsReadOnly();
    makePhantomInputsReadOnly();
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

function addDropdownListeners(): void {
    document.querySelectorAll('.expr-dropdown').forEach(select => {
        if (select instanceof HTMLSelectElement) {
            select.addEventListener('change', (event) => {
                handleDropdownChange(select, 'expr');
            })
        }
    });

    document.querySelectorAll('.type-dropdown').forEach(select => {
        if (select instanceof HTMLSelectElement) {
            select.addEventListener('change', (event) => {
                handleDropdownChange(select, 'type');
            })
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
        }
    })
}

/**
 * Updates the list of inputs which the user can use.
 *
 * Also adds event listeners to the inputs.
 */
function updateActiveInputsList(): void {
    activeInputs = Array.from(document.querySelectorAll('input[data-tree-path]:not([disabled]), select[data-tree-path]:not([disabled])'));
    activeInputs.sort((a, b) => {
        const aPath = a.getAttribute("data-tree-path");
        const bPath = b.getAttribute("data-tree-path");
        return aPath.localeCompare(bPath, undefined, {numeric: true, sensitivity: 'base'});
    })
    activeInputs.forEach(input => {
        input.addEventListener('keydown', handleKeyDown);
        if (input instanceof HTMLInputElement) {
            input.addEventListener('change', () => handleLiteralChanged(input));
            input.addEventListener('input', () => updateTextInputWidth(input));
            input.addEventListener('blur', () => handleLiteralChanged(input));
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

function replaceSelectInputs(): void {
    const selectInputs: NodeListOf<HTMLSelectElement> = document.querySelectorAll('select.expr-dropdown[data-tree-path]:not([disabled])');
    selectInputs.forEach(select => {
        const options = Array.from(select.options).slice(1);
        const treePath = select.getAttribute('data-tree-path');
        const newHtml =
            `<div class="expr-selector-container" data-tree-path="${treePath}">
              <input type="text" class="expr-selector-input" placeholder="Enter Expression" />
              <button class="expr-selector-button">&#9660;</button>
              <div class="expr-selector-dropdown">
                ${options.map(option => option.outerHTML).join('')}
              </div>
            </div>`
        select.outerHTML = newHtml;

        const newSelector = tree.querySelector(`.expr-selector-container[data-tree-path="${treePath}"]`) as HTMLDivElement;
        const input = newSelector.querySelector('.expr-selector-input') as HTMLInputElement;
        const button = newSelector.querySelector('.expr-selector-button') as HTMLButtonElement;
        const dropdown = newSelector.querySelector('.expr-selector-dropdown') as HTMLDivElement;

        dropdown.style.display = 'none';

        input.addEventListener('input', () => updateExprSelectorDropdown(newSelector));
        input.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                selectorEnterPressed(newSelector);
            } else if (event.key === 'ArrowDown') {
                moveSelectorOptionHighlight(newSelector, 1);
            } else if (event.key === 'ArrowUp') {
                moveSelectorOptionHighlight(newSelector, -1);
            }
        });
        input.addEventListener('focus', () => showExprSelectorDropdown(newSelector));
        input.addEventListener('blur', () => hideExprSelectorDropdown(newSelector));

        button.addEventListener('click', () => toggleExprSelectorDropdownDisplay(newSelector));

        const selectorOptions = Array.from(dropdown.querySelectorAll('option'));
        selectorOptions.forEach(option => {
            option.addEventListener('click', () => selectorSelectOption(newSelector, option));
            option.classList.add('expr-selector-option');
        });
    });
}

function updateExprSelectorDropdown(selectorDiv: HTMLDivElement, keepOpenWhenEmpty: boolean = false) {
    const input = selectorDiv.querySelector('.expr-selector-input') as HTMLInputElement;
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const options = Array.from(dropdown.querySelectorAll('option'));

    if (input.value === '' && !keepOpenWhenEmpty) {
        if (dropdown.style.display !== 'none') {
            toggleExprSelectorDropdownDisplay(selectorDiv);
        }
        return;
    }

    if (dropdown.style.display === 'none') {
        toggleExprSelectorDropdownDisplay(selectorDiv);
    }

    const filterText = input.value.toLowerCase();
    options.forEach(option => {
        if (option.innerText.toLowerCase().includes(filterText)) {
            option.style.display = 'block';
        } else {
            option.style.display = 'none';
        }
    });

    setExprSelectorOptionHighlight(selectorDiv, 0);
}

function setExprSelectorOptionHighlight(selectorDiv: HTMLDivElement, highlightIndex: number) {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const options = Array.from(dropdown.querySelectorAll('option'));
    options.forEach(option => option.classList.remove('highlight'));
    const filtered = options.filter(option => option.style.display !== 'none');
    if (highlightIndex >= 0 && highlightIndex < filtered.length) {
        filtered[highlightIndex].classList.add('highlight');
    }
}

function getExprSelectorOptionHighlight(selectorDiv: HTMLDivElement, ignoreHidden: boolean): number {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    let options = Array.from(dropdown.querySelectorAll('option'));

    if (ignoreHidden) {
        options = options.filter(option => option.style.display !== 'none');
    }

    return options.findIndex(option => option.classList.contains('highlight'));
}

function moveSelectorOptionHighlight(selectorDiv: HTMLDivElement, offset: number) {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const options = Array.from(dropdown.querySelectorAll('option'));
    const filtered = options.filter(option => option.style.display !== 'none');
    const currentHighlightIndex = getExprSelectorOptionHighlight(selectorDiv, true);
    const newHighlightIndex = (currentHighlightIndex + offset) % filtered.length;
    setExprSelectorOptionHighlight(selectorDiv, newHighlightIndex);
}

function toggleExprSelectorDropdownDisplay(selectorDiv: HTMLDivElement) {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    if (dropdown.style.display === 'none') {
        showExprSelectorDropdown(selectorDiv);
    } else {
        hideExprSelectorDropdown(selectorDiv);
    }
}

function showExprSelectorDropdown(selectorDiv: HTMLDivElement) {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const button = selectorDiv.querySelector('.expr-selector-button') as HTMLButtonElement;
    if (dropdown.style.display === 'none') {
        button.innerHTML = '&#9650;';
        dropdown.style.display = 'block';
        updateExprSelectorDropdown(selectorDiv, true);
    }
}

function hideExprSelectorDropdown(selectorDiv: HTMLDivElement) {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const button = selectorDiv.querySelector('.expr-selector-button') as HTMLButtonElement;
    button.innerHTML = '&#9660;';
    dropdown.style.display = 'none';
}

function selectorSelectOption(selectorDiv: HTMLDivElement, option: HTMLOptionElement) {
    const input = selectorDiv.querySelector('.expr-selector-input') as HTMLInputElement;
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const button = selectorDiv.querySelector('.expr-selector-button') as HTMLButtonElement;
    input.value = option.innerText;
    dropdown.style.display = 'none';
    button.innerHTML = '&#9660;';
    handleExprSelectorChoice(selectorDiv, option.value);
}

function selectorEnterPressed(selectorDiv: HTMLDivElement) {
    const input = selectorDiv.querySelector('.expr-selector-input') as HTMLInputElement;
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const button = selectorDiv.querySelector('.expr-selector-button') as HTMLButtonElement;
    const options = Array.from(dropdown.querySelectorAll('option'));

    if (dropdown.style.display === 'none') {
        toggleExprSelectorDropdownDisplay(selectorDiv);
        return;
    }

    const selectedIndex = getExprSelectorOptionHighlight(selectorDiv, false);
    const selectedOption = options[selectedIndex];
    if (selectedOption) {
        selectorSelectOption(selectorDiv, selectedOption);
    } else {
        toggleExprSelectorDropdownDisplay(selectorDiv);
    }
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
function updateTextInputWidth(textInput: HTMLInputElement): void {
    const minWidth: number = 2;
    textInput.style.width = Math.max(minWidth, textInput.value.length) + "ch";
}

function setSelectedMode(mode: string): void {
    modeRadios.forEach(radio => {
        radio.checked = radio.value === mode;
    });
}

export function disableInputs(): void {
    activeInputs.forEach(input => {
        input.setAttribute('readonly', "true");
        input.setAttribute('disabled', "true");
    });
    modeRadios.forEach(radio => {
        radio.setAttribute('disabled', "true");
    });
    langSelector.setAttribute('disabled', "true");
}

export function enableInputs(): void {
    activeInputs.forEach(input => {
        input.removeAttribute('readonly');
        input.removeAttribute('disabled');
    });
    modeRadios.forEach(radio => {
        radio.removeAttribute('disabled');
    });
    langSelector.removeAttribute('disabled');
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

function setupDragAndDrop(): void {
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

async function loadTreeFromString(nodeString: string): Promise<void> {
    lastNodeString = nodeString;
    await runAction("IdentityAction", "", [])
}
