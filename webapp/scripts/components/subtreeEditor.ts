import {Subtree} from "./subtree";
import {getExprParsePreviewHtml, getExprText} from "../serverRequest";
import {getCurrentLanguage, getCurrentNodeString} from "../treeManipulation";
import {runAction} from "../actions";
import {ParsePreview} from "./parsePreview";
import {getSelectedMode} from "../utils";
import {handleTabPressedFromPath} from "../interface";

export class SubtreeEditor {
    private readonly subtree: Subtree;

    private readonly container: HTMLDivElement;
    private readonly input: HTMLInputElement;
    private readonly parsePreview: ParsePreview;

    constructor(subtree: Subtree) {
        this.subtree = subtree;

        const exprText = getExprText(getCurrentLanguage(), getCurrentNodeString()!, this.subtree.getTreePathString());
        this.container = document.createElement('div');
        this.container.classList.add('subtree-expr-editor');

        this.input = document.createElement('input');
        this.input.type = 'text';
        this.input.value = exprText;

        this.parsePreview = new ParsePreview(this.container);

        this.container.appendChild(this.input);
        this.container = this.subtree.getElement().appendChild(this.container);

        this.input.addEventListener('input', () => this.onInput());
        this.input.addEventListener('blur', () => this.destroy());
        this.input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                this.submit();
            } else if (e.key === 'Escape') {
                this.destroy();
            } else if (e.key === 'Tab') {
                e.preventDefault();
                handleTabPressedFromPath(this.treePath(), e.shiftKey ? -1 : 1);
            }
        });
        this.input.focus();
        this.onInput();
    }

    private onInput(): void {
        this.input.style.width = (this.input.value.length + 1) + 'ch';
        const parseHtml = getExprParsePreviewHtml(
            getCurrentLanguage(),
            this.getValue(),
            getSelectedMode(),
            getCurrentNodeString()!,
            this.treePath()
        );
        this.parsePreview.show(parseHtml);
    }

    destroy(): void {
        this.container.remove();
    }

    submit(): void {
        runAction("ParseExprAction", this.subtree.getTreePathString(), this.input.value);
    }

    treePath(): string {
        return this.subtree.getTreePathString();
    }

    getValue(): string {
        return this.input.value;
    }
}

let subtreeEditor: SubtreeEditor | null = null;

export function addSubtreeEditor(subtree: Subtree): void {
    if (subtreeEditor) {
        subtreeEditor.destroy();
    }
    subtreeEditor = new SubtreeEditor(subtree);
}
