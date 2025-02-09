import {getSelectedLanguage, getSelectedMode} from "../utils";
import {lastNodeString} from "../treeManipulation";
import {getBlocker} from "../globals/elements";
import {Modal} from "./modal";
// @ts-ignore
import {convertToLaTeX} from "scalajs:main.js";
import {ClassDict} from "../globals/classDict";
import {IdDict} from "../globals/idDict";

let latexModal: Modal | null = null;

function getLatexModal(): Modal {
    if (!latexModal) {
        const outputDiv = document.getElementById(IdDict.EXPORT_OUTPUT_CONTAINER) as HTMLDivElement;
        if (!(outputDiv instanceof HTMLDivElement)) {
            throw new Error('Export output container not found');
        }
        latexModal = new Modal(outputDiv, getBlocker());
    }
    return latexModal;
}

/**
 * Displays the export LaTeX output modal.
 */
export function exportLaTeX(): void {
    const langName = getSelectedLanguage();
    const modeName = getSelectedMode();
    const output: string = convertToLaTeX(langName, modeName, lastNodeString);
    showExportOutput("LaTeX Output", output, "Copy this LaTeX code and use the bussproofs package");
}

function showExportOutput(title: string, output: string, description: string | null): void {
    const outputTextArea = document.getElementById(IdDict.EXPORT_OUTPUT) as HTMLTextAreaElement;
    outputTextArea.value = output;
    const outputTitle = document.getElementById(IdDict.EXPORT_OUTPUT_TITLE);
    if (outputTitle) outputTitle.textContent = title;
    const outputDescription = document.getElementById(IdDict.EXPORT_OUTPUT_DESC);
    if (outputDescription) {
        if (description) {
            outputDescription.textContent = description;
            outputDescription.classList.add(ClassDict.VISIBLE);
        } else {
            outputDescription.classList.remove(ClassDict.VISIBLE);
        }
    }
    getLatexModal().show();
}

/**
 * Copies the LaTeX output to the clipboard.
 */
export function copyExportOutput() {
    const outputTextArea = document.getElementById(IdDict.EXPORT_OUTPUT) as HTMLTextAreaElement;
    outputTextArea.select();
    navigator.clipboard.writeText(outputTextArea.value);
}

/**
 * Closes the export output modal.
 */
export function closeExportOutput() {
    getLatexModal().hide();
}
