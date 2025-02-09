import {LiteralInput} from "./literalInput";

export class LiteralPlaceholder {
    private readonly placeholder: HTMLDivElement;
    private readonly originInput: LiteralInput;

    constructor(placeholder: HTMLDivElement, originInput: LiteralInput) {
        if (placeholder instanceof HTMLInputElement) {
            const replacement = document.createElement('div');
            for (const attr of Array.from(placeholder.attributes)) {
                replacement.setAttribute(attr.name, attr.value);
            }
            replacement.innerText = placeholder.value;
            replacement.classList.add('placeholder');
            placeholder.replaceWith(replacement);
            placeholder = replacement;
        }

        this.placeholder = placeholder;
        this.originInput = originInput;

        this.setupEventListeners();
    }

    private setupEventListeners(): void {
        this.placeholder.addEventListener('mouseover', () => this.originInput.addGuideHighlight());
        this.placeholder.addEventListener('mouseout', () => this.originInput.removeGuideHighlight());
        this.placeholder.addEventListener('click', event => {
            event.preventDefault();
            event.stopPropagation();
            this.originInput.focus()
        });
    }

    public setContent(content: HTMLElement): void {
        this.placeholder.innerHTML = '';
        this.placeholder.appendChild(content);
    }
}
