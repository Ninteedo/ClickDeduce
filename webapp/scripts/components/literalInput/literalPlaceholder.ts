import {LiteralInput} from "./literalInput";
import {ClassDict} from "../../globals/classDict";

export class LiteralPlaceholder {
    private readonly placeholder: HTMLDivElement;
    private readonly originInput: LiteralInput;

    constructor(placeholder: HTMLDivElement, originInput: LiteralInput) {
        if (placeholder instanceof HTMLInputElement) {
            const replacement = document.createElement('div');
            for (const attr of Array.from(placeholder.attributes)) {
                replacement.setAttribute(attr.name, attr.value);
            }
            const contentDiv = document.createElement('div');
            contentDiv.innerText = placeholder.value;
            replacement.appendChild(contentDiv);
            replacement.classList.add(ClassDict.PLACEHOLDER);
            placeholder.replaceWith(replacement);
            placeholder = replacement;
        }

        this.placeholder = placeholder;
        this.originInput = originInput;

        this.setupEventListeners();
    }

    private setupEventListeners(): void {
        this.placeholder.addEventListener('mouseover', () => {
            if (this.originInput.isEnabled()) this.originInput.addGuideHighlight();
        });
        this.placeholder.addEventListener('mouseout', () => this.originInput.removeGuideHighlight());
        this.placeholder.addEventListener('click', event => {
            event.preventDefault();
            event.stopPropagation();
            this.originInput.focus()
        });
    }

    public setContent(content: HTMLElement): void {
        this.placeholder.replaceChildren(content);
    }
}
