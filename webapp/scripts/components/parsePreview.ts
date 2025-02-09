import {Subtree} from "./subtree";
import {ClassDict} from "../globals/classDict";

export class ParsePreview {
    private readonly previewDiv: HTMLDivElement;

    constructor(container: HTMLDivElement) {
        this.previewDiv = document.createElement('div');
        this.previewDiv.classList.add(ClassDict.PARSE_PREVIEW);
        container.appendChild(this.previewDiv);
    }

    show(html: string): void {
        this.previewDiv.innerHTML = html;
        const subtreeElement = this.previewDiv.querySelector(`.${ClassDict.SUBTREE}`);
        if (subtreeElement) {
            const nodeString = subtreeElement.getAttribute('data-node-string');
            if (nodeString) {
                new Subtree(subtreeElement as HTMLDivElement, null, nodeString);
            }
            this.previewDiv.classList.add(ClassDict.SHOW);
        }
    }

    showError(error: string, _errorIndex: number): void {
        this.previewDiv.innerHTML = `<div class="${ClassDict.ERROR}">${error}</div>`;
        this.previewDiv.classList.add(ClassDict.SHOW);
    }

    hide(): void {
        this.previewDiv.classList.remove(ClassDict.SHOW);
    }
}
