import {handleTabPressed, setNextFocusElement} from "../interface";
import {handleLiteralChanged} from "../actions";
import {BaseDropdownSelector} from "./baseDropdownSelector";
import {AbstractTreeInput} from "./abstractTreeInput";
import {getTree, getTreePathOfElement} from "../globals/elements";

export class LiteralInput implements AbstractTreeInput {
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
        this.initialValue = this.getValue();

        this.setupEventListeners();
        this.onInit();
    }

    focus(): void {
        this.input.focus();
        this.input.select();
        this.onFocused();
    }

    blur(): void {
        this.input.blur();
        this.onBlurred();
    }

    disable() {
        this.input.disabled = true;
        this.input.readOnly = true;
    }

    enable() {
        this.input.disabled = false;
        this.input.readOnly = false
    }

    getTreePath(): string {
        return this.treePath;
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
        this.input.addEventListener('keydown', event => this.handleKeydown(event));
    }

    protected onInit(): void {

    }

    protected onInput(): void {
        this.updateInputWidth();
        this.updateLinkedInputPlaceholders();
    }

    protected handleKeydown(evt: KeyboardEvent): void {
        if (evt.key === 'Tab') {
            handleTabPressed(evt);
        } else if (evt.key === 'Enter') {
            setNextFocusElement(this);
            evt.preventDefault();
            handleLiteralChanged(this.input);
        }
    }

    protected onFocused(): void {

    }

    protected onBlurred(): void {
        this.handleInputChanged(true);
    }

    private handleInputChanged(doNotFocus: boolean = false): void {
        if (this.getValue() === this.initialValue) return;
        handleLiteralChanged(this.input, doNotFocus);
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
        if (this.getValue() === value) return;
        this.input.value = value;
        this.onInput();
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

    protected getPlaceholderContent(): HTMLElement {
        const html = `<input class="literal" type="text" value="${this.getValue()}" readonly disabled/>`;
        const div = document.createElement('div');
        div.innerHTML = html;
        return div;
    }
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

class LiteralIntInput extends LiteralInput {
    protected override onInit() {
        this.input.type = 'text';
    }

    protected override onInput(): void {
        super.onInput();
        const original = this.getValue();
        const onlyDigits = original.replace(/[^0-9]/g, '');
        if (original.startsWith('-')) {
            this.setValue('-' + onlyDigits);
        } else {
            this.setValue(onlyDigits);
        }
    }
}

class LiteralIdentifierLookupInput extends LiteralInput {
    constructor(input: HTMLInputElement) {
        super(input);
        const container = input.parentElement as HTMLDivElement;

        new BaseDropdownSelector(container, 'input', 'div.dropdown', 'li');
    }
}

class LiteralBoolInput extends LiteralInput {
    public override getValue(): string {
        return this.input.checked ? 'true' : 'false';
    }
}

export function createLiteralInputs(): LiteralInput[] {
    return Array.from(document.querySelectorAll('input.literal[data-tree-path]:not([disabled])')).map((input: Element) => {
        if (!(input instanceof HTMLInputElement)) throw new Error('Expected input to be an HTMLInputElement');
        return createLiteralInput(input);
    });
}

export function createLiteralInput(input: HTMLInputElement): LiteralInput {
    if (input.classList.contains('identifier-lookup')) {
        return new LiteralIdentifierLookupInput(input);
    } else if (input.classList.contains('integer')) {
        return new LiteralIntInput(input);
    } else if (input.type === 'checkbox') {
        return new LiteralBoolInput(input);
    } else {
        return new LiteralInput(input);
    }
}
