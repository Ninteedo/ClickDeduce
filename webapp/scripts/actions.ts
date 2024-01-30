import {getSelectedLanguage, getSelectedMode} from "./utils";
import {ClickDeduceResponseError} from "./ClickDeduceResponseError";
import {
    disableInputs,
    enableInputs,
    getActiveInputs,
    initialValues,
    lastNodeString,
    treeHistoryIndex,
    updateTree,
    useTreeFromHistory
} from "./treeManipulation";
import {contextMenuSelectedElement, displayError, nextFocusElement} from "./interface";

let copyCache: string = null;

/**
 * Resets the global variables used by the action code.
 */
export function resetCopyCache(): void {
    copyCache = null;
}

/**
 * Handles the form submission event.
 * @param event the form submission event
 * @param url the URL to send the POST request to
 */
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

/**
 * Handles a literal input value being changed.
 *
 * Executes the EditLiteralAction.
 *
 * @param textInput the literal input element
 */
export async function handleLiteralChanged(textInput: HTMLInputElement): Promise<void> {
    const literalValue: string = textInput.value;
    const treePath: string = textInput.getAttribute("data-tree-path");

    if (initialValues.find(([path, value]) => path === treePath && value === literalValue)) {
        console.log(`Skipping redundant action, tree path "${treePath}" already has value "${literalValue}"`);
        console.log(`Initial values: ${JSON.stringify(initialValues)}`);
        return;
    }

    let focusedTreePath: string = null;
    if (nextFocusElement != null) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    await runAction("EditLiteralAction", treePath, [literalValue]).then(() => {
        if (focusedTreePath == null) {
            return;
        }
        let focusedElement: HTMLElement = document.querySelector(`input[data-tree-path="${focusedTreePath}"]`);
        if (focusedElement != null && focusedElement instanceof HTMLElement) {
            focusedElement.focus();
            if (focusedElement instanceof HTMLInputElement) {
                focusedElement.select();
            }
        }
    });
}

/**
 * Handles an expr/type choice dropdown being selected.
 *
 * Executes the SelectExprAction or SelectTypeAction (as appropriate).
 *
 * @param dropdown the expr/type choice dropdown
 * @param kind the kind of dropdown (either "expr" or "type")
 */
export async function handleDropdownChange(dropdown: HTMLSelectElement, kind: string): Promise<void> {
    const selectedValue: string = dropdown.value;
    const subtree: HTMLElement = dropdown.parentElement.parentElement;
    const dataTreePath: string = subtree.getAttribute("data-tree-path");

    let actionName: string;
    if (kind === "type") {
        actionName = "SelectTypeAction";
    } else if (kind === "expr") {
        actionName = "SelectExprAction";
    } else {
        throw new Error(`Unknown dropdown kind: ${kind}`);
    }

    await runAction(actionName, dataTreePath, [selectedValue]);
}

export async function handleExprSelectorChoice(selector: HTMLDivElement, value: string): Promise<void> {
    const input = selector.querySelector('.expr-selector-input') as HTMLInputElement;
    const dropdown = selector.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    const button = selector.querySelector('.expr-selector-button') as HTMLButtonElement;

    let focusedTreePath: string = null;
    console.log(nextFocusElement);
    if (nextFocusElement != null) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    input.value = value;
    const dataTreePath: string = selector.getAttribute("data-tree-path");
    await runAction("SelectExprAction", dataTreePath, [value]).then(() => {
        console.log(focusedTreePath);
        if (focusedTreePath == null) {
            return;
        }

        const children = getActiveInputs().filter(input => input.getAttribute("data-tree-path").startsWith(focusedTreePath));
        console.log(children);
        if (children.length === 0) {
            return;
        }
        let focusedElement = children[0];

        console.log(focusedElement);
        if (focusedElement != null && focusedElement instanceof HTMLElement) {
            focusedElement.focus();
            if (focusedElement instanceof HTMLInputElement) {
                focusedElement.select();
            }
        }
    });
}

/**
 * Runs the given action and updates the tree according to the server's response.
 * @param actionName the name of the action to run
 * @param treePath the tree path of the node to run the action on
 * @param extraArgs any extra arguments to pass to the action
 */
export async function runAction(actionName: string, treePath: string, extraArgs: any[]): Promise<void> {
    if (lastNodeString == null) {
        return;
    }
    disableInputs();

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
            enableInputs();
            return response.text().then(text => {
                throw new ClickDeduceResponseError(text);
            });
        }
        return response;
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, modeName, langName, true);
        enableInputs();
    }).catch(error => {
        enableInputs();
        displayError(error);
        useTreeFromHistory(treeHistoryIndex);
        throw new ClickDeduceResponseError(error.message);
    });
}

/**
 * Clears the selected subtree.
 *
 * Executes the DeleteAction.
 *
 * @param event the triggering event
 */
export async function clearTreeNode(event: Event): Promise<void> {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        const treePath: string = contextMenuSelectedElement.getAttribute("data-tree-path")
        await runAction("DeleteAction", treePath, [])
    }
}

/**
 * Copies the node string of the selected subtree to the copy cache.
 */
export function copyTreeNode(): void {
    copyCache = contextMenuSelectedElement.getAttribute("data-node-string");
}

/**
 * Pastes the node string in the copy cache into the selected subtree, replacing it.
 *
 * Executes the PasteAction.
 */
export async function pasteTreeNode(): Promise<void> {
    if (copyCache) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path");
        await runAction("PasteAction", treePath, [copyCache]);
    }
}
