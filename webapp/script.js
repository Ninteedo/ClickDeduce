let lastNodeString = "";
const tree = document.getElementById('tree');

let treeHistory = [];
let treeHistoryIndex = 0;

const undoButton = document.getElementById('undoButton');
const redoButton = document.getElementById('redoButton');

async function handleSubmit(event, url) {
    // prevent the form from submitting the old-fashioned way
    event.preventDefault();

    // send a POST request to the server
    await fetch(url, {
        method: 'POST', headers: {
            'Content-Type': 'application/json'
        }, body: JSON.stringify({
            langName: getSelectedLanguage(),
        })
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, true);
    });
}

async function loadLangSelector() {
    const langSelectorContainer = document.getElementById('lang-selector-div');

    await fetch('get-lang-selector', {
        method: 'GET'
    }).then(response => response.json()).then(langSelector => {
        langSelectorContainer.innerHTML = langSelector.langSelectorHtml;
    });
}

loadLangSelector();

function getSelectedLanguage() {
    const langSelector = document.getElementById('lang-selector');
    return langSelector.value;
}

function handleDropdownChange(dropdown) {
    const selectedValue = dropdown.value;
    const subtree = dropdown.parentElement.parentElement;
    const dataTreePath = subtree.getAttribute("data-tree-path");

    runAction("SelectExprAction", dataTreePath, [selectedValue]);
}

function handleLiteralChanged(textInput) {
    const literalValue = textInput.value;
    const treePath = textInput.getAttribute("data-tree-path");

    if (nextFocusElement == null) {
        nextFocusElement = textInput;
    }
    const focusedTreePath = nextFocusElement.getAttribute("data-tree-path");

    runAction("EditLiteralAction", treePath, [literalValue]).then(() => {
        let focusedElement = document.querySelector(`[data-tree-path="${focusedTreePath}"]`);
        focusedElement.focus();
        focusedElement.select();
    });
}

function runAction(actionName, treePath, extraArgs) {
    return fetch("/process-action", {
        method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify({
            langName: getSelectedLanguage(), actionName, nodeString: lastNodeString, treePath, extraArgs
        })
    }).then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw Error(response.statusText + "\n" + text)
            });
        }
        return response;
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, true)
    }).catch(error => {
        displayError(error);
        throw error;
    });
}

function updateTree(newTreeHtml, newNodeString, addToHistory = false) {
    tree.innerHTML = newTreeHtml;
    lastNodeString = newNodeString;
    addHoverListeners();
    if (addToHistory && (treeHistory.length === 0 || (newTreeHtml !== treeHistory[treeHistoryIndex][0] || newNodeString !== treeHistory[treeHistoryIndex][1]))) {
        if (treeHistoryIndex < treeHistory.length - 1) {
            treeHistory = treeHistory.slice(0, treeHistoryIndex + 1);
        }
        treeHistoryIndex = treeHistory.push([newTreeHtml, newNodeString]) - 1;
    }
    updateUndoRedoButtons();
    updateActiveInputsList();
}

function useTreeFromHistory(newHistoryIndex) {
    if (newHistoryIndex >= 0 && newHistoryIndex < treeHistory.length) {
        treeHistoryIndex = newHistoryIndex;
        let newHtml, newNodeString;
        [newHtml, newNodeString] = treeHistory[newHistoryIndex];
        updateTree(newHtml, newNodeString, false);
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
        nextFocusElement.select();
        nextFocusElement = null;
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

let contextMenuSelectedElement = null;

document.addEventListener('contextmenu', function (e) {
    let target = e.target;

    while (target && !target.classList.contains('highlight')) {
        target = target.parentElement;
    }

    if (target) {
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
    clearHighlight();
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
    bounds: true, boundsPadding: 0, zoomDoubleClickSpeed: 1,
    onTouch: function(e) {
        // TODO: cannot use on mobile currently
        return false;  // tells the library to not preventDefault.
    },
    filterKey: function(/* e, dx, dy, dz */) {
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
    errorDiv.innerText = error;
    errorDiv.style.display = 'block';
}
