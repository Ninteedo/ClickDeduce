import {LiteralInput} from "./literalInput";

export class LiteralPlaceholder {
    private readonly placeholder: HTMLDivElement;
    private readonly originInput: LiteralInput;

    constructor(placeholder: HTMLDivElement, originInput: LiteralInput) {
        this.placeholder = placeholder;
        this.originInput = originInput;

        this.setupEventListeners();
    }

    private setupEventListeners(): void {
        this.placeholder.addEventListener('mouseover', () => this.originInput.addGuideHighlight());
        this.placeholder.addEventListener('mouseout', () => this.originInput.removeGuideHighlight());
    }

    public setContent(content: HTMLElement): void {
        this.placeholder.innerHTML = '';
        this.placeholder.appendChild(content);
    }
}
