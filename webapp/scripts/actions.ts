import {getSelectedLanguage, getSelectedMode} from "./utils";
import {lastNodeString, reloadCurrentTree, updateTree} from "./treeManipulation";
import {postProcessActionNew, postStartNodeBlankNew} from "./serverRequest";
import {getNodeStringFromPath} from "./utility/parseNodeString";
import {getTreePathOfElement} from "./globals/elements";
import {displayError} from "./components/displayError";
import {centerTree} from "./components/panzoom";
import {getNextFocusTreePath, setFocusElement, setNextFocusElement} from "./focus";
import {disableInputs, enableInputs} from "./activeInputs";
import {ClassDict} from "./globals/classDict";
import {IdDict} from "./globals/idDict";

let copyCache: string | null = null;

/**
 * Resets the global variables used by the action code.
 */
export function resetCopyCache(): void {
    copyCache = null;
}

export function startNodeBlank(): void {
    doStartNodeBlank();
}

export function doStartNodeBlank(event?: Event): void {
    if (event) event.preventDefault();

    const [newNodeString, newHtml] = postStartNodeBlankNew(getSelectedLanguage());
    updateTree(newHtml, newNodeString, getSelectedMode(), getSelectedLanguage(), true);

    centerTree();
}

function parseLiteralValue(inputElement: HTMLInputElement): string {
    if (inputElement.type === "checkbox") {
        return inputElement.checked ? "true" : "false";
    }
    return inputElement.value;
}

/**
 * Handles a literal input value being changed.
 *
 * Executes the EditLiteralAction.
 *
 * @param textInput the literal input element
 * @param doNotFocus if true then element will not be focused after the action
 */
export function handleLiteralChanged(textInput: HTMLInputElement, doNotFocus: boolean = false): void {
    const literalValue: string = parseLiteralValue(textInput);
    const treePath: string = getTreePathOfElement(textInput);
    runAction("EditLiteralAction", treePath, literalValue, doNotFocus);
}

export function exampleLiteralChanged(textInput: HTMLInputElement): void {
    const literalValue: string = textInput.value;

    const exampleLiteralOuter = document.getElementById(IdDict.EXAMPLE_LITERAL_OUTER);
    if (!exampleLiteralOuter) throw new Error('Could not find example-literal-outer');
    const outputDiv = exampleLiteralOuter.querySelector(`.${ClassDict.EVAL_RESULT}`) as HTMLDivElement;

    if (literalValue.match(/^\d+$/)) {
        outputDiv.innerHTML = `
        <span class="${ClassDict.TOOLTIP}">
          <div>
            <div class="${ClassDict.VALUE}"><span>${literalValue}</span></div>
            <span>: </span>
            <div class="${ClassDict.VALUE_TYPE}"><span>Int</span></div>
          </div>
          <div class="${ClassDict.TOOLTIP_TEXT}">
            NumV(${literalValue}): IntType()
          </div>
        </span>
        `;
    } else {
        outputDiv.innerHTML = `
        <span class="${ClassDict.TOOLTIP}">
          <div class="${ClassDict.ERROR_ORIGIN}">error!</div>
          <div class="${ClassDict.TOOLTIP_TEXT}">Num can only accept LiteralInt, not ${literalValue}</div>
        </span>
        `;
    }

    textInput.focus();
    textInput.select();
}

/**
 * Runs the given action and updates the tree according to the server's response.
 * @param actionName the name of the action to run
 * @param treePath the tree path of the node to run the action on
 * @param extraArgs any extra arguments to pass to the action
 * @param doNotFocus if true then the element will not be focused after the action
 */
export function runAction(actionName: string, treePath: string, extraArgs: any[] | any = [], doNotFocus: boolean = false): void {
    if (lastNodeString == null) {
        return;
    }
    disableInputs();

    try {
        const modeName = getSelectedMode();
        const langName = getSelectedLanguage();
        const extraArgsClean: any[] = Array.isArray(extraArgs) ? extraArgs : [extraArgs]
        const [newNodeString, newHtml] = postProcessActionNew(langName, modeName, actionName, lastNodeString, treePath, extraArgsClean);
        updateTree(newHtml, newNodeString, modeName, langName, true);

        const focusPath = getNextFocusTreePath();
        if (focusPath !== null) {
            setFocusElement(focusPath);
            setNextFocusElement(null);
        } else if (!doNotFocus && (!document.activeElement || document.activeElement.tagName === "BODY")) {
            setFocusElement(treePath);
        }
    } catch (e) {
        displayError(e);
        reloadCurrentTree();
    }
    enableInputs();
}

export function deleteTreeNode(treePath: string): void {
    runAction("DeleteAction", treePath);
    setFocusElement(treePath);
}

/**
 * Copies the node string of the selected subtree to the copy cache.
 */
export function copyTreeNode(treePath: string): void {
    copyCache = getNodeStringFromPath(treePath);
}

/**
 * Pastes the node string in the copy cache into the selected subtree, replacing it.
 *
 * Executes the PasteAction.
 */
export function pasteTreeNode(treePath: string): void {
    if (copyCache) {
        runAction("PasteAction", treePath, copyCache);
        setFocusElement(treePath);
    }
}

export function hasCopyCache(): boolean {
    return copyCache !== null;
}
