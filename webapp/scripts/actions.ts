import {getSelectedLanguage, getSelectedMode} from "./utils";
import {ClickDeduceResponseError} from "./ClickDeduceResponseError";
import {initialValues, lastNodeString, treeHistoryIndex, updateTree, useTreeFromHistory} from "./treeManipulation";
import {contextMenuSelectedElement, displayError, nextFocusElement} from "./interface";

let copyCache: string = null;

export function resetCopyCache(): void {
    copyCache = null;
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

export async function handleLiteralChanged(textInput: HTMLInputElement): Promise<void> {
    const literalValue: string = textInput.value;
    const treePath: string = textInput.getAttribute("data-tree-path");

    if (initialValues.find(([path, value]) => path === treePath && value === literalValue)) {
        return;
    }

    let focusedTreePath: string = null;
    if (nextFocusElement != null) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    await runAction("EditLiteralAction", treePath, [literalValue]).then(() => {
        console.log("Focus path: " + focusedTreePath);
        if (focusedTreePath == null) {
            return;
        }
        let focusedElement: HTMLElement = document.querySelector(`[data-tree-path="${focusedTreePath}"]`);
        if (focusedElement != null && focusedElement instanceof HTMLElement) {
            console.log("Focus: " + focusedElement.outerHTML);
            focusedElement.focus();
            if (focusedElement instanceof HTMLInputElement) {
                focusedElement.select();
            }
        }
    });
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

export async function runAction(actionName: string, treePath: string, extraArgs: any[]): Promise<void> {
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
                throw new ClickDeduceResponseError(text);
            });
        }
        return response;
    }).then(response => response.json()).then(updatedTree => {
        updateTree(updatedTree.html, updatedTree.nodeString, modeName, langName, true)
    }).catch(error => {
        displayError(error);
        useTreeFromHistory(treeHistoryIndex);
        throw new ClickDeduceResponseError(error.message);
    });
}

export async function clearTreeNode(event: Event): Promise<void> {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        const treePath: string = contextMenuSelectedElement.getAttribute("data-tree-path")
        await runAction("DeleteAction", treePath, [])
    }
}

export function copyTreeNode(): void {
    copyCache = contextMenuSelectedElement.getAttribute("data-node-string");
}

export async function pasteTreeNode(): Promise<void> {
    if (copyCache) {
        const treePath = contextMenuSelectedElement.getAttribute("data-tree-path");
        await runAction("PasteAction", treePath, [copyCache]);
    }
}
