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

function getSelectedMode() {
    for (const radio of modeRadios) {
        if (radio.checked) {
            return radio.value;
        }
    }
    throw Error("No mode selected");
}

export async function handleSubmit(event: { preventDefault: () => void; }, url: string) {
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

async function loadLangSelector() {
    const langSelectorContainer = document.getElementById('lang-selector-div');

    await fetch('get-lang-selector', {
        method: 'GET'
    }).then(response => response.json()).then(langSelector => {
        langSelectorContainer.innerHTML = langSelector.langSelectorHtml;
    }).then(() => {
        const langSelector = document.getElementById('lang-selector');
        langSelector.addEventListener('change', () => {
            if (lastNodeString !== "") {
                runAction("IdentityAction", "", [])
            }
        })
    });
}

function getSelectedLanguage() {
    return langSelector.value;
}

export function handleDropdownChange(dropdown: HTMLSelectElement, kind: String) {
    const selectedValue = dropdown.value;
    const subtree = dropdown.parentElement.parentElement;
    const dataTreePath = subtree.getAttribute("data-tree-path");

    let actionName = "SelectExprAction";
    if (kind === "type") {
        actionName = "SelectTypeAction";
    }

    runAction(actionName, dataTreePath, [selectedValue]);
}

export function handleLiteralChanged(textInput: HTMLInputElement) {
    const literalValue = textInput.value;
    const treePath = textInput.getAttribute("data-tree-path");

    let focusedTreePath: string = null;
    if (nextFocusElement != null) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    runAction("EditLiteralAction", treePath, [literalValue]).then(() => {
        console.log(focusedTreePath);
        if (focusedTreePath == null) { return; }
        let focusedElement = document.querySelector(`[data-tree-path="${focusedTreePath}"]`);
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
    const modeName = getSelectedMode();
    const langName = getSelectedLanguage();
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

function treeCleanup() {
    addHoverListeners();
    makeOrphanedInputsReadOnly();
    makePhantomInputsReadOnly();
}

function useTreeFromHistory(newHistoryIndex: number) {
    if (newHistoryIndex >= 0 && newHistoryIndex < treeHistory.length) {
        treeHistoryIndex = newHistoryIndex;
        const entry = treeHistory[newHistoryIndex];
        updateTree(entry.html, entry.nodeString, entry.mode, entry.lang, false);
    }
}

function updateUndoRedoButtons() {
    undoButton.disabled = treeHistoryIndex <= 0;
    redoButton.disabled = treeHistoryIndex >= treeHistory.length - 1;
}

export function undo() {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length) {
        useTreeFromHistory(treeHistoryIndex - 1);
    }
}

export function redo() {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length - 1) {
        useTreeFromHistory(treeHistoryIndex + 1);
    }
}

let activeInputs: HTMLInputElement[] = [];

function updateActiveInputsList() {
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

export async function handleTabPressed(e: { code: string; preventDefault: () => void; target: EventTarget; shiftKey: boolean; }) {
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
function updateTextInputWidth(textInput: HTMLInputElement) {
    const minWidth = 2;
    textInput.style.width = Math.max(minWidth, textInput.value.length) + "ch";
}

function clearHighlight() {
    document.querySelector('.highlight')?.classList.remove('highlight');
    contextMenuSelectedElement = null;
}

function addHoverListeners() {
    document.querySelectorAll('.subtree').forEach(div => {
        div.addEventListener('mouseover', (event) => {
            // Stop the event from bubbling up to parent 'subtree' elements
            event.stopPropagation();
            const target = event.currentTarget;

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

function makeOrphanedInputsReadOnly() {
    document.querySelectorAll('#tree select:not([data-tree-path]), #tree input:not([data-tree-path])').forEach(el => {
        el.setAttribute('readonly', "true");
        el.setAttribute('disabled', "true");
    });
}

function makePhantomInputsReadOnly() {
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

export function clearTreeNode(event: Event): void {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path")
        runAction("DeleteAction", treePath, [])
    }
}

let copyCache: string = null;

export function copyTreeNode(event: Event): void {
    copyCache = contextMenuSelectedElement.getAttribute("data-node-string");
}

export function pasteTreeNode(event: Event): void {
    if (copyCache) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path");
        runAction("PasteAction", treePath, [copyCache]);
    }
}

// Tree Panning and Zooming

// Initialize Panzoom
let panzoomInstance: PanZoom;

export function zoomToFit(): void {
    const tree = document.getElementById('tree');
    const container = document.getElementById('tree-container');
    const firstSubtree = tree.children[0];

    const widthScale = container.clientWidth / firstSubtree.clientWidth;
    const heightScale = container.clientHeight / firstSubtree.clientHeight;

    const newScale = Math.min(widthScale, heightScale);

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, newScale);
}

function displayError(error: string): void {
    const errorDiv = document.getElementById('error-message');
    errorDiv.textContent = error;
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');
    setTimeout(() => {
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}

export function initialise() {
    undoButton = <HTMLButtonElement>document.getElementById('undoButton');
    redoButton = <HTMLButtonElement>document.getElementById('redoButton');
    tree = <HTMLDivElement>document.getElementById('tree');
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

    document.addEventListener('contextmenu', function (e: MouseEvent) {
        let target: EventTarget = e.target;

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
            document.getElementById('custom-context-menu').style.display = 'none';
            clearHighlight();
        }
    });

    document.addEventListener('click', function (e: MouseEvent) {
        document.getElementById('custom-context-menu').style.display = 'none';
        if (contextMenuSelectedElement !== null) {
            clearHighlight();
        }
    });

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
