import {getTreePathOfElement, handleKeyDown} from "../interface";
import {handleLiteralChanged} from "../actions";
import {getTree} from "../treeManipulation";

abstract class LiteralInput {
    protected readonly input: HTMLInputElement;
    protected readonly linkedPlaceholders: LiteralPlaceholder[];
    public readonly treePath: string;
    protected readonly initialValue: string;

    protected readonly MIN_WIDTH: number = 2;
    private readonly GUIDE_HIGHLIGHT_CLASS: string = 'guide-highlight';

    constructor(input: HTMLInputElement) {
        this.input = input;
        this.treePath = getTreePathOfElement(input);
        this.linkedPlaceholders = this.createLinkedPlaceholders();
        console.log(this.linkedPlaceholders);
        this.initialValue = this.getValue();

        this.setupEventListeners();
    }

    private createLinkedPlaceholders(): LiteralPlaceholder[] {
        return Array.from(getTree().querySelectorAll(`.literal`))
            .filter((elem: Element) => elem.getAttribute('data-origin') === this.treePath)
            .map((elem: Element) => new LiteralPlaceholder(elem as HTMLDivElement, this));
    }

    private setupEventListeners(): void {
        this.input.addEventListener('input', () => this.onInput());
        this.input.addEventListener('change', () => this.handleInputChanged());
        this.input.addEventListener('focus', () => this.onFocused());
        this.input.addEventListener('blur', () => this.onBlurred());
        this.input.addEventListener('keydown', handleKeyDown);
    }

    protected onInput(): void {
        this.updateInputWidth();
        this.updateLinkedInputPlaceholders();
    }

    protected onFocused(): void {

    }

    protected onBlurred(): void {
        this.handleInputChanged();
    }

    private handleInputChanged(): void {
        if (this.getValue() === this.initialValue) return;
        handleLiteralChanged(this.input);
    }

    private updateInputWidth(): void {
        this.input.style.width = Math.max(this.MIN_WIDTH, this.getValue().length) + "ch";
    }

    private updateLinkedInputPlaceholders(): void {
        this.linkedPlaceholders.forEach((placeholder: LiteralPlaceholder) => {
            placeholder.setContent(this.getPlaceholderContent());
        });
    }

    public setValue(value: string): void {
        this.input.value = value;
        this.handleInputChanged();
    }

    public getValue(): string {
        return this.input.value;
    }

    public addGuideHighlight(): void {
        this.input.classList.add(this.GUIDE_HIGHLIGHT_CLASS);
    }

    public removeGuideHighlight(): void {
        this.input.classList.remove(this.GUIDE_HIGHLIGHT_CLASS);
    }

    protected abstract getPlaceholderContent(): HTMLElement;
}

class LiteralPlaceholder {
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

class LiteralTextInput extends LiteralInput {
    protected getPlaceholderContent(): HTMLElement {
        const html = `<input class="literal" type="text" value="${this.getValue()}" readonly disabled data-test="hello"/>`;
        const div = document.createElement('div');
        div.innerHTML = html;
        return div;
    }
}

class LiteralIdentifierLookupInput extends LiteralInput {
    private readonly container: HTMLDivElement;
    private readonly suggestionsUl: HTMLUListElement;
    private readonly suggestions: HTMLLIElement[];

    constructor(input: HTMLInputElement) {
        super(input);
        this.container = input.parentElement as HTMLDivElement;
        this.suggestionsUl = this.container.querySelector('ul.identifier-suggestions') as HTMLUListElement;
        this.suggestions = Array.from(this.suggestionsUl.querySelectorAll('li'));

        this.updateSuggestions();
    }

    protected override onFocused(): void {
        this.suggestionsUl.style.display = 'block';
    }

    protected override onBlurred(): void {
        this.suggestionsUl.style.display = 'none';
    }

    protected override onInput() {
        super.onInput();
        this.updateSuggestions();
    }

    private updateSuggestions(): void {
        const inputValue = this.getValue();
        this.suggestions.forEach((suggestion: HTMLLIElement) => {
            const suggestionText = suggestion.textContent;
            if (!suggestionText || suggestionText.includes(inputValue)) {
                suggestion.style.display = 'block';
            } else {
                suggestion.style.display = 'none';
            }
        });
    }

    protected getPlaceholderContent(): HTMLElement {
        const html = `<input class="literal" type="text" value="${this.getValue()}" readonly disabled/>`;
        const div = document.createElement('div');
        div.innerHTML = html;
        return div;
    }
}

export function setupLiteralInputs(): void {
    Array.from(document.querySelectorAll('input.literal[data-tree-path]:not([disabled])')).forEach((input: Element) => {
        if (!(input instanceof HTMLInputElement)) throw new Error('Expected input to be an HTMLInputElement');
        if (input.classList.contains('identifier-lookup')) {
            new LiteralIdentifierLookupInput(input);
        } else {
            new LiteralTextInput(input);
        }
    });
}
