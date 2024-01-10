import '../styles/stylesheet.css';
import panzoom, {PanZoom} from "panzoom";

let lastNodeString: string = "";

let treeHistory: { mode: string; html: string; nodeString: string; lang: string }[] = [];
let treeHistoryIndex: number = 0;

let tree: HTMLDivElement;
let undoButton: HTMLButtonElement
let redoButton: HTMLButtonElement;
let modeRadios: HTMLInputElement[];
let langSelector: HTMLSelectElement;

function getSelectedMode(): string {
    for (const radio of modeRadios) {
        if (radio.checked) {
            return radio.value;
        }
    }
    throw Error("No mode selected");
}

export async function handleSubmit(event: Event, url: string): Promise<void> {
    // prevent the form from submitting the old-fashioned way
    event.preventDefault();

    // send a POST request to the server
    await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            langName: getSelectedLanguage(),
        })
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, getSelectedMode(), getSelectedLanguage(), true);
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
                runAction("IdentityAction", "", [])
            }
        })
    });
}

function getSelectedLanguage(): string {
    return langSelector.value;
}

export async function handleDropdownChange(dropdown: HTMLSelectElement, kind: string): Promise<void> {
    const selectedValue: string = dropdown.value;
    const subtree: HTMLElement = dropdown.parentElement.parentElement;
    const dataTreePath: string = subtree.getAttribute("data-tree-path");

    let actionName: string = "SelectExprAction";
    if (kind === "type") {
        actionName = "SelectTypeAction";
    }

    await runAction(actionName, dataTreePath, [selectedValue]);
}

export async function handleLiteralChanged(textInput: HTMLInputElement): Promise<void> {
    const literalValue: string = textInput.value;
    const treePath: string = textInput.getAttribute("data-tree-path");

    let focusedTreePath: string = null;
    if (nextFocusElement != null) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    await runAction("EditLiteralAction", treePath, [literalValue]).then(() => {
        console.log(focusedTreePath);
        if (focusedTreePath == null) { return; }
        let focusedElement: HTMLElement = document.querySelector(`[data-tree-path="${focusedTreePath}"]`);
        if (focusedElement != null && focusedElement instanceof HTMLElement) {
            console.log(focusedElement);
            focusedElement.focus();
            if (focusedElement instanceof HTMLInputElement) {
                focusedElement.select();
            }
        }
    });
}

async function runAction(actionName: string, treePath: string, extraArgs: any[]): Promise<void> {
    const modeName: string = getSelectedMode();
    const langName: string = getSelectedLanguage();
    return fetch("/process-action", {
        method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify({
            langName,
            modeName,
            actionName,
            nodeString: lastNodeString,
            treePath,
            extraArgs
        })
    }).then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw Error(response.statusText + "\n" + text)
            });
        }
        return response;
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, modeName, langName, true)
    }).catch(error => {
        displayError(error);
        useTreeFromHistory(treeHistoryIndex);
        throw error;
    });
}

function updateTree(newTreeHtml: string, newNodeString: string, modeName: string, lang: string, addToHistory: boolean = false): void {
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
}

function useTreeFromHistory(newHistoryIndex: number): void {
    if (newHistoryIndex >= 0 && newHistoryIndex < treeHistory.length) {
        treeHistoryIndex = newHistoryIndex;
        const entry = treeHistory[newHistoryIndex];
        updateTree(entry.html, entry.nodeString, entry.mode, entry.lang, false);
    }
}

function updateUndoRedoButtons(): void {
    undoButton.disabled = treeHistoryIndex <= 0;
    redoButton.disabled = treeHistoryIndex >= treeHistory.length - 1;
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

let activeInputs: HTMLInputElement[] = [];

function updateActiveInputsList(): void {
    activeInputs = Array.from(document.querySelectorAll('input[data-tree-path]:not([disabled]), select[data-tree-path]:not([disabled])'));
    activeInputs.sort((a, b) => {
        const aPath = a.getAttribute("data-tree-path");
        const bPath = b.getAttribute("data-tree-path");
        return aPath.localeCompare(bPath, undefined, {numeric: true, sensitivity: 'base'});
    })
    activeInputs.forEach(input => {
        input.addEventListener('keydown', handleTabPressed);
        if (input.tagName === 'INPUT') {
            input.addEventListener('change', () => handleLiteralChanged(input));
            input.addEventListener('input', () => updateTextInputWidth(input));
        }
    })
}

let nextFocusElement: HTMLElement = null;

export async function handleTabPressed(e: KeyboardEvent): Promise<void> {
    if (e.code === 'Tab' && e.target instanceof HTMLInputElement) {
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

// the text input width is updated to match the text width
function updateTextInputWidth(textInput: HTMLInputElement): void {
    const minWidth: number = 2;
    textInput.style.width = Math.max(minWidth, textInput.value.length) + "ch";
}

function clearHighlight(): void {
    document.querySelector('.highlight')?.classList.remove('highlight');
    contextMenuSelectedElement = null;
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

function hasClassOrParentHasClass(element: HTMLElement, className: string): boolean {
    return element.classList.contains(className) ||
        (element.parentElement && hasClassOrParentHasClass(element.parentElement, className));
}

let contextMenuSelectedElement: HTMLElement = null;

export async function clearTreeNode(event: Event): Promise<void> {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        const treePath: string = contextMenuSelectedElement.getAttribute("data-tree-path")
        await runAction("DeleteAction", treePath, [])
    }
}

let copyCache: string = null;

export function copyTreeNode(): void {
    copyCache = contextMenuSelectedElement.getAttribute("data-node-string");
}

export async function pasteTreeNode(): Promise<void> {
    if (copyCache) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path");
        await runAction("PasteAction", treePath, [copyCache]);
    }
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

// Tree Panning and Zooming

// Initialize Panzoom
let panzoomInstance: PanZoom;

export function zoomToFit(): void {
    const container: HTMLElement = document.getElementById('tree-container');
    const firstSubtree: Element = tree.children[0];

    const widthScale: number = container.clientWidth / firstSubtree.clientWidth;
    const heightScale: number = container.clientHeight / firstSubtree.clientHeight;

    const newScale: number = Math.min(widthScale, heightScale);

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, newScale);
}

function displayError(error: any): void {
    const errorDiv: HTMLDivElement = document.getElementById('error-message') as HTMLDivElement;
    errorDiv.textContent = error.toString();
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');
    setTimeout(() => {
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}

export function initialise(): void {
    treeHistory = [];
    treeHistoryIndex = 0;
    contextMenuSelectedElement = null;
    activeInputs = [];
    nextFocusElement = null;
    copyCache = null;

    undoButton = document.getElementById('undoButton') as HTMLButtonElement;
    redoButton = document.getElementById('redoButton') as HTMLButtonElement;
    tree = document.getElementById('tree') as HTMLDivElement;
    modeRadios = Array.from(document.querySelectorAll('input[name="mode"]'));

    for (const radio of modeRadios) {
        radio.addEventListener('change', () => {
            runAction("IdentityAction", "", []);
        });
    }

    loadLangSelector().then(() => {
        langSelector = document.getElementById('lang-selector') as HTMLSelectElement;
    });
    updateUndoRedoButtons();

    document.getElementById('custom-context-menu').style.display = 'none';
    document.addEventListener('contextmenu', openContextMenu);
    document.addEventListener('click', closeContextMenu);

    panzoomInstance = panzoom(tree, {
        bounds: false, boundsPadding: 0, zoomDoubleClickSpeed: 1,
        onTouch: function (e: Event) {
            // TODO: cannot use on mobile currently
            return false;  // tells the library to not preventDefault.
        },
        filterKey: function (/* e, dx, dy, dz */) {
            return true;  // don't let panzoom handle this event:
        }
    });
}

(window as any).initialise = initialise;
(window as any).handleSubmit = handleSubmit;
(window as any).undo = undo;
(window as any).redo = redo;
(window as any).zoomToFit = zoomToFit;
(window as any).handleTabPressed = handleTabPressed;
(window as any).clearTreeNode = clearTreeNode;
(window as any).copyTreeNode = copyTreeNode;
(window as any).pasteTreeNode = pasteTreeNode;
(window as any).handleDropdownChange = handleDropdownChange;
(window as any).handleLiteralChanged = handleLiteralChanged;
