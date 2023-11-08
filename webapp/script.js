var lastNodeString = "";
const treeContainer = document.getElementById('tree');
var treeHistory = [];
var treeHistoryIndex = 0;

async function handleSubmit(event, url) {
    // prevent the form from submitting the old-fashioned way
    event.preventDefault();

    // get the user's input
    let userInput = document.getElementById('userInput').value;

    // send a POST request to the server
    let response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            text: userInput
        })
    });

    // read the response as text
    let jsonResponse = await response.json();

    // the svgText is the SVG string directly without the JSON structure:
    treeContainer.innerHTML = jsonResponse.html;
    lastNodeString = jsonResponse.nodeString;
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
    if (addToHistory) {
        if (treeHistoryIndex < treeHistory.length - 1) {
            treeHistory = treeHistory.slice(0, treeHistoryIndex + 1);
        }
        treeHistoryIndex = treeHistory.push([newTreeHtml, newNodeString]) - 1;
    }
}

function useTreeFromHistory(newHistoryIndex) {
    if (newHistoryIndex >= 0 && newHistoryIndex < treeHistory.length) {
        let newHtml, newNodeString;
        [newHtml, newNodeString] = treeHistory[newHistoryIndex];
        treeContainer.innerHTML = newHtml;
        lastNodeString = newNodeString;
        addHoverListeners();
        treeHistoryIndex = newHistoryIndex;
    }
}

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
    });

    // Event for when mouse leaves a .subtree
    document.addEventListener('mouseout', (event) => {
        if (event.target.classList.contains('subtree')) {
            event.target.classList.remove('highlight');
        }
    });
}

var contextMenuSelectedElement = null;

document.addEventListener('contextmenu', function (e) {
    var target = e.target;

    if (target.classList.contains('highlight')) {
        e.preventDefault();

        contextMenuSelectedElement = target;

        var menu = document.getElementById('custom-context-menu');
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
