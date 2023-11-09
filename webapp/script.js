var lastNodeString = "";
const treeContainer = document.getElementById('tree');
var treeHistory = [];
var treeHistoryIndex = 0;

const undoButton = document.getElementById('undoButton');
const redoButton = document.getElementById('redoButton');

async function handleSubmit(event, url) {
    // prevent the form from submitting the old-fashioned way
    event.preventDefault();

    // get the user's input
    let userInput = document.getElementById('userInput').value;

    // send a POST request to the server
    await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            text: userInput
        })
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, true);
    });
}

function handleDropdownChange(dropdown) {
    const selectedValue = dropdown.value;
    const subtree = dropdown.parentElement.parentElement;
    const dataTreePath = subtree.getAttribute("data-tree-path")

    runAction("SelectExprAction", dataTreePath, [selectedValue])
}

function handleLiteralChanged(textInput) {
    const literalValue = textInput.value;
    const treePath = textInput.getAttribute("data-tree-path")

    runAction("EditLiteralAction", treePath, [literalValue])
}

function runAction(actionName, treePath, extraArgs) {
    fetch("/process-action", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            actionName,
            nodeString: lastNodeString,
            treePath,
            extraArgs
        })
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, true)
    });
}

function updateTree(newTreeHtml, newNodeString, addToHistory = false) {
    treeContainer.innerHTML = newTreeHtml;
    lastNodeString = newNodeString;
    addHoverListeners();
    if (addToHistory && (treeHistory.length === 0 || (newTreeHtml !== treeHistory[treeHistoryIndex][0] || newNodeString !== treeHistory[treeHistoryIndex][1]))) {
        if (treeHistoryIndex < treeHistory.length - 1) {
            treeHistory = treeHistory.slice(0, treeHistoryIndex + 1);
        }
        treeHistoryIndex = treeHistory.push([newTreeHtml, newNodeString]) - 1;
    }
    updateUndoRedoButtons();
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

// the text input width is updated to match the text width
function updateTextInputWidth(textInput) {
    const minWidth = 2;
    textInput.style.width = Math.max(minWidth, textInput.value.length) + "ch";
}

function addHoverListeners() {
    document.querySelectorAll('.subtree').forEach(div => {
        div.addEventListener('mouseover', (event) => {
            // Stop the event from bubbling up to parent 'subtree' elements
            event.stopPropagation();
            // Remove the highlight from any other 'subtree' elements
            document.querySelectorAll('.subtree').forEach(el => el.classList.remove('highlight'));
            // Add the highlight to the currently hovered over 'subtree'
            event.currentTarget.classList.add('highlight');
        });
        div.addEventListener('mouseout', (event) => {
            // Stop the event from bubbling up to parent 'subtree' elements
            event.stopPropagation();
            // Remove the highlight from the currently hovered over 'subtree'
            event.currentTarget.classList.remove('highlight');
        });
    });
}

var contextMenuSelectedElement = null;

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
    }
});

document.addEventListener('click', function (e) {
    document.getElementById('custom-context-menu').style.display = 'none';
});

function clearTreeNode(event) {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path")
        runAction("DeleteAction", treePath, [])
    }
}
