import {getRulePreview} from "../serverRequest";

import {getCurrentLanguage} from "../langSelector";
import {ClassDict} from "../globals/classDict";

export class RulePreview {
    private readonly previewDiv: HTMLDivElement;

    constructor(container: HTMLDivElement) {
        this.previewDiv = document.createElement('div');
        this.previewDiv.classList.add(ClassDict.RULE_PREVIEW);
        container.appendChild(this.previewDiv);
    }

    show(ruleName: string): void {
        this.previewDiv.innerHTML = getRulePreview(getCurrentLanguage(), ruleName);
        this.previewDiv.classList.add(ClassDict.SHOW);
    }

    hide(): void {
        this.previewDiv.classList.remove(ClassDict.SHOW);
    }
}
