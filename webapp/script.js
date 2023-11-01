var lastNodeString = "";
let treeContainer = document.getElementById('tree');

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
        treeContainer.innerHTML = updatedTree.html;
        lastNodeString = updatedTree.nodeString;
        addHoverListeners();
    });
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

document.addEventListener('contextmenu', function (e) {
    var target = e.target;

    if (target.classList.contains('highlight')) {
        e.preventDefault();

        var menu = document.getElementById('custom-context-menu');
        menu.style.display = 'block';
        menu.style.left = e.pageX + 'px';
        menu.style.top = e.pageY + 'px';
    }
});

document.addEventListener('click', function (e) {
    document.getElementById('custom-context-menu').style.display = 'none';
});
