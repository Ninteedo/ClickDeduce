import {compareTreePaths, getSelectedLanguage, getSelectedMode} from "./utils";
import {
    disableInputs,
    enableInputs,
    getActiveInputs,
    getNodeStringFromPath,
    initialValues,
    lastNodeString,
    treeHistoryIndex,
    updateTree,
    useTreeFromHistory
} from "./treeManipulation";
import {contextMenuSelectedElement, displayError, getTreePathOfElement, nextFocusElement} from "./interface";
import {postProcessActionNew, postStartNodeBlankNew} from "./serverRequest";

let copyCache: string | null = null;

/**
 * Resets the global variables used by the action code.
 */
export function resetCopyCache(): void {
    copyCache = null;
}

export function startNodeBlank(): void {
    doStartNodeBlank(new Event("submit"));
}

/**
 * Handles the form submission event.
 * @param event the form submission event
 */
export function doStartNodeBlank(event?: Event): void {
    // prevent the form from submitting the old-fashioned way
    if (event) event.preventDefault();

    // send a POST request to the server
    const [newNodeString, newHtml] = postStartNodeBlankNew(getSelectedLanguage());
    updateTree(newHtml, newNodeString, getSelectedMode(), getSelectedLanguage(), true);
}

/**
 * Handles a literal input value being changed.
 *
 * Executes the EditLiteralAction.
 *
 * @param textInput the literal input element
 */
export function handleLiteralChanged(textInput: HTMLInputElement): void {
    const literalValue: string = textInput.value;
    const treePath: string = getTreePathOfElement(textInput);

    if (initialValues.find(([path, value]) => path === treePath && value === literalValue)) {
        return;
    }

    let focusedTreePath: string | null = null;
    if (nextFocusElement != null) {
        focusedTreePath = getTreePathOfElement(nextFocusElement);
    }

    runAction("EditLiteralAction", treePath, [literalValue]);

    if (focusedTreePath == null) return;
    let focusedElement: HTMLElement | null = document.querySelector(`input[data-tree-path="${focusedTreePath}"]`);
    if (focusedElement && focusedElement instanceof HTMLElement) {
        focusedElement.focus();
        if (focusedElement instanceof HTMLInputElement) {
            focusedElement.select();
        }
    }
}

export function exampleLiteralChanged(textInput: HTMLInputElement): void {
    const literalValue: string = textInput.value;
    const treePath: string = getTreePathOfElement(textInput);

    if (initialValues.find(([path, value]) => path === treePath && value === literalValue)) {
        return;
    }

    const exampleLiteralOuter = document.getElementById('example-literal-outer');
    if (!exampleLiteralOuter) throw new Error('Could not find example-literal-outer');
    const outputDiv = exampleLiteralOuter.querySelector('.eval-result') as HTMLDivElement;

    if (literalValue.match(/^\d+$/)) {
        outputDiv.innerHTML = `
        <span class="tooltip">
          <div>
            <div class="value"><span>${literalValue}</span></div>
            <span>: </span>
            <div class="value-type"><span>Int</span></div>
          </div>
          <div class="tooltip-text">
            NumV(${literalValue}): IntType()
          </div>
        </span>
        `;
    } else {
        outputDiv.innerHTML = `
        <span class="tooltip">
          <div class="error-origin">error!</div>
          <div class="tooltip-text">Num can only accept LiteralInt, not ${literalValue}</div>
        </span>
        `;
    }

    textInput.focus();
    textInput.select();
}

export function handleExprSelectorChoice(selector: HTMLDivElement, value: string | null): void {
    const input = selector.querySelector('.expr-selector-input') as HTMLInputElement;

    if (value === null) throw new Error("Selected value is null");

    let focusedTreePath: string | null = null;
    if (nextFocusElement) {
        focusedTreePath = nextFocusElement.getAttribute("data-tree-path");
    }

    const kind = selector.getAttribute("data-kind");
    let actionName: string;
    if (kind === "type") {
        actionName = "SelectTypeAction";
    } else if (kind === "expr") {
        actionName = "SelectExprAction";
    } else {
        throw new Error(`Unknown dropdown kind: ${kind}`);
    }

    input.value = value;
    runAction(actionName, getTreePathOfElement(selector), [value])

    if (focusedTreePath === null) return;
    const focusedElement = getActiveInputs().find(input => compareTreePaths(focusedTreePath!, getTreePathOfElement(input)) <= 0);

    if (focusedElement && focusedElement instanceof HTMLElement) {
        focusedElement.focus();
        if (focusedElement instanceof HTMLInputElement) {
            focusedElement.select();
        }
    }
}

/**
 * Runs the given action and updates the tree according to the server's response.
 * @param actionName the name of the action to run
 * @param treePath the tree path of the node to run the action on
 * @param extraArgs any extra arguments to pass to the action
 */
export function runAction(actionName: string, treePath: string, extraArgs: any[]): void {
    if (lastNodeString == null) {
        return;
    }
    disableInputs();

    const modeName: string = getSelectedMode();
    const langName: string = getSelectedLanguage();
    try {
        const [newNodeString, newHtml] = postProcessActionNew(langName, modeName, actionName, lastNodeString, treePath, extraArgs);
        updateTree(newHtml, newNodeString, modeName, langName, true);
    } catch (e) {
        displayError(e);
        useTreeFromHistory(treeHistoryIndex);
    }
    enableInputs();
}

export function deleteTreeNode(treePath: string): void {
    runAction("DeleteAction", treePath, []);
}

/**
 * Clears the selected subtree.
 *
 * Executes the DeleteAction.
 *
 * @param event the triggering event
 */
export function clearTreeNode(event: Event): void {
    event.preventDefault();
    if (contextMenuSelectedElement) {
        deleteTreeNode(getContextMenuSelectedTreePath());
    }
}

/**
 * Copies the node string of the selected subtree to the copy cache.
 */
export function copyTreeNode(treePath: string): void {
    copyCache = getNodeStringFromPath(treePath);
}

export function contextMenuCopy(): void {
    copyTreeNode(getContextMenuSelectedTreePath());
}

/**
 * Pastes the node string in the copy cache into the selected subtree, replacing it.
 *
 * Executes the PasteAction.
 */
export function pasteTreeNode(treePath: string): void {
    if (copyCache) {
        runAction("PasteAction", treePath, [copyCache]);
    }
}

export function contextMenuPaste(): void {
    pasteTreeNode(getContextMenuSelectedTreePath());
}

export function hasCopyCache(): boolean {
    return copyCache !== null;
}

function getContextMenuSelectedTreePath(): string {
    return getTreePathOfElement(contextMenuSelectedElement);
}
