let lastNodeString = "";
const tree = document.getElementById('tree');

let treeHistory = [];
let treeHistoryIndex = 0;

const undoButton = document.getElementById('undoButton');
const redoButton = document.getElementById('redoButton');

const modeRadios = document.querySelectorAll('input[name="mode"]');

for (const radio of modeRadios) {
    radio.addEventListener('change', () => {
        runAction("IdentityAction", "", []);
    });
}

function getSelectedMode() {
    for (const radio of modeRadios) {
        if (radio.checked) {
            return radio.value;
        }
    }
    throw Error("No mode selected");
}

async function handleSubmit(event, url) {
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

loadLangSelector();

function getSelectedLanguage() {
    const langSelector = document.getElementById('lang-selector');
    return langSelector.value;
}

function handleDropdownChange(dropdown, kind) {
    const selectedValue = dropdown.value;
    const subtree = dropdown.parentElement.parentElement;
    const dataTreePath = subtree.getAttribute("data-tree-path");

    let actionName = "SelectExprAction";
    if (kind === "type") {
        actionName = "SelectTypeAction";
    }

    runAction(actionName, dataTreePath, [selectedValue]);
}

function handleLiteralChanged(textInput) {
    const literalValue = textInput.value;
    const treePath = textInput.getAttribute("data-tree-path");

    let focusedTreePath = null;
    if (nextFocusElement != null) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    runAction("EditLiteralAction", treePath, [literalValue]).then(() => {
        console.log(focusedTreePath);
        if (focusedTreePath == null) { return; }
        let focusedElement = document.querySelector(`[data-tree-path="${focusedTreePath}"]`);
        if (focusedElement != null) {
            console.log(focusedElement);
            focusedElement.focus();
            focusedElement.select();
        }
    });
}

function runAction(actionName, treePath, extraArgs) {
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

function updateTree(newTreeHtml, newNodeString, modeName, lang, addToHistory = false) {
    tree.innerHTML = newTreeHtml;
    lastNodeString = newNodeString;
    treeCleanup();
    if (addToHistory && (treeHistory.length === 0 ||
        (newTreeHtml !== treeHistory[treeHistoryIndex][0] || newNodeString !== treeHistory[treeHistoryIndex][1]))) {
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
    modeRadios.forEach(radio => {
        radio.checked = radio.value === modeName;
    });
    const langSelector = document.getElementById('lang-selector');
    langSelector.value = lang;
}

function treeCleanup() {
    addHoverListeners();
    makeOrphanedInputsReadOnly();
    makePhantomInputsReadOnly();
}

function useTreeFromHistory(newHistoryIndex) {
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

updateUndoRedoButtons();

function undo() {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length) {
        useTreeFromHistory(treeHistoryIndex - 1);
    }
}

function redo() {
    if (treeHistoryIndex >= 0 && treeHistoryIndex < treeHistory.length - 1) {
        useTreeFromHistory(treeHistoryIndex + 1);
    }
}

let activeInputs = [];

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

let nextFocusElement = null;

async function handleTabPressed(e) {
    if (e.code === 'Tab') {
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
        if (nextFocusElement.tagName === 'INPUT') {
            nextFocusElement.select();
        }
        // nextFocusElement = null;
    }
}

// the text input width is updated to match the text width
function updateTextInputWidth(textInput) {
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
            // Remove the highlight from any other 'subtree' elements
            if (contextMenuSelectedElement === null) {
                document.querySelectorAll('.subtree').forEach(el => el.classList.remove('highlight'));
                // Add the highlight to the currently hovered over 'subtree'
                event.currentTarget.classList.add('highlight');
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
        el.setAttribute('readonly', true);
        el.setAttribute('disabled', true);
    });
}

function makePhantomInputsReadOnly() {
    document.querySelectorAll('#tree select, #tree input').forEach(el => {
        if (hasClassOrPrentHasClass(el, 'phantom')) {
            el.setAttribute('readonly', true);
            el.setAttribute('disabled', true);
        }
    })
}

function hasClassOrPrentHasClass(element, className) {
    return element.classList.contains(className) ||
        (element.parentElement && hasClassOrPrentHasClass(element.parentElement, className));
}

let contextMenuSelectedElement = null;

document.addEventListener('contextmenu', function (e) {
    let target = e.target;

    while (target && !target.classList.contains('highlight')) {
        target = target.parentElement;
    }

    if (target && !hasClassOrPrentHasClass(target, 'phantom')) {
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

document.addEventListener('click', function (e) {
    document.getElementById('custom-context-menu').style.display = 'none';
    if (contextMenuSelectedElement !== null) {
        clearHighlight();
    }
});

function clearTreeNode(event) {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path")
        runAction("DeleteAction", treePath, [])
    }
}

let copyCache = null;

function copyTreeNode(event) {
    copyCache = contextMenuSelectedElement.getAttribute("data-node-string");
}

function pasteTreeNode(event) {
    if (copyCache) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path");
        runAction("PasteAction", treePath, [copyCache]);
    }
}

// Tree Panning and Zooming

// Initialize Panzoom
const panzoomInstance = panzoom(tree, {
    bounds: false, boundsPadding: 0, zoomDoubleClickSpeed: 1,
    onTouch: function (e) {
        // TODO: cannot use on mobile currently
        return false;  // tells the library to not preventDefault.
    },
    filterKey: function (/* e, dx, dy, dz */) {
        return true;  // don't let panzoom handle this event:
    }
});

function zoomToFit() {
    const tree = document.getElementById('tree');
    const container = document.getElementById('tree-container');
    const firstSubtree = tree.children[0];

    const widthScale = container.clientWidth / firstSubtree.clientWidth;
    const heightScale = container.clientHeight / firstSubtree.clientHeight;

    const newScale = Math.min(widthScale, heightScale);

    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, newScale);
}

function displayError(error) {
    const errorDiv = document.getElementById('error-message');
    errorDiv.textContent = error;
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');
    setTimeout(() => {
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}
