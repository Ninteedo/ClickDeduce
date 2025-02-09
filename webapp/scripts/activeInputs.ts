import {LiteralInput} from "./components/literalInput/literalInput";
import {disableLangSelector, enableLangSelector} from "./langSelector";
import {resumeFileDragAndDrop} from "./saveLoad";
import {unlockPanZoom} from "./components/panzoom";
import {AbstractTreeInput} from "./components/abstractTreeInput";
import {CustomExprSelector} from "./components/customExprSelector";
import {getRootSubtree} from "./treeManipulation";

export let modeRadios: HTMLInputElement[];
export let activeInputs: AbstractTreeInput[] = [];

export function setModeRadios(radios: HTMLInputElement[]): void {
    modeRadios = radios;
}

/**
 * Updates the list of inputs which the user can use.
 *
 * Also adds event listeners to the inputs.
 */
export function updateActiveInputsList(): void {
    activeInputs = getRootSubtree()!.getAllInputs();
    activeInputs.sort((a, b) => {
        return a.getTreePath().localeCompare(b.getTreePath(), undefined, {numeric: true, sensitivity: 'base'});
    });
}

export function getActiveInputs(): AbstractTreeInput[] {
    return activeInputs;
}

export function getLiteralInputs(): LiteralInput[] {
    return getActiveInputs().filter(input => input instanceof LiteralInput) as LiteralInput[];
}

export function getExprSelectors(): CustomExprSelector[] {
    return getActiveInputs().filter(input => input instanceof CustomExprSelector) as CustomExprSelector[];
}

let reEnableInputsId: number = 0;

function incrementReEnableInputsId(): void {
    reEnableInputsId = (reEnableInputsId + 1) % 1000;
}

export function disableInputs(): void {
    activeInputs.forEach(input => input.disable());
    modeRadios.forEach(radio => radio.disabled = true);
    disableLangSelector();

    // re-enable inputs after 5 seconds
    incrementReEnableInputsId();
    const currentId = reEnableInputsId;
    setTimeout(() => {
        if (currentId === reEnableInputsId) {
            enableInputs();
        }
    }, 5000);
}

export function enableInputs(): void {
    incrementReEnableInputsId();
    activeInputs.forEach(input => input.enable());
    modeRadios.forEach(radio => radio.disabled = false);
    enableLangSelector();
    resumeFileDragAndDrop();
    unlockPanZoom();
}
