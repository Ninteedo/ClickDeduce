import {tree} from "./script";
import {hasClassOrParentHasClass} from "./utils";
import {handleLiteralChanged, runAction} from "./actions";
import {clearHighlight, contextMenuSelectedElement, handleKeyDown} from "./interface";

let treeHistory: { mode: string; html: string; nodeString: string; lang: string }[] = [];
export let treeHistoryIndex: number = 0;
let undoButton: HTMLButtonElement
let redoButton: HTMLButtonElement;

let modeRadios: HTMLInputElement[];
let langSelector: HTMLSelectElement;

export let activeInputs: HTMLElement[] = [];
export let initialValues: [string, string][] = [];

export let lastNodeString: string = "";


export async function resetTreeManipulation(): Promise<void> {
    treeHistory = [];
    treeHistoryIndex = 0;
    undoButton = document.getElementById('undoButton') as HTMLButtonElement;
    redoButton = document.getElementById('redoButton') as HTMLButtonElement;
    activeInputs = [];
    initialValues = [];
    lastNodeString = "";

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
}

async function loadLangSelector(): Promise<void> {
    const langSelectorContainer: HTMLDivElement = document.getElementById('lang-selector-div') as HTMLDivElement;

    await fetch('get-lang-selector', {
        method: 'GET'
    }).then(response => response.json()).then(langSelector => {
        langSelectorContainer.innerHTML = langSelector.langSelectorHtml;
    }).then(() => {
        const langSelector: HTMLElement = document.getElementById('lang-selector');
        langSelector.addEventListener('change', () => {
            if (lastNodeString !== "") {
                runAction("IdentityAction", "", []);
            }
        })
    });
}

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
    modeRadios.forEach(radio => {
        radio.checked = radio.value === modeName;
    });
    langSelector.value = lang;
}

function treeCleanup(): void {
    addHoverListeners();
    makeOrphanedInputsReadOnly();
    makePhantomInputsReadOnly();
    setLiteralInitialValues();
}

export function useTreeFromHistory(newHistoryIndex: number): void {
    if (newHistoryIndex >= 0 && newHistoryIndex < treeHistory.length) {
        treeHistoryIndex = newHistoryIndex;
        const entry = treeHistory[newHistoryIndex];
        updateTree(entry.html, entry.nodeString, entry.mode, entry.lang, false);
    }
}

export function updateUndoRedoButtons(): void {
    undoButton.disabled = treeHistoryIndex <= 0;
    redoButton.disabled = treeHistoryIndex >= treeHistory.length - 1;
}

function addHoverListeners(): void {
    document.querySelectorAll('.subtree').forEach(div => {
        div.addEventListener('mouseover', (event) => {
            // Stop the event from bubbling up to parent 'subtree' elements
            event.stopPropagation();
            const target: EventTarget = event.currentTarget;

            // Remove the highlight from any other 'subtree' elements
            if (contextMenuSelectedElement === null) {
                document.querySelectorAll('.subtree').forEach(el => el.classList.remove('highlight'));
                if (target instanceof HTMLElement) {
                    // Add the highlight to the currently hovered over 'subtree'
                    target.classList.add('highlight');
                }
            }
        });
        div.addEventListener('mouseout', (event) => {
            // Stop the event from bubbling up to parent 'subtree' elements
            event.stopPropagation();
            // Remove the highlight from the currently hovered over 'subtree'
            if (contextMenuSelectedElement === null) {
                clearHighlight();
            }
        });
    });
}

function makeOrphanedInputsReadOnly(): void {
    document.querySelectorAll('#tree select:not([data-tree-path]), #tree input:not([data-tree-path])').forEach(el => {
        el.setAttribute('readonly', "true");
        el.setAttribute('disabled', "true");
    });
}

function makePhantomInputsReadOnly(): void {
    document.querySelectorAll('#tree select, #tree input').forEach(el => {
        if (el instanceof HTMLElement && hasClassOrParentHasClass(el, 'phantom')) {
            el.setAttribute('readonly', "true");
            el.setAttribute('disabled', "true");
        }
    })
}

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

function setLiteralInitialValues() {
    document.querySelectorAll('input[data-tree-path]').forEach(input => {
        if (input instanceof HTMLInputElement) {
            initialValues.push([input.getAttribute('data-tree-path'), input.value]);
        }
    });
}

export function undo(): void {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length) {
        useTreeFromHistory(treeHistoryIndex - 1);
    }
}

export function redo(): void {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length - 1) {
        useTreeFromHistory(treeHistoryIndex + 1);
    }
}

// the text input width is updated to match the text width, requires the font to be monospace
function updateTextInputWidth(textInput: HTMLInputElement): void {
    const minWidth: number = 2;
    textInput.style.width = Math.max(minWidth, textInput.value.length) + "ch";
}
