import {getRulePreview} from "../serverRequest";
import {getCurrentLanguage} from "../treeManipulation";

export class RulePreview {
    private readonly previewDiv: HTMLDivElement;
    private readonly RULE_PREVIEW_CLASS = 'rule-preview';
    private readonly VISIBLE_CLASS = 'show';

    constructor(container: HTMLDivElement) {
        this.previewDiv = document.createElement('div');
        this.previewDiv.classList.add(this.RULE_PREVIEW_CLASS);
        container.appendChild(this.previewDiv);
    }

    show(ruleName: string): void {
        this.previewDiv.innerHTML = getRulePreview(getCurrentLanguage(), ruleName);
        this.previewDiv.classList.add(this.VISIBLE_CLASS);
    }

    hide(): void {
        this.previewDiv.classList.remove(this.VISIBLE_CLASS);
    }
}
