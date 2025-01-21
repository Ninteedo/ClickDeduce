import {Subtree} from "./subtree";
import {RulePreview} from "./rulePreview";

export class RuleAnnotation {
    private readonly element: HTMLDivElement;
    private readonly parentSubtree: Subtree;

    private readonly ruleName: string;

    private readonly rulePreview: RulePreview;

    constructor(element: HTMLDivElement, parentSubtree: Subtree) {
        this.element = element;
        this.parentSubtree = parentSubtree;
        this.ruleName = this.element.innerText;

        this.rulePreview = new RulePreview(this.parentSubtree.getElement());

        this.element.addEventListener('mouseover', () => {
            this.rulePreview.show(this.ruleName);
        });
        this.element.addEventListener('mouseout', () => {
            this.rulePreview.hide();
        });
    }
}
