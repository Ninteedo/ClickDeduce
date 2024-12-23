import {getSelectedLanguage, getSelectedMode} from "../utils";
import {lastNodeString} from "../treeManipulation";
import {getBlocker} from "../globals/elements";
// @ts-ignore
import {convertToLaTeX} from "scalajs:main.js";

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
    const outputDiv = document.getElementById('export-output-container') as HTMLDivElement;
    const outputTextArea = document.getElementById('export-output') as HTMLTextAreaElement;
    outputTextArea.value = output;
    const outputTitle = document.getElementById('export-output-title');
    if (outputTitle) outputTitle.textContent = title;
    const outputDescription = document.getElementById('export-output-desc');
    if (outputDescription) {
        if (description) {
            outputDescription.textContent = description;
            outputDescription.classList.add('visible');
        } else {
            outputDescription.classList.remove('visible');
        }
    }
    outputDiv.classList.add('visible');
    getBlocker().classList.add('visible');
}

/**
 * Copies the LaTeX output to the clipboard.
 */
export function copyExportOutput() {
    const outputTextArea = document.getElementById('export-output') as HTMLTextAreaElement;
    outputTextArea.select();
    navigator.clipboard.writeText(outputTextArea.value);
}

/**
 * Closes the export output modal.
 */
export function closeExportOutput() {
    const outputDiv = document.getElementById('export-output-container') as HTMLDivElement;
    outputDiv.classList.remove('visible');
    getBlocker().classList.remove('visible');
}
